import { useEffect } from 'react'
import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Link from '@tiptap/extension-link'
import { Box, Divider, IconButton, Paper, Tooltip } from '@mui/material'
import { FormatBold, FormatItalic, FormatListBulleted, FormatListNumbered, Code, Link as LinkIcon, Undo, Redo } from '@mui/icons-material'
import DOMPurify from 'dompurify'
import { cleanPastedHtml, cleanPastedText } from '../utils/pasteContent.js'

export default function RichTextEditor({ value = '', onChange, onPasteImages, label = 'Contenido', compact = false }) {
  const editor = useEditor({ extensions: [StarterKit, Link.configure({ openOnClick: false })], content: value, editorProps: { attributes: { 'aria-label': label, spellcheck: 'true' } }, onUpdate: ({ editor: e }) => onChange(e.getHTML()) })
  useEffect(() => { if (editor) { const current = compact ? DOMPurify.sanitize(editor.getHTML(), { ALLOWED_TAGS: ['strong', 'b', 'em', 'i', 's', 'strike', 'code'], ALLOWED_ATTR: [] }) : editor.getHTML(); if (value !== current) editor.commands.setContent(value || '') } }, [editor, value, compact])
  if (!editor) return null
  const normalizeEdges = () => {
    const cleaned = cleanPastedHtml(editor.getHTML())
    if (cleaned && cleaned !== editor.getHTML()) editor.commands.setContent(cleaned)
  }
  const insertPastedContent = content => {
    const chain = editor.chain().focus()
    if (editor.isEmpty) chain.clearContent()
    chain.insertContent(content).run()
    normalizeEdges()
  }
  const paste = async event => {
    const clipboard = event.clipboardData
    if (!clipboard) return
    const html = clipboard.getData('text/html')
    const text = cleanPastedText(clipboard.getData('text/plain'))
    const imageFiles = Array.from(clipboard.items || []).filter(item => item.kind === 'file' && item.type.startsWith('image/')).map(item => item.getAsFile()).filter(Boolean)
    if (imageFiles.length && onPasteImages) {
      event.preventDefault(); event.stopPropagation(); onPasteImages(imageFiles)
      if (text.trim()) insertPastedContent({ type: 'text', text })
      return
    }
    const dataImages = Array.from(new DOMParser().parseFromString(html || '', 'text/html').querySelectorAll('img[src^="data:image/"]'))
    if (dataImages.length && onPasteImages) {
      event.preventDefault(); event.stopPropagation()
      const files = await Promise.all(dataImages.map(async (image, index) => { const blob = await fetch(image.src).then(r => r.blob()); return new File([blob], `imagen-pegada-${index + 1}.${blob.type.split('/')[1] || 'png'}`, { type: blob.type }) }))
      onPasteImages(files)
      if (text.trim()) insertPastedContent({ type: 'text', text })
      return
    }
    if (!html && !text) return
    event.preventDefault(); event.stopPropagation()
    if (html) insertPastedContent(cleanPastedHtml(html))
    else insertPastedContent({ type: 'text', text })
  }
  const link = () => { const href = window.prompt('Dirección del enlace'); if (href) editor.chain().focus().extendMarkRange('link').setLink({ href }).run() }
  const action = (title, icon, command, active) => <Tooltip title={title}><IconButton size="small" color={active ? 'primary' : 'default'} onClick={command}>{icon}</IconButton></Tooltip>
  return <Box><Box component="label" sx={{ display: 'block', fontSize: 12, color: 'text.secondary', mb: .5 }}>{label}</Box><Paper variant="outlined" sx={{ overflow: 'hidden' }}>
    <Box sx={{ px: 1, py: .5, bgcolor: 'grey.50', display: 'flex', flexWrap: 'wrap' }}>
      {action('Negrita', <FormatBold/>, () => editor.chain().focus().toggleBold().run(), editor.isActive('bold'))}
      {action('Cursiva', <FormatItalic/>, () => editor.chain().focus().toggleItalic().run(), editor.isActive('italic'))}
      {!compact && action('Lista', <FormatListBulleted/>, () => editor.chain().focus().toggleBulletList().run(), editor.isActive('bulletList'))}
      {!compact && action('Lista numerada', <FormatListNumbered/>, () => editor.chain().focus().toggleOrderedList().run(), editor.isActive('orderedList'))}
      {action('Código', <Code/>, () => compact?editor.chain().focus().toggleCode().run():editor.chain().focus().toggleCodeBlock().run(), editor.isActive(compact?'code':'codeBlock'))}
      {!compact && action('Enlace', <LinkIcon/>, link, editor.isActive('link'))}
      {action('Deshacer', <Undo/>, () => editor.chain().focus().undo().run())}{action('Rehacer', <Redo/>, () => editor.chain().focus().redo().run())}
    </Box><Divider/><EditorContent editor={editor} className={compact?'rich-editor rich-editor-compact':'rich-editor'} onClick={()=>editor.commands.focus()} onPasteCapture={paste}/></Paper></Box>
}
