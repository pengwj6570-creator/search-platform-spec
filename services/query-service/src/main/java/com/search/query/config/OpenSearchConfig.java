package com.search.query.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;

/**
 * Configuration for OpenSearch client
 */
@Configuration
public class OpenSearchConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenSearchConfig.class);

    @Value("${opensearch.hosts:http://localhost:9200}")
    private String hosts;

    @Value("${opensearch.username:}")
    private String username;

    @Value("${opensearch.password:}")
    private String password;

    @Bean
    public RestClient restClient() {
        try {
            String[] hostArray = hosts.split(",");
            HttpHost[] httpHosts = new HttpHost[hostArray.length];

            for (int i = 0; i < hostArray.length; i++) {
                URL url = new URL(hostArray[i].trim());
                String scheme = url.getProtocol();
                String host = url.getHost();
                int port = url.getPort() > 0 ? url.getPort() : (scheme.equals("https") ? 443 : 9200);
                httpHosts[i] = new HttpHost(host, port, scheme);
            }

            RestClientBuilder builder = RestClient.builder(httpHosts);

            // Set default headers
            builder.setDefaultHeaders(new org.apache.http.Header[]{
                    new org.apache.http.message.BasicHeader("Content-Type", "application/json")
            });

            // Configure basic auth if provided
            if (username != null && !username.isEmpty()) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        AuthScope.ANY,
                        new UsernamePasswordCredentials(username, password)
                );
                builder.setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                );
                log.info("Configured basic auth for OpenSearch");
            }

            log.info("Created OpenSearch REST client for hosts: {}", hosts);
            return builder.build();

        } catch (Exception e) {
            log.error("Failed to create OpenSearch REST client", e);
            throw new RuntimeException("Failed to create OpenSearch REST client", e);
        }
    }

    @Bean
    public RestClientTransport openSearchTransport(RestClient restClient) {
        return new RestClientTransport(
                restClient,
                new JacksonJsonpMapper()
        );
    }

    @Bean
    public OpenSearchClient openSearchClient(RestClientTransport transport) {
        return new OpenSearchClient(transport);
    }
}
