/**
 * 从 Markdown 源码直接生成题面 HTML（不经过 KaTeX DOM，保证 sup/sub 标签正确）
 */
import { prepareOIMarkdown } from './oiMarkdownMath'
import { latexToNiukeHtml } from './latexToNiukeHtml'

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
}

function splitBlocks(source: string): string[] {
  const text = source.replace(/\r\n/g, '\n')
  const blocks: string[] = []
  let i = 0

  while (i < text.length) {
    while (i < text.length && text[i] === '\n') i++
    if (i >= text.length) break

    let inFence = false
    const start = i

    while (i < text.length) {
      const lineEnd = text.indexOf('\n', i)
      const end = lineEnd === -1 ? text.length : lineEnd
      const line = text.slice(i, end)
      if (line.trimStart().startsWith('```')) inFence = !inFence

      if (lineEnd === -1) {
        i = text.length
        break
      }
      i = lineEnd + 1
      if (!inFence && line === '') break
    }

    const raw = text.slice(start, i).replace(/\n+$/, '')
    if (raw.trim()) blocks.push(raw)
  }

  return blocks
}

function renderInline(text: string): string {
  let out = ''
  let i = 0

  while (i < text.length) {
    if (text.startsWith('\\(', i)) {
      const end = text.indexOf('\\)', i + 2)
      if (end !== -1) {
        out += latexToNiukeHtml(text.slice(i + 2, end))
        i = end + 2
        continue
      }
    }

    if (text[i] === '$' && text[i + 1] !== '$') {
      let j = i + 1
      while (j < text.length) {
        if (text[j] === '$' && text[j - 1] !== '\\') break
        j++
      }
      if (j < text.length) {
        out += latexToNiukeHtml(text.slice(i + 1, j))
        i = j + 1
        continue
      }
    }

    if (text.startsWith('**', i)) {
      const end = text.indexOf('**', i + 2)
      if (end !== -1) {
        out += `<strong>${renderInline(text.slice(i + 2, end))}</strong>`
        i = end + 2
        continue
      }
    }

    if (text[i] === '`') {
      const end = text.indexOf('`', i + 1)
      if (end !== -1) {
        out += `<code>${escapeHtml(text.slice(i + 1, end))}</code>`
        i = end + 1
        continue
      }
    }

    out += escapeHtml(text[i] ?? '')
    i++
  }

  return out
}

function renderBlock(block: string): string {
  const trimmed = block.trim()
  if (!trimmed) return ''

  if (trimmed.startsWith('```')) {
    const match = trimmed.match(/^```([^\n]*)\n([\s\S]*?)```$/)
    if (match) {
      const lang = match[1]?.trim().toLowerCase() ?? ''
      const body = match[2] ?? ''
      if (/^(?:math|latex|tex)$/i.test(lang)) {
        return `<p style="text-align:center;margin:1em 0">${latexToNiukeHtml(body.trim())}</p>`
      }
      return `<pre><code>${escapeHtml(body.replace(/\n$/, ''))}</code></pre>`
    }
  }

  if (/^\\\[/.test(trimmed)) {
    const inner = trimmed.replace(/^\\\[+\n?/, '').replace(/\n?\\]+$/, '')
    return `<p style="text-align:center;margin:1em 0">${latexToNiukeHtml(inner)}</p>`
  }

  if (/^\$\$/.test(trimmed)) {
    const inner = trimmed.replace(/^\$\$\n?/, '').replace(/\n?\$\$$/, '')
    return `<p style="text-align:center;margin:1em 0">${latexToNiukeHtml(inner)}</p>`
  }

  const heading = trimmed.match(/^(#{1,6})\s+(.+)$/)
  if (heading) {
    const level = heading[1].length
    const tag = `h${level}`
    return `<${tag}>${renderInline(heading[2] ?? '')}</${tag}>`
  }

  if (/^[-*]\s+/.test(trimmed)) {
    const items = trimmed.split('\n').filter((l) => /^[-*]\s+/.test(l.trim()))
    const lis = items.map((l) => `<li>${renderInline(l.trim().replace(/^[-*]\s+/, ''))}</li>`).join('')
    return `<ul>${lis}</ul>`
  }

  const paragraph = trimmed.replace(/\n+/g, ' ')
  return `<p>${renderInline(paragraph)}</p>`
}

export function markdownToNiukeHtml(source: string): string {
  if (!source.trim()) return ''
  const prepared = prepareOIMarkdown(source.replace(/\r\n/g, '\n'))
  return splitBlocks(prepared).map(renderBlock).join('\n')
}
