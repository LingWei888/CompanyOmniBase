import { prepareOIMarkdown } from '../src/utils/oiMarkdownMath.ts'

const raw = String.raw`\iiint_{\Omega} \left( \frac{\partial P}{\partial x} + \frac{\partial Q}{\partial y} + \frac{\partial R}{\partial z} \right) \mathrm{d}V
=
\oiint_{\partial \Omega} \left( P \cos\alpha + Q \cos\beta + R \cos\gamma \right) \mathrm{d}S`

const out = prepareOIMarkdown(raw)
console.log(out.startsWith('$$') && out.endsWith('$$') ? 'wrapped OK' : 'wrap FAIL')
console.log(out.includes('\\oiint') ? 'oiint kept' : 'oiint lost')
