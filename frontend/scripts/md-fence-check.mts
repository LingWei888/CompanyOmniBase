import { healIncompleteMarkdown } from '../src/utils/markdown.ts'

const gluedClose = [
  '```cpp',
  'int main() { return 0; }',
  '```**要点：** `next[i]`表示前缀',
].join('\n')

const gluedOpen = 'C++实现```cpp#include <iostream>\nint main(){}\n```'

const commentFence = [
  '```cpp',
  '//字符串匹配之KMP算法',
  '#include <bits/stdc++.h>',
  '',
  '/**',
  ' * Next[j]：p[0...j-1]的最长相等真前后缀长度',
  ' */',
  'int main() {}',
  '```',
].join('\n')

const streaming = '```cpp\n#include <bits/stdc++.h>\nusing namespace std;\n/**\n * Next'

const a = healIncompleteMarkdown(gluedClose)
if (a.includes('```**')) throw new Error('close still glued')
if (!a.includes('\n\n**要点：**')) throw new Error('要点 not split after fence')

const b = healIncompleteMarkdown(gluedOpen)
if (b.includes('cpp#include')) throw new Error('open lang glued')
if (!b.includes('```cpp\n#include')) throw new Error('expected split include')

const c = healIncompleteMarkdown(commentFence)
if (c.split('\n```').length - 1 > 1) {
  // opening + closing only
}
if (!c.includes(' * Next[j]')) throw new Error('comment line lost')
const open = c.indexOf('```cpp')
const close = c.lastIndexOf('```')
const mid = c.indexOf(' * Next[j]')
if (mid < open || mid > close) throw new Error('comment escaped fence')

const s = healIncompleteMarkdown(streaming)
if (!s.trimEnd().endsWith('```')) throw new Error('streaming fence not virtually closed')

const gluedJava = healIncompleteMarkdown('## Java```javapublic class KMP {}')
if (gluedJava.includes('```javapublic')) throw new Error('java+public still glued')
if (!gluedJava.includes('```java\npublic')) throw new Error('expected java fence split before public')

console.log('OK')
