/** 将 OI 题面 LaTeX 转为可粘贴的 HTML（sup/sub + 常见环境） */

const LATEX_SYMBOLS: Record<string, string> = {
  times: '×',
  cdot: '·',
  le: '≤',
  leq: '≤',
  ge: '≥',
  geq: '≥',
  ne: '≠',
  neq: '≠',
  pm: '±',
  mp: '∓',
  infty: '∞',
  ldots: '…',
  cdots: '⋯',
  dots: '…',
  vdots: '⋮',
  ddots: '⋱',
  quad: '&nbsp;&nbsp;',
  qquad: '&nbsp;&nbsp;&nbsp;&nbsp;',
  ',': ' ',
  ';': ' ',
  '!': '',
  sum: '∑',
  prod: '∏',
  int: '∫',
  iiint: '∭',
  oiint: '∮',
  iint: '∬',
  oint: '∮',
  partial: '∂',
  nabla: '∇',
  lim: 'lim',
  log: 'log',
  ln: 'ln',
  sin: 'sin',
  cos: 'cos',
  tan: 'tan',
  max: 'max',
  min: 'min',
  mod: 'mod',
  bmod: 'mod',
  pmod: 'mod',
  '%': '%',
  left: '',
  right: '',
  Big: '',
  big: '',
  bigg: '',
  Bigg: '',
  dfrac: '',
  displaystyle: '',
  to: '→',
  rightarrow: '→',
  leftarrow: '←',
  Rightarrow: '⇒',
  Leftarrow: '⇐',
}

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
}

function normalizeRow(row: string): string {
  return row
    .replace(/\\+\s*$/g, '')
    .replace(/\s*\n+\s*/g, ' ')
    .replace(/\s{2,}/g, ' ')
    .trim()
}

