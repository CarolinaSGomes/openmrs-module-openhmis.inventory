/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and
 * limitations under the License.
 *
 * Copyright (C) OpenHMIS.  All Rights Reserved.
 */
package org.openmrs.module.openhmis.inventory.api.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.OpenmrsObject;
import org.openmrs.User;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhmis.commons.api.PagingInfo;
import org.openmrs.module.openhmis.commons.api.entity.impl.BaseCustomizableMetadataDataServiceImpl;
import org.openmrs.module.openhmis.commons.api.entity.security.IMetadataAuthorizationPrivileges;
import org.openmrs.module.openhmis.commons.api.f.Action1;
import org.openmrs.module.openhmis.inventory.ModuleSettings;
import org.openmrs.module.openhmis.inventory.api.IItemDataService;
import org.openmrs.module.openhmis.inventory.api.model.Department;
import org.openmrs.module.openhmis.inventory.api.model.Item;
import org.openmrs.module.openhmis.inventory.api.model.ItemPrice;
import org.openmrs.module.openhmis.inventory.api.search.ItemSearch;
import org.openmrs.module.openhmis.inventory.api.util.HibernateCriteriaConstants;
import org.openmrs.module.openhmis.inventory.api.util.PrivilegeConstants;
import org.openmrs.util.RoleConstants;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data service implementation class for {@link Item}s.
 */
