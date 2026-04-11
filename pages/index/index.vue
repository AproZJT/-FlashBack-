<template>
  <view class="page">
    <view class="top-nav">
      <text class="nav-title">FlashBack</text>
      <view class="nav-actions">
        <view class="nav-action ghost" @tap="toggleThemeMode">主题</view>
        <view class="nav-action ghost" @tap="goPcCreator">PC制卡</view>
        <view class="nav-action ghost" @tap="goImport">导入</view>
        <view class="nav-action" @tap="openCreateDialog">+ 新建</view>
      </view>
    </view>

    <view class="profile-card" @tap="goUser">
      <view class="avatar">{{ profile.avatarText }}</view>
      <view class="profile-info">
        <text class="nickname">{{ profile.nickname }}</text>
        <text class="goal">目标：{{ profile.goal }}</text>
      </view>
      <text class="arrow">›</text>
    </view>

    <view class="metric-card" @tap="goMarket">
      <text class="metric-title">今日待复习</text>
      <text class="metric-value">{{ dueCount }}</text>
      <text class="metric-tip">基于遗忘曲线调度（next_review_time）</text>
      <text class="offline-tip" v-if="offlineMeta.pending > 0">离线队列：{{ offlineMeta.pending }} 条待同步（冲突 {{ offlineMeta.conflicts }}）</text>
    </view>

    <view class="metric-card heat-card" @tap="goUser">
      <text class="metric-title">学习热力图（近30天）</text>
      <view class="heatmap-row">
        <view
          v-for="item in heatmap30"
          :key="item.date"
          class="heat-cell"
          :class="heatClass(item.count)"
        ></view>
      </view>
      <text class="metric-tip">0 / 1-5 / 6-15 / 16-30 / 30+</text>
    </view>

    <view class="filter-row" v-if="decks.length">
      <text class="filter-label">只看待复习卡片集</text>
      <switch color="#22c55e" :checked="onlyDue" @change="toggleOnlyDue" />
    </view>

    <view v-if="decks.length" class="deck-grid">
      <view
        v-for="deck in decks"
        :key="deck.id"
        class="deck-card"
        hover-class="none"
        @tap="goDeck(deck.id)"
        @longpress="openRenameDeck(deck)"
      >
        <view class="deck-badge">待复习 {{ deckDue(deck) }}</view>
        <view class="deck-name">{{ deck.name }}</view>
        <view class="deck-meta">{{ deck.cards.length }} 个知识点</view>
        <view class="deck-progress-row">
          <view class="deck-progress-track">
            <view class="deck-progress-fill" :style="{ width: `${deckProgress(deck)}%` }"></view>
          </view>
          <text class="deck-progress-text">{{ deckProgress(deck) }}%</text>
        </view>
        <view class="deck-update">最近更新：{{ formatTime(deck.createdAt || 0) }}</view>
      </view>
    </view>

    <view v-else class="empty-wrap">
      <text class="empty-title">还没有卡片集</text>
      <text class="empty-tip">点击右上角“新建”创建第一组</text>
    </view>

    <view class="bottom-nav">
      <view class="nav-item active" @tap="goHome">卡片</view>
      <view class="nav-item" @tap="goMarket">集市</view>
      <view class="nav-item" @tap="goUser">用户</view>
    </view>
  </view>
</template>

<script>
import { getDecks, createDeck, renameDeck, getUserProfile, getDueReviewCards, getOfflineQueueMeta, getStudyHeatmap } from '@/utils/storage.js';
import { toggleTheme } from '@/utils/theme.js';

function countDue(cards = [], now = Date.now()) {
  return (cards || []).filter(card => !card.next_review_time || Number(card.next_review_time) <= now).length;
}

