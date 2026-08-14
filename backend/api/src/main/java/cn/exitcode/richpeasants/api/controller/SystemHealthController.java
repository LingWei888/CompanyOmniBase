package cn.exitcode.richpeasants.api.controller;

import cn.exitcode.richpeasants.common.result.ApiResult;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Day1 自检：探测 MySQL / RabbitMQ / Elasticsearch 连通性。
 */
@RestController
@RequestMapping("/api/system")
public class SystemHealthController {

    private final JdbcTemplate jdbcTemplate;
    private final ConnectionFactory rabbitConnectionFactory;
    private final ElasticsearchOperations elasticsearchOperations;
    private final HealthEndpoint healthEndpoint;

    public SystemHealthController(JdbcTemplate jdbcTemplate,
                                  ConnectionFactory rabbitConnectionFactory,
                                  ElasticsearchOperations elasticsearchOperations,
                                  HealthEndpoint healthEndpoint) {
        this.jdbcTemplate = jdbcTemplate;
        this.rabbitConnectionFactory = rabbitConnectionFactory;
        this.elasticsearchOperations = elasticsearchOperations;
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping("/self-check")
    public ApiResult<Map<String, Object>> selfCheck() {
        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("mysql", checkMysql());
        checks.put("rabbitmq", checkRabbit());
        checks.put("elasticsearch", checkElasticsearch());
        HealthComponent actuatorHealth = healthEndpoint.health();
        checks.put("actuatorStatus", actuatorHealth.getStatus().getCode());
        return ApiResult.ok(checks);
    }

    private Map<String, Object> checkMysql() {
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Map.of("ok", Integer.valueOf(1).equals(one), "detail", "SELECT 1 = " + one);
        } catch (Exception ex) {
            return Map.of("ok", false, "detail", String.valueOf(ex.getMessage()));
        }
    }

    private Map<String, Object> checkRabbit() {
        try (Connection connection = rabbitConnectionFactory.createConnection()) {
            boolean open = connection.isOpen();
            return Map.of("ok", open, "detail", open ? "connection opened" : "connection closed");
        } catch (Exception ex) {
            return Map.of("ok", false, "detail", String.valueOf(ex.getMessage()));
        }
    }

    private Map<String, Object> checkElasticsearch() {
        try {
            boolean exists = elasticsearchOperations.indexOps(IndexCoordinates.of("kb_health_probe")).exists();
            return Map.of("ok", true, "detail", "reachable, probeExists=" + exists);
        } catch (Exception ex) {
            return Map.of("ok", false, "detail", String.valueOf(ex.getMessage()));
        }
    }
}
