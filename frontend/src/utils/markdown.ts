import MarkdownIt from 'markdown-it'
import texmath from 'markdown-it-texmath'
import katex from 'katex'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js/lib/core'
import { prepareOIMarkdown } from './oiMarkdownMath'

import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import java from 'highlight.js/lib/languages/java'
import python from 'highlight.js/lib/languages/python'
import sql from 'highlight.js/lib/languages/sql'
import xml from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import json from 'highlight.js/lib/languages/json'
import bash from 'highlight.js/lib/languages/bash'
import shell from 'highlight.js/lib/languages/shell'
import yaml from 'highlight.js/lib/languages/yaml'
import markdownLang from 'highlight.js/lib/languages/markdown'
import plaintext from 'highlight.js/lib/languages/plaintext'
import c from 'highlight.js/lib/languages/c'
import cpp from 'highlight.js/lib/languages/cpp'
import go from 'highlight.js/lib/languages/go'
import rust from 'highlight.js/lib/languages/rust'
import kotlin from 'highlight.js/lib/languages/kotlin'
import csharp from 'highlight.js/lib/languages/csharp'

hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('ts', typescript)
hljs.registerLanguage('java', java)
hljs.registerLanguage('python', python)
hljs.registerLanguage('py', python)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('vue', xml)
hljs.registerLanguage('css', css)
hljs.registerLanguage('json', json)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('shell', shell)
hljs.registerLanguage('sh', shell)
hljs.registerLanguage('yaml', yaml)
hljs.registerLanguage('yml', yaml)
hljs.registerLanguage('markdown', markdownLang)
hljs.registerLanguage('md', markdownLang)
hljs.registerLanguage('plaintext', plaintext)
hljs.registerLanguage('text', plaintext)
hljs.registerLanguage('c', c)
hljs.registerLanguage('cpp', cpp)
hljs.registerLanguage('c++', cpp)
hljs.registerLanguage('go', go)
hljs.registerLanguage('golang', go)
hljs.registerLanguage('rust', rust)
hljs.registerLanguage('kotlin', kotlin)
hljs.registerLanguage('csharp', csharp)
hljs.registerLanguage('cs', csharp)

const COPY_BUTTON = [
  '<button type="button" class="md-code-copy" title="复制代码" aria-label="复制代码">',
  '<svg class="md-code-copy-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">',
  '<rect x="9" y="9" width="13" height="13" rx="2"></rect>',
  '<path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>',
  '</svg>',
  '<svg class="md-code-copied-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">',
  '<path d="M20 6L9 17l-5-5"></path>',
  '</svg>',
  '</button>',
].join('')

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function highlightCode(code: string, lang: string) {
  try {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  } catch {
    return escapeHtml(code)
  }
}

function renderCodeBlock(code: string, lang: string) {
  const label = lang || 'code'
  return [
    '<div class="md-code-block">',
    '<div class="md-code-header">',
    `<span class="md-code-lang">${escapeHtml(label)}</span>`,
    COPY_BUTTON,
    '</div>',
    `<pre class="hljs"><code class="language-${escapeHtml(label)}">${highlightCode(code, lang)}</code></pre>`,
    '</div>',
  ].join('')
}

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
})

md.use(texmath, {
  engine: katex,
  // dollars: $ / $$；brackets: \( \) / \[ \]；beg_end: \begin{...}；gitlab: ```math
  delimiters: ['dollars', 'brackets', 'beg_end', 'gitlab'],
  katexOptions: {
    throwOnError: false,
    strict: 'ignore',
    trust: false,
  },
})

