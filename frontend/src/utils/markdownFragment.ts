/** 从渲染后的 DOM 提取 HTML / Markdown 片段（供题面粘贴用） */

import { exportMarkdownToNiukeHtml } from './markdownNiukeExport'
import { latexToNiukeHtml } from './latexToNiukeHtml'
import {
  findKatexClusterRoot,
  katexHtmlToNiukeHtml,
  rootHasKatexMarkup,
  stripKatexDomInPlace,
} from './katexHtmlToNiukeHtml'
import { htmlToNiukePlainText, wrapNiukeExportHtml } from './htmlToNiukePlainText'

const BLOCK_SELECTOR = 'h1,h2,h3,h4,h5,h6,p,hr,ul,ol,blockquote,table,.md-code-block,.katex-display,.katex-display-block,section'

export type MarkdownBlockRange = { start: number; end: number; text: string }

export function splitMarkdownBlocks(source: string): MarkdownBlockRange[] {
  if (!source) return []
  const text = source.replace(/\r\n/g, '\n')
  const blocks: MarkdownBlockRange[] = []
  let i = 0

  while (i < text.length) {
    while (i < text.length && text[i] === '\n') i++
    if (i >= text.length) break

    const start = i
    let inFence = false

    while (i < text.length) {
      const lineEnd = text.indexOf('\n', i)
      const end = lineEnd === -1 ? text.length : lineEnd
      const line = text.slice(i, end)
      const trimmed = line.trimStart()
      if (trimmed.startsWith('```')) inFence = !inFence

      if (lineEnd === -1) {
        i = text.length
        break
      }

      i = lineEnd + 1
      if (!inFence && line === '') break
    }

    const raw = text.slice(start, i).replace(/\n+$/, '')
    if (raw.trim()) {
      blocks.push({ start, end: start + raw.length, text: raw })
    }
  }

  return blocks
}

export function getMarkdownBlockRanges(source: string): MarkdownBlockRange[] {
  return splitMarkdownBlocks(source)
}

function getTopLevelBlocks(container: HTMLElement): HTMLElement[] {
  return Array.from(container.children).filter((el): el is HTMLElement => {
    if (!(el instanceof HTMLElement)) return false
    return el.matches(BLOCK_SELECTOR) || el.classList.contains('md-code-block')
  })
}

export function annotateMarkdownBlocks(container: HTMLElement, blocks: MarkdownBlockRange[]) {
  // 清除旧标注，避免 DOM 结构变化后残留导致选区越界
  container.querySelectorAll('[data-md-start], [data-md-end]').forEach((el) => {
    el.removeAttribute('data-md-start')
    el.removeAttribute('data-md-end')
  })
  const domBlocks = getTopLevelBlocks(container)
  const n = Math.min(domBlocks.length, blocks.length)
  for (let i = 0; i < n; i++) {
    domBlocks[i].dataset.mdStart = String(blocks[i].start)
    domBlocks[i].dataset.mdEnd = String(blocks[i].end)
  }
}

/** 真正内容相交（边界相切不算），避免「选到标题前」仍命中下一节 */
function nodeContentOverlapsRange(node: Node, range: Range): boolean {
  const nodeRange = document.createRange()
  try {
    nodeRange.selectNodeContents(node)
  } catch {
    return false
  }
  return (
    range.compareBoundaryPoints(Range.START_TO_END, nodeRange) < 0
    && range.compareBoundaryPoints(Range.END_TO_START, nodeRange) > 0
  )
}

function elementsOverlappingRange(range: Range, root: HTMLElement): HTMLElement[] {
  const result: HTMLElement[] = []
  root.querySelectorAll('[data-md-start]').forEach((el) => {
    if (nodeContentOverlapsRange(el, range)) result.push(el as HTMLElement)
  })
  return result
}

function sliceFromOverlappingBlocks(range: Range, root: HTMLElement, sourceMarkdown: string): string | null {
  const touched = elementsOverlappingRange(range, root)
  if (touched.length === 0) return null
  let start = Number.POSITIVE_INFINITY
  let end = 0
  for (const el of touched) {
    start = Math.min(start, Number(el.dataset.mdStart ?? start))
    end = Math.max(end, Number(el.dataset.mdEnd ?? end))
  }
  if (!Number.isFinite(start) || end <= start) return null
  return sourceMarkdown.slice(start, end)
}

function removeExportNoise(root: HTMLElement) {
  root.querySelectorAll('.md-code-header, .md-code-copy').forEach((el) => el.remove())
}

