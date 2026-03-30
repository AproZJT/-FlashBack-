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
      <text class="panel-title">建议习惯</text>
      <text class="panel-item">1. 每天固定同一时段复习</text>
      <text class="panel-item">2. 先复习“模糊”再过“掌握”</text>
      <text class="panel-item">3. 每次 10~20 分钟即可</text>
    </view>

    <view class="bottom-nav">
      <view class="nav-item" @tap="goHome">卡片</view>
      <view class="nav-item active" @tap="goUser">用户</view>
    </view>
  </view>
</template>

<script>
import { getUserProfile, updateUserProfile } from '@/utils/storage.js';

export default {
  data() {
    return {
      profile: { nickname: '', avatarText: '', goal: '' },
      submitting: false
    };
  },
  onShow() {
    this.profile = getUserProfile();
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
            success: ({ confirm: ok, content: goal }) => {
              if (!ok || this.submitting) return;
              this.submitting = true;
              const result = updateUserProfile({ nickname, goal });
              if (!result.ok) {
                uni.showToast({ title: result.message, icon: 'none' });
                this.submitting = false;
                return;
              }
              this.profile = getUserProfile();
              uni.showToast({ title: '用户信息已更新', icon: 'none' });
              setTimeout(() => {
                this.submitting = false;
              }, 250);
            }
          });
        }
      });
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
.page { min-height: 100vh; background: #0b1020; padding: 24rpx 24rpx 130rpx; box-sizing: border-box; }
.top-nav {
  height: 86rpx; border-radius: 20rpx; background: #121a2f; border: 1rpx solid #1f2a44;
  display: flex; align-items: center; justify-content: space-between; padding: 0 24rpx;
}
.nav-title { color: #e5edff; font-size: 32rpx; font-weight: 700; }
.nav-action { color: #7ee0b5; font-size: 26rpx; }
.user-card {
  margin-top: 22rpx; background: #121a2f; border: 1rpx solid #1f2a44; border-radius: 24rpx;
  padding: 32rpx 24rpx; display: flex; flex-direction: column; align-items: center;
}
.avatar {
  width: 108rpx; height: 108rpx; border-radius: 54rpx;
  background: linear-gradient(160deg, #35b87a, #1f7a53);
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-size: 44rpx; font-weight: 700;
}
.nickname { margin-top: 16rpx; color: #e5edff; font-size: 34rpx; font-weight: 600; }
.goal { margin-top: 10rpx; color: #8ca0c7; font-size: 24rpx; }
.panel {
  margin-top: 18rpx; background: #121a2f; border: 1rpx solid #1f2a44; border-radius: 20rpx; padding: 24rpx;
}
.panel-title { color: #d8e3ff; font-size: 28rpx; font-weight: 600; display: block; }
.panel-item { color: #8ca0c7; font-size: 24rpx; margin-top: 12rpx; display: block; }
.bottom-nav {
  position: fixed; left: 24rpx; right: 24rpx; bottom: 26rpx;
  background: #121a2f; border: 1rpx solid #1f2a44; border-radius: 20rpx;
  height: 88rpx; display: flex; align-items: center;
}
.nav-item { flex: 1; text-align: center; color: #8ca0c7; font-size: 26rpx; }
.nav-item.active { color: #7ee0b5; font-weight: 600; }
</style>
