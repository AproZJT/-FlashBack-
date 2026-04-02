<template>
  <view class="page">
    <view class="top-nav">
      <text class="nav-title">卡片集市</text>
      <view class="nav-action" @tap="goHome">首页</view>
    </view>

    <view class="list" v-if="decks.length">
      <view class="item" v-for="deck in decks" :key="deck.id">
        <view>
          <text class="name">{{ deck.name }}</text>
          <text class="meta">{{ deck.card_count }} 题 · {{ deck.is_mine ? '我发布的' : '公开卡片集' }}</text>
        </view>
        <button class="clone" :disabled="deck.is_mine" @tap="clone(deck)">{{ deck.is_mine ? '我的' : '一键保存' }}</button>
      </view>
    </view>

    <view v-else class="empty">暂无公开卡片集</view>

    <view class="bottom-nav">
      <view class="nav-item" @tap="goHome">卡片</view>
      <view class="nav-item active">集市</view>
      <view class="nav-item" @tap="goUser">用户</view>
    </view>
  </view>
</template>

<script>
import { getPublicDecks, clonePublicDeck } from '@/utils/storage.js';

export default {
  data() {
    return { decks: [], submitting: false };
  },
  async onShow() {
    this.decks = await getPublicDecks();
  },
  methods: {
    goHome() {
      uni.reLaunch({ url: '/pages/index/index' });
    },
    goUser() {
      uni.reLaunch({ url: '/pages/user/profile' });
    },
    async clone(deck) {
      if (this.submitting || deck.is_mine) return;
      this.submitting = true;
      const res = await clonePublicDeck(deck.id);
      if (!res.ok) {
        uni.showToast({ title: res.message, icon: 'none' });
      } else {
        uni.showToast({ title: '已保存到我的卡片', icon: 'success' });
      }
      setTimeout(async () => {
        this.submitting = false;
        this.decks = await getPublicDecks();
      }, 250);
    }
  }
};
</script>

<style lang="scss">
.page { min-height: 100vh; background: $fb-bg-page; padding: $fb-space-page $fb-space-page 130rpx; box-sizing: border-box; }
.top-nav {
  height: 90rpx; border-radius: $fb-radius-card; background: $fb-bg-surface; border: 1rpx solid $fb-border;
  box-shadow: $fb-shadow-card;
  display:flex; align-items:center; justify-content:space-between; padding:0 24rpx;
}
.nav-title { color:$fb-text-primary; font-size:32rpx; font-weight:700; }
.nav-action { color:$fb-text-accent; font-weight: 600; }
.list { margin-top: 14rpx; display:flex; flex-direction:column; gap:10rpx; }
.item {
  background:$fb-bg-surface; border:1rpx solid $fb-border; border-radius:$fb-radius-card; padding:18rpx 20rpx;
  box-shadow: $fb-shadow-card;
  display:flex; justify-content:space-between; align-items:center;
}
.name { color:$fb-text-primary; font-size:28rpx; font-weight: 700; display:block; }
.meta { color:$fb-text-secondary; font-size:22rpx; margin-top:6rpx; display:block; }
.clone {
  background: linear-gradient(135deg, #6ecbff 0%, #6b4dfb 100%);
  color:#fff; border-radius:30rpx; font-size:22rpx; padding:0 20rpx; border: 0;
}
.empty { color:$fb-text-secondary; text-align:center; margin-top:220rpx; }
.bottom-nav {
  position: fixed; left: 24rpx; right: 24rpx; bottom: 24rpx;
  background: $fb-bg-surface; border: 1rpx solid $fb-border; border-radius: $fb-radius-nav;
  box-shadow: $fb-shadow-nav;
  height: 94rpx; display: flex; align-items: center;
}
.nav-item { flex: 1; text-align: center; color: $fb-text-secondary; font-size: 26rpx; }
.nav-item.active { color: $fb-text-accent; font-weight: 700; }
</style>
