package com.qianfan.tag.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchClientConfig {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "profile-search.enabled", havingValue = "true")
    public RestHighLevelClient restHighLevelClient(ProfileSearchProperties properties) {
        org.elasticsearch.client.RestClientBuilder builder = RestClient.builder(HttpHost.create(properties.getUrl()));
        if (properties.getUsername() != null && !properties.getUsername().trim().isEmpty()) {
            BasicCredentialsProvider credentials = new BasicCredentialsProvider();
            credentials.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword()));
            builder.setHttpClientConfigCallback(http -> http.setDefaultCredentialsProvider(credentials));
        }
        return new RestHighLevelClient(builder);
    }
}
