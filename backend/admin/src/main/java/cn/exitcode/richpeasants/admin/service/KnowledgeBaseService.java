package cn.exitcode.richpeasants.admin.service;

import cn.exitcode.richpeasants.admin.dto.KnowledgeBaseRequest;
import cn.exitcode.richpeasants.common.entity.KnowledgeBase;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.repository.KbDocumentRepository;
import cn.exitcode.richpeasants.common.repository.KnowledgeBaseRepository;
import cn.exitcode.richpeasants.common.result.PageResult;
import cn.exitcode.richpeasants.common.result.ResultCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KbDocumentRepository kbDocumentRepository;

    public KnowledgeBaseService(KnowledgeBaseRepository knowledgeBaseRepository,
                                KbDocumentRepository kbDocumentRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.kbDocumentRepository = kbDocumentRepository;
    }

    public PageResult<KnowledgeBase> page(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), normalizeSize(size));
        return PageResult.from(knowledgeBaseRepository.findAllByOrderByIdDesc(pageable));
    }

    public List<KnowledgeBase> options() {
        return knowledgeBaseRepository.findAllByOrderByIdDesc();
    }

    public KnowledgeBase get(Long id) {
        return knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "知识库不存在"));
    }

    @Transactional
    public KnowledgeBase create(KnowledgeBaseRequest request) {
        String name = request.getName().trim();
        if (knowledgeBaseRepository.existsByName(name)) {
            throw new BusinessException(ResultCode.CONFLICT, "知识库名称已存在");
        }
        KnowledgeBase entity = new KnowledgeBase();
        entity.setName(name);
        entity.setDescription(request.getDescription());
        entity.setEnabled(request.getEnabled() == null || request.getEnabled());
        return knowledgeBaseRepository.save(entity);
    }

    @Transactional
    public KnowledgeBase update(Long id, KnowledgeBaseRequest request) {
        KnowledgeBase entity = get(id);
        String name = request.getName().trim();
        if (knowledgeBaseRepository.existsByNameAndIdNot(name, id)) {
            throw new BusinessException(ResultCode.CONFLICT, "知识库名称已存在");
        }
        entity.setName(name);
        entity.setDescription(request.getDescription());
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }
        return knowledgeBaseRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        KnowledgeBase entity = get(id);
        long docCount = kbDocumentRepository.countByKbId(id);
        if (docCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "知识库下仍有文档，请先删除文档");
        }
        knowledgeBaseRepository.delete(entity);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, 100);
    }
}
