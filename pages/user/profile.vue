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
      <text class="panel-title">学习热力图（最近120天）</text>
      <view class="heatmap-grid">
        <view
          v-for="item in heatmap"
          :key="item.date"
          class="heat-cell"
          :class="heatClass(item.count)"
        ></view>
      </view>
      <text class="panel-item">深色越深，表示当天复习次数越多</text>
    </view>

    <view class="bottom-nav">
      <view class="nav-item" @tap="goHome">卡片</view>
      <view class="nav-item" @tap="goMarket">集市</view>
      <view class="nav-item active" @tap="goUser">用户</view>
    </view>
  </view>
</template>

<script>
import { getUserProfile, updateUserProfile, getStudyHeatmap } from '@/utils/storage.js';

export default {
  data() {
    return {
      profile: { nickname: '', avatarText: '', goal: '' },
      heatmap: [],
      submitting: false
    };
  },
  async onShow() {
    this.profile = await getUserProfile();
    this.heatmap = await getStudyHeatmap(120);
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
    heatClass(count) {
      if (count >= 6) return 'lv-4';
      if (count >= 4) return 'lv-3';
      if (count >= 2) return 'lv-2';
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
.page { min-height: 100vh; background: linear-gradient(180deg, #f8fbff 0%, #eef7ff 60%, #edf9f7 100%); padding: 22rpx 22rpx 130rpx; box-sizing: border-box; }
.top-nav {
  height: 90rpx; border-radius: 24rpx; background: #fff; border: 1rpx solid #d8ebff;
  box-shadow: 0 10rpx 24rpx rgba(85, 141, 204, 0.08);
  display: flex; align-items: center; justify-content: space-between; padding: 0 24rpx;
}
.nav-title { color: #2362b2; font-size: 32rpx; font-weight: 700; }
.nav-action { color: #6b4dfb; font-size: 26rpx; font-weight: 600; }
.user-card {
  margin-top: 14rpx; background: linear-gradient(135deg, #7bc6ff 0%, #89dfd8 100%); border-radius: 24rpx;
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
  margin-top: 12rpx; background: #fff; border: 1rpx solid #d8ebff; border-radius: 20rpx; padding: 20rpx;
  box-shadow: 0 10rpx 22rpx rgba(66, 123, 180, 0.08);
}
.panel-title { color: #3d5f90; font-size: 28rpx; font-weight: 700; display: block; }
.heatmap-grid {
  margin-top: 12rpx;
  display: grid;
  grid-template-columns: repeat(15, 1fr);
  gap: 6rpx;
}
.heat-cell { width: 100%; aspect-ratio: 1; border-radius: 6rpx; }
.lv-0 { background: #edf4ff; }
.lv-1 { background: #9ed8d0; }
.lv-2 { background: #5ec9bd; }
.lv-3 { background: #4fa6dc; }
.lv-4 { background: #5f41f6; }
.panel-item { color: #6e86aa; font-size: 24rpx; margin-top: 10rpx; display: block; }
.bottom-nav {
  position: fixed; left: 24rpx; right: 24rpx; bottom: 24rpx;
  background: #fff; border: 1rpx solid #d8ebff; border-radius: 26rpx;
  box-shadow: 0 10rpx 24rpx rgba(88, 139, 193, 0.15);
  height: 94rpx; display: flex; align-items: center;
}
.nav-item { flex: 1; text-align: center; color: #7b95b9; font-size: 26rpx; }
.nav-item.active { color: #5f41f6; font-weight: 700; }
</style>
