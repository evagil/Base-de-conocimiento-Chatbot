import '@testing-library/jest-dom/vitest'

globalThis.ResizeObserver = class { observe() {} unobserve() {} disconnect() {} }
Object.defineProperty(window, 'matchMedia', { writable: true, value: () => ({ matches: false, addEventListener() {}, removeEventListener() {} }) })
Object.defineProperty(URL, 'createObjectURL', { writable: true, value: () => 'blob:preview' })
Object.defineProperty(URL, 'revokeObjectURL', { writable: true, value: () => {} })
