const DEFAULT_THEME_COLOR = '#165DFF'
const DEFAULT_COLOR_MODE = 'system'

type ColorMode = 'light' | 'dark' | 'system'

const THEME_COLOR_PRESETS = [
  '#165DFF',
  '#3B82F6',
  '#2563EB',
  '#0FC6C2',
  '#14B8A6',
  '#00B42A',
  '#84CC16',
  '#FF7D00',
  '#F59E0B',
  '#F53F3F',
  '#DC2626',
  '#722ED1',
  '#6366F1',
  '#EB0AA4',
  '#8D4EDA',
  '#475569',
  '#111827'
]

interface RgbColor {
  r: number
  g: number
  b: number
}

let systemThemeMediaQuery: MediaQueryList | null = null
let systemThemeListener: ((event: MediaQueryListEvent) => void) | null = null

const clamp = (value: number) => Math.min(255, Math.max(0, Math.round(value)))

export const normalizeHexColor = (value?: string) => {
  if (!value) {
    return DEFAULT_THEME_COLOR
  }

  const normalized = value.trim()
  const hex = normalized.startsWith('#') ? normalized : `#${normalized}`

  return /^#[0-9a-fA-F]{6}$/.test(hex) ? hex.toUpperCase() : DEFAULT_THEME_COLOR
}

export const normalizeColorMode = (value?: string): ColorMode => {
  if (value === 'light' || value === 'dark' || value === 'system') {
    return value
  }
  return DEFAULT_COLOR_MODE
}

const hexToRgb = (hex: string): RgbColor => {
  const normalized = normalizeHexColor(hex).slice(1)
  return {
    r: parseInt(normalized.slice(0, 2), 16),
    g: parseInt(normalized.slice(2, 4), 16),
    b: parseInt(normalized.slice(4, 6), 16)
  }
}

const rgbToString = (color: RgbColor) => `${color.r},${color.g},${color.b}`

const rgbaString = (color: RgbColor, alpha: number) => `rgba(${color.r}, ${color.g}, ${color.b}, ${alpha})`

const mixColor = (source: RgbColor, target: RgbColor, weight: number): RgbColor => ({
  r: clamp(source.r + (target.r - source.r) * weight),
  g: clamp(source.g + (target.g - source.g) * weight),
  b: clamp(source.b + (target.b - source.b) * weight)
})

const buildPalette = (baseHex: string) => {
  const base = hexToRgb(baseHex)
  const white = { r: 255, g: 255, b: 255 }
  const black = { r: 0, g: 0, b: 0 }

  const palette = [
    mixColor(base, white, 0.92),
    mixColor(base, white, 0.8),
    mixColor(base, white, 0.64),
    mixColor(base, white, 0.46),
    mixColor(base, white, 0.24),
    base,
    mixColor(base, black, 0.12),
    mixColor(base, black, 0.24),
    mixColor(base, black, 0.36),
    mixColor(base, black, 0.5)
  ]

  return { base, palette }
}

export const applyThemeColor = (themeColor: string) => {
  if (typeof document === 'undefined') {
    return
  }

  const normalized = normalizeHexColor(themeColor)
  const { base, palette } = buildPalette(normalized)
  const targets = [document.documentElement, document.body].filter(Boolean) as HTMLElement[]

  targets.forEach((target) => {
    target.style.setProperty('--app-primary-color', normalized)
    target.style.setProperty('--app-primary-rgb', rgbToString(base))
    target.style.setProperty('--link-6', rgbToString(base))
    target.style.setProperty('--color-primary-light-1', rgbaString(base, 0.12))
    target.style.setProperty('--color-primary-light-2', rgbaString(base, 0.2))
    target.style.setProperty('--color-primary-light-3', rgbaString(base, 0.32))
    target.style.setProperty('--color-primary-light-4', rgbaString(base, 0.48))

    palette.forEach((color, index) => {
      const value = rgbToString(color)
      target.style.setProperty(`--arcoblue-${index + 1}`, value)
      target.style.setProperty(`--primary-${index + 1}`, value)
    })
  })
}

const resolveSystemMode = () => {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return 'light'
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

const applySurfaceVariables = (mode: 'light' | 'dark') => {
  if (typeof document === 'undefined') {
    return
  }

  const targets = [document.documentElement, document.body].filter(Boolean) as HTMLElement[]
  const surfaceVars = mode === 'dark'
    ? {
        '--app-header-bg': '#17171A',
        '--app-header-text': '#F2F3F5',
        '--app-panel-bg': '#1C1F26',
        '--app-sider-bg': '#111318',
        '--app-content-bg': '#0F1115',
        '--app-border-color': '#2A2D35',
        '--app-muted-text': '#86909C',
        '--app-hover-bg': 'rgba(255, 255, 255, 0.06)',
        '--app-shadow': '0 2px 8px rgba(0, 0, 0, 0.35)'
      }
    : {
        '--app-header-bg': '#FFFFFF',
        '--app-header-text': '#1D2129',
        '--app-panel-bg': '#FFFFFF',
        '--app-sider-bg': '#FFFFFF',
        '--app-content-bg': '#F0F2F5',
        '--app-border-color': '#E5E6EB',
        '--app-muted-text': '#4E5969',
        '--app-hover-bg': 'rgba(0, 0, 0, 0.04)',
        '--app-shadow': '0 2px 8px rgba(0, 0, 0, 0.1)'
      }

  targets.forEach((target) => {
    Object.entries(surfaceVars).forEach(([key, value]) => {
      target.style.setProperty(key, value)
    })
  })
}

export const applyColorMode = (modeValue: string) => {
  if (typeof document === 'undefined') {
    return
  }

  const mode = normalizeColorMode(modeValue)

  if (systemThemeMediaQuery && systemThemeListener) {
    systemThemeMediaQuery.removeEventListener('change', systemThemeListener)
    systemThemeListener = null
  }

  const applyResolvedMode = (resolvedMode: 'light' | 'dark') => {
    if (resolvedMode === 'dark') {
      document.body.setAttribute('arco-theme', 'dark')
    } else {
      document.body.removeAttribute('arco-theme')
    }
    applySurfaceVariables(resolvedMode)
  }

  if (mode === 'system') {
    systemThemeMediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    systemThemeListener = (event: MediaQueryListEvent) => {
      applyResolvedMode(event.matches ? 'dark' : 'light')
    }
    systemThemeMediaQuery.addEventListener('change', systemThemeListener)
    applyResolvedMode(resolveSystemMode())
    return
  }

  systemThemeMediaQuery = null
  applyResolvedMode(mode)
}

export { DEFAULT_THEME_COLOR, DEFAULT_COLOR_MODE, THEME_COLOR_PRESETS }
export type { ColorMode }
