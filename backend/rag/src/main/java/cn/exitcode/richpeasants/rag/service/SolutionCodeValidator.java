package cn.exitcode.richpeasants.rag.service;

import cn.exitcode.richpeasants.common.exception.BusinessException;
import cn.exitcode.richpeasants.common.result.ResultCode;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 可选题解代码格式校验：确认内容更像「可运行/可读的题解代码」，而不是题面或乱码。
 */
final class SolutionCodeValidator {

    private static final int MIN_LEN = 24;
    private static final int MAX_LEN = 80_000;

    private static final Pattern FENCE = Pattern.compile("```");
    private static final Pattern FENCE_LANG = Pattern.compile(
            "```\\s*(c\\+\\+|cpp|cc|cxx|c|java|python|py|go|rust|rs|javascript|js|typescript|ts|csharp|cs|kotlin|kt|pascal|pas|php|ruby|rb|swift|scala|haskell|hs|lua|r|matlab|sql|bash|sh|powershell|ps1)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CODE_SIGNAL = Pattern.compile(
            "(?i)(#include\\b|using\\s+namespace\\b|int\\s+main\\s*\\(|void\\s+main\\s*\\(|"
                    + "public\\s+class\\b|public\\s+static\\s+void\\s+main\\b|"
                    + "def\\s+\\w+\\s*\\(|class\\s+\\w+\\s*[:({]|import\\s+[\\w.]+|"
                    + "from\\s+[\\w.]+\\s+import\\b|cin\\s*>>|cout\\s*<<|scanf\\s*\\(|printf\\s*\\(|"
                    + "System\\.out|BufferedReader|String\\[\\]|fn\\s+main\\s*\\(|"
                    + "package\\s+main\\b|func\\s+main\\s*\\(|console\\.log\\s*\\(|"
                    + "function\\s+\\w+\\s*\\(|const\\s+\\w+\\s*=|let\\s+\\w+\\s*=|"
                    + "\\breturn\\b|\\bwhile\\s*\\(|\\bfor\\s*\\()");

    private static final Pattern PROBLEM_SIGNAL = Pattern.compile(
            "(?i)(##\\s*题目描述|##\\s*输入格式|##\\s*输出格式|##\\s*输入输出样例|"
                    + "【数据范围】|时间限制|空间限制|Problem\\s*Description)");

    private SolutionCodeValidator() {
    }

    /** 空内容视为未提供，校验通过。 */
    static void validateOptional(String raw) {
        if (!StringUtils.hasText(raw)) {
            return;
        }
        String text = raw.replace("\r\n", "\n").trim();
        if (text.length() < MIN_LEN) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "题解代码过短，请粘贴完整题解（建议包含可运行代码）");
        }
        if (text.length() > MAX_LEN) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "题解代码过长，请精简后重试");
        }

        int fenceCount = countMatches(FENCE, text);
        if (fenceCount % 2 != 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "题解代码中的 Markdown 代码围栏未闭合，请检查 ``` 是否成对");
        }

        boolean hasLangFence = FENCE_LANG.matcher(text).find();
        boolean hasCodeSignal = CODE_SIGNAL.matcher(text).find();
        boolean looksBraced = text.contains("{") && text.contains("}");
        boolean looksIndentedBlock = text.lines().filter(l -> l.startsWith("    ") || l.startsWith("\t")).count() >= 3;

        if (!hasLangFence && !hasCodeSignal && !looksBraced && !looksIndentedBlock) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "未能识别为题解代码。请粘贴带语言标记的代码块（如 ```cpp）或含 main/class/def 等的源码");
        }

        int problemHits = countMatches(PROBLEM_SIGNAL, text);
        if (problemHits >= 2 && !hasLangFence && !hasCodeSignal) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "内容更像题面而非题解代码，请只粘贴题解/标程源码");
        }

        // 纯中文说明、几乎无代码符号
        long asciiCodeChars = text.chars()
                .filter(ch -> ch == '{' || ch == '}' || ch == ';' || ch == '(' || ch == ')' || ch == '=' || ch == '#')
                .count();
        if (asciiCodeChars < 3 && !hasLangFence) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "题解代码格式不正确：缺少常见代码符号或代码围栏");
        }

        // 避免整段只有语言标签空围栏
        if (hasLangFence) {
            Matcher m = Pattern.compile("```[^\\n]*\\n([\\s\\S]*?)```").matcher(text);
            boolean anyBody = false;
            while (m.find()) {
                if (StringUtils.hasText(m.group(1)) && m.group(1).trim().length() >= 8) {
                    anyBody = true;
                    break;
                }
            }
            if (fenceCount >= 2 && !anyBody && !hasCodeSignal) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "代码围栏内内容过少，请粘贴完整题解代码");
            }
        }
    }

    private static int countMatches(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    static String normalize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        return raw.replace("\r\n", "\n").trim();
    }

    static boolean looksLikeCodePreview(String raw) {
        if (!StringUtils.hasText(raw)) {
            return false;
        }
        try {
            validateOptional(raw);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    static String summarizeLang(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        Matcher m = FENCE_LANG.matcher(raw);
        if (m.find()) {
            return m.group(1).toLowerCase(Locale.ROOT);
        }
        return "";
    }
}
