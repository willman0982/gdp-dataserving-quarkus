package com.sc.gdp.dataserving.client.graphql;

import com.sc.gdp.dataserving.client.model.DataItem;
import com.sc.gdp.dataserving.client.model.DataStatus;

import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.graphql.client.typesafe.api.Header;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import java.util.List;

/**
 * GraphQL client for data serving API
 */
@GraphQLClientApi(configKey = "data-graphql-client")
@Header(name = "Authorization", method = "getAuthorizationHeader")
public interface DataGraphQLClient {

    // Queries

    @Query("allDataItems")
    List<DataItem> getAllDataItems();

    @Query("dataItem")
    DataItem getDataItemById(@Name("id") Long id);

    @Query("dataItemsByCategory")
    List<DataItem> getDataItemsByCategory(@Name("category") String category);

    @Query("dataItemsByStatus")
    List<DataItem> getDataItemsByStatus(@Name("status") DataStatus status);

    @Query("searchDataItems")
    List<DataItem> searchDataItems(@Name("searchTerm") String searchTerm);

    @Query("dataItemsCount")
    Long getDataItemsCount();

    @Query("dataItemsCountByStatus")
    Long getDataItemsCountByStatus(@Name("status") DataStatus status);

    @Query("currentUser")
    UserInfo getCurrentUser();

    // Mutations

    @Mutation("createDataItem")
    DataItem createDataItem(@Name("input") CreateDataItemInput input);

    @Mutation("updateDataItem")
    DataItem updateDataItem(@Name("id") Long id, @Name("input") UpdateDataItemInput input);

    @Mutation("updateDataItemStatus")
    DataItem updateDataItemStatus(@Name("id") Long id, @Name("status") DataStatus status);

    @Mutation("deleteDataItem")
    Boolean deleteDataItem(@Name("id") Long id);

    // Method to provide authorization header
    default String getAuthorizationHeader() {
        // This will be implemented by the GraphQL client service
        return "";
    }

    // Input Types
    class CreateDataItemInput {
        private String name;
        private String description;
        private String value;
        private String category;

        public CreateDataItemInput() {}

        public CreateDataItemInput(String name, String description, String value, String category) {
            this.name = name;
            this.description = description;
            this.value = value;
            this.category = category;
        }

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

    class UpdateDataItemInput {
        private String name;
        private String description;
        private String value;
        private String category;

        public UpdateDataItemInput() {}

        public UpdateDataItemInput(String name, String description, String value, String category) {
            this.name = name;
            this.description = description;
            this.value = value;
            this.category = category;
        }

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
    class UserInfo {
        private String username;
        private String email;

        public UserInfo() {}

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