function removeKatexAccessibilityClones(root: HTMLElement) {
  root.querySelectorAll('.katex-mathml, math, semantics, annotation').forEach((el) => el.remove())
}

function extractLatexFromKatex(el: Element): string | null {
  const katexEl = el.classList.contains('katex')
    ? el
    : (el.closest('.katex') ?? el.querySelector('.katex'))
  if (katexEl instanceof HTMLElement) {
    const dataTex = katexEl.getAttribute('data-tex')?.trim() ?? katexEl.dataset.tex?.trim()
    if (dataTex) return dataTex
    const ann = katexEl.querySelector('annotation[encoding="application/x-tex"], annotation[encoding="application/x-katex"]')
    const tex = ann?.textContent?.trim()
    if (tex) return tex
  }

  const eqn = el.querySelector('eqn')
  const eqnTex = eqn?.textContent?.trim()
  if (eqnTex) return eqnTex

  return null
}

function resolveKatexElement(el: Element): Element | null {
  if (el.classList.contains('katex') || el.classList.contains('katex-html') || el.classList.contains('katex-base')) {
    return el
  }
  return el.querySelector('.katex, .katex-html, .katex-base')
}

function katexToExportHtml(el: Element): string {
  const katexRoot = el.closest('.katex') ?? el.querySelector('.katex') ?? el
  const latex = extractLatexFromKatex(katexRoot)
  // 有 data-tex / annotation 时一律走 LaTeX→简易 HTML，避免拷出 katex-base 等内部 DOM
  if (latex) {
    return latexToNiukeHtml(latex)
  }

  const htmlPart = katexRoot.querySelector('.katex-html') ?? katexRoot
  return katexHtmlToNiukeHtml(htmlPart)
}

function extractTexFromKatex(el: Element): string | null {
  const latex = extractLatexFromKatex(el)
  if (latex) return latex
  const katexEl = resolveKatexElement(el)
  if (!katexEl) return null
  return katexHtmlToNiukeHtml(katexEl) || null
}


function replaceElementHtml(el: Element, html: string, display: boolean) {
  if (display) {
    const box = document.createElement('div')
    box.setAttribute('style', 'text-align:center;margin:1em 0')
    box.innerHTML = html
    el.replaceWith(box)
    return
  }
  const span = document.createElement('span')
  span.innerHTML = html
  el.replaceWith(span)
}

function convertKatexToSimpleHtml(root: HTMLElement) {
  const displayRoots = Array.from(
    root.querySelectorAll('section, .katex-display, .katex-display-block'),
  ).filter((el) => el.querySelector('.katex, .katex-html, eqn'))

  for (const el of displayRoots) {
    // clone / 离线 DOM 的 isConnected 为 false，改用 root.contains
    if (!root.contains(el)) continue
    const html = katexToExportHtml(el)
    if (!html) continue
    replaceElementHtml(el, html, true)
  }

  root.querySelectorAll('.katex').forEach((el) => {
    if (!root.contains(el)) return
    const html = katexToExportHtml(el)
    if (!html) {
      el.remove()
      return
    }
    replaceElementHtml(el, html, false)
  })

  convertOrphanedKatexClusters(root)
}

function convertOrphanedKatexClusters(root: HTMLElement) {
  let guard = 0
  while (guard++ < 64) {
    const cluster = root.querySelector('.katex-html, .katex-base')
    if (!cluster) break

    const clusterRoot = findKatexClusterRoot(cluster)
    if (!root.contains(clusterRoot)) continue

    const display = clusterRoot.classList.contains('katex-display')
      || !!clusterRoot.closest('.katex-display, .katex-display-block, section')
      || clusterRoot.tagName.toLowerCase() === 'section'

    const html = katexToExportHtml(clusterRoot)
    if (!html) {
      stripKatexDomInPlace(clusterRoot as HTMLElement)
      clusterRoot.replaceWith(document.createTextNode(clusterRoot.textContent ?? ''))
      continue
    }
    replaceElementHtml(clusterRoot, html, display)
  }
}

function finalizeKatexExport(root: HTMLElement) {
  convertOrphanedKatexClusters(root)
  stripKatexDomInPlace(root)

  if (rootHasKatexMarkup(root)) {
    stripKatexDomInPlace(root)
  }
}

