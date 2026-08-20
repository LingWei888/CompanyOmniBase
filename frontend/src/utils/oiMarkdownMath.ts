/**
 * 竞赛题面 / 聊天 Markdown 在渲染前的数学预处理（不修改普通代码块内部）。
 */

type Segment = { fenced: boolean; text: string }

const LATEX_CMD_NAMES =
  'iiint|oiint|oiiint|iint|int|oint|frac|dfrac|partial|left|right|mathrm|mathbf|begin|end|sum|prod|sqrt|cos|sin|tan|log|ln|lim|alpha|beta|gamma|delta|Omega|omega|le|ge|ne|cdot|times|cdots|limits|overline|underline|vec|hat|bar|pm|mp|infty|nabla|Delta|forall|exists|in|notin|subset|supset|cup|cap|rightarrow|xrightarrow|Rightarrow|Leftrightarrow|text|operatorname|displaystyle|to|exp|det|dim|gcd|mod|bmod|pmod'

const LATEX_CMD = new RegExp(`\\\\(?:${LATEX_CMD_NAMES})(?![a-zA-Z])`)

const DISPLAY_CMD = /\\(?:iiint|oiint|oiiint|iint|oint|sum|prod|begin\{(?:cases|align|aligned|alignat|gather|equation|array|matrix|pmatrix|bmatrix|vmatrix|Vmatrix))(?![a-zA-Z])/

const CJK_RE = /[\u4e00-\u9fff]/
const UNICODE_MATH_RE = /[∭∯∬∮∫∂∇∞→≤≥≠αβγδΩωθλμπσφψ]/

function splitByFences(source: string): Segment[] {
  const lines = source.replace(/\r\n/g, '\n').split('\n')
  const segments: Segment[] = []
  let inFence = false
  let current = ''

  for (const line of lines) {
    const trimmed = line.trimStart()
    if (trimmed.startsWith('```')) {
      if (inFence) {
        current += `${line}\n`
        segments.push({ fenced: true, text: current })
        current = ''
        inFence = false
      } else {
        if (current) segments.push({ fenced: false, text: current })
        current = `${line}\n`
        inFence = true
      }
      continue
    }
    current += `${line}\n`
  }
  if (current) segments.push({ fenced: inFence, text: current })
  return segments
}

function normalizeUnicodeMath(source: string): string {
  if (!UNICODE_MATH_RE.test(source) || LATEX_CMD.test(source)) return source
  return source
    .replace(/\u2061/g, '')
    .replace(/\u200b/g, '')
    .replace(/∭/g, '\\iiint ')
    .replace(/∯/g, '\\oiint ')
    .replace(/∬/g, '\\iint ')
    .replace(/∮/g, '\\oint ')
    .replace(/∫/g, '\\int ')
    .replace(/∂/g, '\\partial ')
    .replace(/∇/g, '\\nabla ')
    .replace(/∞/g, '\\infty ')
    .replace(/→/g, '\\to ')
    .replace(/≤/g, '\\le ')
    .replace(/≥/g, '\\ge ')
    .replace(/≠/g, '\\ne ')
    .replace(/α/g, '\\alpha ')
    .replace(/β/g, '\\beta ')
    .replace(/γ/g, '\\gamma ')
    .replace(/δ/g, '\\delta ')
    .replace(/Ω/g, '\\Omega ')
    .replace(/ω/g, '\\omega ')
}

function fixPercentInMath(inner: string): string {
  return inner.replace(/(?<!\\)%/g, '\\%')
}

function fixMathSegment(inner: string): string {
  let s = normalizeUnicodeMath(inner)
  // 双重转义命令：\\log → \log（不影响单独的 \\ 换行）
  s = s.replace(/\\\\([a-zA-Z]+)/g, '\\$1')

  if (/\\begin\{/.test(s)) {
    s = s.replace(/\\\s*\n/g, '\\\\\n')
    s = fixPercentInMath(s)
    return s.replace(/[ \t]+\n/g, '\n').replace(/\n[ \t]+/g, '\n').trim()
  }

  s = s.replace(/\s*\n+\s*/g, ' ').trim()
  s = fixPercentInMath(s)
  s = s.replace(/≤/g, '\\le ').replace(/≥/g, '\\ge ').replace(/≠/g, '\\ne ')
  s = s.replace(/(?<!\\)\.\.\.(?!\.)/g, '\\cdots ')
  s = s.replace(/(\d+)\s*<=\s*([a-zA-Z]\w*)\s*<=\s*(\d+)/g, '$1 \\le $2 \\le $3')
  s = s.replace(/([a-zA-Z]\w*)\s*<=\s*(\d+)/g, '$1 \\le $2')
  s = s.replace(/(\d+)\s*<=\s*([a-zA-Z]\w*)/g, '$1 \\le $2')
  return s.replace(/\s{2,}/g, ' ').trim()
}

/** 仅大型积分/求和等升级为居中块级公式；行内 lim/frac 保持 $...$ */
function shouldUpgradeToDisplay(inner: string): boolean {
  const raw = inner.trim()
  if (!raw) return false
  if (DISPLAY_CMD.test(raw)) return true
  if (raw.length > 120) return true
  return false
}

function wrapInlineOrDisplay(inner: string): string {
  const fixed = fixMathSegment(inner)
  if (shouldUpgradeToDisplay(inner)) return `$$\n${fixed}\n$$`
  return `$${fixed}$`
}

/**
 * 将 \( \) / \[ \]（含双重转义 \\( ）统一为 $ / $$，
 * 避免被 wrapEmbeddedLatexLine 误伤，也比 brackets 定界符更稳。
 */
function normalizeBracketDelimiters(text: string): string {
  let out = text
  // 仅括号定界符的双重转义：\\( → \(（不动 \\[1em] 这类）
  out = out.replace(/\\\\([()])/g, '\\$1')
  // \[...\] → $$...$$
  out = out.replace(/\\\[([\s\S]+?)\\\]/g, (_m, inner: string) => `$$\n${fixMathSegment(inner)}\n$$`)
  // \(...\) → $...$
  out = out.replace(/\\\(([^$\n]+?)\\\)/g, (_m, inner: string) => wrapInlineOrDisplay(inner))
  return out
}

function fixInlineAndDisplayMath(text: string): string {
  let out = normalizeBracketDelimiters(text)
  // $ 独占一行包裹的多行公式 → 块级
  out = out.replace(/^\$\s*\n([\s\S]*?)\n\s*\$/gm, (_m, inner: string) => `$$\n${fixMathSegment(inner)}\n$$`)
  // 先处理 $$...$$
  out = out.replace(/\$\$([\s\S]+?)\$\$/g, (_m, inner: string) => `$$\n${fixMathSegment(inner)}\n$$`)
  // 行内 $...$：不跨行，避免吞掉正文
  out = out.replace(/(?<!\$)\$(?!\$)([^$\n]+?)\$(?!\$)/g, (_m, inner: string) => wrapInlineOrDisplay(inner))
  return out
}

function isPureLatexLine(line: string): boolean {
  const t = line.trim()
  if (!t || t.includes('$') || CJK_RE.test(t)) return false
  if (/\\[([\]()]/.test(t)) return false
  if (!/^\\/.test(t)) return false
  return LATEX_CMD.test(t)
}

function isLatexContinuationLine(line: string): boolean {
  const t = line.trim()
  if (!t || t.includes('$') || CJK_RE.test(t)) return false
  if (/^[=+\-*/.,;:]$/.test(t)) return true
  return isPureLatexLine(line)
}

function isLatexLabelLine(line: string): boolean {
  return /^\s*(?:latex|tex|math)\s*$/i.test(line)
}

function convertLatexFences(text: string): string {
  return text.replace(/```(?:latex|math|tex)\s*\n([\s\S]*?)```/g, (_m, body: string) => `\n$$\n${fixMathSegment(body)}\n$$\n`)
}

const MATH_SLOT = '\uE000M'
const MATH_SLOT_END = '\uE001'

/** 临时掩蔽已有 $$...$$，避免 wrapBareLatexBlocks 对块内 \\iiint 行二次包裹 */
function maskDisplayMath(text: string): { masked: string; blocks: string[] } {
  const blocks: string[] = []
  const masked = text.replace(/\$\$([\s\S]+?)\$\$/g, (whole) => {
    blocks.push(whole)
    return `${MATH_SLOT}${blocks.length - 1}${MATH_SLOT_END}`
  })
  return { masked, blocks }
}

function unmaskDisplayMath(text: string, blocks: string[]): string {
  return text.replace(
    new RegExp(`${MATH_SLOT}(\\d+)${MATH_SLOT_END}`, 'g'),
    (_m, idx: string) => blocks[Number(idx)] ?? '',
  )
}

/** 行内「中文：\iiint...」→ 中文 + 块级公式（不含 \( \) / \[ \] / $） */
function wrapEmbeddedLatexLine(line: string): string {
  if (line.includes('$')) return line
  if (/\\[([\]()]/.test(line)) return line
  if (line.trim().startsWith('$$')) return line
  // 前缀只允许中文/空白/标点，不允许 () 以免吃掉公式括号
  const m = line.match(/^([\u4e00-\u9fff\s：:，,。.！!？?""''+\-]*)(\\.+)$/)
  if (!m) return line
  const prefix = m[1].trimEnd()
  const latex = m[2].trim()
  if (!prefix || !CJK_RE.test(prefix)) return line
  // 必须是大型公式命令开头，避免把 \log / \cos 等行内内容误包成块
  if (!DISPLAY_CMD.test(latex)) return line
  return `${prefix}\n$$\n${fixMathSegment(latex)}\n$$`
}

/** 仅包裹「整行纯 LaTeX」的裸公式，不碰含中文或 $ 的行 */
function wrapBareLatexBlocks(text: string): string {
  const lines = text.split('\n')
  const out: string[] = []
  let i = 0

  while (i < lines.length) {
    if (isLatexLabelLine(lines[i])) {
      i++
      continue
    }

    if (!isPureLatexLine(lines[i])) {
      out.push(wrapEmbeddedLatexLine(lines[i]))
      i++
      continue
    }

    const blockLines: string[] = []
    while (i < lines.length) {
      if (isLatexLabelLine(lines[i])) {
        i++
        continue
      }
      const trimmed = lines[i].trim()
      if (!trimmed) {
        let j = i + 1
        while (j < lines.length && !lines[j].trim()) j++
        if (j < lines.length && isLatexContinuationLine(lines[j])) {
          i = j
          continue
        }
        break
      }
      if (blockLines.length === 0) {
        if (!isPureLatexLine(lines[i])) break
      } else if (!isLatexContinuationLine(lines[i])) {
        break
      }
      blockLines.push(trimmed)
      i++
    }

    const body = fixMathSegment(blockLines.join(' '))
    out.push('$$', body, '$$')
  }

  return out.join('\n')
}

function fixDataRangeLine(text: string): string {
  return text.replace(
    /^(\s*(?:\*\*【数据范围】\*\*\s*)?对于\s*)?(100\s*(?:\\%|%))\s*的数据[，,]\s*(.+)$/gm,
    (whole, prefix = '', pct: string, rest: string) => {
      const tail = rest.trim()
      if (tail.includes('$')) return whole
      const normalized = tail
        .replace(/≤/g, '\\le ')
        .replace(/≥/g, '\\ge ')
        .replace(/(\d+)\s*<=\s*([a-zA-Z]\w*)\s*<=\s*(\d+)/g, '$$1 \\le $2 \\le $3$')
        .replace(/([a-zA-Z]\w*)\s*≤\s*(\d+)/g, '$$1 \\le $2$')
      return `${prefix || ''}$${fixPercentInMath(pct)}$ 的数据，$${fixMathSegment(normalized.replace(/^\$|\$$/g, ''))}$`
    },
  )
}

export function prepareOIMarkdown(source: string): string {
  if (!source) return source
  const normalized = source.replace(/\r\n/g, '\n')
  const withMathFences = convertLatexFences(normalized)
  return splitByFences(withMathFences)
    .map((seg) => {
      if (seg.fenced) return seg.text
      let t = fixInlineAndDisplayMath(seg.text)
      const { masked, blocks } = maskDisplayMath(t)
      t = wrapBareLatexBlocks(masked)
      t = unmaskDisplayMath(t, blocks)
      t = fixDataRangeLine(t)
      return t
    })
    .join('')
}
