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
package org.openmrs.module.openhmis.inventory.api.model;

/**
 * The allowable {@link StockOperation} statuses.
 */
public enum StockOperationStatus {
	/**
	 * The operation is being created but has not yet been submitted.
	 */
	NEW(false),
	/**
	 * The operation has been requested but not yet started.
	 */
	REQUESTED(false),
	/**
	 * The operation has been started and the associated items are being processed.
	 */
	PENDING(true),
	/**
	 * The operation was cancelled and the pending transactions were reversed.
	 */
	CANCELLED(true),
	/**
	 * The operation was completed and the pending transactions were applied.
	 */
	COMPLETED(false),
	/**
	 * The operation was rolled back and all applied transactions were reversed.
	 */
	ROLLBACK(false);

	boolean removedByAutoFinishOperation;

	private StockOperationStatus(boolean r) {
		removedByAutoFinishOperation = r;
	}

	public boolean getRemovedByAutoFinishOperation() {
		return removedByAutoFinishOperation;
	}
}