function unwrapEmptyTexmathSections(root: HTMLElement) {
  root.querySelectorAll('section').forEach((section) => {
    const eqn = section.querySelector('eqn')
    if (!eqn) return
    const tex = eqn.textContent?.trim()
    if (!tex) return
    replaceElementHtml(section, latexToNiukeHtml(tex), true)
  })
}

function stripDataAttributes(root: HTMLElement) {
  root.querySelectorAll('*').forEach((el) => {
    for (const attr of Array.from(el.attributes)) {
      if (attr.name.startsWith('data-md-')) el.removeAttribute(attr.name)
    }
  })
}

function stripExportClasses(root: HTMLElement) {
  root.querySelectorAll('.md-h').forEach((el) => {
    el.removeAttribute('class')
  })
}

function normalizeExportRoot(root: HTMLElement) {
  removeExportNoise(root)
  // 先按 data-tex 转成简单 HTML，再清理残留；切勿先 strip，否则会丢掉 data-tex
  convertKatexToSimpleHtml(root)
  unwrapEmptyTexmathSections(root)
  finalizeKatexExport(root)
  stripDataAttributes(root)
  stripExportClasses(root)
  removeKatexAccessibilityClones(root)
  root.querySelectorAll('.md-code-block pre code').forEach((codeEl) => {
    const code = codeEl.textContent ?? ''
    const pre = codeEl.closest('pre')
    if (pre) {
      pre.textContent = code
      pre.removeAttribute('class')
    }
  })
  root.querySelectorAll('code.md-inline-code').forEach((el) => {
    el.removeAttribute('class')
  })
}

export function prepareExportHtml(root: HTMLElement): string {
  const clone = root.cloneNode(true) as HTMLElement
  normalizeExportRoot(clone)
  return clone.innerHTML.trim()
}

/** 从 Markdown 重新渲染并导出（比读预览 DOM 更可靠） */
export function markdownToExportHtml(markdown: string): string {
  return exportMarkdownToNiukeHtml(markdown)
}

/** 对任意 HTML 字符串做最后一次 KaTeX 残留清理 */
export function processExportHtml(html: string): string {
  if (typeof document === 'undefined') return html.trim()
  const box = document.createElement('div')
  box.innerHTML = html
  normalizeExportRoot(box)
  return box.innerHTML.trim()
}

function katexToLatex(el: Element): string {
  const ann = el.querySelector('annotation[encoding="application/x-tex"], annotation[encoding="application/x-katex"]')
  const tex = ann?.textContent?.trim()
  if (tex) {
    const display = el.classList.contains('katex-display')
      || !!el.closest('.katex-display, .katex-display-block')
      || tex.includes('\n')
    return display ? `$$\n${tex}\n$$` : `$${tex}$`
  }
  const htmlPart = el.querySelector('.katex-html') ?? el
  const plain = htmlPart.textContent?.trim() ?? ''
  return plain ? `$${plain}$` : ''
}

function nodeToMarkdown(node: Node): string {
  if (node.nodeType === Node.TEXT_NODE) {
    return (node.textContent ?? '').replace(/\s+/g, ' ')
  }
  if (node.nodeType !== Node.ELEMENT_NODE) return ''

  const el = node as HTMLElement
  const tag = el.tagName.toLowerCase()

  if (el.classList.contains('katex-mathml') || tag === 'math' || tag === 'annotation' || tag === 'semantics') {
    return ''
  }

  if (el.classList.contains('md-code-block')) {
    const code = el.querySelector('pre code')?.textContent ?? el.querySelector('pre')?.textContent ?? ''
    return `\`\`\`\n${code.replace(/\n$/, '')}\n\`\`\`\n\n`
  }

  if (el.classList.contains('katex') || el.classList.contains('katex-display') || el.classList.contains('katex-display-block')) {
    return katexToLatex(el)
  }

  if (tag === 'h1') return `# ${el.textContent?.trim() ?? ''}\n\n`
  if (tag === 'h2') return `## ${el.textContent?.trim() ?? ''}\n\n`
  if (tag === 'h3') return `### ${el.textContent?.trim() ?? ''}\n\n`
  if (tag === 'h4') return `#### ${el.textContent?.trim() ?? ''}\n\n`
  if (tag === 'h5') return `##### ${el.textContent?.trim() ?? ''}\n\n`
  if (tag === 'h6') return `###### ${el.textContent?.trim() ?? ''}\n\n`
  if (tag === 'hr') return '---\n\n'
  if (tag === 'br') return '\n'

  if (tag === 'strong' || tag === 'b') return `**${el.textContent ?? ''}**`
  if (tag === 'em' || tag === 'i') return `*${el.textContent ?? ''}*`
  if (tag === 'del' || tag === 's') return `~~${el.textContent ?? ''}~~`
  if (tag === 'code' && el.classList.contains('md-inline-code')) return `\`${el.textContent ?? ''}\``

  if (tag === 'a') {
    const href = el.getAttribute('href') ?? ''
    const text = el.textContent ?? ''
    return href ? `[${text}](${href})` : text
  }

  if (tag === 'li') {
    const parent = el.parentElement?.tagName.toLowerCase()
    const prefix = parent === 'ol' ? '1. ' : '- '
    const inner = Array.from(el.childNodes).map(nodeToMarkdown).join('').trim()
    return `${prefix}${inner}\n`
  }

  if (tag === 'ul' || tag === 'ol') {
    return `${Array.from(el.children).map(nodeToMarkdown).join('')}\n`
  }

  if (tag === 'blockquote') {
    const inner = Array.from(el.childNodes).map(nodeToMarkdown).join('').trim()
    return `${inner.split('\n').map((l) => `> ${l}`).join('\n')}\n\n`
  }

  if (tag === 'p' || tag === 'div' || tag === 'section') {
    const inner = Array.from(el.childNodes).map(nodeToMarkdown).join('').trim()
    return inner ? `${inner}\n\n` : ''
  }

  if (tag === 'table') {
    return `${el.outerHTML}\n\n`
  }

  return Array.from(el.childNodes).map(nodeToMarkdown).join('')
}

