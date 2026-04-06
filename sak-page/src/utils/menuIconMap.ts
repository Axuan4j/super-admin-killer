// Font Awesome 图标自动映射
// 后端返回 icon: 'fa-solid fa-chart-line' -> 前端直接使用
export const getIcon = (iconName: string): { prefix: string; iconName: string } => {
  if (!iconName) {
    return { prefix: 'fas', iconName: 'fa-gauge' }
  }

  // 格式: fa-solid fa-chart-line 或 fa ChartLine
  const parts = iconName.split(' ')
  if (parts.length === 2) {
    const prefixMap: Record<string, string> = {
      'fa-solid': 'fas',
      'fa-regular': 'far',
      'fa-brands': 'fab'
    }
    return {
      prefix: prefixMap[parts[0]] || 'fas',
      iconName: parts[1]
    }
  }

  // 兼容驼峰格式: faChartLine -> fas fa-chart-line
  const name = iconName.replace(/^fa/, '')
  const kebab = name.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase()
  return { prefix: 'fas', iconName: kebab }
}
