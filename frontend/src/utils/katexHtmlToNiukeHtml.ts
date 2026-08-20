/** 将 KaTeX 渲染 DOM 转为可粘贴的简单 HTML（sup/sub + 符号） */

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
}

const KATEX_MARKUP_RE = /(?:^|\s)(?:katex|katex-html|katex-base|katex-display|katex-strut|msupsub|mord|mathnormal|mbin|mrel|mopen|mclose|mpunct|vlist|mtight|pstrut|mspace|minner|mop)(?:\s|$)/

export function isKatexMarkupElement(el: Element): boolean {
  if (!(el instanceof HTMLElement)) return false
  if (typeof el.className !== 'string') return false
  return KATEX_MARKUP_RE.test(` ${el.className} `)
}

function shouldSkip(el: HTMLElement): boolean {
  return el.classList.contains('katex-strut')
    || el.classList.contains('mspace')
    || el.classList.contains('pstrut')
    || el.classList.contains('vlist-s')
    || el.classList.contains('vlist-r')
    || el.classList.contains('vlist')
    || el.classList.contains('vlist-t')
    || el.classList.contains('vlist-t2')
    || el.classList.contains('katex-skip')
    || el.classList.contains('katex-skip-fallback')
    || el.classList.contains('katex-sizing')
    || el.classList.contains('reset-size6')
    || el.classList.contains('size3')
}

function scriptContent(el: Element): string {
  const mtight = el.querySelector(':scope .mtight, .mtight')
  if (mtight?.textContent) return mtight.textContent.trim()
  return (el.textContent ?? '').replace(/\s+/g, '').trim()
}

function isSubScript(el: Element): boolean {
  return !!el.querySelector('.vlist-t2')
}

function convertElement(el: HTMLElement): string {
  if (shouldSkip(el)) return ''

  if (el.classList.contains('msupsub')) {
    const content = scriptContent(el)
    if (!content) return ''
    return isSubScript(el)
      ? `<sub>${escapeHtml(content)}</sub>`
      : `<sup>${escapeHtml(content)}</sup>`
  }

  if (el.classList.contains('mord') && el.querySelector('.msupsub')) {
    let out = ''
    for (const child of el.childNodes) {
      if (child instanceof HTMLElement && child.classList.contains('msupsub')) {
        out += convertElement(child)
      } else {
        out += convertNode(child)
      }
    }
    return out
  }

  if (el.classList.contains('mbin') || el.classList.contains('mrel')) {
    const text = (el.textContent ?? '').trim()
    return text ? ` ${escapeHtml(text)} ` : ''
  }

  if (el.classList.contains('mopen') || el.classList.contains('mclose') || el.classList.contains('mpunct')) {
    return escapeHtml(el.textContent ?? '')
  }

  if (
    (el.classList.contains('mord') || el.classList.contains('mathnormal') || el.classList.contains('mop'))
    && !el.querySelector('.msupsub')
  ) {
    return escapeHtml((el.textContent ?? '').trim())
  }

  if (el.classList.contains('katex-base')) {
    let out = ''
    for (const child of el.childNodes) {
      out += convertNode(child)
    }
    return out
  }

  let out = ''
  for (const child of el.childNodes) {
    out += convertNode(child)
  }
  return out
}

function convertNode(node: Node): string {
  if (node.nodeType === Node.TEXT_NODE) {
    return escapeHtml(node.textContent ?? '')
  }
  if (node instanceof HTMLElement) {
    return convertElement(node)
  }
  return ''
}

function convertContainer(container: Element): string {
  let out = ''
  for (const child of container.childNodes) {
    if (child instanceof HTMLElement && child.classList.contains('katex-base')) {
      out += convertElement(child)
      continue
    }
    out += convertNode(child)
  }
  return out.replace(/\s{2,}/g, ' ').trim()
}

export function katexHtmlToNiukeHtml(katexRoot: Element): string {
  if (katexRoot.classList.contains('katex-base')) {
    const parent = katexRoot.parentElement
    if (parent && parent.querySelector(':scope > .katex-base')) {
      return convertContainer(parent)
    }
    return convertElement(katexRoot)
  }

  const htmlEl = katexRoot.classList.contains('katex-html')
    ? katexRoot
    : katexRoot.querySelector('.katex-html') ?? katexRoot

  return convertContainer(htmlEl)
}

function unwrapElement(el: Element) {
  const parent = el.parentNode
  if (!parent) return
  while (el.firstChild) {
    parent.insertBefore(el.firstChild, el)
  }
  parent.removeChild(el)
}

/** 在 DOM 上就地清理残留的 KaTeX 结构（处理复制片段、部分选中等情况） */
export function stripKatexDomInPlace(root: HTMLElement) {
  let guard = 0
  while (guard++ < 32) {
    const msupsubs = root.querySelectorAll('.msupsub')
    if (msupsubs.length === 0) break

    msupsubs.forEach((el) => {
      if (!root.contains(el)) return
      const content = scriptContent(el)
      if (!content) {
        el.remove()
        return
      }
      const node = document.createElement(isSubScript(el) ? 'sub' : 'sup')
      node.textContent = content
      el.replaceWith(node)
    })
  }

  root.querySelectorAll('.mbin, .mrel').forEach((el) => {
    if (!root.contains(el)) return
    const text = (el.textContent ?? '').trim()
    el.replaceWith(document.createTextNode(text ? ` ${text} ` : ''))
  })

  const unwrapClasses = [
    'katex-strut',
    'mspace',
    'pstrut',
    'vlist-s',
    'vlist-r',
    'vlist',
    'vlist-t',
    'vlist-t2',
    'katex-sizing',
    'reset-size6',
    'size3',
    'katex-base',
    'katex-html',
    'katex-display',
    'katex-display-block',
    'katex',
    'mord',
    'mathnormal',
    'mopen',
    'mclose',
    'mpunct',
    'mop',
    'minner',
    'mtight',
  ]

  for (let pass = 0; pass < 8; pass++) {
    let changed = false
    unwrapClasses.forEach((className) => {
      root.querySelectorAll(`.${className}`).forEach((el) => {
        if (!root.contains(el)) return
        if (el.querySelector('.msupsub')) return
        unwrapElement(el)
        changed = true
      })
    })
    if (!changed) break
  }

  root.querySelectorAll('[aria-hidden="true"]').forEach((el) => {
    if (el.classList.contains('katex-html') || el.closest('.katex')) {
      unwrapElement(el)
    }
  })
}

export function findKatexClusterRoot(el: Element): Element {
  if (el.classList.contains('katex') || el.classList.contains('katex-html')) return el

  let node: Element = el
  while (node.parentElement) {
    const parent = node.parentElement
    if (parent.classList.contains('katex') || parent.classList.contains('katex-html')) {
      return parent
    }
    const bases = parent.querySelectorAll(':scope > .katex-base')
    if (bases.length > 0 && Array.from(parent.childNodes).every((child) => {
      if (child.nodeType === Node.TEXT_NODE) return !(child.textContent ?? '').trim()
      return child instanceof HTMLElement && isKatexMarkupElement(child)
    })) {
      node = parent
      continue
    }
    break
  }
  return node
}

export function rootHasKatexMarkup(root: HTMLElement): boolean {
  return !!root.querySelector('.katex, .katex-html, .katex-base, .msupsub, .katex-display, .katex-display-block')
}
