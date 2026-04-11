<template>
  <view class="page">
    <view class="top-nav">
      <text class="nav-title">用户中心</text>
      <view class="nav-action" @tap="openEdit">编辑</view>
    </view>

    <view class="user-card">
      <view class="avatar">{{ profile.avatarText }}</view>
      <text class="nickname">{{ profile.nickname }}</text>
      <text class="goal">学习目标：{{ profile.goal }}</text>
    </view>

    <view class="panel">
      <view class="panel-head">
        <text class="panel-title">学习热力日历</text>
        <view class="month-switch">
          <text class="month-btn" @tap="switchMonth(-1)">‹</text>
          <text class="month-text">{{ viewYear }}年{{ viewMonth }}月</text>
          <text class="month-btn" @tap="switchMonth(1)">›</text>
        </view>
      </view>

      <view class="weekday-row">
        <text v-for="(w, idx) in weekdays" :key="w" class="weekday" :class="{ weekend: idx >= 5 }">{{ w }}</text>
      </view>

      <view class="calendar-grid">
        <view
          v-for="cell in calendarCells"
          :key="cell.key"
          class="day-cell"
          :class="[
            heatClass(cell.count),
            { empty: !cell.inMonth, today: cell.isToday, active: selectedDate === cell.key }
          ]"
          @tap="pickDay(cell)"
        >
          <text class="day-no">{{ cell.day || '' }}</text>
          <text v-if="cell.inMonth && cell.count > 0" class="day-count">{{ cell.count }}</text>
        </view>
      </view>

      <view class="legend-row">
        <text class="legend-text">少</text>
        <view class="legend-cell lv-0"></view>
        <view class="legend-cell lv-1"></view>
        <view class="legend-cell lv-2"></view>
        <view class="legend-cell lv-3"></view>
        <view class="legend-cell lv-4"></view>
        <text class="legend-text">多</text>
      </view>
      <text class="panel-item">分层：0 / 1-5 / 6-15 / 16-30 / 30+</text>
    </view>

    <view class="panel day-detail" v-if="selectedDate">
      <text class="detail-title">{{ selectedDate }} · 复习 {{ selectedCount }} 次</text>
      <view v-if="selectedCards.length" class="detail-list">
        <view class="detail-item" v-for="c in selectedCards" :key="c.id">
          <text class="detail-front">{{ c.front_text || '未命名卡片' }}</text>
          <text class="detail-meta">{{ c.deckName || '未命名卡组' }}</text>
        </view>
      </view>
      <text v-else class="detail-empty">当天暂无可展示的复习卡片明细</text>
    </view>

    <view class="bottom-nav">
      <view class="nav-item" @tap="goHome">卡片</view>
      <view class="nav-item" @tap="goMarket">集市</view>
      <view class="nav-item active" @tap="goUser">用户</view>
    </view>
  </view>
</template>

<script>
import { getUserProfile, updateUserProfile, getStudyHeatmap, getDecks } from '@/utils/storage.js';

function fmtDate(y, m, d) {
  const mm = `${m}`.padStart(2, '0');
  const dd = `${d}`.padStart(2, '0');
  return `${y}-${mm}-${dd}`;
}

function datePart(ts) {
  if (!ts) return '';
  const d = new Date(Number(ts));
  const y = d.getFullYear();
  const m = `${d.getMonth() + 1}`.padStart(2, '0');
  const day = `${d.getDate()}`.padStart(2, '0');
  return `${y}-${m}-${day}`;
}

