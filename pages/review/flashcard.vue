<template>
  <view class="page" @touchstart="onEdgeSwipeStart" @touchmove="onEdgeSwipeMove" @touchend="onEdgeSwipeEnd">
    <view class="top-nav">
      <view class="nav-left" @tap="goBack">‹ 返回</view>
      <view class="nav-title">闪回复习</view>
      <view class="nav-right">{{ currentIndex + 1 }} / {{ cards.length }}</view>
    </view>

    <view class="stats" v-if="cards.length">
      <text class="stats-item">Again {{ stats.again }}</text>
      <text class="stats-item">Hard {{ stats.hard }}</text>
      <text class="stats-item">Good {{ stats.good }}</text>
      <text class="stats-item">Easy {{ stats.easy }}</text>
    </view>

    <view class="stage" v-if="currentCard">
      <view
        class="card-shell"
        :class="[isFlipped ? 'is-flipped' : '', flyClass, cardMotionClass]"
        :style="{ transform: `translateX(${dragOffset}px)` }"
        @tap="flipCard"
        @touchstart.stop="onCardTouchStart"
        @touchmove.stop="onCardTouchMove"
        @touchend.stop="onCardTouchEnd"
      >
        <view class="face front">
          <text class="face-label">问题</text>
          <text class="face-text">{{ currentCard.front_text || currentCard.front || '' }}</text>
          <image v-if="currentCard.front_image_url" class="face-image" :src="currentCard.front_image_url" mode="widthFix" />
          <view v-if="currentCard.audio_url" class="audio-btn" @tap.stop="playAudio(currentCard.audio_url)">▶ 播放音频</view>
          <text class="hint">点击翻转查看解析 / 左右滑动提交反馈</text>
        </view>
        <view class="face back">
          <text class="face-label">解析</text>
          <text class="face-text">{{ currentCard.back_text || currentCard.back || '' }}</text>
          <image v-if="currentCard.back_image_url" class="face-image" :src="currentCard.back_image_url" mode="widthFix" />
          <view v-if="currentCard.audio_url" class="audio-btn" @tap.stop="playAudio(currentCard.audio_url)">▶ 播放音频</view>
        </view>
      </view>
    </view>

    <view class="actions" :class="{ show: isFlipped }">
      <button class="btn again" @tap.stop="answer('again')">Again</button>
      <button class="btn hard" @tap.stop="answer('hard')">Hard</button>
      <button class="btn good" @tap.stop="answer('good')">Good</button>
      <button class="btn easy" @tap.stop="answer('easy')">Easy</button>
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
      <view class="nav-item" @tap="goMarket">集市</view>
      <view class="nav-item" @tap="goUser">用户</view>
    </view>
  </view>
</template>

<script>
import {
  getDeckById,
  reviewCard,
  saveReviewProgress,
  getReviewProgress,
  clearReviewProgress
} from '@/utils/storage.js';

