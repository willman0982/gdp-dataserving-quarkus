package com.sc.gdp.dataserving.service;

import com.sc.gdp.dataserving.model.DataItem;
import com.sc.gdp.dataserving.model.DataStatus;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class DataServiceTest {

    @Inject
    DataService dataService;

    @Test
    public void testGetAllDataItems() {
        List<DataItem> items = dataService.getAllDataItems();
        assertNotNull(items);
        assertTrue(items.size() >= 3); // Should have at least 3 sample items
    }

    @Test
    public void testCreateDataItem() {
        String name = "Test Item";
        String description = "Test Description";
        String value = "Test Value";
        String category = "Test Category";

        DataItem item = dataService.createDataItem(name, description, value, category);

        assertNotNull(item);
        assertNotNull(item.getId());
        assertEquals(name, item.getName());
        assertEquals(description, item.getDescription());
        assertEquals(value, item.getValue());
        assertEquals(category, item.getCategory());
        assertEquals(DataStatus.ACTIVE, item.getStatus());
        assertNotNull(item.getCreatedAt());
        assertNotNull(item.getUpdatedAt());
    }

    @Test
    public void testGetDataItemById() {
        // Create a test item first
        DataItem createdItem = dataService.createDataItem("Test", "Description", "Value", "Category");
        
        Optional<DataItem> retrievedItem = dataService.getDataItemById(createdItem.getId());
        
        assertTrue(retrievedItem.isPresent());
        assertEquals(createdItem.getId(), retrievedItem.get().getId());
        assertEquals(createdItem.getName(), retrievedItem.get().getName());
    }

    @Test
    public void testGetDataItemByIdNotFound() {
        Optional<DataItem> item = dataService.getDataItemById(999999L);
        assertFalse(item.isPresent());
    }

    @Test
    public void testUpdateDataItem() {
        // Create a test item first
        DataItem createdItem = dataService.createDataItem("Original", "Original Description", "Original Value", "Original Category");
        
        String newName = "Updated Name";
        String newDescription = "Updated Description";
        
        Optional<DataItem> updatedItem = dataService.updateDataItem(
                createdItem.getId(), newName, newDescription, null, null);
        
        assertTrue(updatedItem.isPresent());
        assertEquals(newName, updatedItem.get().getName());
        assertEquals(newDescription, updatedItem.get().getDescription());
        assertEquals(createdItem.getValue(), updatedItem.get().getValue()); // Should remain unchanged
        assertEquals(createdItem.getCategory(), updatedItem.get().getCategory()); // Should remain unchanged
    }

    @Test
    public void testUpdateDataItemStatus() {
        // Create a test item first
        DataItem createdItem = dataService.createDataItem("Test", "Description", "Value", "Category");
        
        Optional<DataItem> updatedItem = dataService.updateDataItemStatus(createdItem.getId(), DataStatus.INACTIVE);
        
        assertTrue(updatedItem.isPresent());
        assertEquals(DataStatus.INACTIVE, updatedItem.get().getStatus());
    }

    @Test
    public void testDeleteDataItem() {
        // Create a test item first
        DataItem createdItem = dataService.createDataItem("To Delete", "Description", "Value", "Category");
        
        boolean deleted = dataService.deleteDataItem(createdItem.getId());
        assertTrue(deleted);
        
        // Verify it's actually deleted
        Optional<DataItem> retrievedItem = dataService.getDataItemById(createdItem.getId());
        assertFalse(retrievedItem.isPresent());
    }

    @Test
    public void testDeleteDataItemNotFound() {
        boolean deleted = dataService.deleteDataItem(999999L);
        assertFalse(deleted);
    }

    @Test
    public void testGetDataItemsByCategory() {
        String testCategory = "TestCategory" + System.currentTimeMillis();
        
        // Create test items with the same category
        dataService.createDataItem("Item 1", "Description 1", "Value 1", testCategory);
        dataService.createDataItem("Item 2", "Description 2", "Value 2", testCategory);
        
        List<DataItem> items = dataService.getDataItemsByCategory(testCategory);
        
        assertNotNull(items);
        assertEquals(2, items.size());
        items.forEach(item -> assertEquals(testCategory, item.getCategory()));
    }

    @Test
    public void testGetDataItemsByStatus() {
        List<DataItem> activeItems = dataService.getDataItemsByStatus(DataStatus.ACTIVE);
        assertNotNull(activeItems);
        activeItems.forEach(item -> assertEquals(DataStatus.ACTIVE, item.getStatus()));
    }

    @Test
    public void testSearchDataItems() {
        String searchTerm = "UniqueSearchTerm" + System.currentTimeMillis();
        
        // Create a test item with the search term in name
        dataService.createDataItem(searchTerm + " Item", "Description", "Value", "Category");
        
        List<DataItem> results = dataService.searchDataItems(searchTerm);
        
        assertNotNull(results);
        assertTrue(results.size() >= 1);
        assertTrue(results.stream().anyMatch(item -> item.getName().contains(searchTerm)));
    }

    @Test
    public void testSearchDataItemsEmptyTerm() {
        List<DataItem> results = dataService.searchDataItems("");
        List<DataItem> allItems = dataService.getAllDataItems();
        
        assertEquals(allItems.size(), results.size());
    }

    @Test
    public void testGetDataItemsCount() {
        long initialCount = dataService.getDataItemsCount();
        
        // Create a new item
        dataService.createDataItem("Count Test", "Description", "Value", "Category");
        
        long newCount = dataService.getDataItemsCount();
        assertEquals(initialCount + 1, newCount);
    }

    @Test
    public void testGetDataItemsCountByStatus() {
        long initialActiveCount = dataService.getDataItemsCountByStatus(DataStatus.ACTIVE);
        
        // Create a new active item
        dataService.createDataItem("Active Count Test", "Description", "Value", "Category");
        
        long newActiveCount = dataService.getDataItemsCountByStatus(DataStatus.ACTIVE);
        assertEquals(initialActiveCount + 1, newActiveCount);
    }
}