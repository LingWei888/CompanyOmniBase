package cn.exitcode.richpeasants.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * 启动时打印已注册的接口，便于排查多模块 Controller 是否加载成功。
 */
@Component
public class MappingDiagnosticsRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MappingDiagnosticsRunner.class);

    private final RequestMappingHandlerMapping mapping;

    public MappingDiagnosticsRunner(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mapping) {
        this.mapping = mapping;
    }

    @Override
    public void run(ApplicationArguments args) {
        long adminApis = mapping.getHandlerMethods().keySet().stream()
                .filter(info -> info.getPatternValues().stream().anyMatch(p -> p.startsWith("/api/admin")))
                .count();
        log.info("Registered /api/admin mappings: {}", adminApis);
        if (adminApis == 0) {
            log.error("admin 模块 Controller 未加载！请在 IDEA 中 Maven Reload，并用 api 模块启动 ApiApplication");
        }
    }
}
