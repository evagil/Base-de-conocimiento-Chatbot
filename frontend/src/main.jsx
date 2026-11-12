import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { CssBaseline, ThemeProvider } from '@mui/material'
import App from './app/App.jsx'
import { AuthProvider } from './auth/AuthContext.jsx'
import { theme } from './app/theme.js'
import './styles/global.css'

createRoot(document.getElementById('root')).render(
  <StrictMode><ThemeProvider theme={theme}><CssBaseline/><AuthProvider><BrowserRouter basename={import.meta.env.BASE_URL}><App/></BrowserRouter></AuthProvider></ThemeProvider></StrictMode>
)
