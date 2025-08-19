package com.sc.gdp.dataserving.repository;

import com.sc.gdp.dataserving.model.DataItem;
import com.sc.gdp.dataserving.model.DataStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DataItem entities
 */
@ApplicationScoped
public class DataItemRepository implements PanacheRepository<DataItem> {

    /**
     * Find data items by category
     */
    public List<DataItem> findByCategory(String category) {
        return find("category", category).list();
    }

    /**
     * Find data items by status
     */
    public List<DataItem> findByStatus(DataStatus status) {
        return find("status", status).list();
    }

    /**
     * Search data items by name or description
     */
    public List<DataItem> searchByNameOrDescription(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return listAll(Sort.by("createdAt").descending());
        }
        
        String likePattern = "%" + searchTerm.toLowerCase() + "%";
        return find("LOWER(name) LIKE ?1 OR LOWER(description) LIKE ?1", likePattern)
                .list();
    }

    /**
     * Count data items by status
     */
    public long countByStatus(DataStatus status) {
        return count("status", status);
    }

    /**
     * Find all data items ordered by creation date
     */
    public List<DataItem> findAllOrdered() {
        return listAll(Sort.by("createdAt").descending());
    }

    /**
     * Find data item by ID with null safety
     */
    public Optional<DataItem> findByIdOptional(Long id) {
        return Optional.ofNullable(findById(id));
    }
}