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
.page { min-height: 100vh; background: linear-gradient(180deg, #f8fbff 0%, #eef7ff 60%, #edf9f7 100%); padding: 22rpx 22rpx 130rpx; box-sizing: border-box; }
.top-nav {
  height: 90rpx; border-radius: 24rpx; background: #fff; border: 1rpx solid #d8ebff;
  box-shadow: 0 10rpx 24rpx rgba(85, 141, 204, 0.08);
  display:flex; align-items:center; justify-content:space-between; padding:0 24rpx;
}
.nav-title { color:#2362b2; font-size:32rpx; font-weight:700; }
.nav-action { color:#6b4dfb; font-weight: 600; }
.list { margin-top: 14rpx; display:flex; flex-direction:column; gap:10rpx; }
.item {
  background:#fff; border:1rpx solid #d8ebff; border-radius:20rpx; padding:18rpx 20rpx;
  box-shadow: 0 8rpx 18rpx rgba(84, 136, 194, 0.08);
  display:flex; justify-content:space-between; align-items:center;
}
.name { color:#254f88; font-size:28rpx; font-weight: 700; display:block; }
.meta { color:#6f88ae; font-size:22rpx; margin-top:6rpx; display:block; }
.clone {
  background: linear-gradient(135deg, #6ecbff 0%, #6b4dfb 100%);
  color:#fff; border-radius:30rpx; font-size:22rpx; padding:0 20rpx; border: 0;
}
.empty { color:#86a2c7; text-align:center; margin-top:220rpx; }
.bottom-nav {
  position: fixed; left: 24rpx; right: 24rpx; bottom: 24rpx;
  background: #fff; border: 1rpx solid #d8ebff; border-radius: 26rpx;
  box-shadow: 0 10rpx 24rpx rgba(88, 139, 193, 0.15);
  height: 94rpx; display: flex; align-items: center;
}
.nav-item { flex: 1; text-align: center; color: #7b95b9; font-size: 26rpx; }
.nav-item.active { color: #5f41f6; font-weight: 700; }
</style>
