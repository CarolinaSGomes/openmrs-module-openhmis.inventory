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
		openhmis.url.backboneBase + 'js/model/location',
	],
	function(openhmis, __) {
		openhmis.Department = openhmis.GenericModel.extend({
			meta: {
				name: __("Department"),
				namePlural: __("Departments"),
				openmrsType: 'metadata',
				restUrl: openhmis.url.inventoryModelBase + 'department'
			},

			schema: {
				name: 'Text',
				description: 'Text',
				location: {
                    type: 'LocationSelect',
                	options: new openhmis.GenericCollection(null, {
                	model: openhmis.Location,
                	url: 'v1/location'
                	}),
                	objRef: true
                }
			},

			validate: function(attrs, options) {
				if (!attrs.name) return { name: __("A name is required.") };
				return null;
			},

			parse: function(resp) {
                if (resp) {
            	    if (resp.location && _.isObject(resp.location)) {
            		    resp.location = new openhmis.Location(resp.location);
            		}
            	}
            	return resp;
            },

			toString: function() {
				return this.get('name');
			}
		});

		return openhmis;
	}
);
