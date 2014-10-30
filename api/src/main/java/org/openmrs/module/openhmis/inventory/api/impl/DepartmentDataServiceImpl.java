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

import org.openmrs.api.APIException;
import org.openmrs.module.openhmis.commons.api.entity.impl.BaseMetadataDataServiceImpl;
import org.openmrs.module.openhmis.commons.api.entity.security.IMetadataAuthorizationPrivileges;
import org.openmrs.module.openhmis.inventory.api.IDepartmentDataService;
import org.openmrs.module.openhmis.inventory.api.model.Department;
import org.openmrs.module.openhmis.inventory.api.security.BasicMetadataAuthorizationPrivileges;
import org.springframework.transaction.annotation.Transactional;

import org.openmrs.Location;
import org.openmrs.User;
import org.openmrs.util.RoleConstants;
import org.hibernate.Criteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.api.context.Context;

import java.util.LinkedList;
import java.util.List;

@Transactional
public class DepartmentDataServiceImpl
		extends BaseMetadataDataServiceImpl<Department>
		implements IDepartmentDataService {

    private static final String LOCATIONPROPERTY = "defaultLocation";

    // Method for determining user location
    public void updateLocationUserCriteria(Criteria criteria) {

        User user = Context.getAuthenticatedUser();
        Location location = null;

        if (user.hasRole(RoleConstants.SUPERUSER))
            return;

        try {
            location = Context.getLocationService().getLocation(Integer.parseInt(user.getUserProperty(LOCATIONPROPERTY)));
        } catch (Exception e) {}

        if (location == null) {
            // impossible criterion so that no results will be returned
            criteria.add(Restrictions.isNull("creator"));
            return;
        }

        criteria.add(Restrictions.eq("location", location));
    }

    @Override
    public List<Department> getAll(boolean b, org.openmrs.module.openhmis.commons.api.PagingInfo pagingInfo) {
        User user = Context.getAuthenticatedUser();
        Location location = null;

        if (user.hasRole(RoleConstants.SUPERUSER))
            return super.getAll(b, pagingInfo);

        try {
            location = Context.getLocationService().getLocation(Integer.parseInt(user.getUserProperty(LOCATIONPROPERTY)));
        } catch (Exception e) {}

        if (location == null) {
            return new LinkedList<Department>();
        }

        return super.getAll(b, pagingInfo);
    }

    @Override
    public List<Department> getByNameFragment(java.lang.String s, boolean b,
                                               org.openmrs.module.openhmis.commons.api.PagingInfo pagingInfo) {
        User user = Context.getAuthenticatedUser();
        Location location = null;

        if (user.hasRole(RoleConstants.SUPERUSER))
            return super.getByNameFragment(s, b, pagingInfo);

        try {
            location = Context.getLocationService().getLocation(Integer.parseInt(user.getUserProperty(LOCATIONPROPERTY)));
        } catch (Exception e) {}

        if (location == null) {
            return new LinkedList<Department>();
        }

        return super.getByNameFragment(s, b, pagingInfo);
    }

	@Override
	protected IMetadataAuthorizationPrivileges getPrivileges() {
		return new BasicMetadataAuthorizationPrivileges();
	}

	@Override
	protected void validate(Department entity) throws APIException {
		return;
	}
}
