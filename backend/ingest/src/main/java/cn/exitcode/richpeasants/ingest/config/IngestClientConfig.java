package cn.exitcode.richpeasants.ingest.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(IngestAppProperties.class)
public class IngestClientConfig {

    /**
     * Embedding 专用 RestClient：显式拉长超时，避免硅基流动等网关排队时被默认超时掐断。
     */
    @Bean
    public RestClient embeddingRestClient(IngestAppProperties properties) {
        int connectMs = Math.max(1000, properties.getEmbedding().getConnectTimeoutMs());
        int readMs = Math.max(5000, properties.getEmbedding().getReadTimeoutMs());

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectMs))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readMs));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
