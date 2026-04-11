<template>
  <view class="page">
    <view class="top-nav card-shell">
      <text class="nav-title">卡片集市</text>
      <view class="top-actions">
        <view class="nav-action" @tap="seedMarket">补充数据</view>
        <view class="nav-action" @tap="goHome">首页</view>
      </view>
    </view>

    <view class="list" v-if="decks.length">
      <view class="item card-shell" v-for="deck in decks" :key="deck.id">
        <view class="left">
          <text class="name">{{ deck.name }}</text>
          <text class="meta">{{ deck.card_count }} 题 · {{ deck.is_mine ? '我发布的' : '公开卡片集' }}</text>
        </view>
        <button
          class="action-btn"
          :class="deck.is_mine ? 'is-mine' : 'is-save'"
          :disabled="deck.is_mine"
          @tap="clone(deck)"
        >
          {{ deck.is_mine ? '我的' : '一键保存' }}
        </button>
      </view>
    </view>

    <view v-else class="empty card-shell">
      <text class="empty-title">暂无公开卡片集</text>
      <text class="empty-sub">点击右上角“补充数据”快速注入演示内容</text>
    </view>

    <view class="bottom-nav card-shell">
      <view class="nav-item" @tap="goHome">卡片</view>
      <view class="nav-item active">集市</view>
      <view class="nav-item" @tap="goUser">用户</view>
    </view>
  </view>
</template>

<script>
import { getPublicDecks, clonePublicDeck, seedDemoData } from '@/utils/storage.js';

export default {
  data() {
    return { decks: [], submitting: false, seeding: false };
  },
  async onShow() {
    await this.loadMarket();
  },
  methods: {
    async loadMarket() {
      this.decks = await getPublicDecks();
    },
    goHome() {
      uni.reLaunch({ url: '/pages/index/index' });
    },
    goUser() {
      uni.reLaunch({ url: '/pages/user/profile' });
    },
    async seedMarket() {
      if (this.seeding) return;
      this.seeding = true;
      const res = await seedDemoData();
      this.seeding = false;
      if (!res.ok) {
        uni.showToast({ title: res.message || '补充失败', icon: 'none' });
        return;
      }
      uni.showToast({ title: '已补充演示数据', icon: 'none' });
      await this.loadMarket();
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
        await this.loadMarket();
      }, 250);
    }
  }
};
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fbff 0%, #f3f0ff 55%, #f1fbf7 100%);
  padding: 16rpx 16rpx 124rpx;
  box-sizing: border-box;
}

.card-shell {
  background: #ffffff;
  border-radius: 14rpx;
  box-shadow:
    0 0 0 1px rgba(91, 119, 255, 0.12),
    0 6px 16px rgba(90, 132, 255, 0.12),
    0 10px 28px -16px rgba(72, 98, 196, 0.24),
    0 0 0 1px rgba(255, 255, 255, 0.6);
}

.top-nav {
  height: 82rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16rpx;
  background: linear-gradient(135deg, rgba(117, 142, 255, 0.14) 0%, rgba(90, 205, 165, 0.14) 100%);
}

.nav-title {
  color: #243152;
  font-size: 32rpx;
  font-weight: 700;
  letter-spacing: -0.6px;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.nav-action {
  color: #4a57c9;
  font-size: 24rpx;
  font-weight: 600;
}

.list {
  margin-top: 12rpx;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.item {
  min-height: 96rpx;
  padding: 14rpx 16rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.left {
  flex: 1;
  min-width: 0;
  margin-right: 14rpx;
}

.name {
  display: block;
  color: #293a66;
  font-size: 30rpx;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -0.4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta {
  display: block;
  margin-top: 4rpx;
  color: #6a78a8;
  font-size: 22rpx;
  line-height: 1.35;
}

.action-btn {
  height: 58rpx;
  line-height: 58rpx;
  border-radius: 6px;
  padding: 0 18rpx;
  font-size: 22rpx;
  font-weight: 500;
  border: none;
}

.action-btn.is-save {
  background: linear-gradient(135deg, #5f6fff 0%, #7b4dff 55%, #4bc2ff 100%);
  color: #ffffff;
  box-shadow: 0 8rpx 18rpx rgba(95, 111, 255, 0.32);
}

.action-btn.is-mine {
  background: linear-gradient(135deg, #e8fff3 0%, #d8f7ff 100%);
  color: #2d8a61;
  box-shadow: 0 0 0 1px rgba(111, 206, 162, 0.35);
}

.empty {
  margin-top: 14rpx;
  padding: 24rpx 18rpx;
  text-align: center;
}

.empty-title {
  display: block;
  color: #2b3b69;
  font-size: 26rpx;
  font-weight: 700;
}

.empty-sub {
  display: block;
  margin-top: 8rpx;
  color: #7a89b4;
  font-size: 22rpx;
}

.bottom-nav {
  position: fixed;
  left: 16rpx;
  right: 16rpx;
  bottom: 16rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
}

.nav-item {
  flex: 1;
  text-align: center;
  color: #666666;
  font-size: 24rpx;
  font-weight: 500;
}

.nav-item.active {
  color: #171717;
  font-weight: 600;
}
</style>
