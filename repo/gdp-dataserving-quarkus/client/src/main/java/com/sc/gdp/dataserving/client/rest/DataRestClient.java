package com.sc.gdp.dataserving.client.rest;

import com.sc.gdp.dataserving.client.model.DataItem;
import com.sc.gdp.dataserving.client.model.DataStatus;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

/**
 * REST client for data serving API
 */
@RegisterRestClient(configKey = "com.sc.gdp.dataserving.client.rest.DataRestClient")
@RegisterProvider(AuthenticationRequestFilter.class)
@Path("/api/data")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DataRestClient {

    /**
     * Get all data items
     */
    @GET
    List<DataItem> getAllDataItems();

    /**
     * Get data item by ID
     */
    @GET
    @Path("/{id}")
    DataItem getDataItemById(@PathParam("id") Long id);

    /**
     * Get data items by category
     */
    @GET
    @Path("/category/{category}")
    List<DataItem> getDataItemsByCategory(@PathParam("category") String category);

    /**
     * Get data items by status
     */
    @GET
    @Path("/status/{status}")
    List<DataItem> getDataItemsByStatus(@PathParam("status") DataStatus status);

    /**
     * Search data items
     */
    @GET
    @Path("/search")
    List<DataItem> searchDataItems(@QueryParam("q") String searchTerm);

    /**
     * Create a new data item
     */
    @POST
    DataItem createDataItem(CreateDataItemRequest request);

    /**
     * Update an existing data item
     */
    @PUT
    @Path("/{id}")
    DataItem updateDataItem(@PathParam("id") Long id, UpdateDataItemRequest request);

    /**
     * Update data item status
     */
    @PATCH
    @Path("/{id}/status")
    DataItem updateDataItemStatus(@PathParam("id") Long id, UpdateStatusRequest request);

    /**
     * Delete a data item
     */
    @DELETE
    @Path("/{id}")
    void deleteDataItem(@PathParam("id") Long id);

    /**
     * Get data items count
     */
    @GET
    @Path("/count")
    CountResponse getDataItemsCount();

    /**
     * Get current user info
     */
    @GET
    @Path("/user-info")
    UserInfoResponse getUserInfo();

    // Request/Response DTOs
    class CreateDataItemRequest {
        private String name;
        private String description;
        private String value;
        private String category;

        public CreateDataItemRequest() {}

        public CreateDataItemRequest(String name, String description, String value, String category) {
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

    class UpdateDataItemRequest {
        private String name;
        private String description;
        private String value;
        private String category;

        public UpdateDataItemRequest() {}

        public UpdateDataItemRequest(String name, String description, String value, String category) {
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

    class UpdateStatusRequest {
        private DataStatus status;

        public UpdateStatusRequest() {}

        public UpdateStatusRequest(DataStatus status) {
            this.status = status;
        }

        public DataStatus getStatus() { return status; }
        public void setStatus(DataStatus status) { this.status = status; }
    }

    class CountResponse {
        private long count;

        public CountResponse() {}

        public CountResponse(long count) {
            this.count = count;
        }

        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }

    class UserInfoResponse {
        private String username;
        private String email;

        public UserInfoResponse() {}

        public UserInfoResponse(String username, String email) {
            this.username = username;
            this.email = email;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}