export default {
  data() {
    const now = new Date();
    return {
      profile: { nickname: '', avatarText: '', goal: '' },
      heatmap: [],
      heatmapMap: {},
      allDecks: [],
      viewYear: now.getFullYear(),
      viewMonth: now.getMonth() + 1,
      weekdays: ['一', '二', '三', '四', '五', '六', '日'],
      selectedDate: '',
      selectedCount: 0,
      selectedCards: [],
      submitting: false
    };
  },
  computed: {
    calendarCells() {
      const y = this.viewYear;
      const m = this.viewMonth;
      const first = new Date(y, m - 1, 1);
      const daysInMonth = new Date(y, m, 0).getDate();
      const startWeek = (first.getDay() + 6) % 7;
      const today = datePart(Date.now());

      const cells = [];
      for (let i = 0; i < startWeek; i++) {
        cells.push({ key: `b-${i}`, inMonth: false, day: '', count: 0, isToday: false });
      }
      for (let d = 1; d <= daysInMonth; d++) {
        const key = fmtDate(y, m, d);
        cells.push({
          key,
          inMonth: true,
          day: d,
          count: Number(this.heatmapMap[key] || 0),
          isToday: key === today
        });
      }
      while (cells.length % 7 !== 0) {
        cells.push({ key: `a-${cells.length}`, inMonth: false, day: '', count: 0, isToday: false });
      }
      return cells;
    }
  },
  async onShow() {
    this.profile = await getUserProfile();
    this.heatmap = await getStudyHeatmap(180);
    this.allDecks = await getDecks();
    this.heatmapMap = (this.heatmap || []).reduce((acc, item) => {
      acc[item.date] = Number(item.count || 0);
      return acc;
    }, {});

    const today = datePart(Date.now());
    this.pickDay({ key: today, inMonth: true, count: Number(this.heatmapMap[today] || 0) });
  },
  methods: {
    openEdit() {
      if (this.submitting) return;
      uni.showModal({
        title: '编辑昵称',
        editable: true,
        placeholderText: this.profile.nickname,
        confirmText: '下一步',
        success: ({ confirm, content }) => {
          if (!confirm || this.submitting) return;
          const nickname = content;
          uni.showModal({
            title: '编辑学习目标',
            editable: true,
            placeholderText: this.profile.goal,
            confirmText: '保存',
            success: async ({ confirm: ok, content: goal }) => {
              if (!ok || this.submitting) return;
              this.submitting = true;
              const result = await updateUserProfile({ nickname, goal });
              if (!result.ok) {
                uni.showToast({ title: result.message, icon: 'none' });
                this.submitting = false;
                return;
              }
              this.profile = await getUserProfile();
              uni.showToast({ title: '用户信息已更新', icon: 'none' });
              setTimeout(() => {
                this.submitting = false;
              }, 250);
            }
          });
        }
      });
    },
    switchMonth(delta) {
      let y = this.viewYear;
      let m = this.viewMonth + delta;
      if (m <= 0) {
        y -= 1;
        m = 12;
      }
      if (m >= 13) {
        y += 1;
        m = 1;
      }
      this.viewYear = y;
      this.viewMonth = m;
    },
    pickDay(cell) {
      if (!cell || !cell.inMonth) return;
      this.selectedDate = cell.key;
      this.selectedCount = Number(cell.count || 0);

      const cards = [];
      (this.allDecks || []).forEach((deck) => {
        (deck.cards || []).forEach((card) => {
          if (datePart(card.last_review_time) === cell.key) {
            cards.push({
              id: card.id,
              front_text: card.front_text,
              deckName: deck.name,
              t: Number(card.last_review_time || 0)
            });
          }
        });
      });
      this.selectedCards = cards.sort((a, b) => b.t - a.t).slice(0, 5);
    },
    heatClass(count) {
      if (count >= 31) return 'lv-4';
      if (count >= 16) return 'lv-3';
      if (count >= 6) return 'lv-2';
      if (count >= 1) return 'lv-1';
      return 'lv-0';
    },
    goHome() {
      uni.reLaunch({ url: '/pages/index/index' });
    },
    goMarket() {
      uni.reLaunch({ url: '/pages/market/index' });
    },
    goUser() {
      uni.reLaunch({ url: '/pages/user/profile' });
    }
  }
};
</script>

