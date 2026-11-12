import DOMPurify from 'dompurify'
import RichTextEditor from './RichTextEditor.jsx'

// Keep titles inline; their existing database field admits 200 characters.
function cleanSolutionTitle(value) {
  return DOMPurify.sanitize(value || '', { ALLOWED_TAGS: ['strong', 'b', 'em', 'i', 's', 'strike', 'code'], ALLOWED_ATTR: [] })
}

export default function SolutionTitleEditor({ value, onChange, label = 'Título opcional' }) {
  return <RichTextEditor label={label} value={value} onChange={html => onChange(cleanSolutionTitle(html))} compact/>
}
