/** 可选题解代码格式校验（与后端规则对齐） */

const MIN_LEN = 24
const MAX_LEN = 80_000

const FENCE_LANG =
  /```\s*(c\+\+|cpp|cc|cxx|c|java|python|py|go|rust|rs|javascript|js|typescript|ts|csharp|cs|kotlin|kt|pascal|pas|php|ruby|rb|swift|scala|haskell|hs|lua|r|matlab|sql|bash|sh|powershell|ps1)\b/i

const CODE_SIGNAL =
  /(#include\b|using\s+namespace\b|int\s+main\s*\(|void\s+main\s*\(|public\s+class\b|public\s+static\s+void\s+main\b|def\s+\w+\s*\(|class\s+\w+\s*[:({]|import\s+[\w.]+|from\s+[\w.]+\s+import\b|cin\s*>>|cout\s*<<|scanf\s*\(|printf\s*\(|System\.out|BufferedReader|String\[\]|fn\s+main\s*\(|package\s+main\b|func\s+main\s*\(|console\.log\s*\(|function\s+\w+\s*\(|const\s+\w+\s*=|let\s+\w+\s*=|\breturn\b|\bwhile\s*\(|\bfor\s*\()/i

const PROBLEM_SIGNAL =
  /(##\s*题目描述|##\s*输入格式|##\s*输出格式|##\s*输入输出样例|【数据范围】|时间限制|空间限制|Problem\s*Description)/i

export function normalizeSolutionCode(raw: string | null | undefined): string {
  if (!raw) return ''
  return raw.replace(/\r\n/g, '\n').trim()
}

export function validateSolutionCode(raw: string | null | undefined): string | null {
  const text = normalizeSolutionCode(raw)
  if (!text) return null

  if (text.length < MIN_LEN) {
    return '题解代码过短，请粘贴完整题解（建议包含可运行代码）'
  }
  if (text.length > MAX_LEN) {
    return '题解代码过长，请精简后重试'
  }

  const fenceCount = (text.match(/```/g) ?? []).length
  if (fenceCount % 2 !== 0) {
    return '题解代码中的 Markdown 代码围栏未闭合，请检查 ``` 是否成对'
  }

  const hasLangFence = FENCE_LANG.test(text)
  const hasCodeSignal = CODE_SIGNAL.test(text)
  const looksBraced = text.includes('{') && text.includes('}')
  const looksIndentedBlock = text.split('\n').filter((l) => l.startsWith('    ') || l.startsWith('\t')).length >= 3

  if (!hasLangFence && !hasCodeSignal && !looksBraced && !looksIndentedBlock) {
    return '未能识别为题解代码。请粘贴带语言标记的代码块（如 ```cpp）或含 main/class/def 等的源码'
  }

  const problemHits = (text.match(new RegExp(PROBLEM_SIGNAL.source, 'gi')) ?? []).length
  if (problemHits >= 2 && !hasLangFence && !hasCodeSignal) {
    return '内容更像题面而非题解代码，请只粘贴题解/标程源码'
  }

  let asciiCodeChars = 0
  for (const ch of text) {
    if ('{};()=#'.includes(ch)) asciiCodeChars++
  }
  if (asciiCodeChars < 3 && !hasLangFence) {
    return '题解代码格式不正确：缺少常见代码符号或代码围栏'
  }

  if (hasLangFence && fenceCount >= 2) {
    const bodies = [...text.matchAll(/```[^\n]*\n([\s\S]*?)```/g)]
    const anyBody = bodies.some((m) => (m[1] ?? '').trim().length >= 8)
    if (!anyBody && !hasCodeSignal) {
      return '代码围栏内内容过少，请粘贴完整题解代码'
    }
  }

  return null
}
