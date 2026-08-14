package cn.exitcode.richpeasants.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "cn.exitcode.richpeasants")
@EntityScan(basePackages = "cn.exitcode.richpeasants")
@EnableJpaRepositories(basePackages = "cn.exitcode.richpeasants")
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
