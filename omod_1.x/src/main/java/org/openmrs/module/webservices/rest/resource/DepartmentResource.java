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
package org.openmrs.module.webservices.rest.resource;

import org.openmrs.Location;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhmis.commons.api.PagingInfo;
import org.openmrs.module.openhmis.commons.api.entity.IMetadataDataService;
import org.openmrs.module.openhmis.inventory.api.IDepartmentDataService;
import org.openmrs.module.openhmis.inventory.api.IStockroomDataService;
import org.openmrs.module.openhmis.inventory.api.model.Department;
import org.openmrs.module.openhmis.inventory.api.model.Stockroom;
import org.openmrs.module.openhmis.inventory.web.ModuleRestConstants;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.util.OpenmrsConstants;

/**
 * REST resource representing a {@link Department}.
 */
@Resource(name = ModuleRestConstants.DEPARTMENT_RESOURCE, supportedClass = Department.class,
        supportedOpenmrsVersions = { "1.9.*", "1.10.*", "1.11.*", "1.12.*" })
@Handler(supports = { Department.class }, order = 0)
public class DepartmentResource extends BaseRestMetadataResource<Department> {

	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = super.getRepresentationDescription(rep);
		description.addProperty("description", Representation.REF);

		return description;
	}

	/*
	 * would like to restrict departments via location.
	 * will be hard given there is no association between department and location.
	 */

	@Override
	protected PageableResult doGetAll(RequestContext context) {
		String loc = Context.getAuthenticatedUser().getUserProperty(OpenmrsConstants.USER_PROPERTY_DEFAULT_LOCATION);
		Location ltemp = Context.getLocationService().getLocation(Integer.parseInt(loc));
		PagingInfo pagingInfo = PagingUtil.getPagingInfoFromContext(context);

		return new AlreadyPagedWithLength<Department>(context,
		        Context.getService(IDepartmentDataService.class).getDepartmentsByLocation(
		            ltemp, context.getIncludeAll(), pagingInfo),
		        pagingInfo.hasMoreResults(), pagingInfo.getTotalRecordCount());
	}

	@Override
	public Department newDelegate() {
		return new Department();
	}

	@Override
	public Class<? extends IMetadataDataService<Department>> getServiceClass() {

		return IDepartmentDataService.class;
	}

}