export default {
  data() {
    return {
      decks: [],
      allDecks: [],
      profile: { nickname: '', avatarText: '', goal: '' },
      dueCount: 0,
      heatmap30: [],
      onlyDue: false,
      submitting: false,
      offlineMeta: { pending: 0, conflicts: 0 }
    };
  },
  async onShow() {
    await this.loadDecks();
    this.profile = await getUserProfile();
    const dueCards = await getDueReviewCards();
    this.dueCount = dueCards.length;
    this.heatmap30 = await getStudyHeatmap(30);
    this.offlineMeta = getOfflineQueueMeta();
  },
  methods: {
    async loadDecks() {
      this.allDecks = await getDecks();
      const now = Date.now();
      this.decks = this.onlyDue
        ? this.allDecks.filter(deck => countDue(deck.cards || [], now) > 0)
        : this.allDecks;
    },
    toggleOnlyDue(event) {
      this.onlyDue = !!(event && event.detail && event.detail.value);
      this.loadDecks();
    },
    openCreateDialog() {
      if (this.submitting) return;
      uni.showModal({
        title: '新建卡片集',
        editable: true,
        placeholderText: '例如：数据结构-树与图',
        confirmText: '创建',
        success: async ({ confirm, content }) => {
          if (!confirm || this.submitting) return;
          this.submitting = true;
          const result = await createDeck(content);
          if (!result.ok) {
            uni.showToast({ title: result.message, icon: 'none' });
            this.submitting = false;
            return;
          }
          await this.loadDecks();
          uni.showToast({ title: '创建成功', icon: 'success' });
          setTimeout(() => {
            this.submitting = false;
          }, 250);
        }
      });
    },
    openRenameDeck(deck) {
      if (this.submitting) return;
      uni.showModal({
        title: '重命名卡片集',
        editable: true,
        placeholderText: deck.name,
        confirmText: '保存',
        success: async ({ confirm, content }) => {
          if (!confirm || this.submitting) return;
          this.submitting = true;
          const result = await renameDeck(deck.id, content);
          if (!result.ok) {
            uni.showToast({ title: result.message, icon: 'none' });
            this.submitting = false;
            return;
          }
          await this.loadDecks();
          uni.showToast({ title: '重命名成功', icon: 'none' });
          setTimeout(() => {
            this.submitting = false;
          }, 250);
        }
      });
    },
    goDeck(deckId) {
      uni.navigateTo({ url: `/pages/deck/detail?deckId=${deckId}` });
    },
    goHome() {
      uni.reLaunch({ url: '/pages/index/index' });
    },
    goUser() {
      uni.reLaunch({ url: '/pages/user/profile' });
    },
    goMarket() {
      uni.navigateTo({ url: '/pages/market/index' });
    },
    goImport() {
      uni.navigateTo({ url: '/pages/import/csv' });
    },
    goPcCreator() {
      uni.navigateTo({ url: '/pages/pc/creator' });
    },
    toggleThemeMode() {
      const next = toggleTheme();
      uni.showToast({ title: next === 'dark' ? '已切换深色' : '已切换浅色', icon: 'none' });
    },
    deckDue(deck) {
      return countDue(deck.cards || []);
    },
    deckProgress(deck) {
      const cards = deck.cards || [];
      if (!cards.length) return 0;
      const mastered = cards.filter(c => Number(c.mastery_level || 0) >= 2).length;
      return Math.min(100, Math.round((mastered / cards.length) * 100));
    },
    formatTime(ts) {
      if (!ts) return '--';
      const d = new Date(Number(ts));
      const m = `${d.getMonth() + 1}`.padStart(2, '0');
      const day = `${d.getDate()}`.padStart(2, '0');
      return `${m}-${day}`;
    },
    heatClass(count) {
      if (count >= 31) return 'lv-4';
      if (count >= 16) return 'lv-3';
      if (count >= 6) return 'lv-2';
      if (count >= 1) return 'lv-1';
      return 'lv-0';
    }
  }
};
</script>

