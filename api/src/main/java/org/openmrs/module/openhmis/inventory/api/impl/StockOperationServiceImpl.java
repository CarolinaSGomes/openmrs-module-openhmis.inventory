package org.openmrs.module.openhmis.inventory.api.impl;

import com.google.common.collect.Iterators;
import org.apache.commons.lang.ObjectUtils;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.openhmis.inventory.api.IItemStockDataService;
import org.openmrs.module.openhmis.inventory.api.IStockOperationDataService;
import org.openmrs.module.openhmis.inventory.api.IStockOperationService;
import org.openmrs.module.openhmis.inventory.api.IStockroomDataService;
import org.openmrs.module.openhmis.inventory.api.model.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import org.apache.log4j.Logger;

public class StockOperationServiceImpl
		extends BaseOpenmrsService
		implements IStockOperationService {
	// This is the object that will provide synchronization
	private static final UUID OPERATION_LOCK = UUID.randomUUID();

	private IStockroomDataService stockroomService;
	private IItemStockDataService itemStockService;
	protected IStockOperationDataService operationService;

    private static final Logger logger = Logger.getLogger(StockOperationServiceImpl.class);

	@Autowired
	public StockOperationServiceImpl(IStockOperationDataService operationService,
									 IStockroomDataService stockroomService,
									 IItemStockDataService itemStockService) {
		this.operationService = operationService;
		this.stockroomService = stockroomService;
		this.itemStockService = itemStockService;
	}

	/**
	 * Validates the stock operation.
	 * @param operation The stock operation to validate.
	 * @throws org.openmrs.api.APIException
	 * @should throw an APIException if the type requires a source and the source is null
	 * @should throw an APIException if the type requires a destination and the destination is null
	 * @should throw an APIException if the type requires a patient and the patient is null
	 */
	public static void validateOperation(StockOperation operation) {
		if (operation == null) {
			throw new IllegalArgumentException("The operation to submit must be defined.");
		}
		if (operation.getInstanceType() == null) {
			throw new APIException("The operation instance type must be defined.");
		}
		if (operation.getStatus() == null) {
			throw new APIException("The operation status must be defined.");
		}

		IStockOperationType type = operation.getInstanceType();
		if (type.getHasSource() && operation.getSource() == null) {
			throw new APIException("The operation type (" + type.getName() + ") requires a source stockroom " +
					"but one has not been defined.");
		}
		if (type.getHasDestination() && operation.getDestination() == null) {
			throw new APIException("The operation type (" + type.getName() + ") requires a destination " +
					"stockroom but one has not been defined.");
		}
		if (type.getRecipientRequired() && (operation.getPatient() == null && operation.getInstitution() == null)) {
			throw new APIException("The operation type (" + type.getName() + ") requires a patient or institution " +
					"but one has not been associated.");
		}
	}

	@Override
	public StockOperation submitOperation(StockOperation operation) throws IllegalArgumentException, APIException {
		/*
			Submitting the operation will copy the items to the operation reservations (if not already done) and then
			process those reservations based on the operation state.
		 */
		validateOperation(operation);

		if (operation.getItems() == null || operation.getItems().size() <= 0) {
			throw new APIException("The operation must have at least one operation item defined.");
		}

		// Only allow access to a single caller at a time so that the reservation calculation does not gets messed up
		synchronized (OPERATION_LOCK) {
			if (operation.getStatus() == StockOperationStatus.NEW) {
				for (StockOperationItem item : operation.getItems()) {
					ReservedTransaction tx = new ReservedTransaction(item);
					tx.setCreator(Context.getAuthenticatedUser());
					tx.setDateCreated(new Date());

					operation.addReserved(tx);
				}

				// Perform any required calculations to make the reservations valid
				calculateReservations(operation);

				operation.setStatus(StockOperationStatus.PENDING);
			}

			// Triggers the appropriate status-based event so that the operation type can do what needs doing
			//  Note: applyTransactions will be called as part of the event, if needed
			switch (operation.getStatus()) {
				case PENDING:
					operation.getInstanceType().onPending(operation);
					break;
				case CANCELLED:
					operation.getInstanceType().onCancelled(operation);
					break;
				case COMPLETED:
					operation.getInstanceType().onCompleted(operation);
					break;
			}

			// Save the operation and all sub-objects
			return operationService.save(operation);
		}
	}

	@Override
	public void applyTransactions(Collection<StockOperationTransaction> transactions) {
		if (transactions != null && transactions.size() > 0) {
			StockOperationTransaction[] tx = new StockOperationTransaction[transactions.size() - 1];

			applyTransactions(transactions.toArray(tx));
		}
	}

	@Override
	public void applyTransactions(StockOperationTransaction... transactions) {
		if (transactions == null || transactions.length == 0) {
			// Nothing to do
			return;
		} else if (transactions.length == 1 && transactions[0] == null) {
			// A single null parameter was passed in.  Nothing to do.
			return;
		}

		// Lock on the operation lock in case this method is called directly. If called via submitOperation this lock
		//  will already be acquired and simply reenter.
		synchronized (OPERATION_LOCK) {
			// Note that we don't touch the stockroom operations, transactions, or item stock because that could result
			//  in loading a large number of records from the database that we don't need for this. This means that
			//  any existing stockroom objects must be refreshed before the data updated below will be seen.

			// Create a map to store the tx grouped by item and stockroom
			Map<Pair<Item, Stockroom>, List<StockOperationTransaction>> grouped = createGroupedTransactions(transactions);

			for (Pair<Item, Stockroom> key : grouped.keySet()) {
				Item item = key.getValue0();
				Stockroom stockroom = key.getValue1();
				List<StockOperationTransaction> itemTxs = grouped.get(key);

				// Get the item stock from the stockroom
				ItemStock stock = stockroomService.getItem(stockroom, item);

				// For each item transaction
				int totalQty = 0;
				for (StockOperationTransaction tx : itemTxs) {
					// Sum the total quantity for the item
					totalQty += tx.getQuantity();

					ItemStockDetail detail = null;
					if (stock == null) {
						// Item stock does not exist so create it and then create detail
						stock = new ItemStock();
						stock.setStockroom(tx.getStockroom());
						stock.setItem(tx.getItem());
						stock.setQuantity(0);

						detail = new ItemStockDetail(stock, tx);
						stock.addDetail(detail);
					} else {
						// The stock already exists so try and find the detail
						detail = findDetail(stock, tx);
						if (detail == null) {
							// Could not find the detail so create a new one
							detail = new ItemStockDetail(stock, tx);
							stock.addDetail(detail);
						} else {
							// Found the detail, just update the quantity
							detail.setQuantity(detail.getQuantity() + tx.getQuantity());
						}
					}

					// If the detail quantity is zero or less then remove the record.
					if(detail.getQuantity() <= 0) {
						stock.getDetails().remove(detail);
					}
				}

				// Update the item quantity
				stock.setQuantity(stock.getQuantity() + totalQty);

				if (stock.getQuantity() <= 0) {
					// Remove the stock if the quantity is zero or negative
					itemStockService.purge(stock);
				} else {
					// Save the stock if the quantity is positive
					itemStockService.save(stock);
				}
			}
		}
	}

	/**
	 * THIS SHOULD NOT BE CALLED FROM USER CODE - Code to the interface ({@link org.openmrs.module.openhmis.inventory.api.IStockroomDataService}) not this class.
	 *
	 * Calculates the reservation details for the specified {@link org.openmrs.module.openhmis.inventory.api.model.StockOperation}. This includes calculating any
	 * qualifiers and checking on the details of the source stockroom to create all required transactions to fulfill the
	 * request.
	 * @param operation The stock operation for this transaction
	 * @should use closest expiration from the source stockroom
	 * @should use oldest batch operation with the calculated expiration
	 * @should set the expiration to null if no valid item stock can be found
	 * @should set the batch to null if no valid item stock can be found
	 * @should use date and time for expiration calculation
	 * @should create additional transactions when when multiple details are need to fulfill request
	 * @should create additional null qualifier transaction when there is not enough valid item stock to fulfill request
	 * @should copy source calculation settings into source calculation fields
	 * @should set the batch operation to the specified operation if there is no source stockroom
	 * @should combine transactions for the same item stock and qualifiers
	 * @should handle multiple transactions for the same item but with different qualifiers
	 * @should set the transaction source calculated flags if the source was calculated
	 * @should process non-calculated transactions before calculated transactions
	 * @should set batch operation to past operations before future operations
	 * @should throw APIException if source stockroom is null and the expiration are not specified for an expirable item
	 * @should throw APIException if calculate expiration is false and expiration is null for an expirable item
	 * @should throw IllegalArgumentException if operation is null
	 */
	public void calculateReservations(StockOperation operation) {
		if (operation == null) {
			throw new IllegalArgumentException("The operation must be defined");
		}

		/*
			We want to ensure that duplicated transactions are combined so they don't cause issues when they are processed.
			To do this, we loop through each transaction and build a tuple containing the Item, Expiration Date, and Batch Operation
			and look for it in a map.  If it is not found, we add it and continue to the next transaction. If it is found
			we update the existing transaction to add the quantity and set the calculated batch operation and expiration.
			The rule for the calculated qualifiers is that if either of the transactions (existing or current) was set to
			be calculated then the field is set to true.
		 */
		List<ReservedTransaction> removeList = findDuplicateReservedTransactions(operation);
		for (ReservedTransaction tx : removeList) {
			operation.getReserved().remove(tx);
		}

		// Sort the transactions by item and then non-calculated versus calculated
		List<ReservedTransaction> transactions = sortReservedTransactions(operation);

		/*
			Now we need to check each transaction against the source stockroom item stock (if there is a source stockroom)
			and figure out exactly which specific item stock (called an item stock detail) to take.  This can result in
			new transactions being created if a transaction cannot be fulfilled by a single detail. The calculation
			also needs to take into account others transactions for the same item. To manage this, a Map is created to
			store copies of the item stock detail and then these copies are then updated so that a running tally can be kept
			of what is actually available when processing a specific transaction without modifying the actual detail records.
		 */
		Map<Pair<Stockroom, Item>, ItemStock> stockMap = new HashMap<Pair<Stockroom, Item>, ItemStock>();
		List<ReservedTransaction> newTransactions = new ArrayList<ReservedTransaction>();
		boolean hasSource = operation.getSource() != null;

		for (ReservedTransaction tx : transactions) {
			if (!hasSource) {
				if (tx.getItem().hasExpiration() && tx.getExpiration() == null) {
					throw new APIException("Stock operations with no source stockroom must define an expiration for any expirable items.");
				}

				// Set the batch operation to the current operation because this must be some type of receipt operation
				if (tx.getBatchOperation() == null) {
					tx.setBatchOperation(operation);
					tx.setCalculatedBatch(false);
				}
			} else {
				if (tx.getItem().hasExpiration() && tx.getExpiration() == null && !tx.isCalculatedExpiration()) {
					throw new APIException("The item '" + tx.getItem().getName() + "' requires an expiration date but " +
							"one was not defined or set to be calculated.");
				}

				// Clone the item stock and find the detail record
				ItemStock stock = findAndCloneStock(stockMap, operation.getSource(), tx.getItem());
				findAndUpdateCalculatedDetail(newTransactions, operation, stock, tx);
			}
		}

		// Add any newly created transactions to the operation
		for (ReservedTransaction newTx : newTransactions) {
			operation.addReserved(newTx);
		}
	}

	private void findAndUpdateCalculatedDetail(List<ReservedTransaction> newTransactions, StockOperation operation, ItemStock stock, ReservedTransaction tx) {
		ItemStockDetail detail = findCalculatedDetail(operation, stock, tx);

		if (detail == null) {
            if (tx.getQuantity() > 0) {
                throw new APIException("Resource stockroom does not have sufficient stock.");
            }
			tx.setSourceCalculatedExpiration(true);
			tx.setSourceCalculatedBatch(true);
			tx.setExpiration(null);
			tx.setBatchOperation(null);
		} else {
			// Subtract the tx quantity from the detail and ensure that it has enough to fulfill the request
			detail.setQuantity(detail.getQuantity() - tx.getQuantity());

			// Set the tx fields that derive from the source detail
			tx.setSourceCalculatedExpiration(detail.isCalculatedExpiration());
			tx.setSourceCalculatedBatch(detail.isCalculatedBatch());
			tx.setExpiration(detail.getExpiration());
			tx.setBatchOperation(detail.getBatchOperation());

			if (detail.getQuantity() == 0) {
				stock.getDetails().remove(detail);
			} else if (detail.getQuantity() < 0) {
				stock.getDetails().remove(detail);

				// Set the tx quantity to the number actually deduced from the detail
				tx.setQuantity(tx.getQuantity() + detail.getQuantity());

				// Create a new tx to handle the remaining stock request
				ReservedTransaction newTx = new ReservedTransaction(tx);
				newTx.setQuantity(Math.abs(detail.getQuantity()));

				// Add the new tx to the list of transactions to add to the operations
				newTransactions.add(newTx);

				// Find the details to fulfill this new tx
				findAndUpdateCalculatedDetail(newTransactions, operation, stock, newTx);
			}
		}
	}

	private List<ReservedTransaction> findDuplicateReservedTransactions(StockOperation operation) {
		Map<Triplet<Item, Date, StockOperation>, ReservedTransaction> map = new HashMap<Triplet<Item, Date, StockOperation>, ReservedTransaction>();
		List<ReservedTransaction> removeList = new ArrayList<ReservedTransaction>();

		for (ReservedTransaction tx : operation.getReserved()) {
			Triplet<Item, Date, StockOperation> key = Triplet.with(tx.getItem(), tx.getExpiration(), tx.getBatchOperation());
			if (!map.containsKey(key)) {
				map.put(key, tx);
			} else {
				// Update the existing tx with this tx
				ReservedTransaction existingTx = map.get(key);
				existingTx.setQuantity(existingTx.getQuantity() + tx.getQuantity());
				existingTx.setCalculatedBatch(existingTx.isCalculatedBatch() || tx.isCalculatedBatch());
				existingTx.setCalculatedExpiration(existingTx.isCalculatedExpiration() || tx.isCalculatedExpiration());

				removeList.add(tx);
			}
		}

		return removeList;
	}

	private List<ReservedTransaction> sortReservedTransactions(StockOperation operation) {
		List<ReservedTransaction> transactions = new ArrayList<ReservedTransaction>(operation.getReserved());

		Collections.sort(transactions, new Comparator<ReservedTransaction>() {
			@Override
			public int compare(ReservedTransaction tx1, ReservedTransaction tx2) {
				int result = 0;

				result = tx1.getItem().getId().compareTo(tx2.getItem().getId());
				if (result == 0) {
					result = Boolean.valueOf(tx1.isCalculatedExpiration()).compareTo(tx2.isCalculatedExpiration());

					if (result == 0) {
						result = Boolean.valueOf(tx1.isCalculatedBatch()).compareTo(tx2.isCalculatedBatch());

						if (result == 0) {
							result = tx1.getId().compareTo(tx2.getId());
						}
					}
				}

				return result;
			}
		});

		return transactions;
	}

	private ItemStockDetail findCalculatedDetail(StockOperation operation, ItemStock stock, ReservedTransaction tx) {
		if (stock == null) {
			return null;
		}

		ItemStockDetail detail = null;

		if (stock.getItem().hasExpiration() && tx.isCalculatedExpiration()) {
			List<ItemStockDetail> results = null;
			if (tx.isCalculatedExpiration()) {
				results = findClosestExpiration(stock, new DateTime(operation.getOperationDate()));
			} else {
				results =  findDetailByExpiration(stock, tx.getExpiration());
			}

			if (results.size() == 1) {
				detail = results.get(0);
			} else if (results.size() > 1) {
				detail = findOldestBatch(operation, results);
			}
		} else if (tx.isCalculatedBatch()) {
			detail = findOldestBatch(operation, stock);
		} else {
			detail = findDetail(stock, tx);
		}

		return detail;
	}

	private ItemStockDetail findDetail(ItemStock stock, TransactionBase tx) {
		if (stock == null || stock.getDetails() == null || stock.getDetails().size() == 0) {
			return null;
		}

		// Loop through each detail record and find the first detail with the same expiration and batch operation
		for (ItemStockDetail detail : stock.getDetails()) {
			if (ObjectUtils.equals(detail.getExpiration(), tx.getExpiration()) &&
				ObjectUtils.equals(detail.getBatchOperation(), tx.getBatchOperation())) {
				return detail;
			}
		}

		return null;
	}

	private List<ItemStockDetail> findDetailByExpiration(ItemStock stock, Date date) {
		if (stock == null || stock.getDetails() == null || stock.getDetails().size() == 0) {
			return null;
		}

		if (date == null) {
			throw new IllegalArgumentException("The expiration date to find must be defined.");
		}

		List<ItemStockDetail> results = new ArrayList<ItemStockDetail>();

		// Loop through each detail record and find the first detail with the same expiration and batch operation
		for (ItemStockDetail detail : stock.getDetails()) {
			if (detail.getExpiration() != null && detail.getExpiration().equals(date)) {
				results.add(detail);
			}
		}

		return results;
	}

	private ItemStock findAndCloneStock(Map<Pair<Stockroom, Item>, ItemStock> workingMap, Stockroom stockroom, Item item) {
		Pair<Stockroom, Item> pair = Pair.with(stockroom, item);

		ItemStock stock = workingMap.get(pair);
		if(stock == null) {
			stock = stockroomService.getItem(stockroom, item);
			if (stock != null) {
				stock = new ItemStock(stock);

				workingMap.put(pair, stock);
			}
		}

		return stock;
	}

	private List<ItemStockDetail> findClosestExpiration(ItemStock stock, DateTime date) {
		List<ItemStockDetail> results = new ArrayList<ItemStockDetail>();
		int closest = 0;

		for (ItemStockDetail detail : stock.getDetails()) {
			if (results.size() == 0) {
				results.add(detail);
				closest = Seconds.secondsBetween(date, new DateTime(detail.getExpiration())).getSeconds();
			} else {
				int temp = Seconds.secondsBetween(date, new DateTime(detail.getExpiration())).getSeconds();
				if (temp == closest) {
					results.add(detail);
				} else if (temp < closest) {
					results.clear();
					results.add(detail);

					closest = temp;
				}
			}
		}

		return results;
	}

	private ItemStockDetail findOldestBatch(StockOperation operation, ItemStock stock) {
		return findOldestBatch(operation, stock.getDetails());
	}

	private ItemStockDetail findOldestBatch(StockOperation operation, Collection<ItemStockDetail> details) {
		final DateTime operationTime = new DateTime(operation.getOperationDate());
        Collection<ItemStockDetail> fullDetails = new ArrayList<ItemStockDetail>();

        for (ItemStockDetail detail : details){
            if (detail.getBatchOperation() != null)
                fullDetails.add(detail);
        }

		return Collections.min(fullDetails, new Comparator<ItemStockDetail>() {
            @Override
            public int compare(ItemStockDetail o1, ItemStockDetail o2) {
                DateTime o1Time = new DateTime();
                DateTime o2Time = new DateTime();

                if (o1.getBatchOperation() != null) {
                    if (o1.getBatchOperation().getOperationDate() != null) {
                        o1Time = new DateTime(o1.getBatchOperation().getOperationDate());
                    } else
                        logger.warn("o1 has no operation date for batch operation " + o1.getBatchOperation().getUuid());
                } else
                    logger.warn("o1 item has no batch operation " + o1.getUuid());

                if (o2.getBatchOperation().getOperationDate() != null) {
                    o2Time = new DateTime(o2.getBatchOperation().getOperationDate());
                } else
                    logger.warn("o2 item time has no operation date for batch operation " + o1.getBatchOperation().getUuid());


                return ((Integer) Seconds.secondsBetween(operationTime, o1Time).getSeconds()).compareTo(
                        Seconds.secondsBetween(operationTime, o2Time).getSeconds() + 1);
            }
        });
	}

	private Map<Pair<Item, Stockroom>, List<StockOperationTransaction>> createGroupedTransactions(StockOperationTransaction[] transactions) {
		Map<Pair<Item, Stockroom>, List<StockOperationTransaction>> grouped = new HashMap<Pair<Item, Stockroom>, List<StockOperationTransaction>>();
		for (StockOperationTransaction tx : transactions) {
			if (tx == null) {
				continue;
			}

			Pair<Item, Stockroom> key = Pair.with(tx.getItem(), tx.getStockroom());
			if (!grouped.containsKey(key)) {
				grouped.put(key, new ArrayList<StockOperationTransaction>());
			}

			grouped.get(key).add(tx);
		}
		return grouped;
	}

	private void createNegativeStockDetail(ItemStock stock) {
		ItemStockDetail detail = null;
		if (stock.getDetails().size() > 1) {
			// Other detail records exist that should not be around anymore.  Clear them and create a single
			//  detail record for the unknown stock that has been removed from the stockroom.
			stock.getDetails().clear();

			detail = new ItemStockDetail();
			stock.addDetail(detail);
		} else {
			// Use this single record as the unqualified detail record
			detail = Iterators.get(stock.getDetails().iterator(), 0);
		}

		detail.setItemStock(stock);
		detail.setStockroom(stock.getStockroom());
		detail.setItem(stock.getItem());
		detail.setExpiration(null);
		detail.setBatchOperation(null);
		detail.setCalculatedExpiration(true);
		detail.setCalculatedBatch(true);
		detail.setQuantity(stock.getQuantity());
	}
}

