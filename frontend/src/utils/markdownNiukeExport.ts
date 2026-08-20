/**
 * 题面 HTML 导出：直接从 Markdown 中的 $...$ / $$...$$ 转 sup/sub，
 * 不依赖 KaTeX 渲染 DOM（避免复制出 katex-base 等内部结构）。
 */

import MarkdownIt from 'markdown-it'
import { prepareOIMarkdown } from './oiMarkdownMath'
import { latexToNiukeHtml } from './latexToNiukeHtml'

const md = new MarkdownIt({ html: true, linkify: true, breaks: false })

const BLOCK_MASK = '\uE000'
const INLINE_MASK = '\uE001'

function maskInline(blocks: string[], html: string): string {
  const id = blocks.length
  blocks.push(html)
  return `${INLINE_MASK}${id}${INLINE_MASK}`
}

function maskBlock(blocks: string[], html: string): string {
  const id = blocks.length
  blocks.push(html)
  return `\n\n${BLOCK_MASK}${id}${BLOCK_MASK}\n\n`
}

function wrapDisplayMath(body: string): string {
  return `<div style="text-align:center;margin:1em 0">${body}</div>`
}

function maskMathInMarkdown(source: string): { text: string; blocks: string[] } {
  const blocks: string[] = []
  let text = source.replace(/\r\n/g, '\n')

  text = text.replace(/```(?:latex|math|tex)\s*\n([\s\S]*?)```/g, (_, body) => {
    return maskBlock(blocks, wrapDisplayMath(latexToNiukeHtml(body.trim())))
  })

  text = text.replace(/\$\$([\s\S]+?)\$\$/g, (_, tex) => {
    return maskBlock(blocks, wrapDisplayMath(latexToNiukeHtml(tex.trim())))
  })

  text = text.replace(/\\\[([\s\S]+?)\\\]/g, (_, tex) => {
    return maskBlock(blocks, wrapDisplayMath(latexToNiukeHtml(tex.trim())))
  })

  text = text.replace(/\$([^$\n]+?)\$/g, (_, tex) => {
    return maskInline(blocks, `<span>${latexToNiukeHtml(tex.trim())}</span>`)
  })

  text = text.replace(/\\\(([\s\S]+?)\\\)/g, (_, tex) => {
    return maskInline(blocks, `<span>${latexToNiukeHtml(tex.trim())}</span>`)
  })

  text = collapseInlineMaskParagraphBreaks(text)

  return { text, blocks }
}

/** LLM 常把行内公式单独成行；在 Markdown 渲染前合并，避免每个 $...$ 变成独立 <p> */
function collapseInlineMaskParagraphBreaks(text: string): string {
  const token = `${INLINE_MASK}\\d+${INLINE_MASK}`
  let out = text
  for (let pass = 0; pass < 16; pass++) {
    const prev = out
    out = out
      .replace(new RegExp(`([^\\n${INLINE_MASK}${BLOCK_MASK}]+)\\n\\n\\s*(${token})\\s*\\n\\n([^\\n]+)`, 'g'), '$1 $2 $3')
      .replace(new RegExp(`([^\\n${INLINE_MASK}${BLOCK_MASK}]+)\\n\\n\\s*(${token})\\s*\\n\\n`, 'g'), '$1 $2 ')
      .replace(new RegExp(`\\n\\n\\s*(${token})\\s*\\n\\n([^\\n${INLINE_MASK}${BLOCK_MASK}]+)`, 'g'), ' $1 $2')
      .replace(new RegExp(`([^\\n${INLINE_MASK}${BLOCK_MASK}]+)\\n\\s*(${token})\\s*\\n([^\\n]+)`, 'g'), '$1 $2 $3')
    if (out === prev) break
  }
  return out
}

function unmask(html: string, blocks: string[]): string {
  return html
    .replace(/\uE000(\d+)\uE000/g, (_, id) => blocks[Number(id)] ?? '')
    .replace(/\uE001(\d+)\uE001/g, (_, id) => blocks[Number(id)] ?? '')
}

function mergeFragmentedParagraphs(html: string): string {
  let out = html
  for (let pass = 0; pass < 8; pass++) {
    const prev = out
    out = out
      .replace(/<p>([^<]*)<\/p>\s*<p>(<span[\s\S]*?<\/span>)<\/p>\s*<p>([^<]*)<\/p>/g, '<p>$1$2$3</p>')
      .replace(/<p>([^<]*)<\/p>\s*<p>(<span[\s\S]*?<\/span>)<\/p>/g, '<p>$1$2</p>')
      .replace(/<p>(<span[\s\S]*?<\/span>)<\/p>\s*<p>([^<]*)<\/p>/g, '<p>$1$2</p>')
      .replace(/<p>([^<]*)<\/p>\s*<p>(<span[\s\S]*?<\/span>)([^<]*)<\/p>/g, '<p>$1$2$3</p>')
      .replace(/<p>([（）])<\/p>\s*<p>(<span[\s\S]*?<\/span>)<\/p>\s*<p>([（）][^<]*)<\/p>/g, '<p>$1$2$3</p>')
    if (out === prev) break
  }
  return out
}

function cleanupExportHtml(html: string): string {
  return mergeFragmentedParagraphs(
    html
      .replace(/<p>\s*(<div style="text-align:center)/gi, '$1')
      .replace(/(<\/div>)\s*<\/p>/gi, '$1')
      .replace(/<p>\s*<\/p>/gi, '')
      .replace(/^<\/p>\s*/i, '')
      .replace(/(<br\s*\/?>\s*){2,}/gi, '<br/>')
      .replace(/<\/div>\s*<br\s*\/?>\s*/gi, '</div>')
      .replace(/\s+\n/g, '\n')
      .replace(/\n{3,}/g, '\n\n')
      .trim(),
  )
}

export function exportMarkdownToNiukeHtml(markdown: string): string {
  const prepared = prepareOIMarkdown(markdown.trim())
  if (!prepared) return ''

  const { text, blocks } = maskMathInMarkdown(prepared)
  const rendered = md.render(text)
  return cleanupExportHtml(unmask(rendered, blocks))
}
