package cn.exitcode.richpeasants.admin.service;

import cn.exitcode.richpeasants.admin.dto.LlmModelRequest;
import cn.exitcode.richpeasants.common.entity.LlmModel;
import cn.exitcode.richpeasants.common.enums.LlmModelPurpose;
import cn.exitcode.richpeasants.common.enums.LlmProtocol;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.repository.LlmModelRepository;
import cn.exitcode.richpeasants.common.result.PageResult;
import cn.exitcode.richpeasants.common.result.ResultCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LlmModelService {

    private final LlmModelRepository llmModelRepository;

    public LlmModelService(LlmModelRepository llmModelRepository) {
        this.llmModelRepository = llmModelRepository;
    }

    public PageResult<LlmModel> page(LlmModelPurpose purpose, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), normalizeSize(size));
        if (purpose != null) {
            return PageResult.from(llmModelRepository.findByPurposeOrderByIdDesc(purpose, pageable));
        }
        return PageResult.from(llmModelRepository.findAllByOrderByIdDesc(pageable));
    }

    public LlmModel get(Long id) {
        return llmModelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "模型不存在"));
    }

    @Transactional
    public LlmModel create(LlmModelRequest request) {
        validateProtocol(request.getProtocol());
        validatePurpose(request.getPurpose());
        String name = request.getName().trim();
        if (llmModelRepository.existsByName(name)) {
            throw new BusinessException(ResultCode.CONFLICT, "模型名称已存在");
        }
        LlmModel entity = new LlmModel();
        apply(entity, request, name);
        return llmModelRepository.save(entity);
    }

    @Transactional
    public LlmModel update(Long id, LlmModelRequest request) {
        validateProtocol(request.getProtocol());
        validatePurpose(request.getPurpose());
        LlmModel entity = get(id);
        String name = request.getName().trim();
        if (llmModelRepository.existsByNameAndIdNot(name, id)) {
            throw new BusinessException(ResultCode.CONFLICT, "模型名称已存在");
        }
        apply(entity, request, name);
        return llmModelRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        LlmModel entity = get(id);
        llmModelRepository.delete(entity);
    }

    private void apply(LlmModel entity, LlmModelRequest request, String name) {
        entity.setName(name);
        entity.setProtocol(request.getProtocol());
        entity.setPurpose(request.getPurpose());
        entity.setBaseUrl(trimUrl(request.getBaseUrl()));
        entity.setApiKey(request.getApiKey().trim());
        entity.setModelName(StringUtils.hasText(request.getModelName()) ? request.getModelName().trim() : null);
        if (request.getPurpose() == LlmModelPurpose.EMBEDDING) {
            if (request.getEmbeddingDimension() == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "向量化模型必须填写 embedding 维度");
            }
            entity.setEmbeddingDimension(request.getEmbeddingDimension());
        } else {
            entity.setEmbeddingDimension(null);
        }
        entity.setEnabled(request.getEnabled() == null || request.getEnabled());
        entity.setRemark(request.getRemark());
    }

    private void validateProtocol(LlmProtocol protocol) {
        if (protocol == null || protocol != LlmProtocol.OPENAI) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前仅支持 OpenAI 兼容对接方式");
        }
    }

    private void validatePurpose(LlmModelPurpose purpose) {
        if (purpose == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "模型用途不能为空");
        }
    }

    private String trimUrl(String url) {
        String value = url.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, 100);
    }
}
