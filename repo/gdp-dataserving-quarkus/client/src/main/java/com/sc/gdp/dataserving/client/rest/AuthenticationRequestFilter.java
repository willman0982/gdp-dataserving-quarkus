package com.sc.gdp.dataserving.client.rest;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

/**
 * Request filter to add OIDC authentication token to REST client requests
 */
@Provider
public class AuthenticationRequestFilter implements ClientRequestFilter {

    @Inject
    OidcClient oidcClient;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        try {
            // Get access token from OIDC client
            Tokens tokens = oidcClient.getTokens().await().indefinitely();
            String accessToken = tokens.getAccessToken();
            
            // Add Authorization header with Bearer token
            requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        } catch (Exception e) {
            throw new IOException("Failed to obtain access token", e);
        }
    }
}