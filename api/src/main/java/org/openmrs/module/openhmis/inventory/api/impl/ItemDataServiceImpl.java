/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.openhmis.inventory.api.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Iterators;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.openmrs.*;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhmis.commons.api.PagingInfo;
import org.openmrs.module.openhmis.commons.api.entity.impl.BaseMetadataDataServiceImpl;
import org.openmrs.module.openhmis.commons.api.entity.security.IMetadataAuthorizationPrivileges;
import org.openmrs.module.openhmis.commons.api.f.Action1;
import org.openmrs.module.openhmis.inventory.api.*;
import org.openmrs.module.openhmis.inventory.api.model.*;
import org.openmrs.module.openhmis.inventory.api.search.ItemSearch;
import org.openmrs.module.openhmis.inventory.api.util.HibernateCriteriaConstants;
import org.openmrs.module.openhmis.inventory.api.util.PrivilegeConstants;
import org.openmrs.util.RoleConstants;
import org.springframework.transaction.annotation.Transactional;

import org.openmrs.module.openhmis.commons.api.util.PrivilegeUtil;
import org.openmrs.notification.Alert;
import org.openmrs.api.UserService;
import org.apache.log4j.Logger;
import org.openmrs.Location;

@Transactional
public class ItemDataServiceImpl extends BaseMetadataDataServiceImpl<Item>
		implements IItemDataService, IMetadataAuthorizationPrivileges {

    private static final String LOCATIONPROPERTY = "defaultLocation";
	private static final Logger logger = Logger.getLogger(ItemDataServiceImpl.class);

	@Override
	protected void validate(Item entity) throws APIException {
		return;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Item> getAll(final boolean includeRetired, PagingInfo pagingInfo) {
		IMetadataAuthorizationPrivileges privileges = getPrivileges();
		if (privileges != null && !StringUtils.isEmpty(privileges.getGetPrivilege())) {
			PrivilegeUtil.hasPrivileges(Context.getAuthenticatedUser(), privileges.getGetPrivilege());
		}
		
		return executeCriteria(getEntityClass(), pagingInfo, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
	            updateLocationUserCriteria(criteria);
				if (!includeRetired) {
					criteria.add(Restrictions.eq("retired", false));
				}
			}
		}, getDefaultSort());
	}
	
	@Override
	protected Collection<? extends OpenmrsObject> getRelatedObjects(Item entity) {
		ArrayList<OpenmrsObject> results = new ArrayList<OpenmrsObject>();

		results.addAll(entity.getCodes());
		results.addAll(entity.getPrices());
		results.addAll(entity.getAttributes());

		return results;
	}

    // Method for determining user location
    public void updateLocationUserCriteria(Criteria criteria) {
    	logger.warn("UPDATING LOCATION RESTRICTION");
        User user = Context.getAuthenticatedUser();
        if (user.hasRole(RoleConstants.SUPERUSER))
        {	
        	logger.warn("BYPASSING FOR SUPERUSER");
            return;
        }
        
        Location location = null;
        try {
            location = Context.getLocationService().getLocation(Integer.parseInt(user.getUserProperty(LOCATIONPROPERTY)));
        } catch (Exception e) {
        	logger.warn("COULD NOT RESTRICT BY LOCATION");
        }

        if (location == null) {
            // impossible criterion so that no results will be returned
        	logger.warn("APPLYING IMPOSSIBLE LOCATION RESTRICTION...");
            criteria.add(Restrictions.isNull("creator"));
            return;
        }
        logger.warn("APPLYING LOCATION RESTRICTION " + location.getName() + "...");
        criteria.add(Restrictions.eq(HibernateCriteriaConstants.LOCATION, location));
        logger.warn("SUCCESS!");
        //if (!Context.getAuthenticatedUser().hasRole(RoleConstants.SUPERUSER)) {	criteria.add(Restrictions.eq("location", Context.getLocationService().getLocation(Integer.parseInt(user.getUserProperty("defaultLocation")))));	}
    }

    @Override
    public List<Item> listItemsByDrugId(Integer drugId) throws APIException {
        final Drug drug = Context.getConceptService().getDrug(drugId);

        return executeCriteria(Item.class, new Action1<Criteria>() {
            @Override
            public void apply(Criteria criteria) {
                updateLocationUserCriteria(criteria);
                criteria.add(Restrictions.eq(HibernateCriteriaConstants.DRUG, drug));
            }
        });
    }

    @Override
    public List<Item> listItemsByConceptId(Integer conceptId) throws APIException {
        final Concept concept = Context.getConceptService().getConcept(conceptId);

        return executeCriteria(Item.class, new Action1<Criteria>() {
            @Override
            public void apply(Criteria criteria) {
                updateLocationUserCriteria(criteria);
                criteria.add(Restrictions.eq(HibernateCriteriaConstants.CONCEPT, concept));
            }
        });
    }

    @Override
    public List<Item> listAllItems() throws APIException {
        return executeCriteria(Item.class, new Action1<Criteria>() {
            @Override
            public void apply(Criteria criteria) {
                updateLocationUserCriteria(criteria);
            }
        });
    }

    @Override
    public List<Item> getItemById(Integer itemId) throws APIException {
        final Integer idToLookup = itemId;
        return executeCriteria(Item.class, new Action1<Criteria>() {
            @Override
            public void apply(Criteria criteria) {
                updateLocationUserCriteria(criteria);
                criteria.add(Restrictions.eq(HibernateCriteriaConstants.ID, idToLookup));
            }
        });
    }

    @Override
    public Boolean dispenseItem(Integer itemId, Integer quantity) throws IllegalArgumentException, APIException {
        IStockroomDataService stockroomService = Context.getService(IStockroomDataService.class);
        IStockOperationDataService operationService = Context.getService(IStockOperationDataService.class);
        IStockOperationService stockOpService = Context.getService(IStockOperationService.class);
        IItemStockDataService itemStockDataService = Context.getService(IItemStockDataService.class);

        // Get a stockroom: for now we only have one stockroom per location
        //so it's OK to access directly with index 0
        //1. STOCKROOMS START @ ID 1
        //2. With several stockrooms, one per location, they are still stored in one database, hence only the first stockroom will be selected
        String location = Context.getAuthenticatedUser().getUserProperty(LOCATIONPROPERTY);
        System.out.println("location = " + location);
        Location loc = Context.getLocationService().getDefaultLocation();
        if(location.isEmpty() == false && StringUtils.isNumeric(location)) {
            loc = Context.getLocationService().getLocation(Integer.parseInt(location));
        }

        List<Stockroom> stockrooms = stockroomService.getStockroomsByLocation(loc, false);
        Stockroom stockroom = stockrooms.get(0);
        List<Item> itemList = this.getItemById(itemId);

        if(itemList.isEmpty() == true || itemList.get(0) == null) {
            return false;
        }

        Item item = itemList.get(0);
        // Create a new empty operation
        StockOperation operation = new StockOperation();
        operation.setInstanceType(WellKnownOperationTypes.getDistribution());
        operation.setSource(stockroom);
        operation.setOperationNumber("op by pharmacy");
        operation.setCreator(Context.getAuthenticatedUser());
        operation.setDateCreated(new Date());
        operation.setOperationDate(new Date());
        operation.setStatus(StockOperationStatus.COMPLETED);

        // Create the transactions
        StockOperationTransaction tx = new StockOperationTransaction();
        tx.setItem(item);
        tx.setStockroom(stockroom);
        tx.setQuantity(-1 * quantity);
        tx.setOperation(operation);

        operation.addTransaction(tx);
        operationService.save(operation);
        stockOpService.applyTransactions(tx);
        try
        {
	        ItemStock itemStock = itemStockDataService.getItemStockByItem(item, null).get(0);
	        if (itemStock.getQuantity() < 40)
	        {
		        //Create an Alert
				Alert alert = new Alert();
				alert.setText(itemStock.getItem().getName() + " stock is below 40!");
		        UserService us = Context.getUserService();
		        List<User> users = us.getUsersByRole(new Role("Inventory Manager"));
				for (User user : users)
					alert.addRecipient(user);
				Context.getAlertService().saveAlert(alert);
	        }
        }
        catch (Exception e)
        {}
        Context.flushSession();
        return true;
    }


    @Override
	@Authorized( { PrivilegeConstants.VIEW_ITEMS } )
	@Transactional(readOnly = true)
	public Item getItemByCode(String itemCode) throws APIException {
		if (StringUtils.isEmpty(itemCode)) {
			throw new IllegalArgumentException("The item code must be defined.");
		}
		if (itemCode.length() > 255) {
			throw new IllegalArgumentException(
					"The item code must be less than 256 characters.");
		}

		Criteria criteria = getRepository().createCriteria(getEntityClass());
        updateLocationUserCriteria(criteria);
		criteria.createAlias("codes", "c").add(
				Restrictions.ilike("c.code", itemCode));

		return getRepository().selectSingle(getEntityClass(), criteria);
	}

	@Override
	@Transactional(readOnly = true)
	@Authorized({ PrivilegeConstants.VIEW_ITEMS })
	public List<Item> getItemsByCode(String itemCode, boolean includeRetired)
			throws APIException {
		return getItemsByCode(itemCode, includeRetired, null);
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_ITEMS })
	@Transactional(readOnly = true)
	public List<Item> getItemsByCode(final String itemCode,
			final boolean includeRetired, PagingInfo pagingInfo)
			throws APIException {
		if (StringUtils.isEmpty(itemCode)) {
			throw new NullPointerException("The item code must be defined");
		}

		return executeCriteria(Item.class, pagingInfo, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
                updateLocationUserCriteria(criteria);
				criteria.createAlias("codes", "c").add(
						Restrictions.eq("c.code", itemCode));
				if (!includeRetired) {
					criteria.add(Restrictions.eq(HibernateCriteriaConstants.RETIRED, false));
				}
			}
		}, getDefaultSort());
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_ITEMS })
	@Transactional(readOnly = true)
	public List<Item> getItemsByDepartment(Department department,
			boolean includeRetired) throws APIException {
		return getItemsByDepartment(department, includeRetired, null);
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_ITEMS })
	@Transactional(readOnly = true)
	public List<Item> getItemsByDepartment(final Department department,
			final boolean includeRetired, PagingInfo pagingInfo)
			throws APIException {
		if (department == null) {
			throw new NullPointerException("The department must be defined");
		}

		return executeCriteria(Item.class, pagingInfo, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
                updateLocationUserCriteria(criteria);
				criteria.add(Restrictions.eq(HibernateCriteriaConstants.DEPARTMENT, department));
				if (!includeRetired) {
					criteria.add(Restrictions.eq(HibernateCriteriaConstants.RETIRED, false));
				}
			}
		}, getDefaultSort());
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_ITEMS })
	@Transactional(readOnly = true)
	public List<Item> getItemsByCategory(Category category,
			boolean includeRetired) throws APIException {
		return getItemsByCategory(category, includeRetired, null);
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_ITEMS })
	@Transactional(readOnly = true)
	public List<Item> getItemsByCategory(final Category category,
			final boolean includeRetired, PagingInfo pagingInfo)
			throws APIException {
		if (category == null) {
			throw new NullPointerException("The category must be defined");
		}

		return executeCriteria(Item.class, pagingInfo, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
                updateLocationUserCriteria(criteria);
				criteria.add(Restrictions.eq(HibernateCriteriaConstants.CATEGORY, category));
				if (!includeRetired) {
					criteria.add(Restrictions.eq(HibernateCriteriaConstants.RETIRED, false));
				}
			}
		}, getDefaultSort());
	}

	@Override
	public List<Item> getItems(Department department,
			Category category, boolean includeRetired) throws APIException {
		return getItems(department, category,
				includeRetired, null);
	}

	@Override
	public List<Item> getItems(
			final Department department, final Category category,
			final boolean includeRetired, PagingInfo pagingInfo)
			throws APIException {
		if (department == null) {
			throw new NullPointerException("The department must be defined");
		}
		if (category == null) {
			throw new NullPointerException("The category must be defined");
		}

		return executeCriteria(Item.class, pagingInfo, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
                updateLocationUserCriteria(criteria);
				criteria.add(Restrictions.eq(HibernateCriteriaConstants.DEPARTMENT, department));
				criteria.add(Restrictions.eq(HibernateCriteriaConstants.CATEGORY, category));
				if (!includeRetired) {
					criteria.add(Restrictions.eq(HibernateCriteriaConstants.RETIRED, false));
				}
			}
		}, getDefaultSort());
	}

	@Override
	public List<Item> getItems(Category category, String name,
			boolean includeRetired) throws APIException {
		return getItems(category, name, includeRetired, null);
	}

	@Override
	public List<Item> getItems(final Category category, final String name,
			final boolean includeRetired, PagingInfo pagingInfo)
			throws APIException {
		if (category == null) {
			throw new NullPointerException("The category must be defined");
		}
		if (StringUtils.isEmpty(name)) {
			throw new IllegalArgumentException("The item code must be defined.");
		}
		if (name.length() > 255) {
			throw new IllegalArgumentException(
					"The item code must be less than 256 characters.");
		}

		return executeCriteria(Item.class, pagingInfo, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
                updateLocationUserCriteria(criteria);
				criteria.add(Restrictions.eq(HibernateCriteriaConstants.CATEGORY, category)).add(
						Restrictions.ilike(HibernateCriteriaConstants.NAME, name, MatchMode.START));

				if (!includeRetired) {
					criteria.add(Restrictions.eq(HibernateCriteriaConstants.RETIRED, false));
				}
			}
		}, getDefaultSort());
	}

	@Override
	public List<Item> getItemsByDepartmentAndCategoryAndName(Department department, Category category,
			String name, boolean includeRetired) throws APIException {
		return getItems(department, category, name, includeRetired, null);
	}

	@Override
	public List<Item> getItems(final Department department,
			final Category category, final String name,
			final boolean includeRetired, PagingInfo pagingInfo)
			throws APIException {
		if (department == null) {
			throw new NullPointerException("The department must be defined");
		}
		if (category == null) {
			throw new NullPointerException("The category must be defined");
		}
		if (StringUtils.isEmpty(name)) {
			throw new IllegalArgumentException("The item code must be defined.");
		}
		if (name.length() > 255) {
			throw new IllegalArgumentException(
					"The item code must be less than 256 characters.");
		}

		return executeCriteria(Item.class, pagingInfo, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
                updateLocationUserCriteria(criteria);
				criteria.add(Restrictions.eq(HibernateCriteriaConstants.DEPARTMENT, department))
						.add(Restrictions.eq(HibernateCriteriaConstants.CATEGORY, category))
						.add(Restrictions.ilike(HibernateCriteriaConstants.NAME, name, MatchMode.START));

				if (!includeRetired) {
					criteria.add(Restrictions.eq(HibernateCriteriaConstants.RETIRED, false));
				}
			}
		}, getDefaultSort());
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_ITEMS })
	@Transactional(readOnly = true)
	public List<Item> getItems(Department department, String name,
			boolean includeRetired) throws APIException {
		return getItems(department, name, includeRetired, null);
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_ITEMS })
	@Transactional(readOnly = true)
	public List<Item> getItems(final Department department, final String name,
			final boolean includeRetired, PagingInfo pagingInfo)
			throws APIException {
		if (department == null) {
			throw new NullPointerException("The department must be defined");
		}
		if (StringUtils.isEmpty(name)) {
			throw new IllegalArgumentException("The item code must be defined.");
		}
		if (name.length() > 255) {
			throw new IllegalArgumentException(
					"The item code must be less than 256 characters.");
		}

		return executeCriteria(Item.class, pagingInfo, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
                updateLocationUserCriteria(criteria);
				criteria.add(Restrictions.eq(HibernateCriteriaConstants.DEPARTMENT, department)).add(
						Restrictions.ilike(HibernateCriteriaConstants.NAME, name, MatchMode.START));

				if (!includeRetired) {
					criteria.add(Restrictions.eq(HibernateCriteriaConstants.RETIRED, false));
				}
			}
		}, getDefaultSort());
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_ITEMS })
	public List<Item> getItemsByItemSearch(ItemSearch itemSearch) {
		return getItemsByItemSearch(itemSearch, null);
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_ITEMS })
	public List<Item> getItemsByItemSearch(final ItemSearch itemSearch, PagingInfo pagingInfo) {
		if (itemSearch == null) {
			throw new NullPointerException("The item search must be defined.");
		} else if (itemSearch.getTemplate() == null) {
			throw new NullPointerException(
					"The item search template must be defined.");
		}

		return executeCriteria(Item.class, pagingInfo, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
                updateLocationUserCriteria(criteria);
				itemSearch.updateCriteria(criteria);
			}
		}, getDefaultSort());
	}

	@Override
	public List<Item> getItemsByConcept(final Concept concept) {
		return executeCriteria(Item.class, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
                updateLocationUserCriteria(criteria);
				criteria.add(Restrictions.eq(HibernateCriteriaConstants.CONCEPT, concept));
			}
		});
	}

	@Override
	public List<Item> getItemsWithoutConcept(final List<Integer> excludedItemsIds, final Integer resultLimit) {
		return executeCriteria(Item.class, null, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
                updateLocationUserCriteria(criteria);
				criteria.add(Restrictions.isNull(HibernateCriteriaConstants.CONCEPT)).add(
						Restrictions.eq(HibernateCriteriaConstants.RETIRED, false)).add(
						Restrictions.eq(HibernateCriteriaConstants.CONCEPT_ACCEPTED, false));
				if (excludedItemsIds != null && excludedItemsIds.size() > 0) {
					criteria.add(Restrictions.not(Restrictions.in(HibernateCriteriaConstants.ID, excludedItemsIds.toArray())));
				}
				if (resultLimit != null && resultLimit > 0) {
					criteria.setMaxResults(resultLimit);
				}
			}
		}, getDefaultSort());
	}

	@Override
	protected IMetadataAuthorizationPrivileges getPrivileges() {
		return this;
	}

	@Override
	public String getSavePrivilege() {
		return PrivilegeConstants.MANAGE_ITEMS;
	}

	@Override
	public String getPurgePrivilege() {
		return PrivilegeConstants.PURGE_ITEMS;
	}

	@Override
	public String getGetPrivilege() {
		return PrivilegeConstants.VIEW_ITEMS;
	}

	@Override
	public String getRetirePrivilege() {
		return PrivilegeConstants.MANAGE_ITEMS;
	}

}
