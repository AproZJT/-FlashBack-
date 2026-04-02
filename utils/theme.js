const THEME_KEY = 'flashback_theme_v1';

export function getTheme() {
  return uni.getStorageSync(THEME_KEY) || 'light';
}

export function setTheme(theme) {
  const next = theme === 'dark' ? 'dark' : 'light';
  uni.setStorageSync(THEME_KEY, next);
  uni.$emit('flashback:theme-change', { theme: next });
}

export function toggleTheme() {
  const next = getTheme() === 'dark' ? 'light' : 'dark';
  setTheme(next);
  return next;
}
