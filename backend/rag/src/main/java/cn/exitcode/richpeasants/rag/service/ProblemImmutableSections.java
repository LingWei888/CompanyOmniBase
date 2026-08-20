package cn.exitcode.richpeasants.rag.service;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从原题提取「不可改」章节（输入/输出格式、样例、数据范围），并在生成后回填。
 */
final class ProblemImmutableSections {

    private static final Set<String> H2_ORDER = Set.of(
            "题目背景", "题目描述", "输入格式", "输出格式", "说明/提示"
    );

    private static final Pattern H2_SAMPLE = Pattern.compile("^输入输出样例\\s*#(\\d+)\\s*$");
    private static final Pattern H3_INPUT = Pattern.compile("^输入\\s*#(\\d+)\\s*$");
    private static final Pattern H3_OUTPUT = Pattern.compile("^输出\\s*#(\\d+)\\s*$");

    record Sample(int index, String inputBlock, String outputBlock) {
    }

    record Extracted(
            String inputFormat,
            String outputFormat,
            List<Sample> samples,
            String dataRangeHint
    ) {
        boolean hasSamples() {
            return samples != null && !samples.isEmpty();
        }
    }

    private ProblemImmutableSections() {
    }

    static Extracted extract(String original) {
        if (!StringUtils.hasText(original)) {
            return new Extracted("", "", List.of(), "");
        }
        Map<String, String> h2 = parseH2Sections(original);
        List<Sample> samples = parseSamples(original);
        String dataRange = extractDataRange(h2.getOrDefault("说明/提示", ""));
        return new Extracted(
                h2.getOrDefault("输入格式", "").trim(),
                h2.getOrDefault("输出格式", "").trim(),
                samples,
                dataRange
        );
    }

    /**
     * 用原题不可变部分覆盖生成稿中的对应章节，保证 I/O 与样例一致。
     */
    static String merge(String generated, Extracted extracted) {
        if (!StringUtils.hasText(generated) || extracted == null) {
            return generated;
        }
        String text = generated.replace("\r\n", "\n").replace('\r', '\n');
        Map<String, String> h2 = parseH2Sections(text);
        Map<String, String> sampleSections = parseSampleSections(text);

        if (StringUtils.hasText(extracted.inputFormat())) {
            h2.put("输入格式", extracted.inputFormat());
        }
        if (StringUtils.hasText(extracted.outputFormat())) {
            h2.put("输出格式", extracted.outputFormat());
        }

        if (extracted.hasSamples()) {
            for (Sample sample : extracted.samples()) {
                String key = "输入输出样例 #" + sample.index();
                sampleSections.put(key, renderSampleSection(sample));
            }
        }

        return rebuildDocument(text, h2, sampleSections, extracted.dataRangeHint());
    }

    private static Map<String, String> parseH2Sections(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        List<String> lines = List.of(text.split("\n", -1));
        String currentKey = null;
        StringBuilder body = new StringBuilder();

        for (String line : lines) {
            String core = stripHeadingMarks(line.trim());
            if (H2_ORDER.contains(core)) {
                if (currentKey != null) {
                    map.put(currentKey, body.toString().trim());
                }
                currentKey = core;
                body = new StringBuilder();
                continue;
            }
            if (H2_SAMPLE.matcher(core).matches()) {
                if (currentKey != null) {
                    map.put(currentKey, body.toString().trim());
                }
                currentKey = null;
                body = new StringBuilder();
                continue;
            }
            if (currentKey != null) {
                if (!body.isEmpty()) {
                    body.append('\n');
                }
                body.append(line);
            }
        }
        if (currentKey != null) {
            map.put(currentKey, body.toString().trim());
        }
        return map;
    }

    private static Map<String, String> parseSampleSections(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        List<String> lines = List.of(text.split("\n", -1));
        String currentSampleKey = null;
        StringBuilder section = new StringBuilder();

        for (String line : lines) {
            String core = stripHeadingMarks(line.trim());
            Matcher m = H2_SAMPLE.matcher(core);
            if (m.matches()) {
                if (currentSampleKey != null) {
                    map.put(currentSampleKey, section.toString().trim());
                }
                currentSampleKey = "输入输出样例 #" + m.group(1);
                section = new StringBuilder();
                continue;
            }
            if (currentSampleKey != null) {
                String nextCore = stripHeadingMarks(line.trim());
                if (H2_ORDER.contains(nextCore) || H2_SAMPLE.matcher(nextCore).matches()) {
                    map.put(currentSampleKey, section.toString().trim());
                    currentSampleKey = null;
                    section = new StringBuilder();
                    if (H2_SAMPLE.matcher(nextCore).matches()) {
                        Matcher sm = H2_SAMPLE.matcher(nextCore);
                        sm.matches();
                        currentSampleKey = "输入输出样例 #" + sm.group(1);
                        section = new StringBuilder();
                    }
                    continue;
                }
                if (!section.isEmpty()) {
                    section.append('\n');
                }
                section.append(line);
            }
        }
        if (currentSampleKey != null) {
            map.put(currentSampleKey, section.toString().trim());
        }
        return map;
    }