@Transactional
public class ItemDataServiceImpl extends BaseCustomizableMetadataDataServiceImpl<Item>
        implements IItemDataService, IMetadataAuthorizationPrivileges {

	private static final int MAX_ITEM_CODE_LENGTH = 255;

	@Override
	protected void validate(Item entity) {
		return;
	}

	@Override
	protected Collection<? extends OpenmrsObject> getRelatedObjects(Item entity) {
		ArrayList<OpenmrsObject> results = new ArrayList<OpenmrsObject>();

		results.addAll(entity.getCodes());
		results.addAll(entity.getPrices());
		results.addAll(entity.getAttributes());

		return results;
	}

	private void locationRestrictionCheck(Criteria criteria) {
		//Checking if Location Restriction is enabled
		if (ModuleSettings.areItemsRestrictedByLocation()) {
			//Checking if user is Superuser
			User user = Context.getAuthenticatedUser();
			if (user.hasRole(RoleConstants.SUPERUSER))
				return;
			//Retrieving defaultLocation from user property
			Location location;
			try {
				location = Context.getLocationService().getLocation(
				    Integer.parseInt(user.getUserProperty("defaultLocation"))
				        );
			} catch (Exception e) {
				return;
			}

			if (location == null) {
				// impossible criterion so that no results will be returned
				criteria.add(Restrictions.isNull("creator"));
				return;
			}

			criteria.add(Restrictions.eq(HibernateCriteriaConstants.LOCATION, location));
		}
	}

	@Override
	@Transactional(readOnly = true)
	@Authorized({ PrivilegeConstants.VIEW_ITEMS })
	public Item getItemByCode(String itemCode) {
		if (StringUtils.isEmpty(itemCode)) {
			throw new IllegalArgumentException("The item code must be defined.");
		}
		if (itemCode.length() > MAX_ITEM_CODE_LENGTH) {
			throw new IllegalArgumentException("The item code must be less than 256 characters.");
		}

		Criteria criteria = getRepository().createCriteria(getEntityClass());
		criteria.createAlias("codes", "c").add(Restrictions.ilike("c.code", itemCode));
		locationRestrictionCheck(criteria);

		return getRepository().selectSingle(getEntityClass(), criteria);
	}

	@Override
	@Transactional(readOnly = true)
	@Authorized({ PrivilegeConstants.VIEW_ITEMS })
	public List<Item> getItemsByCode(String itemCode, boolean includeRetired) {
		return getItemsByCode(itemCode, includeRetired, null);
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_ITEMS })
	@Transactional(readOnly = true)
	public List<Item> getItemsByCode(final String itemCode, final boolean includeRetired, PagingInfo pagingInfo) {
		if (StringUtils.isEmpty(itemCode)) {
			throw new NullPointerException("The item code must be defined");
		}

		return executeCriteria(Item.class, pagingInfo, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
				criteria.createAlias("codes", "c").add(Restrictions.eq("c.code", itemCode));
				locationRestrictionCheck(criteria);
				if (!includeRetired) {
					criteria.add(Restrictions.eq(HibernateCriteriaConstants.RETIRED, false));
				}
			}
		}, getDefaultSort());
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_ITEMS })
	@Transactional(readOnly = true)
	public List<Item> getItemsByDepartment(Department department, boolean includeRetired) {
		return getItemsByDepartment(department, includeRetired, null);
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_ITEMS })
	@Transactional(readOnly = true)
	public List<Item> getItemsByDepartment(final Department department, final boolean includeRetired,
	        PagingInfo pagingInfo) {
		if (department == null) {
			throw new NullPointerException("The department must be defined");
		}

		return executeCriteria(Item.class, pagingInfo, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
				criteria.add(Restrictions.eq(HibernateCriteriaConstants.DEPARTMENT, department));
				locationRestrictionCheck(criteria);
				if (!includeRetired) {
					criteria.add(Restrictions.eq(HibernateCriteriaConstants.RETIRED, false));
				}
			}
		}, getDefaultSort());
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_ITEMS })
	@Transactional(readOnly = true)
	public List<Item> getItems(Department department, String name, boolean includeRetired) {
		return getItems(department, name, includeRetired, null);
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_ITEMS })
	@Transactional(readOnly = true)
	public List<Item> getItems(final Department department, final String name, final boolean includeRetired,
	        PagingInfo pagingInfo) {
		if (department == null) {
			throw new NullPointerException("The department must be defined");
		}
		if (StringUtils.isEmpty(name)) {
			throw new IllegalArgumentException("The item code must be defined.");
		}
		if (name.length() > MAX_ITEM_CODE_LENGTH) {
			throw new IllegalArgumentException("The item code must be less than 256 characters.");
		}

		return executeCriteria(Item.class, pagingInfo, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
				criteria.add(Restrictions.eq(HibernateCriteriaConstants.DEPARTMENT, department)).add(
				    Restrictions.ilike(HibernateCriteriaConstants.NAME, name, MatchMode.START));

				locationRestrictionCheck(criteria);
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
			throw new NullPointerException("The item search template must be defined.");
		}

		return executeCriteria(Item.class, pagingInfo, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
				locationRestrictionCheck(criteria);
				itemSearch.updateCriteria(criteria);
			}
		}, getDefaultSort());
	}

	@Override
	public List<Item> getItemsByConcept(final Concept concept) {
		return executeCriteria(Item.class, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
				locationRestrictionCheck(criteria);
				criteria.add(Restrictions.eq(HibernateCriteriaConstants.CONCEPT, concept));
			}
		});
	}

	@Override
	public ItemPrice getItemPriceByUuid(final String uuid) {
		Criteria criteria = getRepository().createCriteria(ItemPrice.class);
		criteria.add(Restrictions.eq("uuid", uuid));

		return getRepository().selectSingle(ItemPrice.class, criteria);
	}

	@Override
	public List<Item> getItemsWithoutConcept(final List<Integer> excludedItemsIds, final Integer resultLimit) {
		return executeCriteria(Item.class, null, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
				criteria.add(Restrictions.isNull(HibernateCriteriaConstants.CONCEPT))
				        .add(Restrictions.eq(HibernateCriteriaConstants.RETIRED, false))
				        .add(Restrictions.eq(HibernateCriteriaConstants.CONCEPT_ACCEPTED, false));
				if (excludedItemsIds != null && excludedItemsIds.size() > 0) {
					criteria.add(Restrictions.not(Restrictions.in(HibernateCriteriaConstants.ID,
					    excludedItemsIds.toArray())));
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
