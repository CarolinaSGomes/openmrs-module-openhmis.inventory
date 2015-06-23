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
	    openhmis.url.backboneBase + 'js/model/user',
		openhmis.url.backboneBase + 'js/model/role',
        openhmis.url.backboneBase + 'js/model/patient',
		openhmis.url.backboneBase + 'js/model/openhmis',
	    openhmis.url.inventoryBase + 'js/model/stockroom',
        openhmis.url.inventoryBase + 'js/model/institution'
    ],
    function(openhmis, __) {
        openhmis.OperationAttributeType = openhmis.AttributeTypeBase.extend({
			meta: {
				restUrl: openhmis.url.inventoryModelBase + 'stockOperationAttributeType'
			}
		});

        openhmis.OperationType = openhmis.CustomizableInstanceTypeBase.extend({
		    meta: {
				name: __("Operation Type"),
				namePlural: __("Operation Types"),
				restUrl: openhmis.url.inventoryModelBase + 'stockOperationType'
		    },

		    attributeTypeClass: openhmis.OperationAttributeType,

			schema: {
			    name: { type: 'Text' },
			    description: { type: 'TextArea' },
			    hasSource: {
				    type: 'TrueFalseCheckbox',
				    editorAttrs: { disabled: true }
			    },
			    hasDestination: {
				    type: 'TrueFalseCheckbox',
				    editorAttrs: { disabled: true }
			    },
			    hasRecipient: {
				    type: 'TrueFalseCheckbox',
				    editorAttrs: { disabled: true }
			    },
			    availableWhenReserved: {
				    type: 'TrueFalseCheckbox',
			        editorAttrs: { disabled: true }
			    },
			    user: {
				    type: 'UserSelect',
				    options: new openhmis.GenericCollection(null, {
					    model: openhmis.User,
					    url: 'v1/user'
				    }),
				    objRef: true
			    },
			    role: {
				    type: 'RoleSelect',
				    options: new openhmis.GenericCollection(null, {
					    model: openhmis.Role,
					    url: 'v1/role'
				    }),
				    objRef: true
			    }
		    },

			validate: function(attrs, options) {
			    if (!attrs.name) return { name: __("A name is required.") };
			    return null;
		    },

		    toString: function() {
			    if (this.get("name")) {
				    return this.get("name");
			    }

			    return openhmis.GenericModel.prototype.toString.call(this);
		    }
	    });

        openhmis.OperationAttribute = openhmis.InstanceAttributeBase.extend({
            attributeClass: openhmis.OperationAttributeType
        });

        openhmis.TransactionBase = openhmis.GenericModel.extend({
		    initialize: function(attributes, options) {
			    openhmis.GenericModel.prototype.initialize.call(this, attributes, options);

                this.schema.operation = { type: 'NestedModel', model: openhmis.Operation, objRef: true };
                this.schema.item = { type: 'NestedModel', model: openhmis.Item, objRef: true };
			    this.schema.quantity = { type: 'BasicNumber' };
			    this.schema.expiration = { type: 'Date', format: openhmis.dateFormatLocale };
			    this.schema.dateCreated = { type: 'Date', format: openhmis.dateTimeFormatLocale };
			    this.schema.batchOperation = { type: 'NestedModel', model: openhmis.Operation, objRef: true };
			    this.schema.calculatedExpiration = {type: 'checkbox'};
			    this.schema.calculatedBatch = {type: 'checkbox'};
		    },

		    parse: function(resp) {
			    if (resp) {
                    if (resp.operation && _.isObject(resp.operation)) {
                        resp.operation = new openhmis.Operation(resp.operation);
                    }

                    if (resp.item && _.isObject(resp.item)) {
                        resp.item = new openhmis.Item(resp.item);
                    }

                    if (resp.batchOperation && _.isObject(resp.batchOperation)) {
					    resp.batchOperation = new openhmis.Operation(resp.batchOperation);
				    }
			    }

			    return resp;
		    },

            toString: function() {
                var expiration = this.get("expiration");
                var exp = ": ";
                if (expiration) {
                    exp = " (" + openhmis.dateFormatLocale(expiration) + "): ";
                }

                return this.get("item").name + exp + this.get("quantity")
            }
	    });

	    openhmis.ReservedTransaction = openhmis.TransactionBase.extend({
		    meta: {
			    name: __("Reservation Transaction"),
			    namePlural: __("Reservation Transactions"),
			    openmrsType: 'metadata',
			    restUrl: openhmis.url.inventoryModelBase + 'reservationTransaction'
		    },

			schema: {
				available: { type: 'checkbox' }
			}
	    });

	    openhmis.OperationTransaction = openhmis.TransactionBase.extend({
		    meta: {
			    name: __("Operation Transaction"),
			    namePlural: __("Operation Transactions"),
			    openmrsType: 'metadata',
			    restUrl: openhmis.url.inventoryModelBase + 'stockOperationTransaction'
		    },

		    schema: {
			    stockroom: {
				    type: 'StockroomSelect',
				    options: new openhmis.GenericCollection(null, {
					    model: openhmis.Stockroom,
					    url: openhmis.url.inventoryModelBase + '/stockroom'
				    }),
				    objRef: true
			    },
			    patient: { type: 'Object', model: openhmis.Patient, objRef: true },
                institution: { type: 'Object', model: openhmis.Institution, objRef: true}
		    },

		    parse: function(resp) {
			    openhmis.TransactionBase.prototype.parse.call(this, resp);

			    if (resp) {
				    if (resp.stockroom && _.isObject(resp.stockroom)) {
					    resp.stockroom = new openhmis.Stockroom(resp.stockroom);
				    }

				    if (resp.patient && _.isObject(resp.patient)) {
					    resp.patient = new openhmis.Patient(resp.patient);
				    }

				    if (resp.institution && _.isObject(resp.institution)) {
					    resp.institution = new openhmis.Institution(resp.institution);
				    }
			    }

			    return resp;
		    }
	    });

        openhmis.NewOperation = openhmis.GenericModel.extend({
            meta: {
                name: __("Operation"),
                namePlural: __("Operations"),
                openmrsType: 'metadata',
                restUrl: openhmis.url.inventoryModelBase + 'stockOperation'
            },

            schema: {},

            OperationStatus: {
                NEW:        "NEW",
                PENDING:	"PENDING",
                CANCELLED:	"CANCELLED",
                COMPLETED:	"COMPLETED"
            },

            initialize: function(attrs, options) {
                openhmis.GenericModel.prototype.initialize.call(this, attrs, options);

                this.schema.operationNumber = { 
            			type: 'Text',
						title: 'Batch Number' 
					};
                this.schema.status = {
                    type: 'Text',
                    readonly: 'readonly',
                    hidden: true
                };
                this.schema.instanceType = {
                    type: 'OperationTypeSelect',
                        title: 'Operation Type',
                        options: new openhmis.GenericCollection(null, {
                            model: openhmis.OperationType,
                            url: openhmis.url.inventoryModelBase + 'stockOperationType',
                            queryString: "v=full"
                    }),
                    objRef: true
                };
                this.schema.items = {
                    type: 'List',
                    itemType: 'NestedModel',
                    model: openhmis.OperationItem,
                    hidden: true
                };
                this.schema.source = {
                    type: 'StockroomSelect',
                    options: new openhmis.GenericCollection(null, {
                        model: openhmis.Stockroom,
                        url: openhmis.url.inventoryModelBase + 'stockroom'
                    }),
                    objRef: true
                };
                this.schema.destination = {
                    type: 'StockroomSelect',
                    options: new openhmis.GenericCollection(null, {
                        model: openhmis.Stockroom,
                        url: openhmis.url.inventoryModelBase + 'stockroom'
                    }),
                    objRef: true
                };
                this.schema.institution = {
                    type: 'InstitutionSelect',
                    options: new openhmis.GenericCollection(null, {
                        model: openhmis.Institution,
                        url: openhmis.url.inventoryModelBase + 'institution'
                    }),
                    objRef: true
                };
                this.schema.attributes = {
                    hidden: true
                };

                if (!this.get("status")) {
                    this.set("status", this.OperationStatus.NEW);
                }
            },

            parse: function(resp) {
                if (resp) {
                    if (resp.instanceType && _.isObject(resp.instanceType)) {
                        resp.instanceType = new openhmis.OperationType(resp.instanceType);
                    }

                    if (resp.source) {
                        resp.source = new openhmis.Stockroom(resp.source);
                    }
                    if (resp.destination) {
                        resp.destination = new openhmis.Stockroom(resp.destination);
                    }
                    if (resp.institution) {
                        resp.institution = new openhmis.Institution(resp.institution);
                    }

                    if (resp.attributes) {
                        resp.attributes = new openhmis.GenericCollection(resp.attributes,
                            { model: openhmis.OperationAttribute }).models;
                    }
                }

                return resp;
            },

            validate: function(goAhead) {
                // By default, backbone validates every time we try try to alter the model.  We don't want to be bothered
                // with this until we care.
                if (goAhead !== true) {
                    return null;
                }

                var errors = [];
                var operationNumber = this.get("operationNumber");
                if (operationNumber === undefined || operationNumber === '') {
                    errors.push({
                        selector: ".field-operationNumber",
                        message: "An operation must have an operation number."
                    });
                }
                
                if (this.get("instanceType") === undefined) {
                    errors.push({
                        selector: ".field-instanceType",
                        message: "An operation must have an operation type."
                    });
                } else {
                    var operationType = this.get("instanceType");
                    if (operationType.get("hasSource") &&
                        (this.get("source") === undefined || this.get("source").id === "")) {
                        errors.push({
                            selector: ".field-source",
                            message: "The operation type " + operationType.get("name") + " requires a source stockroom"
                        });
                    }
                    if (operationType.get("hasDestination") &&
                        (this.get("destination") === undefined || this.get("destination").id === "")) {
                        errors.push({
                            selector: ".field-destination",
                            message: "The operation type " + operationType.get("name") + " requires a destination stockroom"
                        });
                    }
                   
                }

                // TODO: Should the operation type user/role check happen here?

                var items = this.get("items");
                if (items === undefined || items.length === 0) {
                    errors.push({
                        selector: ".item-stock",
                        message: "An operation must contain at least one item.",
                        selectParent: true
                    });
                } else  {
                     var operationType = this.get("instanceType");
                     var itemError = false;
                     items.each(function(item) {
                     	if (item.get("quantity") === 0 || (item.get("quantity") < 0 &&
                     	operationType.get("name") != "Adjustment")) {
                     		itemError = true;
                     	}
                     });
                     if (itemError) {
                     	errors.push({
                             selector: "th.field-quantity",
                             message: "The item quantity is not allowed for this operation"
                         });
                     }
                }

                if (errors.length === 0) {
                    return null;
                } else {
                    return errors;
                }

            },

            toString: function() {
                if (this.get("operationNumber")) {
                    return this.get("operationNumber");
                } else {
                    return "Operation";
                }
            }
        });

        openhmis.Operation = openhmis.NewOperation.extend({
            schema: {
                operationNumber: 'Text',
                dateCreated: {
		            type: 'Text',
                    editorAttrs: { disabled: true },
		            format: openhmis.dateTimeFormatLocale
	            }
            }
        });

        openhmis.OperationItem = openhmis.ItemStockDetailBase.extend({
            meta: {
                name: __("Operation Item"),
                namePlural: __("Operation Items"),
                openmrsType: 'metadata',
                restUrl: openhmis.url.inventoryModelBase + 'stockOperationItem'
            },

            schema: {
                operation: {
                    type: 'NestedModel',
                    model: openhmis.Operation,
                    objRef: true
                }
            },

            toString: function() {
                return this.get('item.name');
            }
        });

        return openhmis;
    }
);
