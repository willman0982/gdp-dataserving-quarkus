package com.sc.gdp.dataserving.client.service;

import com.sc.gdp.dataserving.client.graphql.DataGraphQLClient;
import com.sc.gdp.dataserving.client.model.DataItem;
import com.sc.gdp.dataserving.client.model.DataStatus;
import com.sc.gdp.dataserving.client.rest.DataRestClient;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

/**
 * Service class that provides both REST and GraphQL client functionality
 */
@ApplicationScoped
public class DataClientService {

    @Inject
    @RestClient
    DataRestClient restClient;

    @Inject
    OidcClient oidcClient;

    private DataGraphQLClient graphqlClient;

    /**
     * Get GraphQL client with authentication
     */
    private DataGraphQLClient getGraphQLClient() {
        if (graphqlClient == null) {
            String authHeader = getAuthorizationHeader();
            graphqlClient = TypesafeGraphQLClientBuilder.newBuilder()
                    .configKey("data-graphql-client")
                    .header("Authorization", authHeader)
                    .build(DataGraphQLClient.class);
        }
        return graphqlClient;
    }

    /**
     * Get authorization header with Bearer token
     */
    private String getAuthorizationHeader() {
        try {
            Tokens tokens = oidcClient.getTokens().await().indefinitely();
            return "Bearer " + tokens.getAccessToken();
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain access token", e);
        }
    }

    // REST Client Methods

    /**
     * Get all data items using REST
     */
    public List<DataItem> getAllDataItemsRest() {
        return restClient.getAllDataItems();
    }

    /**
     * Get data item by ID using REST
     */
    public DataItem getDataItemByIdRest(Long id) {
        return restClient.getDataItemById(id);
    }

    /**
     * Create data item using REST
     */
    public DataItem createDataItemRest(String name, String description, String value, String category) {
        DataRestClient.CreateDataItemRequest request = new DataRestClient.CreateDataItemRequest(
                name, description, value, category);
        return restClient.createDataItem(request);
    }

    /**
     * Update data item using REST
     */
    public DataItem updateDataItemRest(Long id, String name, String description, String value, String category) {
        DataRestClient.UpdateDataItemRequest request = new DataRestClient.UpdateDataItemRequest(
                name, description, value, category);
        return restClient.updateDataItem(id, request);
    }

    /**
     * Delete data item using REST
     */
    public void deleteDataItemRest(Long id) {
        restClient.deleteDataItem(id);
    }

    /**
     * Search data items using REST
     */
    public List<DataItem> searchDataItemsRest(String searchTerm) {
        return restClient.searchDataItems(searchTerm);
    }

    /**
     * Get data items by category using REST
     */
    public List<DataItem> getDataItemsByCategoryRest(String category) {
        return restClient.getDataItemsByCategory(category);
    }

    /**
     * Get data items by status using REST
     */
    public List<DataItem> getDataItemsByStatusRest(DataStatus status) {
        return restClient.getDataItemsByStatus(status);
    }

    // GraphQL Client Methods

    /**
     * Get all data items using GraphQL
     */
    public List<DataItem> getAllDataItemsGraphQL() {
        return getGraphQLClient().getAllDataItems();
    }

    /**
     * Get data item by ID using GraphQL
     */
    public DataItem getDataItemByIdGraphQL(Long id) {
        return getGraphQLClient().getDataItemById(id);
    }

    /**
     * Create data item using GraphQL
     */
    public DataItem createDataItemGraphQL(String name, String description, String value, String category) {
        DataGraphQLClient.CreateDataItemInput input = new DataGraphQLClient.CreateDataItemInput(
                name, description, value, category);
        return getGraphQLClient().createDataItem(input);
    }

    /**
     * Update data item using GraphQL
     */
    public DataItem updateDataItemGraphQL(Long id, String name, String description, String value, String category) {
        DataGraphQLClient.UpdateDataItemInput input = new DataGraphQLClient.UpdateDataItemInput(
                name, description, value, category);
        return getGraphQLClient().updateDataItem(id, input);
    }

    /**
     * Delete data item using GraphQL
     */
    public Boolean deleteDataItemGraphQL(Long id) {
        return getGraphQLClient().deleteDataItem(id);
    }

    /**
     * Search data items using GraphQL
     */
    public List<DataItem> searchDataItemsGraphQL(String searchTerm) {
        return getGraphQLClient().searchDataItems(searchTerm);
    }

    /**
     * Get data items by category using GraphQL
     */
    public List<DataItem> getDataItemsByCategoryGraphQL(String category) {
        return getGraphQLClient().getDataItemsByCategory(category);
    }

    /**
     * Get data items by status using GraphQL
     */
    public List<DataItem> getDataItemsByStatusGraphQL(DataStatus status) {
        return getGraphQLClient().getDataItemsByStatus(status);
    }

    /**
     * Get data items count using GraphQL
     */
    public Long getDataItemsCountGraphQL() {
        return getGraphQLClient().getDataItemsCount();
    }

    /**
     * Get current user info using GraphQL
     */
    public DataGraphQLClient.UserInfo getCurrentUserGraphQL() {
        return getGraphQLClient().getCurrentUser();
    }

    // Utility Methods

    /**
     * Test connectivity to both REST and GraphQL endpoints
     */
    public String testConnectivity() {
        try {
            // Test REST endpoint
            List<DataItem> restItems = getAllDataItemsRest();
            
            // Test GraphQL endpoint
            List<DataItem> graphqlItems = getAllDataItemsGraphQL();
            
            return String.format("Connectivity test successful. REST returned %d items, GraphQL returned %d items.", 
                    restItems.size(), graphqlItems.size());
        } catch (Exception e) {
            return "Connectivity test failed: " + e.getMessage();
        }
    }
}