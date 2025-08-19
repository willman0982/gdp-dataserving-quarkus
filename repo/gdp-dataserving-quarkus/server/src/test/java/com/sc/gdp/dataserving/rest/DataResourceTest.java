package com.sc.gdp.dataserving.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
public class DataResourceTest {

    @Test
    public void testGetAllDataItemsEndpoint() {
        given()
                .when().get("/api/data")
                .then()
                .statusCode(401); // Should return 401 without authentication
    }

    @Test
    public void testGetDataItemsCountEndpoint() {
        given()
                .when().get("/api/data/count")
                .then()
                .statusCode(401); // Should return 401 without authentication
    }

    // Note: These tests would require proper OIDC setup for full testing
    // In a real scenario, you would mock the JWT or set up test containers with Keycloak
}