    private static List<Sample> parseSamples(String text) {
        Map<String, String> sampleSections = parseSampleSections(text);
        List<Sample> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : sampleSections.entrySet()) {
            int index = Integer.parseInt(entry.getKey().replace("输入输出样例 #", ""));
            String section = entry.getValue();
            String input = extractIoBlock(section, true);
            String output = extractIoBlock(section, false);
            list.add(new Sample(index, input, output));
        }
        list.sort((a, b) -> Integer.compare(a.index(), b.index()));
        return list;
    }

    private static String extractIoBlock(String section, boolean input) {
        List<String> lines = List.of(section.split("\n", -1));
        Pattern pat = input ? H3_INPUT : H3_OUTPUT;
        int i = 0;
        while (i < lines.size()) {
            String core = stripHeadingMarks(lines.get(i).trim());
            if (pat.matcher(core).matches()) {
                return extractFenceAfter(lines, i + 1);
            }
            i++;
        }
        return "";
    }

    private static String extractFenceAfter(List<String> lines, int start) {
        int i = start;
        while (i < lines.size() && lines.get(i).trim().isEmpty()) {
            i++;
        }
        if (i >= lines.size()) {
            return "";
        }
        if (lines.get(i).trim().startsWith("```")) {
            StringBuilder content = new StringBuilder();
            i++;
            while (i < lines.size()) {
                if (lines.get(i).trim().startsWith("```")) {
                    break;
                }
                if (!content.isEmpty()) {
                    content.append('\n');
                }
                content.append(lines.get(i));
                i++;
            }
            return content.toString();
        }
        StringBuilder content = new StringBuilder();
        while (i < lines.size()) {
            String core = stripHeadingMarks(lines.get(i).trim());
            if (H3_INPUT.matcher(core).matches() || H3_OUTPUT.matcher(core).matches()) {
                break;
            }
            if (!content.isEmpty()) {
                content.append('\n');
            }
            content.append(lines.get(i));
            i++;
        }
        return content.toString().trim();
    }

    private static String renderSampleSection(Sample sample) {
        return """
                ### 输入 #%d
                
                ```
                %s
                ```
                
                ### 输出 #%d
                
                ```
                %s
                ```""".formatted(
                sample.index(),
                sample.inputBlock() == null ? "" : sample.inputBlock().strip(),
                sample.index(),
                sample.outputBlock() == null ? "" : sample.outputBlock().strip()
        ).strip();
    }

    private static String extractDataRange(String hints) {
        if (!StringUtils.hasText(hints)) {
            return "";
        }
        Pattern p = Pattern.compile("(\\*\\*【数据范围】\\*\\*[\\s\\S]*?)(?=\\n\\n|$)", Pattern.MULTILINE);
        Matcher m = p.matcher(hints);
        if (m.find()) {
            return m.group(1).trim();
        }
        if (hints.contains("数据范围") || hints.contains("\\le") || hints.contains("≤")) {
            return hints.trim();
        }
        return "";
    }

    private static String rebuildDocument(String generated,
                                          Map<String, String> h2,
                                          Map<String, String> sampleSections,
                                          String dataRangeHint) {
        List<String> lines = List.of(generated.split("\n", -1));
        String title = "# 题面";
        for (String line : lines) {
            if (line.trim().startsWith("#")) {
                title = line.trim();
                break;
            }
        }

        StringBuilder out = new StringBuilder(title).append("\n\n");

        appendSection(out, "题目背景", h2.get("题目背景"));
        appendSection(out, "题目描述", h2.get("题目描述"));
        appendSection(out, "输入格式", h2.get("输入格式"));
        appendSection(out, "输出格式", h2.get("输出格式"));

        List<String> sampleKeys = new ArrayList<>(sampleSections.keySet());
        sampleKeys.sort((a, b) -> {
            int ia = Integer.parseInt(a.replace("输入输出样例 #", ""));
            int ib = Integer.parseInt(b.replace("输入输出样例 #", ""));
            return Integer.compare(ia, ib);
        });
        for (String key : sampleKeys) {
            out.append("## ").append(key).append("\n\n");
            out.append(sampleSections.get(key)).append("\n\n");
        }

        String hints = h2.getOrDefault("说明/提示", "");
        if (StringUtils.hasText(dataRangeHint) && StringUtils.hasText(hints) && !hints.contains(dataRangeHint)) {
            hints = mergeHints(hints, dataRangeHint);
        }
        appendSection(out, "说明/提示", hints);

        return out.toString().trim();
    }

    private static String mergeHints(String hints, String dataRange) {
        if (!StringUtils.hasText(hints)) {
            return dataRange;
        }
        if (hints.contains("【数据范围】") && dataRange.contains("【数据范围】")) {
            return replaceDataRange(hints, dataRange);
        }
        return hints.trim() + "\n\n" + dataRange.trim();
    }

    private static String replaceDataRange(String hints, String dataRange) {
        Pattern p = Pattern.compile("\\*\\*【数据范围】\\*\\*[\\s\\S]*?(?=\\n\\n|$)");
        Matcher m = p.matcher(hints);
        if (m.find() && dataRange.contains("【数据范围】")) {
            return m.replaceFirst(Matcher.quoteReplacement(dataRange.trim()));
        }
        return hints;
    }

    private static void appendSection(StringBuilder out, String name, String body) {
        if (!StringUtils.hasText(body)) {
            return;
        }
        out.append("## ").append(name).append("\n\n");
        out.append(body.trim()).append("\n\n");
    }

    private static String stripHeadingMarks(String line) {
        return line.replaceFirst("^#+\\s*", "").trim();
    }
}
