package org.openmrs.module.openhmis.inventory.api.impl;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Drug;
import org.openmrs.Location;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhmis.commons.api.entity.impl.BaseMetadataDataServiceImpl;
import org.openmrs.module.openhmis.commons.api.entity.security.IMetadataAuthorizationPrivileges;
import org.openmrs.module.openhmis.commons.api.f.Action1;
import org.openmrs.module.openhmis.inventory.api.IPharmacyConnectorService;
import org.openmrs.module.openhmis.inventory.api.model.Item;
import org.openmrs.module.openhmis.inventory.api.util.HibernateCriteriaConstants;
import org.openmrs.module.openhmis.inventory.api.util.PrivilegeConstants;
import org.openmrs.Concept;
import org.openmrs.util.RoleConstants;

import java.util.List;

/**
 * Created by kmridev1 on 11/7/14.
 */

public class PharmacyConnectorServiceImpl extends BaseMetadataDataServiceImpl<Item>
        implements IPharmacyConnectorService,IMetadataAuthorizationPrivileges  {

    private static final String LOCATIONPROPERTY = "defaultLocation";

   @Override
    protected void validate(Item entity) throws APIException {
        return;
    }

    // Method for determining user location
    public void updateLocationUserCriteria(Criteria criteria) {

        User user = Context.getAuthenticatedUser();
        Location location = null;

        if (user.hasRole(RoleConstants.SUPERUSER))
            return;

        try {
            location = Context.getLocationService().getLocation(Integer.parseInt(user.getUserProperty(LOCATIONPROPERTY)));
        } catch (Exception e) {}

        if (location == null) {
            // impossible criterion so that no results will be returned
            criteria.add(Restrictions.isNull("creator"));
            return;
        }

        criteria.add(Restrictions.eq("location", location));
    }

    @Override
    public List<Item> listItemsByDrugId(Integer drugId) throws APIException {
        final Drug drug = Context.getConceptService().getDrug(drugId);

        return executeCriteria(Item.class, new Action1<Criteria>() {
            @Override
            public void apply(Criteria criteria) {
                updateLocationUserCriteria(criteria);
                criteria.add(Restrictions.eq(HibernateCriteriaConstants.DRUG, drug));
            }
        });
    }

    @Override
    public List<Item> listItemsByConceptId(Integer conceptId) throws APIException {
        final Concept concept = Context.getConceptService().getConcept(conceptId);

        return executeCriteria(Item.class, new Action1<Criteria>() {
            @Override
            public void apply(Criteria criteria) {
                updateLocationUserCriteria(criteria);
                criteria.add(Restrictions.eq(HibernateCriteriaConstants.CONCEPT, concept));
            }
        });
    }

    @Override
    public List<Item> listAllItems() throws APIException {
        return executeCriteria(Item.class, new Action1<Criteria>() {
            @Override
            public void apply(Criteria criteria) {
                updateLocationUserCriteria(criteria);
            }
        });
    }

    @Override
    public Boolean dispenseItem(Integer itemId, Integer quantity) throws IllegalArgumentException, APIException {
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
