package cn.exitcode.richpeasants.api.publicapi;

import cn.exitcode.richpeasants.common.entity.LlmModel;
import cn.exitcode.richpeasants.common.entity.SysConfig;
import cn.exitcode.richpeasants.common.repository.LlmModelRepository;
import cn.exitcode.richpeasants.common.repository.SysConfigRepository;
import cn.exitcode.richpeasants.common.result.ApiResult;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public")
public class PublicSiteController {

    private final SysConfigRepository sysConfigRepository;
    private final LlmModelRepository llmModelRepository;

    public PublicSiteController(SysConfigRepository sysConfigRepository,
                                LlmModelRepository llmModelRepository) {
        this.sysConfigRepository = sysConfigRepository;
        this.llmModelRepository = llmModelRepository;
    }

    @GetMapping("/site")
    public ApiResult<SiteInfoResponse> site() {
        Map<String, String> map = new HashMap<>();
        for (SysConfig config : sysConfigRepository.findAll()) {
            map.put(config.getConfigKey(), config.getConfigValue() == null ? "" : config.getConfigValue());
        }
        String name = firstNonBlank(map.get("site_name"), "企业知识库智能问答");
        String description = firstNonBlank(map.get("site_description"), "基于企业知识库的智能问答助手");
        String logo = map.getOrDefault("site_logo", "");
        return ApiResult.ok(new SiteInfoResponse(name, description, logo));
    }

    @GetMapping("/models")
    public ApiResult<List<PublicModelOption>> models() {
        List<PublicModelOption> list = llmModelRepository.findByEnabledTrueOrderByIdAsc().stream()
                .map(this::toOption)
                .collect(Collectors.toList());
        return ApiResult.ok(list);
    }

    private PublicModelOption toOption(LlmModel model) {
        return new PublicModelOption(model.getId(), model.getName(), model.getModelName(), model.getRemark());
    }

    private String firstNonBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