md.renderer.rules.fence = (tokens, idx) => {
  const token = tokens[idx]
  const info = token.info ? md.utils.unescapeAll(token.info).trim() : ''
  const lang = info.split(/\s+/)[0] || ''
  // 兜底：未预处理的 latex/math 围栏仍渲染为公式而非代码
  if (/^(?:latex|math|tex)$/i.test(lang)) {
    const body = token.content.trim()
    try {
      const html = katex.renderToString(body, {
        displayMode: true,
        throwOnError: false,
        strict: 'ignore',
        trust: false,
      })
      return `<div class="katex-display-block">${html}</div>`
    } catch {
      return renderCodeBlock(token.content, lang)
    }
  }
  return renderCodeBlock(token.content, lang)
}

md.renderer.rules.heading_open = (tokens, idx) => {
  const level = tokens[idx].tag
  return `<${level} class="md-h ${level}">`
}

md.renderer.rules.code_inline = (tokens, idx) => {
  return `<code class="md-inline-code">${escapeHtml(tokens[idx].content)}</code>`
}

const PURIFY_OPTS: DOMPurify.Config = {
  USE_PROFILES: { html: true, svg: true },
  ALLOW_DATA_ATTR: true,
  ADD_TAGS: [
    'button',
    'svg',
    'path',
    'rect',
    'span',
    'math',
    'semantics',
    'mrow',
    'mi',
    'mo',
    'mn',
    'msup',
    'msub',
    'mfrac',
    'mspace',
    'mtext',
    'mtable',
    'mtr',
    'mtd',
    'annotation',
  ],
  ADD_ATTR: [
    'class',
    'type',
    'title',
    'aria-label',
    'aria-hidden',
    'viewBox',
    'width',
    'height',
    'fill',
    'stroke',
    'stroke-width',
    'stroke-linecap',
    'stroke-linejoin',
    'd',
    'x',
    'y',
    'rx',
    'style',
    'xmlns',
    'encoding',
    'display',
    'data-tex',
  ],
}

function isLineStart(text: string, index: number) {
  return index === 0 || text[index - 1] === '\n'
}

const FENCE_LANGS = [
  'javascript',
  'typescript',
  'plaintext',
  'markdown',
  'python',
  'golang',
  'kotlin',
  'csharp',
  'c++',
  'c#',
  'java',
  'json',
  'html',
  'yaml',
  'yml',
  'bash',
  'shell',
  'rust',
  'text',
  'cpp',
  'css',
  'xml',
  'vue',
  'sql',
  'md',
  'py',
  'ts',
  'js',
  'sh',
  'go',
  'cs',
  'c',
].sort((a, b) => b.length - a.length)

