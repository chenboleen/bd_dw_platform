package com.kiro.metadata.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Elasticsearch 配置类
 * 使用纯 co.elastic.clients Java 客户端，不依赖 spring-data-elasticsearch，
 * 彻底避免 Spring Boot 3.2.x 的 factoryBeanObjectType 兼容性问题。
 */
@Slf4j
@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.uris}")
    private String uris;

    @Value("${elasticsearch.username:}")
    private String username;

    @Value("${elasticsearch.password:}")
    private String password;

    @Value("${elasticsearch.connection-timeout:5000}")
    private int connectionTimeoutMs;

    @Value("${elasticsearch.socket-timeout:30000}")
    private int socketTimeoutMs;

    /**
     * 创建 Elasticsearch 低级 RestClient
     */
    @Bean
    public RestClient restClient() {
        log.info("初始化 Elasticsearch RestClient，连接地址: {}", uris);
        return buildRestClient();
    }

    /**
     * 创建 Elasticsearch 高级 Java 客户端
     * 注册 JavaTimeModule 以支持 LocalDateTime 序列化
     */
    @Bean
    public ElasticsearchClient elasticsearchClient() {
        log.info("初始化 Elasticsearch Java 客户端");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ElasticsearchTransport transport = new RestClientTransport(
                restClient(), new JacksonJsonpMapper(mapper));
        return new ElasticsearchClient(transport);
    }

    private RestClient buildRestClient() {
        String[] uriArray = uris.split(",");
        HttpHost[] hosts = new HttpHost[uriArray.length];

        for (int i = 0; i < uriArray.length; i++) {
            String uri = uriArray[i].trim()
                    .replace("http://", "")
                    .replace("https://", "");
            String[] parts = uri.split(":");
            String hostname = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
            hosts[i] = new HttpHost(hostname, port, "http");
        }

        RestClientBuilder builder = RestClient.builder(hosts)
                .setRequestConfigCallback(cfg -> cfg
                        .setConnectTimeout(connectionTimeoutMs)
                        .setSocketTimeout(socketTimeoutMs));

        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            builder.setHttpClientConfigCallback(httpClient -> {
                CredentialsProvider cp = new BasicCredentialsProvider();
                cp.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(username, password));
                log.info("Elasticsearch 客户端已配置认证信息");
                return httpClient.setDefaultCredentialsProvider(cp);
            });
        }

        return builder.build();
    }
}
