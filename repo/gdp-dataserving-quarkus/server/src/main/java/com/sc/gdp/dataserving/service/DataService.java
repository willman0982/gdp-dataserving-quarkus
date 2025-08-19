package com.sc.gdp.dataserving.service;

import com.sc.gdp.dataserving.model.DataItem;
import com.sc.gdp.dataserving.model.DataStatus;
import com.sc.gdp.dataserving.repository.DataItemRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service class for managing data items with database persistence
 */
@ApplicationScoped
public class DataService {
    
    @Inject
    DataItemRepository repository;
    
    /**
     * Get all data items
     */
    public List<DataItem> getAllDataItems() {
        return repository.findAllOrdered();
    }

    /**
     * Get data item by ID
     */
    public Optional<DataItem> getDataItemById(Long id) {
        return repository.findByIdOptional(id);
    }

    /**
     * Get data items by category
     */
    public List<DataItem> getDataItemsByCategory(String category) {
        return repository.findByCategory(category);
    }

    /**
     * Get data items by status
     */
    public List<DataItem> getDataItemsByStatus(DataStatus status) {
        return repository.findByStatus(status);
    }
    
    /**
     * Create a new data item
     */
    @Transactional
    public DataItem createDataItem(String name, String description, String value, String category) {
        DataItem item = new DataItem(name, description, value, category);
        repository.persist(item);
        return item;
    }
    
    /**
     * Update an existing data item
     */
    @Transactional
    public Optional<DataItem> updateDataItem(Long id, String name, String description, String value, String category) {
        Optional<DataItem> optionalItem = repository.findByIdOptional(id);
        if (optionalItem.isPresent()) {
            DataItem item = optionalItem.get();
            if (name != null) item.setName(name);
            if (description != null) item.setDescription(description);
            if (value != null) item.setValue(value);
            if (category != null) item.setCategory(category);
            // updatedAt will be set automatically by @PreUpdate
            return Optional.of(item);
        }
        return Optional.empty();
    }
    
    /**
     * Update data item status
     */
    @Transactional
    public Optional<DataItem> updateDataItemStatus(Long id, DataStatus status) {
        Optional<DataItem> optionalItem = repository.findByIdOptional(id);
        if (optionalItem.isPresent()) {
            DataItem item = optionalItem.get();
            item.setStatus(status);
            // updatedAt will be set automatically by @PreUpdate
            return Optional.of(item);
        }
        return Optional.empty();
    }
    
    /**
     * Delete a data item
     */
    @Transactional
    public boolean deleteDataItem(Long id) {
        return repository.deleteById(id);
    }
    
    /**
     * Search data items by name or description
     */
    public List<DataItem> searchDataItems(String searchTerm) {
        return repository.searchByNameOrDescription(searchTerm);
    }
    
    /**
     * Get data items count
     */
    public long getDataItemsCount() {
        return repository.count();
    }
    
    /**
     * Get data items count by status
     */
    public long getDataItemsCountByStatus(DataStatus status) {
        return repository.countByStatus(status);
    }
}