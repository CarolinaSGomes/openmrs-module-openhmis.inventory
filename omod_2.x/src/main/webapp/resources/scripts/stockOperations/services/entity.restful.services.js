(function() {
    'use strict';

    angular.module('app.restfulServices').service('StockOperationRestfulService', StockOperationRestfulService);

    StockOperationRestfulService.$inject = ['EntityRestFactory', 'PaginationService'];

    function StockOperationRestfulService(EntityRestFactory, PaginationService) {
        var service;

        service = {
            stockroom: stockroom,
            stockOperation: stockOperation,
            searchStockOperation: searchStockOperation,
            stockOperationItem: stockOperationItem,
            stockOperationTransaction: stockOperationTransaction,
            rollbackOperation: rollbackOperation,
            loadStockOperationTypes: loadStockOperationTypes,
            loadStockRooms: loadStockRooms,
            searchStockOperationItems: searchStockOperationItems,
        };

        return service

        function searchStockOperationItems(q, successCallback){
            var requestParams = {};
            requestParams['rest_entity_name'] = 'item';
            requestParams['has_physical_inventory'] = 'true';
            requestParams['q'] = q;
            requestParams['limit'] = 10;
            EntityRestFactory.loadEntities(requestParams,successCallback, errorCallback);
        }

        /**
         * search stock operations
         * @param rest_entity_name
         * @param currentPage
         * @param limit
         * @param operationItem_uuid
         * @param operation_status
         * @param operationType_uuid
         * @param stockroom_uuid
         * @param successCallback
         */
        function searchStockOperation(rest_entity_name, currentPage, limit, operationItem_uuid, operation_status, operationType_uuid, stockroom_uuid, successCallback){
            var requestParams = PaginationService.paginateParams(currentPage, limit, false, '');
            requestParams['rest_entity_name'] = rest_entity_name;

            if(angular.isDefined(operation_status) && operation_status !== undefined && operation_status !== ''){
                requestParams['operation_status'] = operation_status;
            }

            if(angular.isDefined(operationType_uuid) && operationType_uuid !== undefined && operationType_uuid !== ''){
                requestParams['operationType_uuid'] = operationType_uuid;
            }

            if(angular.isDefined(stockroom_uuid) && stockroom_uuid !== undefined && stockroom_uuid !== ''){
                requestParams['stockroom_uuid'] = stockroom_uuid;
            }

            if(angular.isDefined(operationItem_uuid) && operationItem_uuid !== undefined && operationItem_uuid !== '') {
                requestParams['operationItem_uuid'] = operationItem_uuid;
            }

            EntityRestFactory.loadEntities(requestParams,
                successCallback,
                errorCallback
            );
        }

        /**
         * load list of operation types
         * @param rest_entity_name
         * @param successCallback
         */
        function loadStockOperationTypes(rest_entity_name, successCallback){
            var requestParams = {};
            requestParams['rest_entity_name'] = rest_entity_name;
            EntityRestFactory.loadEntities(requestParams,successCallback, errorCallback);
        }

        /**
         * load list of stockrooms
         * @param rest_entity_name
         * @param successCallback
         */
        function loadStockRooms(rest_entity_name, successCallback){
            var requestParams = {};
            requestParams['rest_entity_name'] = rest_entity_name;
            EntityRestFactory.loadEntities(requestParams, successCallback, errorCallback);
        }

        /**
         * load stockrooms given an operation uuid
         * @param operation_uuid
         * @param currentPage
         * @param limit
         * @param q
         * @param successCallback
         */
        function stockroom(operation_uuid, currentPage, limit, q, successCallback){
            ws_call('stockroom', operation_uuid, currentPage, limit, q, successCallback);
        }

        /**
         * retrieve stock operations
         * @param operation_uuid
         * @param rest_entity_name
         * @param successCallback
         */
        function stockOperation(operation_uuid, rest_entity_name, successCallback){
            if(angular.isDefined(operation_uuid) && operation_uuid !== '' && operation_uuid !== undefined){
                var requestParams = {};
                requestParams['rest_entity_name'] = rest_entity_name + '/' + operation_uuid;

                EntityRestFactory.loadEntities(requestParams,
                    successCallback,
                    errorCallback
                );
            }
        }

        /**
         * roll back an operation
         * @param operation_uuid
         * @param rest_entity_name
         * @param successCallback
         */
        function rollbackOperation(operation_uuid, rest_entity_name, successCallback) {
            if (angular.isDefined(operation_uuid) && operation_uuid !== '' && operation_uuid !== undefined) {
                var requestParams = {};
                requestParams['status'] = "ROLLBACK";

                EntityRestFactory.post(rest_entity_name, operation_uuid, requestParams,
                    successCallback,
                    errorCallback
                );

                successCallback(operation_uuid);
            }
        }

        /**
         * load stock operation items
         * @param operation_uuid
         * @param currentPage
         * @param limit
         * @param successCallback
         */
        function stockOperationItem(operation_uuid, currentPage, limit, successCallback){
            ws_call('stockOperationItem', operation_uuid, currentPage, limit, successCallback);
        }

        /**
         * load stock operations
         * @param operation_uuid
         * @param currentPage
         * @param limit
         * @param successCallback
         */
        function stockOperationTransaction(operation_uuid, currentPage, limit, successCallback){
            ws_call('stockOperationTransaction', operation_uuid, currentPage, limit, successCallback);
        }

        /**
         * Make restful calls for operation web services that require pagination
         * @param rest_entity_name
         * @param operation_uuid
         * @param currentPage
         * @param limit
         * @param successfulCallback
         */
        function ws_call(rest_entity_name, operation_uuid, currentPage, limit, successCallback){
            if(angular.isDefined(operation_uuid) && operation_uuid !== '' && operation_uuid !== undefined){
                var requestParams = PaginationService.paginateParams(currentPage, limit, false, '');
                requestParams['rest_entity_name'] = rest_entity_name;
                requestParams['operation_uuid'] = operation_uuid;

                EntityRestFactory.loadEntities(requestParams,
                    successCallback,
                    errorCallback
                );
            }
        }

        function errorCallback(error){
            console.log(error);
        }
    }
})();