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
package org.openmrs.module.webservices.rest.search;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.openhmis.commons.api.PagingInfo;
import org.openmrs.module.openhmis.inventory.api.IStockOperationDataService;
import org.openmrs.module.openhmis.inventory.api.IStockroomDataService;
import org.openmrs.module.openhmis.inventory.api.model.StockOperation;
import org.openmrs.module.openhmis.inventory.api.model.StockOperationStatus;
import org.openmrs.module.openhmis.inventory.api.model.Stockroom;
import org.openmrs.module.openhmis.inventory.api.search.StockOperationSearch;
import org.openmrs.module.openhmis.inventory.web.ModuleRestConstants;
import org.openmrs.module.webservices.rest.resource.AlreadyPagedWithLength;
import org.openmrs.module.webservices.rest.resource.PagingUtil;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.api.SearchConfig;
import org.openmrs.module.webservices.rest.web.resource.api.SearchHandler;
import org.openmrs.module.webservices.rest.web.resource.api.SearchQuery;
import org.openmrs.module.webservices.rest.web.resource.impl.EmptySearchResult;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class StockOperationSearchHandler implements SearchHandler {
	protected Log log = LogFactory.getLog(getClass());

	private final SearchConfig searchConfig = new SearchConfig("default", ModuleRestConstants.OPERATION_RESOURCE,
			Arrays.asList("1.11.*"),
			Arrays.asList(
					new SearchQuery.Builder("Finds stock operations with an optional status and/or stockroom.")
							.withOptionalParameters("status", "stockroom_uuid")
							.build()

			)
	);

	private IStockroomDataService stockroomDataService;
	private IStockOperationDataService operationDataService;

	@Autowired
	public StockOperationSearchHandler(IStockroomDataService stockroomDataService, IStockOperationDataService operationDataService) {
		this.stockroomDataService = stockroomDataService;
		this.operationDataService = operationDataService;
	}

	@Override
	public SearchConfig getSearchConfig() {
		return searchConfig;
	}

	@Override
	public PageableResult search(RequestContext context) throws ResponseException {
		String statusText = context.getParameter("status");
		String stockroomText = context.getParameter("stockroom_uuid");

		StockOperationStatus status = null;
		if (!StringUtils.isEmpty(statusText)) {
			status = StockOperationStatus.valueOf(statusText);

			if (status == null) {
				log.warn("Could not parse Stock Operation Status '" + statusText + "'");

				return new EmptySearchResult();
			}
		}

		Stockroom stockroom = null;
		if (!StringUtils.isEmpty(stockroomText)) {
			stockroom = stockroomDataService.getByUuid(stockroomText);

			if (stockroom == null) {
				log.warn("Could not find stockroom '" + stockroomText + "'");

				return new EmptySearchResult();
			}
		}

		StockOperationSearch search = null;
		if (status != null) {
			search = new StockOperationSearch();
			search.getTemplate().setStatus(status);
		}

		PagingInfo pagingInfo = PagingUtil.getPagingInfoFromContext(context);
		List<StockOperation> operations;
		if (stockroom == null) {
			if (search == null) {
				// No search was defined so just return everything (excluding retired)
				operations = operationDataService.getAll(false, pagingInfo);
			} else {
				// Return the operations with the specified status
				operations = operationDataService.getOperations(search, pagingInfo);
			}
		} else {
			// Return the operations for the specified stockroom and status
			operations = stockroomDataService.getOperations(stockroom, search, pagingInfo);
		}

		if (operations == null || operations.size() == 0) {
			return new EmptySearchResult();
		} else {
			return new AlreadyPagedWithLength<StockOperation>(context, operations, pagingInfo.hasMoreResults(),
					pagingInfo.getTotalRecordCount());
		}
	}
}
