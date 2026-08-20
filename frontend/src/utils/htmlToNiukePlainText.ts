/** 将导出 HTML 转为粘贴友好的纯文本（Unicode 上下标，避免 sup 被拆行） */

const SUPERSCRIPT: Record<string, string> = {
  '0': '⁰',
  '1': '¹',
  '2': '²',
  '3': '³',
  '4': '⁴',
  '5': '⁵',
  '6': '⁶',
  '7': '⁷',
  '8': '⁸',
  '9': '⁹',
  '+': '⁺',
  '-': '⁻',
  '=': '⁼',
  '(': '⁽',
  ')': '⁾',
  'n': 'ⁿ',
  'i': 'ⁱ',
  'a': 'ᵃ',
  'b': 'ᵇ',
  'c': 'ᶜ',
  'd': 'ᵈ',
  'e': 'ᵉ',
  'x': 'ˣ',
  'y': 'ʸ',
}

const SUBSCRIPT: Record<string, string> = {
  '0': '₀',
  '1': '₁',
  '2': '₂',
  '3': '₃',
  '4': '₄',
  '5': '₅',
  '6': '₆',
  '7': '₇',
  '8': '₈',
  '9': '₉',
  '+': '₊',
  '-': '₋',
  '=': '₌',
  '(': '₍',
  ')': '₎',
  'a': 'ₐ',
  'e': 'ₑ',
  'i': 'ᵢ',
  'j': 'ⱼ',
  'k': 'ₖ',
  'n': 'ₙ',
  'x': 'ₓ',
}

function toScript(text: string, map: Record<string, string>): string {
  let out = ''
  for (const ch of text) {
    out += map[ch] ?? ch
  }
  return out
}

function walkPlain(node: Node): string {
  if (node.nodeType === Node.TEXT_NODE) {
    return (node.textContent ?? '').replace(/\u200b/g, '')
  }
  if (node.nodeType !== Node.ELEMENT_NODE) return ''

  const el = node as HTMLElement
  const tag = el.tagName.toLowerCase()

  if (tag === 'sup') {
    return toScript(el.textContent ?? '', SUPERSCRIPT)
  }
  if (tag === 'sub') {
    return toScript(el.textContent ?? '', SUBSCRIPT)
  }
  if (tag === 'br') return '\n'
  if (tag === 'p' || tag === 'div' || tag === 'section' || tag === 'h1' || tag === 'h2'
    || tag === 'h3' || tag === 'h4' || tag === 'h5' || tag === 'h6') {
    const inner = Array.from(el.childNodes).map(walkPlain).join('')
    return `${inner}\n\n`
  }
  if (tag === 'li') {
    return `- ${Array.from(el.childNodes).map(walkPlain).join('').trim()}\n`
  }

  return Array.from(el.childNodes).map(walkPlain).join('')
}

export function htmlToNiukePlainText(html: string): string {
  if (typeof document === 'undefined') {
    return html.replace(/<[^>]+>/g, '')
  }
  const box = document.createElement('div')
  box.innerHTML = html
  return walkPlain(box)
    .replace(/\u200b/g, '')
    .replace(/\n{3,}/g, '\n\n')
    .replace(/[ \t]+\n/g, '\n')
    .trim()
}

export function wrapNiukeExportHtml(body: string): string {
  return `<!DOCTYPE html><html><head><meta charset="utf-8"></head><body>${body}</body></html>`
}
