package org.openmrs.module.openhmis.inventory.api;

import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhmis.commons.api.f.Action2;
import org.openmrs.module.openhmis.inventory.api.model.Item;
import org.openmrs.module.openhmis.commons.api.entity.IMetadataDataServiceTest;
import org.openmrs.module.openhmis.inventory.api.model.ItemCode;
import org.openmrs.module.openhmis.inventory.api.model.ItemPrice;
import org.openmrs.Drug;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by kmridev1 on 11/7/14.
 */
public class IPharmacyConnectorServiceTest extends IMetadataDataServiceTest<IPharmacyConnectorService, Item> {

    private IDepartmentDataService departmentService;
    private ICategoryDataService categoryService;
    private static SessionFactory sessionFactory;

    public static final String ITEM_DATASET = TestConstants.BASE_DATASET_DIR + "ItemTest.xml";
    private int itemCount = 0;

    @Override
    public void before() throws Exception {
        super.before();

        departmentService = Context.getService(IDepartmentDataService.class);
        categoryService = Context.getService(ICategoryDataService.class);

        executeDataSet(IDepartmentDataServiceTest.DEPARTMENT_DATASET);
        executeDataSet(ICategoryDataServiceTest.CATEGORY_DATASET);
        executeDataSet(ITEM_DATASET);
    }

    @Override
    protected int getTestEntityCount() {
        return 7 + itemCount;
    }

    @Override
    public Item createEntity(boolean valid) {
        if (departmentService == null) {
            departmentService = Context.getService(IDepartmentDataService.class);
        }
        if (categoryService == null) {
            categoryService = Context.getService(ICategoryDataService.class);
        }

        Item item = new Item();
        item.setDepartment(departmentService.getById(0));
        item.setCreator(Context.getAuthenticatedUser());

        if (valid) {
            item.setName("Test Name " + itemCount);
        }

        item.setDescription("Test Description");

        item.setCategory(categoryService.getById(0));
        item.setConcept(Context.getConceptService().getConcept(itemCount));
        item.setHasPhysicalInventory(true);

        item.addCode("one", "Test Code 010");
        item.addCode("two", "Test Code 011");

        ItemPrice price = item.addPrice("default", BigDecimal.valueOf(100));
        item.addPrice("second", BigDecimal.valueOf(200));
        item.setDefaultPrice(price);

        Drug drug = Context.getConceptService().getDrug(itemCount % 7);
        item.setDrug(drug);

        itemCount++;

        return item;
    }

    @Override
    protected void updateEntityFields(Item item) {
        item.setDepartment(departmentService.getById(1));
        item.setDescription(item.getDescription() + " Updated");
        item.setName(item.getName() + " Updated");
        item.setHasPhysicalInventory(!item.hasPhysicalInventory());

        Set<ItemCode> codes = item.getCodes();
        if (codes.size() > 0) {
            // Update an existing code
            Iterator<ItemCode> iterator = codes.iterator();
            ItemCode code = iterator.next();
            code.setName(code.getName() + " Updated");
            code.setCode(code.getCode() + " Updated");

            if (codes.size() > 1) {
                // Delete an existing code
                code = iterator.next();

                item.removeCode(code);
            }
        }

        // Add a new code
        item.addCode("three", "Test Code 012");

        Set<ItemPrice> prices = item.getPrices();
        if (prices.size() > 0) {
            // Update n existing price
            Iterator<ItemPrice> iterator = prices.iterator();
            ItemPrice price = iterator.next();
            price.setName(price.getName() + " Updated");
            price.setPrice(price.getPrice().multiply(BigDecimal.valueOf(10)));

            if (prices.size() > 1) {
                // Delete an existing price
                price = iterator.next();

                item.removePrice(price);
            }
        }

        // Add a new price
        ItemPrice price = item.addPrice("third", BigDecimal.valueOf(3));

        item.setDefaultPrice(price);
    }

    @Override
    protected void assertEntity(Item expected, Item actual) {
        super.assertEntity(expected, actual);

        Assert.assertNotNull(expected.getDepartment());
        Assert.assertNotNull(actual.getDepartment());
        Assert.assertEquals(expected.getDepartment().getId(), actual.getDepartment().getId());
        Assert.assertEquals(expected.hasExpiration(), actual.hasExpiration());
        Assert.assertEquals(expected.hasPhysicalInventory(), actual.hasPhysicalInventory());

        if (expected.getConcept() == null) {
            Assert.assertNull(actual.getConcept());
        } else {
            Assert.assertEquals(expected.getConcept().getId(), actual.getConcept().getId());
        }

        if (expected.getCategory() == null) {
            Assert.assertNull(actual.getCategory());
        } else {
            Assert.assertEquals(expected.getCategory().getId(), actual.getCategory().getId());
        }

        assertCollection(expected.getCodes(), actual.getCodes(), new Action2<ItemCode, ItemCode>() {
            @Override
            public void apply(ItemCode expectedCode, ItemCode actualCode) {
                assertOpenmrsMetadata(expectedCode, actualCode);

                Assert.assertEquals(expectedCode.getName(), actualCode.getName());
                Assert.assertEquals(expectedCode.getCode(), actualCode.getCode());
            }
        });

        assertCollection(expected.getPrices(), actual.getPrices(), new Action2<ItemPrice, ItemPrice>() {
            @Override
            public void apply(ItemPrice expectedPrice, ItemPrice actualPrice) {
                assertOpenmrsMetadata(expectedPrice, actualPrice);

                Assert.assertEquals(expectedPrice.getName(), actualPrice.getName());
                Assert.assertEquals(expectedPrice.getPrice(), actualPrice.getPrice());
            }
        });
    }

    @Test
    public void test_listItemsByDrugId() throws Exception {
        List<Item> items = service.listItemsByDrugId(0);
        Assert.assertNotNull(items);
        Assert.assertEquals(7, items.size());
    }

    @Test
    public void test_listItemsByConceptId() throws Exception {
        for(int i = 0; i < 2; i++) {
            Item item = createEntity(true);
            item.setName("Item " + itemCount);

            item = service.save(item);
            Context.flushSession();
        }
        List<Item> items = service.listItemsByConceptId(Context.getConceptService().getConcept(0).getConceptId());
        Assert.assertNotNull(items);
        Assert.assertEquals(1, items.size());
    }

    @Test
    public void test_listAllItems() throws Exception {
        for(int i = 0; i < 7; i++) {
            Item item = createEntity(true);
            item.setName("Item " + itemCount);

            item = service.save(item);
            Context.flushSession();
        }
        List<Item> items = service.listAllItems();
        Assert.assertNotNull(items);
        Assert.assertEquals(getTestEntityCount(), items.size());
    }
}
