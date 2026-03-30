<template>
  <view class="page">
    <view class="top-nav">
      <view class="nav-left" @tap="goBack">‹ 返回</view>
      <view class="nav-title">闪回复习</view>
      <view class="nav-right">{{ currentIndex + 1 }} / {{ cards.length }}</view>
    </view>

    <view class="stats" v-if="cards.length">
      <text class="stats-item">掌握 {{ stats.master }}</text>
      <text class="stats-sep">·</text>
      <text class="stats-item">模糊 {{ stats.blur }}</text>
    </view>

    <view class="stage" v-if="currentCard">
      <view class="card-shell" :class="[isFlipped ? 'is-flipped' : '', flyClass, cardMotionClass]" @tap="flipCard">
        <view class="face front">
          <text class="face-label">问题</text>
          <text class="face-text">{{ currentCard.front }}</text>
          <text class="hint">点击翻转查看解析</text>
        </view>
        <view class="face back">
          <text class="face-label">解析</text>
          <text class="face-text">{{ currentCard.back }}</text>
        </view>
      </view>
    </view>

    <view class="actions" :class="{ show: isFlipped }">
      <button class="btn blur" @tap.stop="answer('blur')">模糊</button>
      <button class="btn master" @tap.stop="answer('master')">掌握</button>
    </view>

    <view v-if="!cards.length" class="empty">
      <text class="empty-title">该卡片集暂无题目</text>
      <button class="empty-btn" @tap="goBack">返回卡片管理</button>
    </view>

    <view v-if="finished" class="finish-mask" @tap="goBack">
      <view class="finish-card" @tap.stop>
        <text class="finish-title">本轮复习完成</text>
        <text class="finish-tip">继续保持，记忆会更稳固。</text>
        <button class="finish-btn" @tap="goBack">返回卡片管理</button>
      </view>
    </view>

    <view class="bottom-nav">
      <view class="nav-item active" @tap="goHome">卡片</view>
      <view class="nav-item" @tap="goUser">用户</view>
    </view>
  </view>
</template>

<script>
import {
  getDeckById,
  markCardResult,
  saveReviewProgress,
  getReviewProgress,
  clearReviewProgress
} from '@/utils/storage.js';

export default {
  data() {
    return {
      deckId: '',
      cards: [],
      currentIndex: 0,
      isFlipped: false,
      flyClass: '',
      cardMotionClass: 'card-enter',
      finished: false,
      stats: {
        master: 0,
        blur: 0
      }
    };
  },
  computed: {
    currentCard() {
      return this.cards[this.currentIndex];
    }
  },
  onLoad(query) {
    this.deckId = query.deckId || '';
    const deck = getDeckById(this.deckId);
    this.cards = deck?.cards || [];
    const saved = getReviewProgress(this.deckId);
    if (saved && this.cards.length) {
      this.currentIndex = Math.min(saved.index || 0, this.cards.length - 1);
      this.stats = saved.stats || { master: 0, blur: 0 };
    }
    if (this.cards.length) this.playEnterMotion();
  },
  onHide() {
    this.persistProgress();
  },
  onUnload() {
    this.persistProgress();
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    goHome() {
      uni.reLaunch({ url: '/pages/index/index' });
    },
    goUser() {
      uni.reLaunch({ url: '/pages/user/profile' });
    },
    flipCard() {
      if (!this.currentCard || this.flyClass || this.cardMotionClass === 'card-enter') return;
      this.isFlipped = !this.isFlipped;
    },
    answer(type) {
      if (!this.currentCard || !this.isFlipped || this.flyClass) return;
      this.flyClass = type === 'master' ? 'fly-right' : 'fly-left';
      if (type === 'master') this.stats.master += 1;
      else this.stats.blur += 1;
      markCardResult(this.deckId, this.currentCard.id, type);
      setTimeout(() => {
        this.nextCard();
      }, 360);
    },
    playEnterMotion() {
      this.cardMotionClass = 'card-enter';
      setTimeout(() => {
        this.cardMotionClass = '';
      }, 360);
    },
    persistProgress() {
      if (!this.deckId || !this.cards.length) return;
      if (this.finished) {
        clearReviewProgress(this.deckId);
        return;
      }
      saveReviewProgress(this.deckId, {
        index: this.currentIndex,
        stats: this.stats
      });
    },
    nextCard() {
      this.flyClass = '';
      this.isFlipped = false;
      if (this.currentIndex >= this.cards.length - 1) {
        this.finished = true;
        clearReviewProgress(this.deckId);
        return;
      }
      this.currentIndex += 1;
      this.persistProgress();
      this.playEnterMotion();
    }
  }
};
</script>

