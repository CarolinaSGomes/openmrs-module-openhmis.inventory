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

import com.google.common.collect.Iterators;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.*;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.openmrs.Location;
import org.openmrs.OpenmrsObject;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhmis.commons.api.PagingInfo;
import org.openmrs.module.openhmis.commons.api.entity.impl.BaseCustomizableMetadataDataServiceImpl;
import org.openmrs.module.openhmis.commons.api.f.Action;
import org.openmrs.module.openhmis.commons.api.f.Action1;
import org.openmrs.module.openhmis.inventory.api.IItemStockDataService;
import org.openmrs.module.openhmis.inventory.api.IStockOperationDataService;
import org.openmrs.module.openhmis.inventory.api.model.*;
import org.openmrs.module.openhmis.inventory.api.search.StockOperationSearch;
import org.openmrs.module.openhmis.inventory.api.security.BasicMetadataAuthorizationPrivileges;
import org.openmrs.util.RoleConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.*;

public class StockOperationDataServiceImpl
		extends BaseCustomizableMetadataDataServiceImpl<StockOperation>
		implements IStockOperationDataService {
	@Override
	protected BasicMetadataAuthorizationPrivileges getPrivileges() {
		return new BasicMetadataAuthorizationPrivileges();
	}

	@Override
	protected void validate(StockOperation operation) throws IllegalArgumentException, APIException {
		StockOperationServiceImpl.validateOperation(operation);
	}

	@Override
	protected Order[] getDefaultSort() {
		// Return operations ordered by creation date, desc
		return new Order[] { Order.desc("dateCreated") };
	}

	private static final String LOCATIONPROPERTY = "defaultLocation";

	@Override
	public StockOperation getOperationByNumber(String number) {
		if (StringUtils.isEmpty(number)) {
			throw new IllegalArgumentException("The operation number to find must be defined.");
		}
		if (number.length() > 255) {
			throw new IllegalArgumentException("The operation number must be less than 256 characters.");
		}

		Criteria criteria = getRepository().createCriteria(getEntityClass());
		criteria.add(Restrictions.eq("operationNumber", number));

		return getRepository().selectSingle(getEntityClass(), criteria);
	}

	@Override
	public List<StockOperation> getOperationsByRoom(final Stockroom stockroom, PagingInfo paging) {
		if (stockroom == null) {
			throw new IllegalArgumentException("The stockroom must be defined.");
		}

		return executeCriteria(StockOperation.class, paging, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
				criteria.add(Restrictions.or(
						Restrictions.eq("source", stockroom),
						Restrictions.eq("destination", stockroom)
				));
			}
		}, getDefaultSort());
	}

	@Override
	public List<StockOperationItem> getItemsByOperation(final StockOperation operation, PagingInfo paging) throws IllegalArgumentException, APIException {
		if (operation == null) {
			throw new IllegalArgumentException("The operation must be defined.");
		}

		return executeCriteria(StockOperationItem.class, paging, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
				criteria.add(Restrictions.eq("operation", operation));
				criteria.createCriteria("item", "i");
			}
		}, Order.asc("i.name"));
	}

	@Override
	public List<StockOperation> getUserOperations(User user, PagingInfo paging) {
		return getUserOperations(user, null, paging);
	}

	@Override
	public List<StockOperation> getUserOperations(final User user, final StockOperationStatus status, PagingInfo paging) {
		if (user == null) {
			throw new IllegalArgumentException("The user must be defined.");
		}

		// Get all the roles for this user (this traverses the role relationships to get any parent roles)
		final Set<Role> roles = user.getAllRoles();

		List<StockOperation> stockOperations =  executeCriteria(StockOperation.class, paging, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
				DetachedCriteria subQuery = DetachedCriteria.forClass(IStockOperationType.class);
				subQuery.setProjection(Property.forName("id"));

				// Add user/role filter
				if (roles != null && roles.size() > 0) {
					subQuery.add(Restrictions.or(
							// Types that require user approval
							Restrictions.eq("user", user),
							// Types that require role approval
							Restrictions.in("role", roles)
					));
				} else {
					// Types that require user approval
					subQuery.add(Restrictions.eq("user", user));
				}

				if (status != null) {
					criteria.add(Restrictions.and(
							Restrictions.eq("status", status),
							Restrictions.or(
									// Transactions created by the user
									Restrictions.eq("creator", user),
									Property.forName("instanceType").in(subQuery)
							)
					));
				} else {
					criteria.add(Restrictions.or(
							// Transactions created by the user
							Restrictions.eq("creator", user),
							Property.forName("instanceType").in(subQuery)
					)
					);
				}
			}
		}, Order.desc("dateCreated")
		);

		//This code ensures that the resulting operations are all belonging to the appropriate stockrooms from the user defaultLocation
		List<StockOperation> result = new ArrayList<StockOperation>();
		Location location = Context.getLocationService().getLocation(Integer.parseInt(Context.getAuthenticatedUser().getUserProperty(LOCATIONPROPERTY)));
		for (StockOperation stockOperation : stockOperations) {
			if (ensuredStockOperationBelongsToLocation(stockOperation, location) == true){
				result.add(stockOperation);
			}
		}
		return result;
	}

	public boolean ensuredStockOperationBelongsToLocation (StockOperation stockOperation, Location locus){
		if (Context.getAuthenticatedUser().hasRole(RoleConstants.SUPERUSER)){
			return true;
		} else if (stockOperation.getSource() != null) {
			if (locus.getId() == (stockOperation.getSource().getLocation().getId())) {
				return true;
			}
		} else if(stockOperation.getDestination() != null) {
			if (locus.getId() == (stockOperation.getDestination().getLocation().getId())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<StockOperation> getOperations(StockOperationSearch search) {
		return getOperations(search, null);
	}

	@Override
	public List<StockOperation> getOperations(final StockOperationSearch search, PagingInfo paging) {
		if (search == null) {
			throw new IllegalArgumentException("The operation search must be defined.");
		} else if (search.getTemplate() == null) {
			throw new IllegalArgumentException("The operation search template must be defined.");
		}

		List<StockOperation> stockOperations = new ArrayList<StockOperation>();
		List<StockOperation> result = new ArrayList<StockOperation>();
		Location location = Context.getLocationService().getLocation(Integer.parseInt(Context.getAuthenticatedUser().getUserProperty(LOCATIONPROPERTY)));

		stockOperations = executeCriteria(StockOperation.class, paging, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
				search.updateCriteria(criteria);
			}
		}, getDefaultSort());

		//This code ensures that the resulting operations are all belonging to the appropriate stockrooms from the user defaultLocation
		for (StockOperation stockOperation : stockOperations) {
			if (ensuredStockOperationBelongsToLocation(stockOperation, location) == true){
				result.add(stockOperation);
			}
		}

		return result;
	}


	@Override
	public List<StockOperation> getAll(PagingInfo paging) {
		return getAll(false, paging);
	}

	@Override
	public List<StockOperation> getAll() {
		return getAll(false, null);
	}

	@Override
	public List<StockOperation> getAll(final boolean includeRetired) {
		return getAll(includeRetired, null);
	}

	@Override
	public List<StockOperation> getAll(final boolean includeRetired, PagingInfo paging) {


		List<StockOperation> stockOperations = executeCriteria(StockOperation.class, paging, new Action1<Criteria>() {
			public void apply(Criteria criteria) {
				if (!includeRetired) {
					criteria.add(Restrictions.eq("retired", false));
				}
			}
		}, getDefaultSort());

		//This code ensures that the resulting operations are all belonging to the appropriate stockrooms from the user defaultLocation
		List<StockOperation> result = new ArrayList<StockOperation>();
		Location location = Context.getLocationService().getLocation(Integer.parseInt(Context.getAuthenticatedUser().getUserProperty(LOCATIONPROPERTY)));
		for (StockOperation stockOperation : stockOperations) {
			if (ensuredStockOperationBelongsToLocation(stockOperation, location) == true){
				result.add(stockOperation);
			}
		}
		return result;
	}

	@Override
	public void purge(StockOperation operation) throws APIException {
		if (operation != null && (
				(operation.getReserved() != null && operation.getReserved().size() > 0) ||
				(operation.getTransactions() != null && operation.getTransactions().size() > 0))
			) {
			throw new APIException("Stock operations can not be deleted if there are any associated transactions.");
		}

		super.purge(operation);
	}
}

