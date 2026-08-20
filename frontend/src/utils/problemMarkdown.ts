/**
 * 题面 Markdown 规范化（与后端 ProblemMarkdownNormalizer 规则一致，用于预览兜底）。
 */
const H2_EXACT = new Set(['题目背景', '题目描述', '输入格式', '输出格式', '说明/提示'])
const H2_SAMPLE = /^输入输出样例\s*#\d+\s*$/
const H3_IO = /^(输入|输出)\s*#\d+\s*$/

function stripHashes(line: string) {
  return line.replace(/^#+\s*/, '').trim()
}

function isArtifact(line: string) {
  const t = line.trim()
  return t === 'code' || t === '```code' || t === 'markdown'
}

function isSectionHeading(line: string) {
  const core = stripHashes(line.trim())
  return H2_EXACT.has(core) || H2_SAMPLE.test(core) || H3_IO.test(core)
}

function enforceTitle(markdown: string, targetTitle: string) {
  const title = targetTitle.trim().startsWith('#') ? targetTitle.trim() : `# ${targetTitle.trim()}`
  if (!markdown.trim()) return title
  const lines = markdown.replace(/\r\n/g, '\n').split('\n')
  let i = 0
  while (i < lines.length && !lines[i]?.trim()) i++
  if (i >= lines.length) return title
  lines[i] = title
  return lines.join('\n').trim()
}

export function normalizeProblemMarkdown(markdown: string, targetTitle: string): string {
  if (!markdown.trim()) return enforceTitle('', targetTitle)
  const lines = markdown.replace(/\r\n/g, '\n').split('\n')
  const out: string[] = []
  let titleDone = false

  for (let i = 0; i < lines.length; i++) {
    const raw = lines[i] ?? ''
    const trimmed = raw.trim()
    if (!trimmed) {
      out.push('')
      continue
    }
    if (isArtifact(trimmed)) continue

    if (!titleDone) {
      const title = targetTitle.trim().startsWith('#') ? targetTitle.trim() : `# ${targetTitle.trim()}`
      out.push(title)
      titleDone = true
      continue
    }

    const core = stripHashes(trimmed)
    if (H2_EXACT.has(core) || H2_SAMPLE.test(core)) {
      out.push(`## ${core}`)
      continue
    }
    if (H3_IO.test(core)) {
      out.push(`### ${core}`)
      i = wrapIoBlock(lines, i, out)
      continue
    }
    if (trimmed.startsWith('```')) {
      i = copyFence(lines, i, out)
      continue
    }
    out.push(raw)
  }

  return enforceTitle(out.join('\n').trim(), targetTitle)
}

function wrapIoBlock(lines: string[], h3Index: number, out: string[]): number {
  let i = h3Index + 1
  while (i < lines.length && !(lines[i] ?? '').trim()) {
    out.push('')
    i++
  }
  if (i >= lines.length) {
    out.push('', '```', '', '```')
    return i - 1
  }
  if ((lines[i] ?? '').trim().startsWith('```')) {
    return copyFence(lines, i, out)
  }
  const content: string[] = []
  while (i < lines.length) {
    const t = (lines[i] ?? '').trim()
    if (isArtifact(t)) {
      i++
      continue
    }
    if (isSectionHeading(t)) break
    content.push(lines[i] ?? '')
    i++
  }
  out.push('', '```', ...content, '```')
  return i - 1
}

function copyFence(lines: string[], start: number, out: string[]): number {
  out.push(lines[start] ?? '')
  let i = start + 1
  while (i < lines.length) {
    out.push(lines[i] ?? '')
    if ((lines[i] ?? '').trim().startsWith('```') && i > start) return i
    i++
  }
  out.push('```')
  return i - 1
}
