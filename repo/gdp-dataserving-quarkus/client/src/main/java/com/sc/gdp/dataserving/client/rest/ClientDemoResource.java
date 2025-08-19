package com.sc.gdp.dataserving.client.rest;

import com.sc.gdp.dataserving.client.model.DataItem;
import com.sc.gdp.dataserving.client.model.DataStatus;
import com.sc.gdp.dataserving.client.service.DataClientService;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * Demo REST resource to showcase client functionality
 */
@Path("/client-demo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClientDemoResource {

    @Inject
    DataClientService clientService;

    /**
     * Test connectivity to server
     */
    @GET
    @Path("/test")
    public Response testConnectivity() {
        try {
            String result = clientService.testConnectivity();
            return Response.ok(new TestResult(true, result)).build();
        } catch (Exception e) {
            return Response.ok(new TestResult(false, "Test failed: " + e.getMessage())).build();
        }
    }

    /**
     * Get all data items using REST
     */
    @GET
    @Path("/rest/items")
    public Response getAllDataItemsRest() {
        try {
            List<DataItem> items = clientService.getAllDataItemsRest();
            return Response.ok(items).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to fetch data via REST: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get all data items using GraphQL
     */
    @GET
    @Path("/graphql/items")
    public Response getAllDataItemsGraphQL() {
        try {
            List<DataItem> items = clientService.getAllDataItemsGraphQL();
            return Response.ok(items).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to fetch data via GraphQL: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get data item by ID using REST
     */
    @GET
    @Path("/rest/items/{id}")
    public Response getDataItemByIdRest(@PathParam("id") Long id) {
        try {
            DataItem item = clientService.getDataItemByIdRest(id);
            return Response.ok(item).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to fetch data via REST: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get data item by ID using GraphQL
     */
    @GET
    @Path("/graphql/items/{id}")
    public Response getDataItemByIdGraphQL(@PathParam("id") Long id) {
        try {
            DataItem item = clientService.getDataItemByIdGraphQL(id);
            return Response.ok(item).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to fetch data via GraphQL: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Create data item using REST
     */
    @POST
    @Path("/rest/items")
    public Response createDataItemRest(CreateItemRequest request) {
        try {
            DataItem item = clientService.createDataItemRest(
                    request.getName(),
                    request.getDescription(),
                    request.getValue(),
                    request.getCategory()
            );
            return Response.status(Response.Status.CREATED).entity(item).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to create data via REST: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Create data item using GraphQL
     */
    @POST
    @Path("/graphql/items")
    public Response createDataItemGraphQL(CreateItemRequest request) {
        try {
            DataItem item = clientService.createDataItemGraphQL(
                    request.getName(),
                    request.getDescription(),
                    request.getValue(),
                    request.getCategory()
            );
            return Response.status(Response.Status.CREATED).entity(item).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to create data via GraphQL: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Search data items using REST
     */
    @GET
    @Path("/rest/search")
    public Response searchDataItemsRest(@QueryParam("q") String searchTerm) {
        try {
            List<DataItem> items = clientService.searchDataItemsRest(searchTerm);
            return Response.ok(items).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to search data via REST: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Search data items using GraphQL
     */
    @GET
    @Path("/graphql/search")
    public Response searchDataItemsGraphQL(@QueryParam("q") String searchTerm) {
        try {
            List<DataItem> items = clientService.searchDataItemsGraphQL(searchTerm);
            return Response.ok(items).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to search data via GraphQL: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get data items by category using REST
     */
    @GET
    @Path("/rest/category/{category}")
    public Response getDataItemsByCategoryRest(@PathParam("category") String category) {
        try {
            List<DataItem> items = clientService.getDataItemsByCategoryRest(category);
            return Response.ok(items).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to fetch data by category via REST: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get data items by status using GraphQL
     */
    @GET
    @Path("/graphql/status/{status}")
    public Response getDataItemsByStatusGraphQL(@PathParam("status") DataStatus status) {
        try {
            List<DataItem> items = clientService.getDataItemsByStatusGraphQL(status);
            return Response.ok(items).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to fetch data by status via GraphQL: " + e.getMessage()))
                    .build();
        }
    }

    // DTOs
    public static class CreateItemRequest {
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

    public static class TestResult {
        private boolean success;
        private String message;

        public TestResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}