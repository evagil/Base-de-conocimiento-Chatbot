import { expect, it } from 'vitest'
import { cleanPastedHtml, cleanPastedText } from './pasteContent.js'
it('keeps code whitespace, lists, links and interior spacing', () => {
  expect(cleanPastedHtml('<pre><code>\n  código\n</code></pre>')).toBe('<pre><code>\n  código\n</code></pre>')
  expect(cleanPastedHtml('<ul><li><strong>Uno</strong></li><li>Dos</li></ul>')).toContain('<li><strong>Uno</strong></li>')
  expect(cleanPastedHtml('<p><a href="https://example.com">Enlace</a></p>')).toContain('href="https://example.com"')
  expect(cleanPastedText('\n  primero\n\nsegundo\n')).toBe('  primero\n\nsegundo')
})
it('cleans empty fragments and unsafe HTML', () => {
  expect(cleanPastedHtml('<!-- fragment --><p><br></p>')).toBe('')
  expect(cleanPastedHtml('<p onclick="alert(1)">Texto</p><script>alert(1)</script>')).toBe('<p>Texto</p>')
})
