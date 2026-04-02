<template>
  <view class="page">
    <view class="top-nav">
      <text class="nav-title">FlashBack</text>
      <view class="nav-action" @tap="openCreateDialog">+ 新建</view>
    </view>

    <view class="profile-card" @tap="goUser">
      <view class="avatar">{{ profile.avatarText }}</view>
      <view class="profile-info">
        <text class="nickname">{{ profile.nickname }}</text>
        <text class="goal">目标：{{ profile.goal }}</text>
      </view>
      <text class="arrow">›</text>
    </view>

    <view class="due-card" @tap="goMarket">
      <text class="due-title">待复习卡片</text>
      <text class="due-value">{{ dueCount }} 张</text>
      <text class="due-tip">基于遗忘曲线调度（next_review_time）</text>
    </view>

    <view class="filter-row" v-if="decks.length">
      <text class="filter-label">只看待复习卡片集</text>
      <switch color="#22c55e" :checked="onlyDue" @change="toggleOnlyDue" />
    </view>

    <view v-if="decks.length" class="deck-list">
      <view
        v-for="deck in decks"
        :key="deck.id"
        class="deck-card"
        hover-class="none"
        @tap="goDeck(deck.id)"
        @longpress="openRenameDeck(deck)"
      >
        <view class="deck-name">{{ deck.name }}</view>
        <view class="deck-meta">{{ deck.cards.length }} 个知识点 · 长按重命名</view>
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
import { getDecks, createDeck, renameDeck, getUserProfile, getDueReviewCards } from '@/utils/storage.js';

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
      onlyDue: false,
      submitting: false
    };
  },
  async onShow() {
    await this.loadDecks();
    this.profile = await getUserProfile();
    const dueCards = await getDueReviewCards();
    this.dueCount = dueCards.length;
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
    }
  }
};
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fbff 0%, #eef7ff 60%, #edf9f7 100%);
  padding: 22rpx 22rpx 130rpx;
  box-sizing: border-box;
}
.top-nav {
  height: 90rpx;
  border-radius: 24rpx;
  background: #ffffff;
  border: 1rpx solid #d8ebff;
  box-shadow: 0 10rpx 24rpx rgba(85, 141, 204, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
}
.nav-title { color: #2362b2; font-size: 32rpx; font-weight: 700; }
.nav-action { color: #6b4dfb; font-size: 26rpx; font-weight: 600; }
.profile-card {
  margin-top: 14rpx;
  background: linear-gradient(135deg, #6ecbff 0%, #8de0dc 100%);
  border-radius: 24rpx;
  padding: 20rpx;
  display: flex;
  align-items: center;
  box-shadow: 0 14rpx 26rpx rgba(94, 192, 229, 0.2);
}
.avatar {
  width: 74rpx; height: 74rpx; border-radius: 37rpx;
  background: rgba(255, 255, 255, 0.9);
  color: #2a79b9; display: flex; align-items: center; justify-content: center;
  font-size: 30rpx; font-weight: 700;
}
.profile-info { margin-left: 14rpx; flex: 1; }
.nickname { color: #fff; font-size: 28rpx; font-weight: 700; display: block; }
.goal { color: rgba(255,255,255,0.92); font-size: 22rpx; margin-top: 4rpx; display: block; }
.arrow { color: #fff; font-size: 34rpx; }
.due-card {
  margin-top: 12rpx;
  background: #ffffff;
  border: 1rpx solid #d8ebff;
  border-radius: 22rpx;
  padding: 18rpx 20rpx;
  box-shadow: 0 10rpx 22rpx rgba(66, 123, 180, 0.08);
}
.due-title { color: #5f7ca8; font-size: 22rpx; display: block; }
.due-value { color: #5b3ff2; font-size: 40rpx; font-weight: 800; display: block; margin-top: 6rpx; }
.due-tip { color: #7f98bf; font-size: 20rpx; display: block; margin-top: 4rpx; }
.filter-row {
  margin-top: 10rpx;
  background: #ffffff;
  border: 1rpx solid #d8ebff;
  border-radius: 18rpx;
  padding: 14rpx 18rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.filter-label { color: #4d6d9b; font-size: 24rpx; }
.deck-list { margin-top: 14rpx; display: flex; flex-direction: column; gap: 10rpx; }
.deck-card {
  background: #ffffff;
  border: 1rpx solid #d8ebff;
  border-radius: 20rpx;
  padding: 18rpx 20rpx;
  box-shadow: 0 8rpx 18rpx rgba(84, 136, 194, 0.08);
}
.deck-name { color: #254f88; font-size: 30rpx; font-weight: 700; }
.deck-meta { margin-top: 6rpx; color: #6f88ae; font-size: 22rpx; }
.empty-wrap { margin-top: 220rpx; text-align: center; }
.empty-title { color: #5376a8; font-size: 32rpx; }
.empty-tip { color: #86a2c7; font-size: 23rpx; margin-top: 8rpx; display: block; }
.bottom-nav {
  position: fixed; left: 24rpx; right: 24rpx; bottom: 24rpx;
  background: #ffffff;
  border: 1rpx solid #d8ebff;
  border-radius: 26rpx;
  box-shadow: 0 10rpx 24rpx rgba(88, 139, 193, 0.15);
  height: 94rpx; display: flex; align-items: center;
}
.nav-item { flex: 1; text-align: center; color: #7b95b9; font-size: 26rpx; }
.nav-item.active { color: #5f41f6; font-weight: 700; }
</style>
