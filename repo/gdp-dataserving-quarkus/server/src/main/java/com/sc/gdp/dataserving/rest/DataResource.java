package com.sc.gdp.dataserving.rest;

import com.sc.gdp.dataserving.model.DataItem;
import com.sc.gdp.dataserving.model.DataStatus;
import com.sc.gdp.dataserving.service.DataService;
import com.sc.gdp.dataserving.service.DataSourceService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Optional;

/**
 * REST API resource for data serving
 */
@Path("/api/data")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DataResource {

    @Inject
    DataService dataService;

    @Inject
    DataSourceService dataSourceService;

    @Inject
    JsonWebToken jwt;

    /**
     * Get all data items
     */
    @GET
    @RolesAllowed({"user", "admin"})
    public Response getAllDataItems() {
        List<DataItem> items = dataSourceService.getAllDataItems();
        return Response.ok(items).build();
    }

    /**
     * Get data item by ID
     */
    @GET
    @Path("/{id}")
    @RolesAllowed({"user", "admin"})
    public Response getDataItemById(@PathParam("id") Long id) {
        Optional<DataItem> item = dataSourceService.getDataItemById(id);
        if (item.isPresent()) {
            return Response.ok(item.get()).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Data item with ID " + id + " not found")
                    .build();
        }
    }

    /**
     * Get data items by category
     */
    @GET
    @Path("/category/{category}")
    @RolesAllowed({"user", "admin"})
    public Response getDataItemsByCategory(@PathParam("category") String category) {
        List<DataItem> items = dataSourceService.getDataItemsByCategory(category);
        return Response.ok(items).build();
    }

    /**
     * Get data items by status
     */
    @GET
    @Path("/status/{status}")
    @RolesAllowed({"user", "admin"})
    public Response getDataItemsByStatus(@PathParam("status") DataStatus status) {
        List<DataItem> items = dataSourceService.getDataItemsByStatus(status);
        return Response.ok(items).build();
    }

    /**
     * Search data items
     */
    @GET
    @Path("/search")
    @RolesAllowed({"user", "admin"})
    public Response searchDataItems(@QueryParam("q") String searchTerm) {
        List<DataItem> items = dataSourceService.searchDataItems(searchTerm);
        return Response.ok(items).build();
    }

    /**
     * Create a new data item
     */
    @POST
    @RolesAllowed({"admin"})
    public Response createDataItem(CreateDataItemRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Name is required")
                    .build();
        }

        DataItem item = dataService.createDataItem(
                request.getName(),
                request.getDescription(),
                request.getValue(),
                request.getCategory()
        );

        return Response.status(Response.Status.CREATED).entity(item).build();
    }

    /**
     * Update an existing data item
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed({"admin"})
    public Response updateDataItem(@PathParam("id") Long id, UpdateDataItemRequest request) {
        Optional<DataItem> updatedItem = dataService.updateDataItem(
                id,
                request.getName(),
                request.getDescription(),
                request.getValue(),
                request.getCategory()
        );

        if (updatedItem.isPresent()) {
            return Response.ok(updatedItem.get()).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Data item with ID " + id + " not found")
                    .build();
        }
    }

    /**
     * Update data item status
     */
    @PATCH
    @Path("/{id}/status")
    @RolesAllowed({"admin"})
    public Response updateDataItemStatus(@PathParam("id") Long id, UpdateStatusRequest request) {
        Optional<DataItem> updatedItem = dataService.updateDataItemStatus(id, request.getStatus());

        if (updatedItem.isPresent()) {
            return Response.ok(updatedItem.get()).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Data item with ID " + id + " not found")
                    .build();
        }
    }

    /**
     * Delete a data item
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed({"admin"})
    public Response deleteDataItem(@PathParam("id") Long id) {
        boolean deleted = dataService.deleteDataItem(id);
        if (deleted) {
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Data item with ID " + id + " not found")
                    .build();
        }
    }

    /**
     * Get data items count
     */
    @GET
    @Path("/count")
    @RolesAllowed({"user", "admin"})
    public Response getDataItemsCount() {
        long count = dataSourceService.getDataItemsCount();
        return Response.ok(new CountResponse(count)).build();
    }

    /**
     * Get current user info
     */
    @GET
    @Path("/user-info")
    @RolesAllowed({"user", "admin"})
    public Response getUserInfo() {
        String username = jwt.getName();
        String email = jwt.getClaim("email");
        return Response.ok(new UserInfoResponse(username, email)).build();
    }

    // Request/Response DTOs
    public static class CreateDataItemRequest {
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

    public static class UpdateDataItemRequest {
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

    public static class UpdateStatusRequest {
        private DataStatus status;

        public DataStatus getStatus() { return status; }
        public void setStatus(DataStatus status) { this.status = status; }
    }

    public static class CountResponse {
        private long count;

        public CountResponse(long count) { this.count = count; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }

    public static class UserInfoResponse {
        private String username;
        private String email;

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