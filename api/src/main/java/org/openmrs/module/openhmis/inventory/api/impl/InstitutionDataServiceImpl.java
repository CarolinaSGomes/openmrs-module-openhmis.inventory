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

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Location;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhmis.commons.api.entity.impl.BaseMetadataDataServiceImpl;
import org.openmrs.module.openhmis.commons.api.entity.security.IMetadataAuthorizationPrivileges;
import org.openmrs.module.openhmis.inventory.api.IInstitutionDataService;
import org.openmrs.module.openhmis.inventory.api.model.Institution;
import org.openmrs.module.openhmis.inventory.api.security.BasicMetadataAuthorizationPrivileges;
import org.openmrs.util.RoleConstants;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

@Transactional
public class InstitutionDataServiceImpl
		extends BaseMetadataDataServiceImpl<Institution>
		implements IInstitutionDataService {

    private static final String LOCATIONPROPERTY = "defaultLocation";

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
    public List<Institution> getAll(boolean b, org.openmrs.module.openhmis.commons.api.PagingInfo pagingInfo) {
        User user = Context.getAuthenticatedUser();
        Location location = null;

        if (user.hasRole(RoleConstants.SUPERUSER))
            return super.getAll(b, pagingInfo);

        try {
            location = Context.getLocationService().getLocation(Integer.parseInt(user.getUserProperty(LOCATIONPROPERTY)));
        } catch (Exception e) {}

        if (location == null) {
            return new LinkedList<Institution>();
        }

        return super.getAll(b, pagingInfo);
    }

    @Override
    public List<Institution> getByNameFragment(java.lang.String s, boolean b,
                                               org.openmrs.module.openhmis.commons.api.PagingInfo pagingInfo) {
        User user = Context.getAuthenticatedUser();
        Location location = null;

        if (user.hasRole(RoleConstants.SUPERUSER))
            return super.getByNameFragment(s, b, pagingInfo);

        try {
            location = Context.getLocationService().getLocation(Integer.parseInt(user.getUserProperty(LOCATIONPROPERTY)));
        } catch (Exception e) {}

        if (location == null) {
            return new LinkedList<Institution>();
        }

        return super.getByNameFragment(s, b, pagingInfo);
    }


    @Override
	protected IMetadataAuthorizationPrivileges getPrivileges() {
		return new BasicMetadataAuthorizationPrivileges();
	}

	@Override
	protected void validate(Institution entity) throws APIException {
		return;
	}
}

