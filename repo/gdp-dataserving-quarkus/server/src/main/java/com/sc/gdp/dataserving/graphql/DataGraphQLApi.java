package com.sc.gdp.dataserving.graphql;

import com.sc.gdp.dataserving.model.DataItem;
import com.sc.gdp.dataserving.model.DataStatus;
import com.sc.gdp.dataserving.service.DataService;
import com.sc.gdp.dataserving.service.DataSourceService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.*;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Optional;

/**
 * GraphQL API for data serving
 */
@GraphQLApi
public class DataGraphQLApi {

    @Inject
    DataService dataService;

    @Inject
    DataSourceService dataSourceService;

    @Inject
    JsonWebToken jwt;

    // Queries

    /**
     * Get all data items
     */
    @Query("allDataItems")
    @Description("Get all data items")
    @RolesAllowed({"user", "admin"})
    public List<DataItem> getAllDataItems() {
        return dataSourceService.getAllDataItems();
    }

    /**
     * Get data item by ID
     */
    @Query("dataItem")
    @Description("Get data item by ID")
    @RolesAllowed({"user", "admin"})
    public DataItem getDataItemById(@Name("id") Long id) throws GraphQLException {
        Optional<DataItem> item = dataSourceService.getDataItemById(id);
        if (item.isPresent()) {
            return item.get();
        } else {
            throw new GraphQLException("Data item with ID " + id + " not found");
        }
    }

    /**
     * Get data items by category
     */
    @Query("dataItemsByCategory")
    @Description("Get data items by category")
    @RolesAllowed({"user", "admin"})
    public List<DataItem> getDataItemsByCategory(@Name("category") String category) {
        return dataSourceService.getDataItemsByCategory(category);
    }

    /**
     * Get data items by status
     */
    @Query("dataItemsByStatus")
    @Description("Get data items by status")
    @RolesAllowed({"user", "admin"})
    public List<DataItem> getDataItemsByStatus(@Name("status") DataStatus status) {
        return dataSourceService.getDataItemsByStatus(status);
    }

    /**
     * Search data items
     */
    @Query("searchDataItems")
    @Description("Search data items by name or description")
    @RolesAllowed({"user", "admin"})
    public List<DataItem> searchDataItems(@Name("searchTerm") String searchTerm) {
        return dataSourceService.searchDataItems(searchTerm);
    }

    /**
     * Get data items count
     */
    @Query("dataItemsCount")
    @Description("Get total count of data items")
    @RolesAllowed({"user", "admin"})
    public Long getDataItemsCount() {
        return dataSourceService.getDataItemsCount();
    }

    /**
     * Get data items count by status
     */
    @Query("dataItemsCountByStatus")
    @Description("Get count of data items by status")
    @RolesAllowed({"user", "admin"})
    public Long getDataItemsCountByStatus(@Name("status") DataStatus status) {
        return dataSourceService.getDataItemsCountByStatus(status);
    }

    /**
     * Get current user info
     */
    @Query("currentUser")
    @Description("Get current authenticated user information")
    @RolesAllowed({"user", "admin"})
    public UserInfo getCurrentUser() {
        String username = jwt.getName();
        String email = jwt.getClaim("email");
        return new UserInfo(username, email);
    }

    // Mutations

    /**
     * Create a new data item
     */
    @Mutation("createDataItem")
    @Description("Create a new data item")
    @RolesAllowed({"admin"})
    public DataItem createDataItem(@Name("input") CreateDataItemInput input) throws GraphQLException {
        if (input.getName() == null || input.getName().trim().isEmpty()) {
            throw new GraphQLException("Name is required");
        }

        return dataService.createDataItem(
                input.getName(),
                input.getDescription(),
                input.getValue(),
                input.getCategory()
        );
    }

    /**
     * Update an existing data item
     */
    @Mutation("updateDataItem")
    @Description("Update an existing data item")
    @RolesAllowed({"admin"})
    public DataItem updateDataItem(@Name("id") Long id, @Name("input") UpdateDataItemInput input) throws GraphQLException {
        Optional<DataItem> updatedItem = dataService.updateDataItem(
                id,
                input.getName(),
                input.getDescription(),
                input.getValue(),
                input.getCategory()
        );

        if (updatedItem.isPresent()) {
            return updatedItem.get();
        } else {
            throw new GraphQLException("Data item with ID " + id + " not found");
        }
    }

    /**
     * Update data item status
     */
    @Mutation("updateDataItemStatus")
    @Description("Update data item status")
    @RolesAllowed({"admin"})
    public DataItem updateDataItemStatus(@Name("id") Long id, @Name("status") DataStatus status) throws GraphQLException {
        Optional<DataItem> updatedItem = dataService.updateDataItemStatus(id, status);

        if (updatedItem.isPresent()) {
            return updatedItem.get();
        } else {
            throw new GraphQLException("Data item with ID " + id + " not found");
        }
    }

    /**
     * Delete a data item
     */
    @Mutation("deleteDataItem")
    @Description("Delete a data item")
    @RolesAllowed({"admin"})
    public Boolean deleteDataItem(@Name("id") Long id) throws GraphQLException {
        boolean deleted = dataService.deleteDataItem(id);
        if (!deleted) {
            throw new GraphQLException("Data item with ID " + id + " not found");
        }
        return true;
    }

    // Input Types

    @Input("CreateDataItemInput")
    @Description("Input for creating a new data item")
    public static class CreateDataItemInput {
        private String name;
        private String description;
        private String value;
        private String category;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }

    @Input("UpdateDataItemInput")
    @Description("Input for updating an existing data item")
    public static class UpdateDataItemInput {
        private String name;
        private String description;
        private String value;
        private String category;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }

    // Output Types

    @Type("UserInfo")
    @Description("User information")
    public static class UserInfo {
        private String username;
        private String email;

        public UserInfo(String username, String email) {
            this.username = username;
            this.email = email;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}