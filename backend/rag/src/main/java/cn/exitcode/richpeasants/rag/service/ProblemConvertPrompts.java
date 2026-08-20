package cn.exitcode.richpeasants.rag.service;

import cn.exitcode.richpeasants.rag.dto.ProblemConvertRequest;
import org.springframework.util.StringUtils;

final class ProblemConvertPrompts {

    private ProblemConvertPrompts() {
    }

    static final double CONVERT_TEMPERATURE = 0.62;

    static final String SYSTEM_PROMPT = """
            你是 OI/ICPC 竞赛题面「换皮」编辑：在**不改变题目本质与判题结果**的前提下，按「目标标题」的主题改写题面叙事。
            
            ## 你要做的（可变）
            - 用目标标题的世界观/故事/场景，**重写** ## 题目背景、## 题目描述（及 ## 说明/提示 中的文字提示，可换说法）。
            - 把抽象数学对象包装成符合标题的设定（如「小码的暴击游戏」可用游戏、卡牌、连击等比喻），但**数学关系、变量含义、约束条件必须与原文等价**。
            - 首行 `#` 必须等于目标标题，不得保留 P 号、NOIP 等旧标题。
            
            ## 你必须原样保留的（不可变 —— 与判题结果一致）
            - ## 输入格式、## 输出格式：变量名、格式说明、顺序、含义与原文等价；若无输入写「无」则仍为「无」。
            - 所有 ## 输入输出样例 中 ### 输入 / ### 输出 代码块内容：**逐字一致**（数字、空格、换行、`* * *`、`...` 占位均不得改）。
            - **【数据范围】**及所有数值上下界（如 $n\\le 50$、$1\\le n\\le 50$）不得改变。
            
            ## 若提供了题解代码（可选补充）
            - 题解仅用于理解算法意图与变量含义，**防止换皮跑偏**。
            - 不得把题解代码写进输出题面；不得泄露具体实现步骤到题面正文。
            - 换皮后的题意必须仍能被该题解正确求解（IO、约束、目标与题解一致）。
            
            ## 输出结构
            # {目标标题}
            ## 题目背景（原题有则写，可换皮）
            ## 题目描述（必须换皮改写，禁止整段照抄原文）
            ## 输入格式
            ## 输出格式
            ## 输入输出样例 #1
            ### 输入 #1
            ```
            ...
            ```
            ### 输出 #1
            ```
            ...
            ```
            ## 说明/提示
            
            ## Markdown 与 LaTeX
            - 章节用 # / ## / ###；样例必须用 ``` 围栏，禁止写单词 code。
            - 数学公式用 $...$（行内）或 $$...$$ / \\[...\\]（块级）；复杂积分/求和等长公式用块级；百分号写 $\\%$；不等号 $\\le$ $\\ge$；省略号 $\\cdots$；乘号 $\\times$。
            - 也可使用 \\begin{equation}...\\end{equation} 或 ```math ... ``` 代码围栏包裹公式。
            - 数据范围示例：对于 $100\\%$ 的数据，$1 \\le n \\le 50$。
            - 只输出 Markdown，不要解释。
            """;

    static final String RESKIN_EXAMPLE = """
            【目标标题】小码的暴击游戏
            【原题核心】9 个数分成 3 组组成 3 个三位数，比例 1:2:3，输出所有方案。
            
            【题目描述改写示例（数学等价，叙事不同）】
            小码在暴击游戏里拿到数字牌 $1\\sim 9$ 各一张，要分成三叠组成三个三位「连击数」，且数值成 $1:2:3$……
            
            【输入格式/输出格式/样例数字】必须与原文完全一致，样例输出第一行仍是 `192 384 576`。
            """;

    static String buildUserPrompt(ProblemConvertRequest request, ProblemImmutableSections.Extracted extracted) {
        String targetTitle = request.getReferenceNickname().trim();
        StringBuilder immutableHint = new StringBuilder();
        if (StringUtils.hasText(extracted.inputFormat())) {
            immutableHint.append("\n【原文输入格式 — 输出须与此等价】\n").append(extracted.inputFormat());
        }
        if (StringUtils.hasText(extracted.outputFormat())) {
            immutableHint.append("\n\n【原文输出格式 — 输出须与此等价】\n").append(extracted.outputFormat());
        }
        if (extracted.hasSamples()) {
            immutableHint.append("\n\n【原文样例 — 代码块须逐字保留】");
            for (ProblemImmutableSections.Sample s : extracted.samples()) {
                immutableHint.append("\n样例 #").append(s.index()).append(" 输入:\\n```\\n")
                        .append(s.inputBlock()).append("\\n```");
                immutableHint.append("\n样例 #").append(s.index()).append(" 输出:\\n```\\n")
                        .append(s.outputBlock()).append("\\n```");
            }
        }
        if (StringUtils.hasText(extracted.dataRangeHint())) {
            immutableHint.append("\n\n【原文数据范围 — 数值不得改】\n").append(extracted.dataRangeHint());
        }

        String solutionHint = "";
        String solution = SolutionCodeValidator.normalize(request.getSolutionCode());
        if (StringUtils.hasText(solution)) {
            String lang = SolutionCodeValidator.summarizeLang(solution);
            solutionHint = """
                    
                    【题解代码 — 可选参考，用于防止换皮跑偏】
                    - 仅作算法意图与变量语义锚定；**禁止**把代码写进输出题面。
                    - 换皮后的题意必须仍可被下述题解正确求解。
                    %s
                    ---
                    %s
                    ---
                    """.formatted(
                    StringUtils.hasText(lang) ? ("语言提示：" + lang) : "语言：未标注（请从代码自行判断）",
                    solution);
        }

        return """
                【目标标题】（首行 # 必须完全一致）
                %s
                
                【换皮示例】
                %s
                %s
                %s
                
                【待转换原题】
                ---
                %s
                ---
                
                请输出完整 Markdown：
                1. 题目背景、题目描述按「%s」主题改写，但算法本质不变；
                2. 输入格式、输出格式、样例代码块、数据范围与原文保持一致；
                3. 首行必须是 # %s。
                """.formatted(
                targetTitle,
                RESKIN_EXAMPLE,
                immutableHint,
                solutionHint,
                request.getOriginalText().trim(),
                targetTitle,
                targetTitle);
    }
}
