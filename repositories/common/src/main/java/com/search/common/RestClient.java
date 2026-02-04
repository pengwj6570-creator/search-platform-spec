package com.search.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * REST client utility for making HTTP requests.
 * Provides synchronous and asynchronous HTTP operations with proper error handling.
 */
public class RestClient {

    private static final Logger logger = LoggerFactory.getLogger(RestClient.class);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final int defaultTimeoutSeconds;

    /**
     * Create a REST client with the specified base URL.
     *
     * @param baseUrl the base URL for all requests
     */
    public RestClient(String baseUrl) {
        this(baseUrl, 30);
    }

    /**
     * Create a REST client with the specified base URL and timeout.
     *
     * @param baseUrl             the base URL for all requests
     * @param defaultTimeoutSeconds the default request timeout in seconds
     */
    public RestClient(String baseUrl, int defaultTimeoutSeconds) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(defaultTimeoutSeconds))
                .build();
        logger.debug("RestClient initialized with baseUrl: {}", this.baseUrl);
    }

    /**
     * Create a REST client with custom HttpClient configuration.
     *
     * @param httpClient the HttpClient to use
     * @param baseUrl    the base URL for all requests
     */
    public RestClient(HttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.defaultTimeoutSeconds = 30;
    }

    /**
     * Perform a GET request.
     *
     * @param path the request path (appended to base URL)
     * @return the response body
     * @throws IOException if the request fails
     */
    public String get(String path) throws IOException {
        return get(path, null);
    }

    /**
     * Perform a GET request with headers.
     *
     * @param path    the request path (appended to base URL)
     * @param headers the request headers
     * @return the response body
     * @throws IOException if the request fails
     */
    public String get(String path, Map<String, String> headers) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(buildUri(path))
                .GET()
                .timeout(Duration.ofSeconds(defaultTimeoutSeconds));

        if (headers != null) {
            headers.forEach(builder::header);
        }

        return execute(builder.build());
    }

    /**
     * Perform a POST request with a JSON body.
     *
     * @param path the request path (appended to base URL)
     * @param body the JSON body
     * @return the response body
     * @throws IOException if the request fails
     */
    public String post(String path, String body) throws IOException {
        return post(path, body, null);
    }

    /**
     * Perform a POST request with a JSON body and headers.
     *
     * @param path    the request path (appended to base URL)
     * @param body    the JSON body
     * @param headers the request headers
     * @return the response body
     * @throws IOException if the request fails
     */
    public String post(String path, String body, Map<String, String> headers) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(buildUri(path))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(defaultTimeoutSeconds));

        if (headers != null) {
            headers.forEach(builder::header);
        } else {
            builder.header("Content-Type", "application/json");
        }

        return execute(builder.build());
    }

    /**
     * Perform a PUT request with a JSON body.
     *
     * @param path the request path (appended to base URL)
     * @param body the JSON body
     * @return the response body
     * @throws IOException if the request fails
     */
    public String put(String path, String body) throws IOException {
        return put(path, body, null);
    }

    /**
     * Perform a PUT request with a JSON body and headers.
     *
     * @param path    the request path (appended to base URL)
     * @param body    the JSON body
     * @param headers the request headers
     * @return the response body
     * @throws IOException if the request fails
     */
    public String put(String path, String body, Map<String, String> headers) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(buildUri(path))
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(defaultTimeoutSeconds));

        if (headers != null) {
            headers.forEach(builder::header);
        } else {
            builder.header("Content-Type", "application/json");
        }

        return execute(builder.build());
    }

    /**
     * Perform a DELETE request.
     *
     * @param path the request path (appended to base URL)
     * @return the response body
     * @throws IOException if the request fails
     */
    public String delete(String path) throws IOException {
        return delete(path, null);
    }

    /**
     * Perform a DELETE request with headers.
     *
     * @param path    the request path (appended to base URL)
     * @param headers the request headers
     * @return the response body
     * @throws IOException if the request fails
     */
    public String delete(String path, Map<String, String> headers) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(buildUri(path))
                .DELETE()
                .timeout(Duration.ofSeconds(defaultTimeoutSeconds));

        if (headers != null) {
            headers.forEach(builder::header);
        }

        return execute(builder.build());
    }

    /**
     * Execute the HTTP request and return the response body.
     *
     * @param request the HTTP request
     * @return the response body
     * @throws IOException if the request fails
     */
    private String execute(HttpRequest request) throws IOException {
        try {
            logger.debug("Executing {} request to: {}", request.method(), request.uri());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                logger.error("Request failed with status {}: {}", response.statusCode(), response.body());
                throw new IOException("HTTP error " + response.statusCode() + ": " + response.body());
            }

            logger.debug("Request completed with status: {}", response.statusCode());
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    /**
     * Build the full URI by combining base URL and path.
     *
     * @param path the request path
     * @return the full URI
     */
    private URI buildUri(String path) {
        String fullPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(baseUrl + fullPath);
    }

    /**
     * Get the base URL.
     *
     * @return the base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Close the client and release resources.
     */
    public void close() {
        // HttpClient does not need explicit closing in Java 11+
        logger.debug("RestClient closed");
    }
}
