package cn.exitcode.richpeasants.admin.service;

import cn.exitcode.richpeasants.admin.dto.SysConfigItemRequest;
import cn.exitcode.richpeasants.common.entity.SysConfig;
import cn.exitcode.richpeasants.common.repository.SysConfigRepository;
import cn.exitcode.richpeasants.common.storage.MinioStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysConfigService {

    public static final String KEY_SITE_LOGO = "site_logo";

    private final SysConfigRepository sysConfigRepository;
    private final MinioStorageService minioStorageService;

    public SysConfigService(SysConfigRepository sysConfigRepository,
                            MinioStorageService minioStorageService) {
        this.sysConfigRepository = sysConfigRepository;
        this.minioStorageService = minioStorageService;
    }

    public List<SysConfig> list() {
        return sysConfigRepository.findAll();
    }

    public Map<String, String> asMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (SysConfig config : sysConfigRepository.findAll()) {
            map.put(config.getConfigKey(), config.getConfigValue());
        }
        return map;
    }

    @Transactional
    public List<SysConfig> saveBatch(List<SysConfigItemRequest> items) {
        for (SysConfigItemRequest item : items) {
            String key = item.getConfigKey().trim();
            SysConfig config = sysConfigRepository.findByConfigKey(key).orElseGet(SysConfig::new);
            config.setConfigKey(key);
            config.setConfigValue(item.getConfigValue());
            if (item.getRemark() != null) {
                config.setRemark(item.getRemark());
            } else if (config.getRemark() == null) {
                config.setRemark("");
            }
            sysConfigRepository.save(config);
        }
        return list();
    }

    @Transactional
    public String uploadLogo(MultipartFile file) {
        String url = minioStorageService.uploadSiteAsset(file, "site");
        SysConfig config = sysConfigRepository.findByConfigKey(KEY_SITE_LOGO).orElseGet(SysConfig::new);
        config.setConfigKey(KEY_SITE_LOGO);
        config.setConfigValue(url);
        if (config.getRemark() == null) {
            config.setRemark("站点 Logo URL");
        }
        sysConfigRepository.save(config);
        return url;
    }
}
