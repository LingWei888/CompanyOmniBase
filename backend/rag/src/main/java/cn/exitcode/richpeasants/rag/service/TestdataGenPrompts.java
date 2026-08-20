package cn.exitcode.richpeasants.rag.service;

import cn.exitcode.richpeasants.rag.dto.TestdataGenRequest;
import org.springframework.util.StringUtils;

final class TestdataGenPrompts {

    private TestdataGenPrompts() {
    }

    static final double GEN_TEMPERATURE = 0.35;

    static final String SYSTEM_PROMPT = """
            你是竞赛题目「测试数据生成器」编写助手。根据题面（及可选的题解）写出一份**完整可运行的 Python 3 脚本**，供用户在本机一次性生成成对测试数据。
            
            ## 输出要求（必须遵守）
            - **只输出 Python 源码本身**，不要 Markdown 解释，不要用 ```python 围栏包裹。
            - 脚本须可直接 `python gen.py` 运行（或带参数运行）。
            - 使用 `argparse` 或等价方式支持至少：
              - `--seed` 随机种子（默认固定整数）
              - `--out` 输出目录（默认 `testdata`）
              - `--count` 生成组数（默认如 20）
            - **必须成对写出答案数据**：对 `--count N`，在输出目录生成：
              `1.in`、`1.out`、`2.in`、`2.out`、…、`N.in`、`N.out`
              （从 **1** 开始编号，不要用 `01.in` 这类前导零；不要只生成 .in）。
            - 每个 `k.out` 必须是对应 `k.in` 的**正确标准输出**（符合题面输出格式），禁止空文件或占位说明代替答案。
            - 脚本内须实现可调用的参考解法（如 `solve(input_text) -> output_text`），用它根据生成的输入写出 `.out`；不要让用户再手动跑标程。
            - **不要**在脚本里自动下载网络资源；**不要**假设沙箱外路径。
            
            ## 数据质量
            - 严格遵守题面输入格式与数据范围；非法数据禁止出现。
            - 必须覆盖典型边界：最小/最大、全相同、递增/递减、单测/多测（若题面有）、特殊构造（空图、链式、星形等按题意相关则加入）。
            - 区分规模档位：至少包含一小批「小数据」便于手测，以及贴近上限的「大数据」若干组（注意：生成与求解过程应可在普通电脑几十秒内完成；大数据档不要卡死）。
            - 使用 `random.Random(seed)`，保证同种子可复现。
            
            ## 若提供了题解代码
            - 用于理解算法与 IO 约定，并据此在脚本中写出**等价的 Python 参考解**（可简化但须正确）。
            - **不要**原样粘贴非 Python 题解进生成器；也不要依赖外部编译运行其它语言。
            
            ## 代码风格
            - 标准库即可（`argparse`/`os`/`pathlib`/`random`/`math`/`sys`/`io` 等）。
            - 顶部用简短 docstring 说明：例如 `python gen.py --out testdata --count 20 --seed 1` 会得到 1.in/1.out … 20.in/20.out。
            - 函数拆分清晰：`gen_case` / `solve` / `write_pair` 等。
            """;

    static String buildUserPrompt(TestdataGenRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("【原题全文】\n---\n")
                .append(request.getOriginalText().trim())
                .append("\n---\n");

        String solution = SolutionCodeValidator.normalize(request.getSolutionCode());
        if (StringUtils.hasText(solution)) {
            String lang = SolutionCodeValidator.summarizeLang(solution);
            sb.append("\n【题解代码 — 可选参考】\n");
            if (StringUtils.hasText(lang)) {
                sb.append("语言提示：").append(lang).append('\n');
            }
            sb.append("请据此实现脚本内的 Python 参考解 `solve`，用于写出正确的 .out。\n---\n")
                    .append(solution)
                    .append("\n---\n");
        } else {
            sb.append("\n（未提供题解）请根据题意自行实现正确的 Python 参考解 `solve`，保证 .out 正确。\n");
        }

        sb.append("""
                
                请直接输出完整 Python 3 生成脚本（不要解释、不要代码围栏）。
                要求：运行后按编号成对生成 1.in/1.out … N.in/N.out。
                """);
        return sb.toString();
    }

    /** 去掉模型可能包上的 markdown 围栏 */
    static String stripToPython(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String text = raw.replace("\r\n", "\n").trim();
        if (text.startsWith("```")) {
            int firstNl = text.indexOf('\n');
            if (firstNl > 0) {
                text = text.substring(firstNl + 1);
            }
            int fence = text.lastIndexOf("```");
            if (fence >= 0) {
                text = text.substring(0, fence);
            }
            text = text.trim();
        }
        return text;
    }
}