<style lang="scss">
.page { min-height: 100vh; background: #0b1020; display: flex; flex-direction: column; padding-bottom: 118rpx; }
.top-nav {
  margin: 24rpx 24rpx 0;
  height: 86rpx; border-radius: 20rpx; background: #121a2f; border: 1rpx solid #1f2a44;
  display: flex; align-items: center; justify-content: space-between; padding: 0 20rpx;
}
.nav-left,.nav-right { color: #7ee0b5; font-size: 24rpx; }
.nav-title { color: #e5edff; font-size: 28rpx; font-weight: 600; }
.stats { display: flex; justify-content: center; margin-top: 16rpx; color: #8ca0c7; font-size: 24rpx; }
.stats-sep { margin: 0 8rpx; }
.stage { flex: 1; display: flex; align-items: center; justify-content: center; padding: 10rpx 30rpx 0; perspective: 1200rpx; }
.card-shell { width: 100%; height: 64vh; position: relative; transform-style: preserve-3d; transition: transform .56s cubic-bezier(.2,.75,.2,1.04); }
.card-shell.is-flipped { transform: rotateY(180deg); }
.card-enter { animation: cardEnter .34s cubic-bezier(.22,.8,.25,1) both; }
.face {
  position: absolute; inset: 0; background: #121a2f; border: 1rpx solid #1f2a44; border-radius: 30rpx;
  box-shadow: 0 24rpx 40rpx rgba(0, 0, 0, 0.35);
  backface-visibility: hidden; -webkit-backface-visibility: hidden;
  padding: 44rpx 34rpx; display: flex; flex-direction: column; box-sizing: border-box;
}
.back { transform: rotateY(180deg); }
.face-label { color: #8ca0c7; font-size: 24rpx; }
.face-text { margin-top: 24rpx; color: #e7eeff; font-size: 36rpx; font-weight: 650; line-height: 1.62; }
.hint { margin-top: auto; color: #7084ad; font-size: 24rpx; }
.actions { display: flex; gap: 20rpx; padding: 0 30rpx 28rpx; opacity: 0; transform: translateY(30rpx); pointer-events: none; }
.actions.show { opacity: 1; transform: translateY(0); pointer-events: auto; animation: springIn .36s cubic-bezier(.18,.9,.26,1.2); }
.btn { flex: 1; height: 88rpx; line-height: 88rpx; border-radius: 44rpx; color: #fff; font-size: 30rpx; font-weight: 600; }
.blur { background: #475569; }
.master { background: linear-gradient(180deg, #2bb070 0%, #1f7a53 100%); }
.fly-left { animation: flyLeft .36s forwards cubic-bezier(.16,.84,.44,1); }
.fly-right { animation: flyRight .36s forwards cubic-bezier(.16,.84,.44,1); }
@keyframes flyLeft { 20% { transform: translateX(-12%) rotate(-2deg); } to { transform: translateX(-130%) rotate(-11deg); opacity: 0; } }
@keyframes flyRight { 20% { transform: translateX(12%) rotate(2deg); } to { transform: translateX(130%) rotate(11deg); opacity: 0; } }
@keyframes cardEnter { from { opacity: 0; transform: translateY(24rpx) scale(.97); } to { opacity: 1; transform: translateY(0) scale(1); } }
@keyframes springIn { 0% { transform: translateY(26rpx) scale(.95); } 60% { transform: translateY(-4rpx) scale(1.02); } 100% { transform: translateY(0) scale(1); } }
.empty { padding: 220rpx 30rpx 0; text-align: center; }
.empty-title { color: #d3dcf3; font-size: 30rpx; }
.empty-btn { margin-top: 20rpx; background: #1f7a53; color: #fff; border-radius: 40rpx; }
.finish-mask { position: fixed; inset: 0; background: rgba(7,10,18,.56); display: flex; align-items: center; justify-content: center; }
.finish-card { width: 78%; background: #121a2f; border: 1rpx solid #1f2a44; border-radius: 22rpx; padding: 40rpx 28rpx; text-align: center; }
.finish-title { color: #e7eeff; font-size: 34rpx; font-weight: 700; }
.finish-tip { display: block; margin-top: 12rpx; color: #8ca0c7; font-size: 24rpx; }
.finish-btn { margin-top: 24rpx; background: #1f7a53; color: #fff; border-radius: 40rpx; }
.bottom-nav {
  position: fixed; left: 24rpx; right: 24rpx; bottom: 26rpx;
  background: #121a2f; border: 1rpx solid #1f2a44; border-radius: 20rpx;
  height: 88rpx; display: flex; align-items: center;
}
.nav-item { flex: 1; text-align: center; color: #8ca0c7; font-size: 26rpx; }
.nav-item.active { color: #7ee0b5; font-weight: 600; }
</style>
