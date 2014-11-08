package org.openmrs.module.openhmis.inventory.api.impl;

import org.openmrs.api.APIException;
import org.openmrs.module.openhmis.commons.api.entity.impl.BaseMetadataDataServiceImpl;
import org.openmrs.module.openhmis.commons.api.entity.security.IMetadataAuthorizationPrivileges;
import org.openmrs.module.openhmis.inventory.api.IPharmacyConnectorService;
import org.openmrs.module.openhmis.inventory.api.model.Item;
import org.openmrs.module.openhmis.inventory.api.util.PrivilegeConstants;

import java.util.List;

/**
 * Created by kmridev1 on 11/7/14.
 */
public class PharmacyConnectorServiceImpl extends BaseMetadataDataServiceImpl<Item> implements IPharmacyConnectorService {
    @Override
    protected void validate(Item entity) throws APIException {
        return;
    }

    @Override
    public List<Item> listItemsByDrugId(Integer drugId) throws APIException {
        return null;
    }

    @Override
    public List<Item> listItemsByConceptId(Integer conceptId) throws APIException {
        return null;
    }

    @Override
    public List<Item> listAllItems() throws APIException {
        return null;
    }

    @Override
    public Boolean dispenseItem(Integer itemId, Integer quantity, String unit) throws IllegalArgumentException, APIException {
        return null;
    }

    @Override
    public List<String> listDispenseUnitsToItem(Integer item_id) throws APIException {
        return null;
    }

    @Override
    protected IMetadataAuthorizationPrivileges getPrivileges() {
        return this;
    }

    @Override
    public String getSavePrivilege() {
        return PrivilegeConstants.MANAGE_ITEMS;
    }

    @Override
    public String getPurgePrivilege() {
        return PrivilegeConstants.PURGE_ITEMS;
    }

    @Override
    public String getGetPrivilege() {
        return PrivilegeConstants.VIEW_ITEMS;
    }

    @Override
    public String getRetirePrivilege() {
        return PrivilegeConstants.MANAGE_ITEMS;
    }
}
