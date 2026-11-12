import { alpha, createTheme } from '@mui/material/styles'

const brand = { main: '#326f91', dark: '#265571', light: '#4083a5', contrastText: '#ffffff', 50: '#eef5f9' }

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: brand,
    secondary: brand,
    success: brand,
    info: brand,
    warning: brand,
    error: brand,
    text: { primary: brand.main, secondary: '#526d7d' },
    background: { default: '#f4f7f9', paper: '#ffffff' },
    divider: '#dce5eb',
  },
  shape: { borderRadius: 12 },
  typography: { fontFamily: 'Inter, Segoe UI, sans-serif', h4: { fontWeight: 750 }, h5: { fontWeight: 720 }, h6: { fontWeight: 700 }, button: { textTransform: 'none', fontWeight: 650 } },
  components: {
    MuiButton: { defaultProps: { disableElevation: true } },
    MuiIconButton: { defaultProps: { color: 'primary' } },
    MuiListItemIcon: { styleOverrides: { root: { color: brand.main } } },
    MuiPaper: { styleOverrides: { root: { backgroundImage: 'none' } } },
    MuiCssBaseline: { styleOverrides: {
      '.rich-editor .tiptap:focus': { boxShadow: `inset 0 0 0 2px ${alpha(brand.main, .25)}` },
    } },
  },
})