export function htmlFragmentToMarkdown(root: HTMLElement | DocumentFragment): string {
  const wrapper = document.createElement('div')
  wrapper.appendChild(root.cloneNode(true))
  normalizeExportRoot(wrapper)
  const md = Array.from(wrapper.childNodes).map(nodeToMarkdown).join('')
  return md.replace(/\n{3,}/g, '\n\n').trim()
}

export function rangeToHtml(range: Range, root?: HTMLElement, sourceMarkdown = ''): string {
  // 优先从 Markdown 重渲染：选区含 KaTeX 时 DOM 文本对不上，不能因此退回预览 DOM
  if (root && sourceMarkdown) {
    const slice = sliceFromOverlappingBlocks(range, root, sourceMarkdown)
    if (slice?.trim()) {
      return markdownToExportHtml(slice)
    }
  }

  const fragment = range.cloneContents()
  const wrapper = document.createElement('div')
  wrapper.appendChild(fragment)
  return prepareExportHtml(wrapper)
}

export function rangeToMarkdown(range: Range, root: HTMLElement, source = ''): string {
  if (source) {
    const slice = sliceFromOverlappingBlocks(range, root, source)
    if (slice?.trim()) {
      return slice.trim()
    }
  }

  const fragment = range.cloneContents()
  const wrapper = document.createElement('div')
  wrapper.appendChild(fragment)
  return htmlFragmentToMarkdown(wrapper)
}

export function elementExportHtml(el: HTMLElement): string {
  return prepareExportHtml(el)
}

export { htmlToNiukePlainText, wrapNiukeExportHtml } from './htmlToNiukePlainText'

export async function copyHtmlSource(html: string) {
  const source = processExportHtml(html.trim())
  if (!source) throw new Error('empty html')

  // HTML 源码编辑器通常读取 text/plain，内容应为带标签的 HTML 字符串
  if (navigator.clipboard?.write && typeof ClipboardItem !== 'undefined') {
    try {
      await navigator.clipboard.write([
        new ClipboardItem({
          'text/plain': new Blob([source], { type: 'text/plain' }),
          'text/html': new Blob([wrapNiukeExportHtml(source)], { type: 'text/html' }),
        }),
      ])
      return
    } catch {
      // fallback
    }
  }

  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(source)
    return
  }

  throw new Error('clipboard unavailable')
}

export async function copyTextPlain(text: string) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }
  const area = document.createElement('textarea')
  area.value = text
  area.setAttribute('readonly', '')
  area.style.position = 'fixed'
  area.style.left = '-9999px'
  document.body.appendChild(area)
  area.select()
  document.execCommand('copy')
  document.body.removeChild(area)
}

export function getSelectionInRoot(root: HTMLElement): Range | null {
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0 || sel.isCollapsed) return null
  const range = sel.getRangeAt(0)
  if (!root.contains(range.commonAncestorContainer)) return null
  return range
}
