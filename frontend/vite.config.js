import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    base: env.VITE_BASE_PATH || '/',
    plugins: [react()],
    server: { port: 5173 },
    test: {
      environment: 'jsdom',
      setupFiles: './src/test/setup.js',
      coverage: {
        provider: 'v8',
        reporter: ['text', 'html'],
        include: ['src/api/**/*.js', 'src/hooks/**/*.js', 'src/utils/**/*.js'],
        thresholds: { lines: 91, statements: 91, functions: 91, branches: 80 }
      }
    }
  }
})
