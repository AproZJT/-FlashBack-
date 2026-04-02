<script>
import { getTheme } from '@/utils/theme.js';

function applyTheme(theme) {
  if (typeof document === 'undefined') return;
  const root = document.documentElement;
  root.setAttribute('data-theme', theme === 'dark' ? 'dark' : 'light');
}

export default {
  onLaunch() {
    console.log('FlashBack Launch');
    applyTheme(getTheme());
    uni.$on('flashback:theme-change', ({ theme }) => applyTheme(theme));
  }
};
</script>

<style lang="scss">
:root,
page[data-theme='light'] {
  --fb-bg-page: #f8fbff;
  --fb-bg-surface: #ffffff;
  --fb-border: #d8ebff;
  --fb-text-primary: #244f88;
  --fb-text-secondary: #6f88ae;
  --fb-shadow: 0 10rpx 24rpx rgba(88, 139, 193, 0.15);
}

page[data-theme='dark'] {
  --fb-bg-page: #0f172a;
  --fb-bg-surface: #111827;
  --fb-border: #1f2937;
  --fb-text-primary: #dbeafe;
  --fb-text-secondary: #93c5fd;
  --fb-shadow: 0 10rpx 24rpx rgba(0, 0, 0, 0.45);
}

page {
  background: var(--fb-bg-page);
  color: var(--fb-text-primary);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

button { border: none; }
button::after { border: none; }
</style>
