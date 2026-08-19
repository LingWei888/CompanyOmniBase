package cn.exitcode.richpeasants.rag.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(RagAppProperties.class)
public class RagClientConfig {

    @Bean
    public RestClient chatRestClient(RagAppProperties properties) {
        int connectMs = Math.max(1000, properties.getConnectTimeoutMs());
        int readMs = Math.max(5000, properties.getReadTimeoutMs());

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