<style lang="scss">
.page { min-height: 100vh; background: $fb-bg-page; padding: $fb-space-page $fb-space-page 130rpx; box-sizing: border-box; }
.top-nav {
  height: 90rpx; border-radius: $fb-radius-card; background: $fb-bg-surface; border: 1rpx solid $fb-border;
  box-shadow: $fb-shadow-card; display: flex; align-items: center; justify-content: space-between; padding: 0 24rpx;
}
.nav-title { color: $fb-text-primary; font-size: 32rpx; font-weight: 700; }
.nav-actions { display: flex; align-items: center; gap: 12rpx; }
.nav-action { color: $fb-text-accent; font-size: 24rpx; font-weight: 600; }
.nav-action.ghost { color: $fb-text-secondary; }
.profile-card {
  margin-top: 14rpx; background: linear-gradient(135deg, #6ecbff 0%, #8de0dc 100%);
  border-radius: $fb-radius-card; padding: 20rpx; display: flex; align-items: center;
  box-shadow: 0 14rpx 26rpx rgba(94, 192, 229, 0.2);
}
.avatar {
  width: 74rpx; height: 74rpx; border-radius: 37rpx; background: rgba(255, 255, 255, 0.9);
  color: #2a79b9; display: flex; align-items: center; justify-content: center; font-size: 30rpx; font-weight: 700;
}
.profile-info { margin-left: 14rpx; flex: 1; }
.nickname { color: #fff; font-size: 28rpx; font-weight: 700; display: block; }
.goal { color: rgba(255,255,255,0.92); font-size: 22rpx; margin-top: 4rpx; display: block; }
.arrow { color: #fff; font-size: 34rpx; }
.metric-card { margin-top: 12rpx; background: $fb-bg-surface; border: 1rpx solid #ebebeb; border-radius: $fb-radius-card; padding: 18rpx 20rpx; box-shadow: 0px 0px 0px 1px rgba(0,0,0,0.08), 0px 2px 2px rgba(0,0,0,0.04); }
.metric-title { color: #4d4d4d; font-size: 22rpx; display: block; }
.metric-value { color: #171717; font-size: 54rpx; font-weight: 600; display: block; margin-top: 6rpx; letter-spacing: -0.8rpx; }
.metric-tip,.offline-tip { color: #666666; font-size: 20rpx; display: block; margin-top: 4rpx; }
.heat-card { padding-bottom: 16rpx; }
.heatmap-row { margin-top: 10rpx; display: grid; grid-template-columns: repeat(15, 1fr); gap: 6rpx; }
.heat-cell { width: 100%; aspect-ratio: 1; border-radius: 6rpx; }
.lv-0 { background: $fb-heat-lv0; }
.lv-1 { background: $fb-heat-lv1; }
.lv-2 { background: $fb-heat-lv2; }
.lv-3 { background: $fb-heat-lv3; }
.lv-4 { background: $fb-heat-lv4; }
.filter-row { margin-top: 10rpx; background: $fb-bg-surface; border: 1rpx solid $fb-border; border-radius: 18rpx; padding: 14rpx 18rpx; display: flex; align-items: center; justify-content: space-between; }
.filter-label { color: $fb-text-primary; font-size: 24rpx; }
.deck-grid { margin-top: 14rpx; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10rpx; }
.deck-card { background: $fb-bg-surface; border: 1rpx solid $fb-border; border-radius: $fb-radius-card; padding: 16rpx; box-shadow: $fb-shadow-card; }
.deck-badge { display: inline-block; padding: 6rpx 12rpx; border-radius: 999rpx; font-size: 20rpx; color: #178a57; background: #def9ec; }
.deck-name { margin-top: 10rpx; color: $fb-text-primary; font-size: 28rpx; font-weight: 700; }
.deck-meta { margin-top: 4rpx; color: $fb-text-secondary; font-size: 22rpx; }
.deck-progress-row { margin-top: 10rpx; display: flex; align-items: center; gap: 8rpx; }
.deck-progress-track { flex: 1; height: 10rpx; border-radius: 999rpx; background: #e9f1ff; overflow: hidden; }
.deck-progress-fill { height: 100%; border-radius: 999rpx; background: linear-gradient(90deg, #5e9dff 0%, #35c58e 100%); }
.deck-progress-text { color: $fb-text-secondary; font-size: 20rpx; }
.deck-update { margin-top: 8rpx; color: $fb-text-secondary; font-size: 20rpx; }
.empty-wrap { margin-top: 220rpx; text-align: center; }
.empty-title { color: $fb-text-primary; font-size: 32rpx; }
.empty-tip { color: $fb-text-secondary; font-size: 23rpx; margin-top: 8rpx; display: block; }
.bottom-nav { position: fixed; left: 24rpx; right: 24rpx; bottom: 24rpx; background: $fb-bg-surface; border: 1rpx solid $fb-border; border-radius: $fb-radius-nav; box-shadow: $fb-shadow-nav; height: 94rpx; display: flex; align-items: center; }
.nav-item { flex: 1; text-align: center; color: $fb-text-secondary; font-size: 26rpx; }
.nav-item.active { color: $fb-text-accent; font-weight: 700; }
</style>
