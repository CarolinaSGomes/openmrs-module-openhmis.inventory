package org.openmrs.module.openhmis.inventory.api;

import java.util.List;

import org.openmrs.Concept;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.APIException;
import org.openmrs.module.openhmis.commons.api.PagingInfo;
import org.openmrs.module.openhmis.commons.api.entity.IMetadataDataService;
import org.openmrs.module.openhmis.inventory.api.model.Category;
import org.openmrs.module.openhmis.inventory.api.model.Department;
import org.openmrs.module.openhmis.inventory.api.model.Item;
import org.openmrs.module.openhmis.inventory.api.search.ItemSearch;
import org.openmrs.module.openhmis.inventory.api.util.PrivilegeConstants;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by kmridev1 on 11/7/14.
 */
public interface IPharmacyConnectorService  extends IMetadataDataService<Item> {

    @Transactional(readOnly =  true)
    @Authorized( {PrivilegeConstants.VIEW_ITEMS})
    public List<Item> listItemsByDrugId(Integer drugId) throws APIException;

    @Transactional(readOnly =  true)
    @Authorized( {PrivilegeConstants.VIEW_ITEMS})
    public List<Item> listItemsByConceptId(Integer conceptId) throws APIException;

    @Transactional(readOnly =  true)
    @Authorized( {PrivilegeConstants.VIEW_ITEMS})
    public List<Item> listAllItems() throws APIException;

    @Transactional
    @Authorized( {PrivilegeConstants.MANAGE_OPERATIONS})
    public Boolean dispenseItem(Integer itemId, Integer quantity, String unit) throws IllegalArgumentException, APIException;

    @Transactional(readOnly =  true)
    @Authorized( {PrivilegeConstants.VIEW_ITEMS})
    public List<String> listDispenseUnitsToItem(Integer item_id) throws APIException;
}
