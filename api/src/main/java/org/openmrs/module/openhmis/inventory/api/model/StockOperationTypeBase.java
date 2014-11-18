package org.openmrs.module.openhmis.inventory.api.model;

import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhmis.commons.api.entity.model.BaseCustomizableInstanceType;
import org.openmrs.module.openhmis.commons.api.f.Action2;
import org.openmrs.module.openhmis.inventory.api.IStockOperationDataService;
import org.openmrs.module.openhmis.inventory.api.IStockOperationService;

import java.util.Set;
import java.util.TreeSet;

public abstract class StockOperationTypeBase
		extends BaseCustomizableInstanceType<StockOperationAttributeType>
		implements IStockOperationType {
	public static final long serialVersionUID = 0L;

	private Integer id;
	private Boolean hasSource;
	private Boolean hasDestination;
	private Boolean hasRecipient;
	private Boolean recipientRequired;
	private Boolean availableWhenReserved;
	private User user;
	private Role role;

	@Override
	public abstract void onPending(StockOperation operation);

	@Override
	public abstract void onCancelled(StockOperation operation);

	@Override
	public abstract void onCompleted(StockOperation operation);

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public Boolean getHasSource() {
		return hasSource;
	}

	@Override
	public void setHasSource(Boolean hasSource) {
		this.hasSource = hasSource;
	}

	@Override
	public Boolean getHasDestination() {
		return hasDestination;
	}

	@Override
	public void setHasDestination(Boolean hasDestination) {
		this.hasDestination = hasDestination;
	}

	@Override
	public Boolean getHasRecipient() {
		return hasRecipient;
	}

	@Override
	public void setHasRecipient(Boolean hasPatient) {
		this.hasRecipient = hasPatient;
	}

	@Override
	public Boolean getRecipientRequired() {
		return recipientRequired;
	}

	@Override
	public void setRecipientRequired(Boolean recipientRequired) {
		this.recipientRequired = recipientRequired;
	}

	@Override
	public Boolean getAvailableWhenReserved() {
		return availableWhenReserved;
	}

	@Override
	public void setAvailableWhenReserved(Boolean availableInTransit) {
		this.availableWhenReserved = availableInTransit;
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public void setUser(User user) {
		this.user = user;
	}

	@Override
	public Role getRole() {
		return role;
	}

	@Override
	public void setRole(Role role) {
		this.role = role;
	}

	protected Set<StockOperationTransaction> executeCopyReserved(
			StockOperation operation,
			Action2<ReservedTransaction, StockOperationTransaction> action) {
		Set<StockOperationTransaction> transactions = new TreeSet<StockOperationTransaction>();

		// Loop through the reserved transactions
		for (ReservedTransaction inTransit : operation.getReserved()) {
			// Create a new operation transaction as a copy of the reserved transaction
			StockOperationTransaction tx = new StockOperationTransaction(inTransit);

			// Apply the action
			action.apply(inTransit, tx);

			// Add the operation transaction to the operation
			operation.addTransaction(tx);

			// Add the operation transaction to the copied transaction list
			transactions.add(tx);
		}

		IStockOperationService service = Context.getService(IStockOperationService.class);
		service.applyTransactions(transactions);

		return transactions;
	}

	protected Set<StockOperationTransaction> executeCopyReservedAndClear(
			StockOperation operation,
			Action2<ReservedTransaction, StockOperationTransaction> action) {
		Set<StockOperationTransaction> result = executeCopyReserved(operation, action);

		// Clear out the transactions for the operation
		operation.getReserved().clear();

		return result;
	}
}

