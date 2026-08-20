package cn.exitcode.richpeasants.rag.service;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 将模型输出的题面文本规范为标准竞赛题面 Markdown（标题、章节、样例代码块）。
 */
final class ProblemMarkdownNormalizer {

    private static final Set<String> H2_EXACT = Set.of(
            "题目背景", "题目描述", "输入格式", "输出格式", "说明/提示"
    );

    private static final Pattern H2_SAMPLE = Pattern.compile("^输入输出样例\\s*#\\d+\\s*$");
    private static final Pattern H3_IO = Pattern.compile("^(输入|输出)\\s*#\\d+\\s*$");

    private ProblemMarkdownNormalizer() {
    }

    static String normalize(String markdown, String targetTitle) {
        if (!StringUtils.hasText(markdown)) {
            return enforceTitle("", targetTitle);
        }
        String text = markdown.replace("\r\n", "\n").replace('\r', '\n');
        List<String> lines = new ArrayList<>(List.of(text.split("\n", -1)));
        List<String> out = new ArrayList<>();

        boolean titleDone = false;
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String trimmed = raw.trim();

            if (!StringUtils.hasText(trimmed)) {
                out.add("");
                continue;
            }

            if (isArtifactLine(trimmed)) {
                continue;
            }

            if (!titleDone) {
                out.add(enforceTitleLine(trimmed, targetTitle));
                titleDone = true;
                continue;
            }

            if (isH2Exact(stripHashes(trimmed)) || H2_SAMPLE.matcher(stripHashes(trimmed)).matches()) {
                out.add("## " + stripHashes(trimmed));
                continue;
            }

            if (H3_IO.matcher(stripHashes(trimmed)).matches()) {
                out.add("### " + stripHashes(trimmed));
                i = copyOrWrapIoBlock(lines, i, out);
                continue;
            }

            if (trimmed.startsWith("```")) {
                i = copyFenceBlock(lines, i, out);
                continue;
            }

            out.add(raw);
        }

        String joined = String.join("\n", out).trim();
        return enforceTitle(joined, targetTitle);
    }

    static String enforceTitle(String markdown, String targetTitle) {
        if (!StringUtils.hasText(targetTitle)) {
            return markdown == null ? "" : markdown.trim();
        }
        String normalizedTarget = targetTitle.trim().startsWith("#")
                ? targetTitle.trim()
                : "# " + targetTitle.trim();
        if (!StringUtils.hasText(markdown)) {
            return normalizedTarget;
        }
        String[] lines = markdown.split("\n", -1);
        int firstContent = 0;
        while (firstContent < lines.length && lines[firstContent].isBlank()) {
            firstContent++;
        }
        if (firstContent >= lines.length) {
            return normalizedTarget;
        }
        lines[firstContent] = normalizedTarget;
        return String.join("\n", lines).trim();
    }

    private static String enforceTitleLine(String trimmed, String targetTitle) {
        if (StringUtils.hasText(targetTitle)) {
            return targetTitle.trim().startsWith("#")
                    ? targetTitle.trim()
                    : "# " + targetTitle.trim();
        }
        if (trimmed.startsWith("#")) {
            return trimmed;
        }
        return "# " + trimmed;
    }

    private static boolean isArtifactLine(String trimmed) {
        return "code".equalsIgnoreCase(trimmed)
                || "```code".equalsIgnoreCase(trimmed)
                || "markdown".equalsIgnoreCase(trimmed);
    }

    private static boolean isH2Exact(String text) {
        return H2_EXACT.contains(text);
    }

    private static String stripHashes(String line) {
        return line.replaceFirst("^#+\\s*", "").trim();
    }

    /** 复制完整围栏块，或修复缺少围栏的样例内容 */
    private static int copyOrWrapIoBlock(List<String> lines, int h3Index, List<String> out) {
        int i = h3Index + 1;
        while (i < lines.size() && lines.get(i).trim().isEmpty()) {
            out.add("");
            i++;
        }
        if (i >= lines.size()) {
            out.add("");
            appendEmptyFence(out);
            return i - 1;
        }
        if (lines.get(i).trim().startsWith("```")) {
            return copyFenceBlock(lines, i, out);
        }

        List<String> content = new ArrayList<>();
        while (i < lines.size()) {
            String t = lines.get(i).trim();
            if (isArtifactLine(t)) {
                i++;
                continue;
            }
            if (isSectionHeading(t)) {
                break;
            }
            content.add(lines.get(i));
            i++;
        }
        out.add("");
        out.add("```");
        out.addAll(content);
        out.add("```");
        return i - 1;
    }

    private static int copyFenceBlock(List<String> lines, int start, List<String> out) {
        out.add(lines.get(start));
        int i = start + 1;
        while (i < lines.size()) {
            out.add(lines.get(i));
            if (lines.get(i).trim().startsWith("```")) {
                return i;
            }
            i++;
        }
        out.add("```");
        return i - 1;
    }

    private static void appendEmptyFence(List<String> out) {
        out.add("```");
        out.add("");
        out.add("```");
    }

    private static boolean isSectionHeading(String trimmed) {
        if (!StringUtils.hasText(trimmed)) {
            return false;
        }
        String core = stripHashes(trimmed);
        return isH2Exact(core)
                || H2_SAMPLE.matcher(core).matches()
                || H3_IO.matcher(core).matches();
    }
}
