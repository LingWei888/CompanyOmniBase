package cn.exitcode.richpeasants.rag.service;

import cn.exitcode.richpeasants.common.entity.ProblemConvertRecord;
import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.repository.ProblemConvertRecordRepository;
import cn.exitcode.richpeasants.common.result.ResultCode;
import cn.exitcode.richpeasants.common.security.LoginUser;
import cn.exitcode.richpeasants.rag.dto.ProblemConvertRecordDetailResponse;
import cn.exitcode.richpeasants.rag.dto.ProblemConvertRecordItemResponse;
import cn.exitcode.richpeasants.rag.dto.ProblemConvertRecordUpsertRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ProblemConvertRecordService {

    private static final Pattern TITLE_LINE = Pattern.compile("^#\\s+(.+)$", Pattern.MULTILINE);

    private final ProblemConvertRecordRepository repository;

    public ProblemConvertRecordService(ProblemConvertRecordRepository repository) {
        this.repository = repository;
    }

    public List<ProblemConvertRecordItemResponse> list(LoginUser user) {
        requireUser(user);
        return repository.findByUserIdOrderByUpdatedAtDesc(user.getUserId()).stream()
                .map(this::toItem)
                .collect(Collectors.toList());
    }

    public ProblemConvertRecordDetailResponse detail(LoginUser user, Long id) {
        return toDetail(requireOwned(user, id));
    }

    @Transactional
    public ProblemConvertRecordDetailResponse create(LoginUser user) {
        requireUser(user);
        // 复用空白草稿，避免堆多个「未命名转换」
        List<ProblemConvertRecord> existing = repository.findByUserIdOrderByUpdatedAtDesc(user.getUserId());
        for (ProblemConvertRecord record : existing) {
            if (isBlankDraft(record)) {
                record.setUpdatedAt(java.time.LocalDateTime.now());
                return toDetail(repository.save(record));
            }
        }
        ProblemConvertRecord record = new ProblemConvertRecord();
        record.setUserId(user.getUserId());
        record.setTitle("未命名转换");
        record.setReferenceNickname("");
        record.setOriginalText("");
        record.setResultMarkdown("");
        record.setSolutionCode("");
        return toDetail(repository.save(record));
    }

    @Transactional
    public ProblemConvertRecordDetailResponse update(LoginUser user, Long id, ProblemConvertRecordUpsertRequest request) {
        ProblemConvertRecord record = requireOwned(user, id);
        applyUpsert(record, request);
        return toDetail(repository.save(record));
    }

    @Transactional
    public void delete(LoginUser user, Long id) {
        ProblemConvertRecord record = requireOwned(user, id);
        repository.delete(record);
    }

    private void applyUpsert(ProblemConvertRecord record, ProblemConvertRecordUpsertRequest request) {
        String reference = request.getReferenceNickname() == null ? "" : request.getReferenceNickname().trim();
        String original = request.getOriginalText() == null ? "" : request.getOriginalText();
        String markdown = request.getResultMarkdown() == null ? "" : request.getResultMarkdown();
        String solution = SolutionCodeValidator.normalize(request.getSolutionCode());
        if (StringUtils.hasText(solution)) {
            SolutionCodeValidator.validateOptional(solution);
        }
        String title = StringUtils.hasText(request.getTitle())
                ? request.getTitle().trim()
                : resolveTitle(reference, markdown);

        record.setReferenceNickname(reference);
        record.setOriginalText(original);
        record.setResultMarkdown(markdown);
        record.setSolutionCode(solution);
        record.setTitle(title);
    }

    private static String resolveTitle(String reference, String markdown) {
        if (StringUtils.hasText(reference)) {
            return truncate(reference, 200);
        }
        Matcher m = TITLE_LINE.matcher(markdown == null ? "" : markdown);
        if (m.find()) {
            return truncate(m.group(1).trim(), 200);
        }
        return "未命名转换";
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) return value;
        return value.substring(0, max);
    }

    private static boolean isBlankDraft(ProblemConvertRecord record) {
        return !StringUtils.hasText(record.getReferenceNickname())
                && !StringUtils.hasText(record.getOriginalText())
                && !StringUtils.hasText(record.getResultMarkdown())
                && !StringUtils.hasText(record.getSolutionCode());
    }

    private ProblemConvertRecord requireOwned(LoginUser user, Long id) {
        requireUser(user);
        return repository.findByIdAndUserId(id, user.getUserId())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "转换记录不存在"));
    }

    private void requireUser(LoginUser user) {
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
    }

    private ProblemConvertRecordItemResponse toItem(ProblemConvertRecord record) {
        return new ProblemConvertRecordItemResponse(
                record.getId(),
                record.getTitle(),
                record.getReferenceNickname(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }

    private ProblemConvertRecordDetailResponse toDetail(ProblemConvertRecord record) {
        ProblemConvertRecordDetailResponse detail = new ProblemConvertRecordDetailResponse();
        detail.setId(record.getId());
        detail.setTitle(record.getTitle());
        detail.setReferenceNickname(record.getReferenceNickname());
        detail.setOriginalText(record.getOriginalText());
        detail.setResultMarkdown(record.getResultMarkdown());
        detail.setSolutionCode(record.getSolutionCode() == null ? "" : record.getSolutionCode());
        detail.setCreatedAt(record.getCreatedAt());
        detail.setUpdatedAt(record.getUpdatedAt());
        return detail;
    }
}