function splitLatexRows(body: string): string[] {
  return body
    .replace(/\r\n/g, '\n')
    .replace(/\\+\s*\n/g, '\n')
    .split(/\\\\+|\n(?=\s*(?:\\begin\{|[a-zA-Z\\]))/)
    .map(normalizeRow)
    .filter(Boolean)
}

function collapseBreaks(html: string): string {
  return html.replace(/(<br\s*\/?>\s*){2,}/gi, '<br/>')
}

function parseLatex(tex: string): string {
  return new LatexToHtmlParser(tex).parse()
}

/** 按行数生成左侧花括号：⎧ / ⎪ / ⎨ / ⎩，保证从上到下完整包住 */
function leftBracePieces(n: number): string[] {
  if (n <= 0) return []
  if (n === 1) return ['{']
  if (n === 2) return ['⎧', '⎩']
  if (n === 3) return ['⎧', '⎨', '⎩']
  const pieces = Array.from({ length: n }, () => '⎪')
  pieces[0] = '⎧'
  pieces[n - 1] = '⎩'
  pieces[Math.floor((n - 1) / 2)] = '⎨'
  return pieces
}

/**
 * cases：每行一个括号片段，用 table 保证与公式行一一对齐。
 * 避免单字符 ⎧ + 多行公式时只显示上半截。
 */
function renderCases(rows: string[]): string {
  const pieces = leftBracePieces(rows.length)
  const trs = rows
    .map((row, i) => {
      const brace = pieces[i] ?? '⎪'
      const cells = row.split('&').map((cell) => parseLatex(cell.trim()))
      const body = cells.join('<span style="display:inline-block;width:1.2em"></span>')
      return [
        '<tr>',
        `<td style="padding:0 0.28em 0 0;vertical-align:middle;font-size:1.55em;line-height:1;text-align:center;font-family:'Cambria Math',Cambria,'Times New Roman',serif">${brace}</td>`,
        `<td style="padding:0.06em 0;vertical-align:middle;text-align:left;line-height:1.85">${body}</td>`,
        '</tr>',
      ].join('')
    })
    .join('')
  return `<table cellspacing="0" cellpadding="0" style="display:inline-table;border-collapse:collapse;border:none;vertical-align:middle;margin:0.2em 0;text-align:left"><tbody>${trs}</tbody></table>`
}

function renderAligned(rows: string[]): string {
  const lines = collapseBreaks(rows.map((row) => {
    const cells = row.split('&').map((cell) => parseLatex(cell.trim()))
    return cells.join('<span style="display:inline-block;width:1.5em"></span>')
  }).join('<br/>'))
  return `<div style="text-align:center;line-height:1.85">${lines}</div>`
}

function renderMatrix(rows: string[], left: string, right: string): string {
  const parsed = rows.map((row) => row.split('&').map((cell) => parseLatex(cell.trim())))
  const table = parsed.map((cells) =>
    `<tr>${cells.map((c) => `<td style="padding:0.15em 0.35em;text-align:center">${c}</td>`).join('')}</tr>`,
  ).join('')
  return [
    '<span style="display:inline-block;vertical-align:middle">',
    `<span style="padding:0 0.15em">${escapeHtml(left)}</span>`,
    `<table style="display:inline-block;vertical-align:middle;border-collapse:collapse;margin:0 0.1em">${table}</table>`,
    `<span style="padding:0 0.15em">${escapeHtml(right)}</span>`,
    '</span>',
  ].join('')
}

function matrixDelimiters(env: string): [string, string] {
  switch (env) {
    case 'pmatrix': return ['(', ')']
    case 'bmatrix': return ['[', ']']
    case 'Bmatrix': return ['{', '}']
    case 'vmatrix': return ['|', '|']
    case 'Vmatrix': return ['‖', '‖']
    default: return ['', '']
  }
}

class LatexToHtmlParser {
  private i = 0

  constructor(private readonly s: string) {}

  parse(): string {
    return this.parseUntil('')
  }

  private parseUntil(terminators: string): string {
    let out = ''
    while (this.i < this.s.length) {
      if (terminators.includes(this.s[this.i] ?? '')) break
      out += this.parseToken()
    }
    return out
  }

  private parseToken(): string {
    const ch = this.s[this.i]
    if (ch === '\\') {
      this.i++
      return this.parseCommand()
    }
    if (ch === '^') {
      this.i++
      return `<sup>${parseLatex(this.readArgRaw())}</sup>`
    }
    if (ch === '_') {
      this.i++
      return `<sub>${parseLatex(this.readArgRaw())}</sub>`
    }
    if (ch === '{') {
      this.i++
      const inner = this.parseUntil('}')
      this.i++
      return inner
    }
    if (ch === '&') {
      this.i++
      return '<span style="display:inline-block;width:1.5em"></span>'
    }
    this.i++
    return escapeHtml(ch ?? '')
  }

  private parseCommand(): string {
    if (this.s[this.i] === '\\') {
      this.i++
      return '<br/>'
    }
    if (this.s[this.i] === '\n' || this.s[this.i] === '\r') {
      if (this.s[this.i] === '\r' && this.s[this.i + 1] === '\n') this.i++
      this.i++
      return ' '
    }

    let cmd = ''
    if (/[a-zA-Z]/.test(this.s[this.i] ?? '')) {
      while (/[a-zA-Z]/.test(this.s[this.i] ?? '')) {
        cmd += this.s[this.i++]
      }
    } else if (this.s[this.i] != null) {
      cmd = this.s[this.i++]
    }

    switch (cmd) {
      case 'frac':
      case 'dfrac': {
        const num = this.readArgRaw()
        const den = this.readArgRaw()
        return `<sup>${parseLatex(num)}</sup>&frasl;<sub>${parseLatex(den)}</sub>`
      }
      case 'begin': {
        const envName = this.readArgRaw().trim()
        let colSpec = ''
        if (envName === 'array' && this.s[this.i] === '{') {
          colSpec = this.readArgRaw()
        }
        const body = this.readUntilEnd(envName)
        return this.renderEnvironment(envName, body, colSpec)
      }
      case 'end':
        this.readArgRaw()
        return ''
      case 'text':
      case 'mathrm':
      case 'mathbf':
      case 'mathit':
      case 'operatorname':
        return escapeHtml(this.readArgRaw())
      case 'sqrt': {
        if (this.s[this.i] === '[') {
          const end = this.s.indexOf(']', this.i)
          if (end !== -1) {
            const idx = this.s.slice(this.i + 1, end)
            this.i = end + 1
            const body = this.readArgRaw()
            return `<sup>${parseLatex(idx)}</sup>√(${parseLatex(body)})`
          }
        }
        const body = this.readArgRaw()
        return `√(${parseLatex(body)})`
      }
      default: {
        const sym = LATEX_SYMBOLS[cmd]
        if (sym !== undefined) return sym
        return escapeHtml(cmd)
      }
    }
  }

  private renderEnvironment(envName: string, body: string, _colSpec = ''): string {
    const rows = splitLatexRows(body)
    switch (envName) {
      case 'cases':
        return renderCases(rows.length ? rows : [body.trim()])
      case 'align':
      case 'aligned':
      case 'alignat':
      case 'gather':
      case 'equation':
      case 'equation*':
        return renderAligned(rows.length ? rows : [body.trim()])
      case 'matrix':
      case 'pmatrix':
      case 'bmatrix':
      case 'Bmatrix':
      case 'vmatrix':
      case 'Vmatrix': {
        const [left, right] = matrixDelimiters(envName)
        return renderMatrix(rows.length ? rows : [body.trim()], left, right)
      }
      case 'array':
        return renderMatrix(rows.length ? rows : [body.trim()], '', '')
      default:
        return parseLatex(body.trim())
    }
  }

  private readUntilEnd(envName: string): string {
    const start = this.i
    let depth = 0
    while (this.i < this.s.length) {
      if (this.s.startsWith('\\begin{', this.i)) {
        depth++
        this.i += 7
        while (this.i < this.s.length && this.s[this.i] !== '}') this.i++
        if (this.i < this.s.length) this.i++
        continue
      }
      if (this.s.startsWith('\\end{', this.i)) {
        const m = this.s.slice(this.i).match(/^\\end\{([^}]*)\}/)
        if (m) {
          const name = m[1] ?? ''
          if (name === envName && depth === 0) {
            const body = this.s.slice(start, this.i)
            this.i += m[0].length
            return body
          }
          if (name) depth = Math.max(0, depth - 1)
          this.i += m[0].length
          continue
        }
      }
      this.i++
    }
    return this.s.slice(start)
  }

  private readArgRaw(): string {
    this.skipSpace()
    if (this.s[this.i] === '{') {
      this.i++
      let depth = 1
      const start = this.i
      while (this.i < this.s.length && depth > 0) {
        if (this.s[this.i] === '{') depth++
        else if (this.s[this.i] === '}') depth--
        if (depth > 0) this.i++
      }
      const content = this.s.slice(start, this.i)
      this.i++
      return content
    }
    if (this.s[this.i] != null) return this.s[this.i++]
    return ''
  }

  private skipSpace() {
    while (this.i < this.s.length && (this.s[this.i] === ' ' || this.s[this.i] === '\t')) this.i++
  }
}

export function latexToNiukeHtml(tex: string): string {
  const trimmed = tex.trim()
  if (!trimmed) return ''
  return new LatexToHtmlParser(trimmed).parse()
}
