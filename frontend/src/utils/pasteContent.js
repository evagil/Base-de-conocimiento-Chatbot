import DOMPurify from 'dompurify'

// Clipboard HTML often contains empty paragraphs around the selected fragment.
// Only clean its edges: interior spacing and preformatted code are meaningful.
export function cleanPastedHtml(html) {
  const body = new DOMParser().parseFromString(DOMPurify.sanitize(html), 'text/html').body
  const empty = node => {
    if (node.nodeType === 3) return !node.textContent.replace(/[\s\u200b\ufeff]/g, '')
    if (node.nodeType !== 1) return true
    if (!['P', 'DIV', 'SPAN', 'BR', 'B', 'STRONG', 'I', 'EM'].includes(node.tagName)) return false
    return Array.from(node.childNodes).every(empty)
  }
  const trim = element => {
    if (['PRE', 'CODE'].includes(element.tagName)) return
    while (element.firstChild && empty(element.firstChild)) element.firstChild.remove()
    while (element.lastChild && empty(element.lastChild)) element.lastChild.remove()
    const first = element.firstElementChild, last = element.lastElementChild
    if (first) trim(first)
    if (last && last !== first) trim(last)
  }
  trim(body)
  return body.innerHTML
}

export const cleanPastedText = text => text.replace(/^(?:[^\S\r\n]*\r?\n)+|(?:\r?\n[^\S\r\n]*)+$/g, '')