const SWIPE_SUBMIT_THRESHOLD = 72;
const SWIPE_STRONG_THRESHOLD = 130;

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
      dragOffset: 0,
      submitting: false,
      stats: {
        again: 0,
        hard: 0,
        good: 0,
        easy: 0
      },
      edgeSwipe: {
        tracking: false,
        startX: 0,
        startY: 0,
        shouldHandle: false
      },
      cardSwipe: {
        tracking: false,
        startX: 0,
        startY: 0
      }
    };
  },
  computed: {
    currentCard() {
      return this.cards[this.currentIndex];
    }
  },
  async onLoad(query) {
    this.deckId = query.deckId || '';
    await this.loadDeck();
    const saved = getReviewProgress(this.deckId);
    if (saved && this.cards.length) {
      this.currentIndex = Math.min(saved.index || 0, this.cards.length - 1);
      this.stats = saved.stats || { again: 0, hard: 0, good: 0, easy: 0 };
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
    async loadDeck() {
      const deck = await getDeckById(this.deckId);
      this.cards = deck?.cards || [];
    },
    onEdgeSwipeStart(event) {
      const touch = event.touches && event.touches[0];
      if (!touch) return;
      const { clientX, clientY } = touch;
      this.edgeSwipe.tracking = true;
      this.edgeSwipe.startX = clientX;
      this.edgeSwipe.startY = clientY;
      this.edgeSwipe.shouldHandle = clientX <= 28;
    },
    onEdgeSwipeMove(event) {
      if (!this.edgeSwipe.tracking || !this.edgeSwipe.shouldHandle) return;
      const touch = event.touches && event.touches[0];
      if (!touch) return;
      const deltaX = touch.clientX - this.edgeSwipe.startX;
      const deltaY = Math.abs(touch.clientY - this.edgeSwipe.startY);
      if (deltaX > 22 && deltaY < 42) {
        this.edgeSwipe.shouldHandle = false;
        this.goBack();
      }
    },
    onEdgeSwipeEnd() {
      this.edgeSwipe.tracking = false;
      this.edgeSwipe.shouldHandle = false;
    },
    onCardTouchStart(event) {
      const touch = event.touches && event.touches[0];
      if (!touch || this.submitting || !this.currentCard) return;
      this.cardSwipe.tracking = true;
      this.cardSwipe.startX = touch.clientX;
      this.cardSwipe.startY = touch.clientY;
      this.dragOffset = 0;
    },
    onCardTouchMove(event) {
      if (!this.cardSwipe.tracking || this.submitting) return;
      const touch = event.touches && event.touches[0];
      if (!touch) return;
      const deltaX = touch.clientX - this.cardSwipe.startX;
      const deltaY = Math.abs(touch.clientY - this.cardSwipe.startY);
      if (deltaY > 48) return;
      this.dragOffset = Math.max(-180, Math.min(180, deltaX));
    },
    async onCardTouchEnd() {
      if (!this.cardSwipe.tracking || this.submitting) return;
      this.cardSwipe.tracking = false;
      const x = this.dragOffset;
      this.dragOffset = 0;
      if (Math.abs(x) < SWIPE_SUBMIT_THRESHOLD) return;
      if (!this.isFlipped) {
        uni.showToast({ title: '请先翻转查看解析', icon: 'none' });
        return;
      }
      if (x < 0) {
        await this.answer(Math.abs(x) >= SWIPE_STRONG_THRESHOLD ? 'again' : 'hard');
      } else {
        await this.answer(Math.abs(x) >= SWIPE_STRONG_THRESHOLD ? 'easy' : 'good');
      }
    },
    goBack() {
      uni.navigateBack();
    },
    goHome() {
      uni.reLaunch({ url: '/pages/index/index' });
    },
    goMarket() {
      uni.reLaunch({ url: '/pages/market/index' });
    },
    goUser() {
      uni.reLaunch({ url: '/pages/user/profile' });
    },
    flipCard() {
      if (!this.currentCard || this.flyClass || this.cardMotionClass === 'card-enter') return;
      this.isFlipped = !this.isFlipped;
    },
    playAudio(url) {
      if (!url) return;
      const audio = uni.createInnerAudioContext();
      audio.src = url;
      audio.play();
    },
    async answer(type) {
      if (!this.currentCard || !this.isFlipped || this.flyClass || this.submitting) return;
      this.submitting = true;
      this.flyClass = type === 'again' || type === 'hard' ? 'fly-left' : 'fly-right';
      this.stats[type] = (this.stats[type] || 0) + 1;

      const version = Number(this.currentCard.version || 0);
      const result = await reviewCard(this.deckId, this.currentCard.id, type, version);
      if (!result.ok) {
        this.submitting = false;
        this.flyClass = '';
        uni.showToast({ title: result.message || '提交失败', icon: 'none' });
        await this.loadDeck();
        return;
      }

      setTimeout(async () => {
        await this.nextCard();
        this.submitting = false;
      }, 260);
    },
    playEnterMotion() {
      this.cardMotionClass = 'card-enter';
      setTimeout(() => {
        this.cardMotionClass = '';
      }, 320);
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
    async nextCard() {
      this.flyClass = '';
      this.isFlipped = false;
      this.dragOffset = 0;

      await this.loadDeck();

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
.page {
  min-height: 100vh;
  background: $fb-bg-page;
  display: flex;
  flex-direction: column;
  padding-bottom: 118rpx;
}
.top-nav {
  margin: 24rpx 24rpx 0;
  height: 86rpx;
  border-radius: 20rpx;
  background: $fb-bg-surface;
  border: 1rpx solid $fb-border;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20rpx;
}
.nav-left,.nav-right { color: $fb-text-secondary; font-size: 24rpx; }
.nav-title { color: $fb-text-primary; font-size: 28rpx; font-weight: 700; }
.stats { display: flex; justify-content: center; gap: 14rpx; margin-top: 14rpx; color: $fb-text-secondary; font-size: 22rpx; }
.stage { flex: 1; display: flex; align-items: center; justify-content: center; padding: 10rpx 30rpx 0; perspective: 1200rpx; }
.card-shell {
  width: 100%;
  height: 64vh;
  position: relative;
  transform-style: preserve-3d;
  transition: transform .42s cubic-bezier(.2,.75,.2,1.04);
}
.card-shell.is-flipped { transform: rotateY(180deg); }
.card-enter { animation: cardEnter .30s cubic-bezier(.22,.8,.25,1) both; }
.face {
  position: absolute;
  inset: 0;
  background: $fb-bg-surface;
  border: 1rpx solid $fb-border;
  border-radius: 30rpx;
  box-shadow: 0 16rpx 30rpx rgba(48, 102, 161, 0.12);
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  padding: 44rpx 34rpx;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
.back { transform: rotateY(180deg); }
.face-label { color: $fb-text-secondary; font-size: 24rpx; }
.face-text { margin-top: 24rpx; color: $fb-text-primary; font-size: 34rpx; font-weight: 650; line-height: 1.62; }
.face-image { margin-top: 16rpx; width: 100%; border-radius: 14rpx; border: 1rpx solid $fb-border; }
.audio-btn { margin-top: 12rpx; width: 220rpx; text-align: center; padding: 10rpx 14rpx; border-radius: 999rpx; background: #edf2ff; color: #365c95; font-size: 22rpx; }
.hint { margin-top: auto; color: $fb-text-secondary; font-size: 24rpx; }
.actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12rpx;
  padding: 0 30rpx 28rpx;
  opacity: 0;
  transform: translateY(30rpx);
  pointer-events: none;
}
.actions.show { opacity: 1; transform: translateY(0); pointer-events: auto; animation: springIn .30s cubic-bezier(.18,.9,.26,1.2); }
.btn { height: 84rpx; line-height: 84rpx; border-radius: 42rpx; color: #fff; font-size: 28rpx; font-weight: 700; }
.again { background: $fb-review-again; }
.hard { background: $fb-review-hard; }
.good { background: $fb-review-good; }
.easy { background: $fb-review-easy; }
.fly-left { animation: flyLeft .28s forwards cubic-bezier(.16,.84,.44,1); }
.fly-right { animation: flyRight .28s forwards cubic-bezier(.16,.84,.44,1); }
@keyframes flyLeft { 20% { transform: translateX(-12%) rotate(-2deg); } to { transform: translateX(-120%) rotate(-10deg); opacity: 0; } }
@keyframes flyRight { 20% { transform: translateX(12%) rotate(2deg); } to { transform: translateX(120%) rotate(10deg); opacity: 0; } }
@keyframes cardEnter { from { opacity: 0; transform: translateY(24rpx) scale(.97); } to { opacity: 1; transform: translateY(0) scale(1); } }
@keyframes springIn { 0% { transform: translateY(20rpx) scale(.95); } 100% { transform: translateY(0) scale(1); } }
.empty { padding: 220rpx 30rpx 0; text-align: center; }
.empty-title { color: $fb-text-primary; font-size: 30rpx; }
.empty-btn { margin-top: 20rpx; background: $fb-review-easy; color: #fff; border-radius: 40rpx; }
.finish-mask { position: fixed; inset: 0; background: rgba(21, 39, 64, .36); display: flex; align-items: center; justify-content: center; }
.finish-card { width: 78%; background: $fb-bg-surface; border: 1rpx solid $fb-border; border-radius: 22rpx; padding: 40rpx 28rpx; text-align: center; }
.finish-title { color: $fb-text-primary; font-size: 34rpx; font-weight: 700; }
.finish-tip { display: block; margin-top: 12rpx; color: $fb-text-secondary; font-size: 24rpx; }
.finish-btn { margin-top: 24rpx; background: $fb-review-easy; color: #fff; border-radius: 40rpx; }
.bottom-nav {
  position: fixed; left: 24rpx; right: 24rpx; bottom: 24rpx;
  background: $fb-bg-surface; border: 1rpx solid $fb-border; border-radius: 26rpx;
  box-shadow: 0 10rpx 24rpx rgba(88, 139, 193, 0.15);
  height: 94rpx; display: flex; align-items: center;
}
.nav-item { flex: 1; text-align: center; color: $fb-text-secondary; font-size: 26rpx; }
.nav-item.active { color: #5f41f6; font-weight: 700; }
</style>
