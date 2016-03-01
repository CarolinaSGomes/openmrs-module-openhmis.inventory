(function() {
	'use strict';

	var app = angular.module('app.operationsTypeFunctionsFactory', []);
	app.service('OperationsTypeFunctions', OperationsTypeFunctions);

	OperationsTypeFunctions.$inject = [];

	function OperationsTypeFunctions() {
		var service;
		service = {
			addMessageLabels : addMessageLabels,
			addAttributeType : addAttributeType,
			insertOperationTypesTemporaryId : insertOperationTypesTemporaryId,
			removeOperationTypesTemporaryId : removeOperationTypesTemporaryId,
			removeAttributeType : removeAttributeType,
			removeFromList : removeFromList,
			editAttributeType : editAttributeType,
			addExtraFormatListElements : addExtraFormatListElements
		};

		return service;

		/**
		 * Displays a popup dialog box with the attribute types . Saves the code on clicking the 'Ok' button
		 * @param $scope
		 */
		function addAttributeType($scope) {
			$scope.editAttributeTypeTitle = '';
			$scope.addAttributeTypeTitle = $scope.messageLabels['openhmis.inventory.general.add']
					+ ' '
					+ $scope.messageLabels['openhmis.inventory.attribute.type.name'];
			$scope.saveButton = $scope.messageLabels['general.save'];
			var dialog = emr
					.setupConfirmationDialog({
						selector : '#attribute-types-dialog',
						actions : {
							confirm : function() {
								$scope.entity.attributeTypes = $scope.entity.attributeTypes
										|| [];
								$scope.submitted = true;
								if (angular.isDefined($scope.attributeType)
										&& $scope.attributeType.name !== "") {
									$scope.entity.attributeTypes
											.push($scope.attributeType);
									insertOperationTypesTemporaryId(
											$scope.entity.attributeTypes,
											$scope.attributeType);
									$scope.attributeType = {};
									console.log($scope.attributeTypes);
								}
								$scope.$apply();
								dialog.close();
							},
							cancel : function() {
								dialog.close();
							}
						}
					});

			dialog.show();
		}

		/**
		 * Opens a popup dialog box to edit an attribute Type
		 * @param attributeType
		 * @param ngDialog
		 * @param $scope
		 */
		function editAttributeType(attributeType, $scope) {
			$scope.attributeType = attributeType;
			$scope.editAttributeTypeTitle = $scope.messageLabels['openhmis.inventory.general.edit']
					+ ' '
					+ $scope.messageLabels['openhmis.inventory.attribute.type.name'];
			$scope.editButton = $scope.messageLabels['general.update'];
			$scope.addAttributeTypeTitle = '';
			var dialog = emr.setupConfirmationDialog({
				selector : '#attribute-types-dialog',
				actions : {
					confirm : function() {
						$scope.attributeType = {};
						dialog.close();
					},
					cancel : function() {
						$scope.attributeType = {};
						dialog.close();
					}
				}
			});

			dialog.show();
		}

		/**
		 * ng-repeat requires that every item have a unique identifier.
		 * This function sets a temporary unique id for all attribute types in the list.
		 * @param operationTypes (attributeTypes)
		 * @param operationType - optional
		 */
		function insertOperationTypesTemporaryId(attributeTypes, attributeType) {
			if (angular.isDefined(attributeType)) {
				var index = attributeTypes.indexOf(attributeType);
				attributeType.id = index;
			} else {
				for ( var attributeType in attributeTypes) {
					var index = attributeTypes.indexOf(attributeType);
					attributeType.id = index;
				}
			}
		}

		/**
		 * Removes an attribute Type from the list
		 * @param attribute Type
		 * @param attribute Types
		 */
		function removeAttributeType(attributeType, attributeTypes) {
			removeFromList(attributeType, attributeTypes);
		}

		/**
		 * Searches an attribute type and removes it from the list
		 * @param attribute type
		 * @param attribute Types
		 */
		function removeFromList(attributeType, attributeTypes) {
			var index = attributeTypes.indexOf(attributeType);
			attributeTypes.splice(index, 1);
		}

		/**
		 * Remove the temporary unique id from all operation types (attributetypes) before submitting.
		 * @param items
		 */
		function removeOperationTypesTemporaryId(attributeTypes) {
			for ( var index in attributeTypes) {
				var attributeType = attributeTypes[index];
				delete attributeType.id;
			}
		}

		function addExtraFormatListElements(formatFields) {
			for ( var format in formatFields) {
				switch (formatFields[format]) {
					// As per PersonAttributeTypeFormController.java, remove inapplicable formats
					case "java.util.Date" :
					case "org.openmrs.Patient.exitReason" :
					case "org.openmrs.DrugOrder.discontinuedReason" :
						formatFields[format] = undefined;
						break;
				}
			}
			do {
				var undefinedId = _.indexOf(formatFields, undefined);
				if (undefinedId !== -1)
					formatFields.splice(undefinedId, 1);
			} while (undefinedId !== -1)
			formatFields.unshift("java.lang.Character");
			formatFields.unshift("java.lang.Integer");
			formatFields.unshift("java.lang.Float");
			formatFields.unshift("java.lang.Boolean");
			return formatFields;
		}

		function addMessageLabels() {
			var messages = {};
			messages['openhmis.inventory.general.add'] = emr
					.message('openhmis.inventory.general.add');
			messages['openhmis.inventory.attribute.type.name'] = emr
					.message('openhmis.inventory.attribute.type.name');
			messages['openhmis.inventory.general.edit'] = emr
					.message('openhmis.inventory.general.edit');
			messages['openhmis.inventory.general.saveChanges'] = emr
					.message("openhmis.inventory.general.saveChanges");
			messages['openhmis.inventory.general.confirm'] = emr
					.message("openhmis.inventory.general.confirm");
			messages['openhmis.inventory.operations.type.sourceLabel'] = emr
					.message("openhmis.inventory.operations.type.sourceLabel");
			messages['openhmis.inventory.operations.type.destinationLabel'] = emr
					.message("openhmis.inventory.operations.type.destinationLabel");
			messages['openhmis.inventory.operations.type.recipientLabel'] = emr
					.message("openhmis.inventory.operations.type.recipientLabel");
			messages['openhmis.inventory.operations.type.availableWhenReservedLabel'] = emr
					.message("openhmis.inventory.operations.type.availableWhenReservedLabel");
			messages['openhmis.inventory.operations.type.userLabel'] = emr
					.message("openhmis.inventory.operations.type.userLabel");
			messages['openhmis.inventory.operations.type.roleLabel'] = emr
					.message("openhmis.inventory.operations.type.roleLabel");
			messages['openhmis.inventory.attribute.type.namePlural'] = emr
					.message("openhmis.inventory.attribute.type.namePlural");
			messages['PersonAttributeType.format'] = emr
					.message("PersonAttributeType.format");
			messages['PersonAttributeType.foreignKey'] = emr
					.message("PersonAttributeType.foreignKey");
			messages['PatientIdentifierType.format'] = emr
					.message("PatientIdentifierType.format");
			messages['FormField.required'] = emr.message("FormField.required");
			messages['Field.attributeName'] = emr
					.message("Field.attributeName");
			messages['Obs.order'] = emr.message("Obs.order");
			return messages;
		}

	}

})();