<style lang="scss">
.page { min-height: 100vh; background: $fb-bg-page; padding: $fb-space-page $fb-space-page 130rpx; box-sizing: border-box; }
.top-nav {
  height: 90rpx; border-radius: $fb-radius-card; background: $fb-bg-surface; border: 1rpx solid $fb-border;
  box-shadow: $fb-shadow-card;
  display: flex; align-items: center; justify-content: space-between; padding: 0 24rpx;
}
.nav-title { color: $fb-text-primary; font-size: 32rpx; font-weight: 700; }
.nav-action { color: $fb-text-accent; font-size: 26rpx; font-weight: 600; }
.user-card {
  margin-top: 14rpx; background: linear-gradient(135deg, #7bc6ff 0%, #89dfd8 100%); border-radius: $fb-radius-card;
  padding: 26rpx 24rpx; display: flex; flex-direction: column; align-items: center;
  box-shadow: 0 14rpx 26rpx rgba(94, 192, 229, 0.2);
}
.avatar {
  width: 108rpx; height: 108rpx; border-radius: 54rpx;
  background: rgba(255,255,255,.9);
  color: #2a79b9; display: flex; align-items: center; justify-content: center;
  font-size: 44rpx; font-weight: 700;
}
.nickname { margin-top: 14rpx; color: #fff; font-size: 34rpx; font-weight: 700; }
.goal { margin-top: 6rpx; color: rgba(255,255,255,.95); font-size: 24rpx; }
.panel {
  margin-top: 12rpx; background: $fb-bg-surface; border: 1rpx solid $fb-border; border-radius: $fb-radius-card; padding: 20rpx;
  box-shadow: $fb-shadow-card;
}
.panel-head { display: flex; align-items: center; justify-content: space-between; gap: 8rpx; }
.panel-title { color: $fb-text-primary; font-size: 28rpx; font-weight: 700; display: block; }
.month-switch { display: flex; align-items: center; gap: 10rpx; }
.month-btn { color: $fb-text-accent; font-size: 30rpx; padding: 0 8rpx; }
.month-text { color: $fb-text-secondary; font-size: 22rpx; }
.weekday-row { margin-top: 12rpx; display: grid; grid-template-columns: repeat(7, 1fr); }
.weekday { text-align: center; color: $fb-text-secondary; font-size: 20rpx; }
.weekday.weekend { color: #93a7c9; }
.calendar-grid { margin-top: 8rpx; display: grid; grid-template-columns: repeat(7, 1fr); gap: 6rpx; }
.day-cell {
  min-height: 72rpx;
  border-radius: 8rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.day-cell.empty { opacity: 0.16; }
.day-cell.today { box-shadow: 0 0 0 2rpx rgba(10, 114, 239, 0.45) inset; }
.day-cell.active { box-shadow: 0 0 0 2rpx rgba(23, 23, 23, 0.3) inset; }
.day-no { color: #2d3b56; font-size: 21rpx; font-weight: 500; }
.day-count { margin-top: 2rpx; color: #1d2a45; font-size: 18rpx; }
.lv-0 { background: $fb-heat-lv0; }
.lv-1 { background: $fb-heat-lv1; }
.lv-2 { background: $fb-heat-lv2; }
.lv-3 { background: $fb-heat-lv3; }
.lv-4 { background: $fb-heat-lv4; }
.legend-row { margin-top: 12rpx; display: flex; align-items: center; justify-content: flex-end; gap: 6rpx; }
.legend-cell { width: 18rpx; height: 18rpx; border-radius: 4rpx; }
.legend-text { color: $fb-text-secondary; font-size: 20rpx; }
.panel-item { color: $fb-text-secondary; font-size: 22rpx; margin-top: 8rpx; display: block; }
.day-detail { margin-top: 10rpx; }
.detail-title { color: $fb-text-primary; font-size: 24rpx; font-weight: 600; }
.detail-list { margin-top: 8rpx; display: flex; flex-direction: column; gap: 6rpx; }
.detail-item { padding: 10rpx 12rpx; border-radius: 10rpx; background: #f5f8fe; }
.detail-front { display: block; color: #1f3558; font-size: 22rpx; }
.detail-meta { display: block; margin-top: 4rpx; color: #6f88ae; font-size: 20rpx; }
.detail-empty { margin-top: 8rpx; color: #6f88ae; font-size: 22rpx; }
.bottom-nav {
  position: fixed; left: 24rpx; right: 24rpx; bottom: 24rpx;
  background: $fb-bg-surface; border: 1rpx solid $fb-border; border-radius: $fb-radius-nav;
  box-shadow: $fb-shadow-nav;
  height: 94rpx; display: flex; align-items: center;
}
.nav-item { flex: 1; text-align: center; color: $fb-text-secondary; font-size: 26rpx; }
.nav-item.active { color: $fb-text-accent; font-weight: 700; }
</style>
