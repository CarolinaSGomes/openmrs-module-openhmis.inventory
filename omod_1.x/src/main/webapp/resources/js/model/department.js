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
define(
	[
		openhmis.url.backboneBase + 'js/openhmis',
		openhmis.url.backboneBase + 'js/lib/i18n',
		openhmis.url.backboneBase + 'js/model/generic',
		openhmis.url.backboneBase + 'js/model/location'
	],
	function(openhmis, __) {
		openhmis.Department = openhmis.GenericModel.extend({
			meta: {
				name: __(openhmis.getMessage('openhmis.inventory.department.name')),
				namePlural: __(openhmis.getMessage('openhmis.inventory.department.namePlural')),
				openmrsType: 'metadata',
				restUrl: openhmis.url.inventoryModelBase + 'department'
			},

			schema: {
				name: 'Text',
				location: {
                	type: 'LocationSelectMod',
                	options: new openhmis.GenericCollection(null, {
                		 model: openhmis.LocationEdit,
                		 //location restriction
                		 url: ($('.locationRestriction').val() == 'true' ? 'v2/inventory/location' : 'v1/location')
                	}),
                	objRef: true
                },
				description: 'Text'
			},

			validate: function(attrs, options) {
				if (!attrs.name) return { name: __(openhmis.getMessage('openhmis.inventory.nameRequiredError')) };
				if (!attrs.location) return { location: __(openhmis.getMessage('openhmis.inventory.locationRequiredError')) };
				return null;
			},

			toString: function() {
				return this.get('name');
			}
		});

		return openhmis;
	}
);
