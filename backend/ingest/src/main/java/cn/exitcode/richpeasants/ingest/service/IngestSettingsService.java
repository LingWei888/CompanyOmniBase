package cn.exitcode.richpeasants.ingest.service;

import cn.exitcode.richpeasants.common.config.IngestConfigKeys;
import cn.exitcode.richpeasants.common.entity.SysConfig;
import cn.exitcode.richpeasants.common.repository.SysConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IngestSettingsService {

    private final SysConfigRepository sysConfigRepository;

    public IngestSettingsService(SysConfigRepository sysConfigRepository) {
        this.sysConfigRepository = sysConfigRepository;
    }

    public int defaultChunkSize() {
        return readInt(IngestConfigKeys.CHUNK_SIZE, IngestConfigKeys.DEFAULT_CHUNK_SIZE);
    }

    public int defaultChunkOverlap() {
        return readInt(IngestConfigKeys.CHUNK_OVERLAP, IngestConfigKeys.DEFAULT_CHUNK_OVERLAP);
    }

    private int readInt(String key, int fallback) {
        return sysConfigRepository.findByConfigKey(key)
                .map(SysConfig::getConfigValue)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException ex) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }
}
