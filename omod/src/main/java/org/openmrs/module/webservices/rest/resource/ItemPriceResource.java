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
package org.openmrs.module.webservices.rest.resource;

import org.openmrs.module.openhmis.commons.api.entity.IMetadataDataService;
import org.openmrs.module.openhmis.inventory.api.model.ItemPrice;
import org.openmrs.module.openhmis.inventory.web.ModuleRestConstants;
import org.openmrs.module.webservices.rest.helper.Converter;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.ConversionException;

@Resource(name= ModuleRestConstants.ITEM_PRICE_RESOURCE, supportedClass=ItemPrice.class, supportedOpenmrsVersions={"1.11"})
public class ItemPriceResource extends BaseRestMetadataResource<ItemPrice> implements IMetadataDataServiceResource<ItemPrice> {
	@PropertySetter(value = "price")
	public void setPrice(ItemPrice instance, Object price) throws ConversionException {
		instance.setPrice(Converter.objectToBigDecimal(price));
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = super.getRepresentationDescription(rep);
		description.addProperty("price");

		return description;
	}

	@Override
	public Class<IMetadataDataService<ItemPrice>> getServiceClass() {
		return null;
	}

	@Override
	public ItemPrice newDelegate() {
		return new ItemPrice();
	}

}
