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

    <view v-if="decks.length" class="deck-list">
      <view
        v-for="deck in decks"
        :key="deck.id"
        class="deck-card"
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
      <view class="nav-item" @tap="goUser">用户</view>
    </view>
  </view>
</template>

<script>
import { getDecks, createDeck, renameDeck, getUserProfile } from '@/utils/storage.js';

export default {
  data() {
    return {
      decks: [],
      profile: { nickname: '', avatarText: '', goal: '' },
      submitting: false
    };
  },
  onShow() {
    this.loadDecks();
    this.profile = getUserProfile();
  },
  methods: {
    loadDecks() {
      this.decks = getDecks();
    },
    openCreateDialog() {
      if (this.submitting) return;
      uni.showModal({
        title: '新建卡片集',
        editable: true,
        placeholderText: '例如：数据结构-树与图',
        confirmText: '创建',
        success: ({ confirm, content }) => {
          if (!confirm || this.submitting) return;
          this.submitting = true;
          const result = createDeck(content);
          if (!result.ok) {
            uni.showToast({ title: result.message, icon: 'none' });
            this.submitting = false;
            return;
          }
          this.loadDecks();
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
        success: ({ confirm, content }) => {
          if (!confirm || this.submitting) return;
          this.submitting = true;
          const result = renameDeck(deck.id, content);
          if (!result.ok) {
            uni.showToast({ title: result.message, icon: 'none' });
            this.submitting = false;
            return;
          }
          this.loadDecks();
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
    }
  }
};
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  background: #0b1020;
  padding: 26rpx 24rpx 130rpx;
  box-sizing: border-box;
}
.top-nav {
  height: 86rpx;
  border-radius: 20rpx;
  background: #121a2f;
  border: 1rpx solid #1f2a44;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
}
.nav-title { color: #e5edff; font-size: 32rpx; font-weight: 700; }
.nav-action { color: #7ee0b5; font-size: 26rpx; }
.profile-card {
  margin-top: 20rpx;
  background: #121a2f;
  border: 1rpx solid #1f2a44;
  border-radius: 20rpx;
  padding: 22rpx;
  display: flex;
  align-items: center;
}
.avatar {
  width: 72rpx; height: 72rpx; border-radius: 36rpx;
  background: linear-gradient(160deg, #35b87a, #1f7a53);
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-size: 30rpx; font-weight: 700;
}
.profile-info { margin-left: 16rpx; flex: 1; }
.nickname { color: #e5edff; font-size: 28rpx; font-weight: 600; display: block; }
.goal { color: #8ca0c7; font-size: 22rpx; margin-top: 6rpx; display: block; }
.arrow { color: #8ca0c7; font-size: 34rpx; }
.deck-list { margin-top: 20rpx; display: flex; flex-direction: column; gap: 14rpx; }
.deck-card { background: #121a2f; border: 1rpx solid #1f2a44; border-radius: 20rpx; padding: 24rpx; }
.deck-name { color: #e7eeff; font-size: 31rpx; font-weight: 600; }
.deck-meta { margin-top: 10rpx; color: #8ca0c7; font-size: 22rpx; }
.empty-wrap { margin-top: 260rpx; text-align: center; }
.empty-title { color: #d3dcf3; font-size: 32rpx; }
.empty-tip { color: #8ca0c7; font-size: 23rpx; margin-top: 8rpx; display: block; }
.bottom-nav {
  position: fixed; left: 24rpx; right: 24rpx; bottom: 26rpx;
  background: #121a2f; border: 1rpx solid #1f2a44; border-radius: 20rpx;
  height: 88rpx; display: flex; align-items: center;
}
.nav-item { flex: 1; text-align: center; color: #8ca0c7; font-size: 26rpx; }
.nav-item.active { color: #7ee0b5; font-weight: 600; }
</style>
