package cn.exitcode.richpeasants.rag.service;

import cn.exitcode.richpeasants.common.entity.TestdataGenRecord;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.repository.TestdataGenRecordRepository;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.common.security.LoginUser;
import cn.exitcode.richpeasants.rag.dto.TestdataGenRecordDetailResponse;
import cn.exitcode.richpeasants.rag.dto.TestdataGenRecordItemResponse;
import cn.exitcode.richpeasants.rag.dto.TestdataGenRecordUpsertRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TestdataGenRecordService {

    private final TestdataGenRecordRepository repository;

    public TestdataGenRecordService(TestdataGenRecordRepository repository) {
        this.repository = repository;
    }

    public List<TestdataGenRecordItemResponse> list(LoginUser user) {
        requireUser(user);
        return repository.findByUserIdOrderByUpdatedAtDesc(user.getUserId()).stream()
                .map(this::toItem)
                .collect(Collectors.toList());
    }

    public TestdataGenRecordDetailResponse detail(LoginUser user, Long id) {
        return toDetail(requireOwned(user, id));
    }

    @Transactional
    public TestdataGenRecordDetailResponse create(LoginUser user) {
        requireUser(user);
        // 复用空白草稿，避免堆多个「未命名生成」
        List<TestdataGenRecord> existing = repository.findByUserIdOrderByUpdatedAtDesc(user.getUserId());
        for (TestdataGenRecord record : existing) {
            if (isBlankDraft(record)) {
                record.setUpdatedAt(java.time.LocalDateTime.now());
                return toDetail(repository.save(record));
            }
        }
        TestdataGenRecord record = new TestdataGenRecord();
        record.setUserId(user.getUserId());
        record.setTitle("未命名生成");
        record.setOriginalText("");
        record.setResultPython("");
        record.setSolutionCode("");
        return toDetail(repository.save(record));
    }

    @Transactional
    public TestdataGenRecordDetailResponse update(LoginUser user, Long id, TestdataGenRecordUpsertRequest request) {
        TestdataGenRecord record = requireOwned(user, id);
        applyUpsert(record, request);
        return toDetail(repository.save(record));
    }

    @Transactional
    public void delete(LoginUser user, Long id) {
        TestdataGenRecord record = requireOwned(user, id);
        repository.delete(record);
    }

    private void applyUpsert(TestdataGenRecord record, TestdataGenRecordUpsertRequest request) {
        String original = request.getOriginalText() == null ? "" : request.getOriginalText();
        String python = request.getResultPython() == null ? "" : request.getResultPython();
        String solution = SolutionCodeValidator.normalize(request.getSolutionCode());
        if (StringUtils.hasText(solution)) {
            SolutionCodeValidator.validateOptional(solution);
        }
        String title = StringUtils.hasText(request.getTitle())
                ? request.getTitle().trim()
                : resolveTitle(original);

        record.setOriginalText(original);
        record.setResultPython(python);
        record.setSolutionCode(solution);
        record.setTitle(title);
    }

    private static String resolveTitle(String original) {
        if (!StringUtils.hasText(original)) {
            return "未命名生成";
        }
        String firstLine = original.trim().lines().findFirst().orElse("").trim();
        if (!StringUtils.hasText(firstLine)) {
            return "未命名生成";
        }
        // 去掉 markdown 标题前缀
        if (firstLine.startsWith("#")) {
            firstLine = firstLine.replaceFirst("^#+\\s*", "").trim();
        }
        return truncate(firstLine.isEmpty() ? "未命名生成" : firstLine, 200);
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) return value;
        return value.substring(0, max);
    }

    private static boolean isBlankDraft(TestdataGenRecord record) {
        return !StringUtils.hasText(record.getOriginalText())
                && !StringUtils.hasText(record.getResultPython())
                && !StringUtils.hasText(record.getSolutionCode());
    }

    private TestdataGenRecord requireOwned(LoginUser user, Long id) {
        requireUser(user);
        return repository.findByIdAndUserId(id, user.getUserId())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "生成记录不存在"));
    }

    private void requireUser(LoginUser user) {
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
    }

    private TestdataGenRecordItemResponse toItem(TestdataGenRecord record) {
        return new TestdataGenRecordItemResponse(
                record.getId(),
                record.getTitle(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }

    private TestdataGenRecordDetailResponse toDetail(TestdataGenRecord record) {
        TestdataGenRecordDetailResponse detail = new TestdataGenRecordDetailResponse();
        detail.setId(record.getId());
        detail.setTitle(record.getTitle());
        detail.setOriginalText(record.getOriginalText());
        detail.setResultPython(record.getResultPython());
        detail.setSolutionCode(record.getSolutionCode() == null ? "" : record.getSolutionCode());
        detail.setCreatedAt(record.getCreatedAt());
        detail.setUpdatedAt(record.getUpdatedAt());
        return detail;
    }
}