const GLUED_AFTER_LANG =
  /^(public|private|protected|import|include|package|class|interface|enum|def|func|fn|pub|int|void|using|from|const|let|var|struct|return|#|\{|<|>)/i

function readIdentEnd(text: string, start: number): number {
  if (start >= text.length) return start
  const slice = text.slice(start)
  if (/^c\+\+/i.test(slice)) return start + 3
  if (/^c#/i.test(slice)) return start + 2
  let k = start
  while (k < text.length && /[a-zA-Z0-9_+-]/.test(text[k])) {
    k++
  }
  return k
}

function readFenceLangEnd(text: string, start: number): number {
  const identEnd = readIdentEnd(text, start)
  const ident = text.slice(start, identEnd)
  const lower = ident.toLowerCase()
  for (const lang of FENCE_LANGS) {
    if (lower === lang) return identEnd
    if (lower.startsWith(lang) && ident.length > lang.length) {
      const rest = ident.slice(lang.length)
      if (GLUED_AFTER_LANG.test(rest)) {
        return start + lang.length
      }
    }
  }
  return identEnd
}

/**
 * 仅修模型把围栏粘在同一行的情况，不猜测列表/加粗/标题。
 * ```cpp#include → ```cpp\n#include
 * ```**要点** → ```\n\n**要点**
 */
export function repairGluedFences(source: string): string {
  let text = source.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  let out = ''
  let i = 0
  let inFence = false

  while (i < text.length) {
    if (text.startsWith('```', i)) {
      if (out.length > 0 && out[out.length - 1] !== '\n') {
        out += '\n'
      }
      if (!inFence) {
        const langEnd = readFenceLangEnd(text, i + 3)
        out += text.slice(i, langEnd)
        inFence = true
        if (langEnd < text.length && text[langEnd] !== '\n') {
          out += '\n'
        }
        i = langEnd
      } else {
        out += '```'
        i += 3
        while (i < text.length && (text[i] === ' ' || text[i] === '\t')) i++
        inFence = false
        if (i < text.length && text[i] !== '\n') {
          out += '\n\n'
        }
      }
      continue
    }
    out += text[i]
    i++
  }
  return out
}

/**
 * 流式虚拟补全：只补未闭合围栏 / 加粗 / 删除线 / 行内代码，
 * 不改已闭合代码块内部（避免把 C 注释当成 Markdown）。
 */
export function healIncompleteMarkdown(source: string): string {
  let text = repairGluedFences(source)
  if (!text) return text

  let inFence = false
  for (let i = 0; i < text.length; i++) {
    if (!text.startsWith('```', i) || !isLineStart(text, i)) continue
    if (!inFence) {
      inFence = true
      i += 2
      continue
    }
    let k = i + 3
    while (k < text.length && (text[k] === ' ' || text[k] === '\t')) k++
    if (k >= text.length || text[k] === '\n') {
      inFence = false
      i = k
    }
  }
  if (inFence) {
    if (!text.endsWith('\n')) text += '\n'
    text += '```'
  }

  let bold = false
  let strike = false
  let inlineCode = false
  inFence = false
  for (let i = 0; i < text.length; i++) {
    if (text.startsWith('```', i) && isLineStart(text, i)) {
      if (!inFence) {
        inFence = true
        i += 2
        continue
      }
      let k = i + 3
      while (k < text.length && (text[k] === ' ' || text[k] === '\t')) k++
      if (k >= text.length || text[k] === '\n') {
        inFence = false
        i = k - 1
      }
      continue
    }
    if (inFence) continue
    if (text.startsWith('**', i)) {
      bold = !bold
      i++
      continue
    }
    if (text.startsWith('~~', i)) {
      strike = !strike
      i++
      continue
    }
    if (text[i] === '`') {
      inlineCode = !inlineCode
    }
  }

  if (inlineCode) text += '`'
  if (bold) text += '**'
  if (strike) text += '~~'
  return text
}

function decorateCitations(html: string): string {
  return html.replace(/(<pre[\s\S]*?<\/pre>)|\[(\d+)\]/gi, (whole, pre?: string, num?: string) => {
    if (pre) return pre
    return `<span class="md-cite">[${num}]</span>`
  })
}

/** 在 DOMPurify 可能剥离 MathML/annotation 前，把 TeX 存到 data-tex */
function preserveKatexTex(html: string): string {
  if (typeof document === 'undefined') return html
  const box = document.createElement('div')
  box.innerHTML = html
  box.querySelectorAll('.katex').forEach((node) => {
    const el = node as HTMLElement
    if (el.dataset.tex) return
    const ann = el.querySelector('annotation[encoding="application/x-tex"], annotation[encoding="application/x-katex"]')
    const tex = ann?.textContent?.trim()
    if (tex) el.dataset.tex = tex
  })
  return box.innerHTML
}

/** @deprecated 使用 healIncompleteMarkdown */
export function normalizeMarkdown(source: string): string {
  return healIncompleteMarkdown(source)
}

export function stabilizeStreamingMarkdown(source: string): string {
  return healIncompleteMarkdown(source)
}

export function renderMarkdown(source: string, _options?: { streaming?: boolean }): string {
  const raw = source == null ? '' : String(source)
  if (!raw.trim()) return ''
  const prepared = healIncompleteMarkdown(prepareOIMarkdown(raw))
  const html = md.render(prepared)
  return DOMPurify.sanitize(decorateCitations(preserveKatexTex(html)), PURIFY_OPTS) as string
}
