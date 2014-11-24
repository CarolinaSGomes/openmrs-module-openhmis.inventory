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
package org.openmrs.module.openhmis.inventory.api.search;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.openhmis.commons.api.entity.search.BaseMetadataTemplateSearch;
import org.openmrs.module.openhmis.inventory.api.model.Item;

import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.util.RoleConstants;
import org.openmrs.User;
import org.apache.log4j.Logger;
import org.openmrs.Location;

public class ItemSearch extends BaseMetadataTemplateSearch<Item> {
	public static final long serialVersionUID = 0L;

    private static final String LOCATIONPROPERTY = "defaultLocation";
	private static final Logger logger = Logger.getLogger(ItemSearch.class);

    // Method for determining user location
    public void updateLocationUserCriteria(Criteria criteria) {
    	logger.warn("UPDATING LOCATION RESTRICTION");
        User user = Context.getAuthenticatedUser();
        if (user.hasRole(RoleConstants.SUPERUSER))
            return;
        
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
        criteria.add(Restrictions.eq("location", location));
        logger.warn("SUCCESS!");
    }
	
	public ItemSearch() {
		this(new Item(), StringComparisonType.EQUAL, false);
	}

	public ItemSearch(Item itemTemplate) {
		this(itemTemplate, StringComparisonType.EQUAL, false);
	}

	public ItemSearch(Item itemTemplate, Boolean includeRetired) {
		this(itemTemplate, StringComparisonType.EQUAL, includeRetired);
	}

	public ItemSearch(Item itemTemplate, StringComparisonType nameComparisonType, Boolean includeRetired) {
		super(itemTemplate, nameComparisonType, includeRetired);
	}

	private ComparisonType conceptComparisonType;

	public ComparisonType getConceptComparisonType() {
		return conceptComparisonType;
	}

	public void setConceptComparisonType(ComparisonType conceptComparisonType) {
		this.conceptComparisonType = conceptComparisonType;
	}

	@Override
	public void updateCriteria(Criteria criteria) {
		super.updateCriteria(criteria);

		Item item = getTemplate();
		if (item.getDepartment() != null) {
			criteria.add(Restrictions.eq("department", item.getDepartment()));
		}
		if (item.getCategory() != null) {
			criteria.add(Restrictions.eq("category", item.getCategory()));
		}
		if (item.getConcept() != null ||
				(conceptComparisonType != null && conceptComparisonType != ComparisonType.EQUAL)) {
			criteria.add(createCriterion("concept", item.getConcept(), conceptComparisonType));
		}
		if (item.getHasExpiration() != null) {
			criteria.add(Restrictions.eq("hasExpiration", item.getHasExpiration()));
		}
		if (item.getHasPhysicalInventory() != null) {
			criteria.add(Restrictions.eq("hasPhysicalInventory", item.getHasPhysicalInventory()));
		}
		if (item.getConceptAccepted() != null) {
			criteria.add(Restrictions.eq("conceptAccepted", item.getConceptAccepted()));
		}
		//Finally ensure that the location restriction is applied
        updateLocationUserCriteria(criteria);
	}
}

