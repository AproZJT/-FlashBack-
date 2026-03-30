<template>
  <view class="page">
    <view class="top-nav">
      <view class="nav-left" @tap="goHome">‹ 首页</view>
      <view class="nav-title">{{ deck.name || '卡片管理' }}</view>
      <view class="nav-right" @tap="createCard">+ 新建</view>
    </view>

    <scroll-view class="list-wrap" scroll-y>
      <view v-if="cards.length" class="list-inner">
        <view
          v-for="card in cards"
          :key="card.id"
          class="swipe-row"
          @touchstart="onTouchStart(card.id, $event)"
          @touchmove="onTouchMove(card.id, $event)"
          @touchend="onTouchEnd(card.id)"
        >
          <view class="delete-action" @tap="confirmDelete(card.id)">删除</view>
          <view
            class="card-item"
            :style="{ transform: `translateX(${swipeOffsetMap[card.id] || 0}rpx)` }"
            @tap="openReviewPopup(card)"
            @longpress="openEditCard(card)"
          >
            <view class="card-row">
              <text class="card-front">{{ card.front_text }}</text>
              <view class="status-tag" :class="statusClass(card.mastery_level)">{{ statusText(card.mastery_level) }}</view>
            </view>
            <text class="card-tip">点击复习 · 长按编辑 · 左滑删除</text>
          </view>
        </view>
      </view>
      <view v-else class="empty-wrap">
        <text class="empty-title">还没有知识点</text>
        <text class="empty-tip">点击右上角“新建”添加第一题</text>
      </view>
    </scroll-view>

    <view class="bottom-main">
      <button class="start-btn" @tap="startReviewFromFirstUnlearned" :disabled="!cards.length">开始复习</button>
    </view>

    <view class="bottom-nav">
      <view class="nav-item active" @tap="goHome">卡片</view>
      <view class="nav-item" @tap="goUser">用户</view>
    </view>

    <view v-if="popupVisible" class="mask" @tap="closePopup">
      <view class="popup-wrap" @tap.stop>
        <view class="flash-card" :class="{ flipped: popupFlipped }" @tap="popupFlip">
          <view class="face front">
            <text class="face-label">问题</text>
            <text class="face-text">{{ activeCard.front_text }}</text>
            <text class="face-tip">点击翻转查看答案</text>
          </view>
          <view class="face back">
            <text class="face-label">解析</text>
            <text class="face-text">{{ activeCard.back_text }}</text>
          </view>
        </view>

        <view class="popup-actions" :class="{ show: popupFlipped }">
          <button class="feedback-btn forget" @tap.stop="submitFeedback('forget')">忘记</button>
          <button class="feedback-btn blur" @tap.stop="submitFeedback('blur')">模糊</button>
          <button class="feedback-btn master" @tap.stop="submitFeedback('master')">掌握</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getDeckById, addCard, updateCard, deleteCard, reviewCard } from '@/utils/storage.js';

const MAX_LEFT = -164;

export default {
  data() {
    return {
      deckId: '',
      deck: {},
      cards: [],
      swipeOffsetMap: {},
      touchStartXMap: {},
      submitting: false,
      popupVisible: false,
      popupFlipped: false,
      activeCard: {},
      reviewQueue: [],
      reviewQueueIndex: -1
    };
  },
  onLoad(query) {
    this.deckId = query.deckId || '';
    this.loadDeck();
  },
  onShow() {
    this.loadDeck();
  },
  methods: {
    loadDeck() {
      const target = getDeckById(this.deckId);
      if (!target) {
        uni.showToast({ title: '卡片集不存在', icon: 'none' });
        setTimeout(() => this.goHome(), 500);
        return;
      }
      this.deck = target;
      this.cards = target.cards || [];
      this.swipeOffsetMap = {};
    },
    statusText(level) {
      if (level === 3) return '🟢 熟练';
      if (level === 2) return '🟡 模糊';
      if (level === 1) return '🔴 生疏';
      return '⚪ 未复习';
    },
    statusClass(level) {
      if (level === 3) return 's-master';
      if (level === 2) return 's-blur';
      if (level === 1) return 's-forget';
      return 's-none';
    },
    openReviewPopup(card) {
      this.activeCard = card;
      this.popupVisible = true;
      this.popupFlipped = false;
    },
    startReviewFromFirstUnlearned() {
      if (!this.cards.length) return;
      const ordered = [...this.cards].sort((a, b) => Number(a.created_at || 0) - Number(b.created_at || 0));
      const firstUnlearnedIndex = ordered.findIndex(item => Number(item.mastery_level || 0) === 0);
      const startIndex = firstUnlearnedIndex >= 0 ? firstUnlearnedIndex : 0;

      this.reviewQueue = ordered;
      this.reviewQueueIndex = startIndex;
      this.openReviewPopup(this.reviewQueue[this.reviewQueueIndex]);
    },
    popupFlip() {
      this.popupFlipped = !this.popupFlipped;
    },
    closePopup() {
      this.popupVisible = false;
      this.popupFlipped = false;
      this.activeCard = {};
      this.reviewQueue = [];
      this.reviewQueueIndex = -1;
    },
    submitFeedback(type) {
      if (this.submitting || !this.activeCard.id) return;
      if (!this.popupFlipped) {
        uni.showToast({ title: '请先翻转查看答案', icon: 'none' });
        return;
      }
      this.submitting = true;
      const result = reviewCard(this.deckId, this.activeCard.id, type);
      if (!result.ok) {
        uni.showToast({ title: result.message, icon: 'none' });
        this.submitting = false;
        return;
      }
      this.loadDeck();

      if (this.reviewQueue.length && this.reviewQueueIndex >= 0) {
        this.reviewQueueIndex += 1;
        if (this.reviewQueueIndex < this.reviewQueue.length) {
          const next = this.reviewQueue[this.reviewQueueIndex];
          setTimeout(() => {
            this.activeCard = next;
            this.popupFlipped = false;
            this.submitting = false;
          }, 200);
          return;
        }
      }

      setTimeout(() => {
        this.closePopup();
        this.reviewQueue = [];
        this.reviewQueueIndex = -1;
        this.submitting = false;
      }, 200);
    },
    createCard() {
      if (this.submitting) return;
      uni.showModal({
        title: '新建知识点',
        editable: true,
        placeholderText: '输入正面问题',
        confirmText: '下一步',
        success: ({ confirm, content }) => {
          if (!confirm || this.submitting) return;
          const front = content;
          uni.showModal({
            title: '填写解析',
            editable: true,
            placeholderText: '输入背面答案解析',
            confirmText: '保存',
            success: ({ confirm: ok, content: answer }) => {
              if (!ok || this.submitting) return;
              this.submitting = true;
              const result = addCard(this.deckId, { front, back: answer });
              if (!result.ok) {
                uni.showToast({ title: result.message, icon: 'none' });
                this.submitting = false;
                return;
              }
              this.loadDeck();
              uni.showToast({ title: '添加成功', icon: 'success' });
              setTimeout(() => {
                this.submitting = false;
              }, 300);
            }
          });
        }
      });
    },
    onTouchStart(cardId, event) {
      this.touchStartXMap[cardId] = event.touches[0].clientX;
    },
    onTouchMove(cardId, event) {
      const startX = this.touchStartXMap[cardId];
      if (startX === undefined) return;
      const delta = event.touches[0].clientX - startX;
      const offset = Math.max(MAX_LEFT, Math.min(0, delta));
      this.$set(this.swipeOffsetMap, cardId, offset);
    },
    onTouchEnd(cardId) {
      const current = this.swipeOffsetMap[cardId] || 0;
      this.$set(this.swipeOffsetMap, cardId, current < -82 ? MAX_LEFT : 0);
      delete this.touchStartXMap[cardId];
    },
    confirmDelete(cardId) {
      if (this.submitting) return;
      uni.showModal({
        title: '确认删除',
        content: '删除后无法恢复',
        success: ({ confirm }) => {
          if (!confirm || this.submitting) return;
          this.submitting = true;
          const result = deleteCard(this.deckId, cardId);
          if (!result.ok) {
            uni.showToast({ title: result.message, icon: 'none' });
            this.submitting = false;
            return;
          }
          this.loadDeck();
          uni.showToast({ title: '已删除', icon: 'none' });
          setTimeout(() => {
            this.submitting = false;
          }, 250);
        }
      });
    },
    openEditCard(card) {
      if (this.submitting) return;
      uni.showModal({
        title: '编辑问题',
        editable: true,
        placeholderText: card.front_text,
        confirmText: '下一步',
        success: ({ confirm, content }) => {
          if (!confirm || this.submitting) return;
          const front = content;
          uni.showModal({
            title: '编辑解析',
            editable: true,
            placeholderText: card.back_text,
            confirmText: '保存',
            success: ({ confirm: ok, content: answer }) => {
              if (!ok || this.submitting) return;
              this.submitting = true;
              const result = updateCard(this.deckId, card.id, { front, back: answer });
              if (!result.ok) {
                uni.showToast({ title: result.message, icon: 'none' });
                this.submitting = false;
                return;
              }
              this.loadDeck();
              uni.showToast({ title: '已更新', icon: 'none' });
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
.page { min-height: 100vh; background: #0b1020; padding: 24rpx 24rpx 128rpx; box-sizing: border-box; }
.top-nav { height: 86rpx; border-radius: 20rpx; background: #121a2f; border: 1rpx solid #1f2a44; display: flex; align-items: center; justify-content: space-between; padding: 0 22rpx; }
.nav-left,.nav-right { color: #7ee0b5; font-size: 25rpx; }
.nav-title { color: #e5edff; font-size: 28rpx; font-weight: 600; max-width: 360rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.list-wrap { margin-top: 18rpx; max-height: calc(100vh - 240rpx); }
.list-inner { display: flex; flex-direction: column; gap: 14rpx; }
.swipe-row { position: relative; overflow: hidden; border-radius: 18rpx; }
.delete-action { position: absolute; right: 0; top: 0; width: 164rpx; height: 100%; background: linear-gradient(180deg, #ef5d6a, #d93f4e); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 28rpx; }
.card-item { position: relative; z-index: 2; background: #121a2f; border: 1rpx solid #1f2a44; border-radius: 18rpx; padding: 22rpx; transition: transform .18s ease; }
.card-row { display: flex; gap: 12rpx; align-items: center; }
.card-front { flex: 1; color: #e7eeff; font-size: 28rpx; line-height: 1.55; }
.status-tag { padding: 8rpx 14rpx; border-radius: 14rpx; font-size: 20rpx; }
.s-master { background: rgba(34,197,94,.18); color: #86efac; }
.s-blur { background: rgba(245,158,11,.18); color: #fcd34d; }
.s-forget { background: rgba(248,113,113,.18); color: #fca5a5; }
.s-none { background: rgba(148,163,184,.18); color: #cbd5e1; }
.card-tip { display: block; margin-top: 10rpx; color: #8ca0c7; font-size: 22rpx; }
.empty-wrap { margin-top: 260rpx; text-align: center; }
.empty-title { color: #d3dcf3; font-size: 32rpx; }
.empty-tip { color: #8ca0c7; font-size: 23rpx; margin-top: 8rpx; display: block; }
.bottom-main { position: fixed; left: 0; right: 0; bottom: 126rpx; display: flex; justify-content: center; }
.start-btn {
  width: 86%; height: 88rpx; line-height: 88rpx; border-radius: 44rpx;
  background: linear-gradient(180deg, #2bb070 0%, #1f7a53 100%);
  color: #fff; font-size: 30rpx; font-weight: 600;
}
.start-btn[disabled] { background: #475569; color: #9fb0d4; }
.bottom-nav { position: fixed; left: 24rpx; right: 24rpx; bottom: 26rpx; background: #121a2f; border: 1rpx solid #1f2a44; border-radius: 20rpx; height: 88rpx; display: flex; align-items: center; }
.nav-item { flex: 1; text-align: center; color: #8ca0c7; font-size: 26rpx; }
.nav-item.active { color: #7ee0b5; font-weight: 600; }
.mask { position: fixed; inset: 0; background: rgba(8,12,24,.68); display: flex; align-items: center; justify-content: center; z-index: 30; padding: 30rpx; box-sizing: border-box; }
.popup-wrap { width: 100%; }
.flash-card { width: 100%; height: 56vh; position: relative; transform-style: preserve-3d; transition: transform .52s cubic-bezier(.2,.75,.2,1.04); }
.flash-card.flipped { transform: rotateY(180deg); }
.face { position: absolute; inset: 0; background: #fff; border-radius: 28rpx; box-shadow: 0 26rpx 46rpx rgba(0,0,0,.35); backface-visibility: hidden; -webkit-backface-visibility: hidden; padding: 40rpx 34rpx; box-sizing: border-box; display: flex; flex-direction: column; }
.face.back { transform: rotateY(180deg); }
.face-label { color: #6b7280; font-size: 24rpx; }
.face-text { margin-top: 20rpx; color: #111827; font-size: 34rpx; font-weight: 600; line-height: 1.6; }
.face-tip { margin-top: auto; color: #9ca3af; font-size: 24rpx; }
.popup-actions { margin-top: 24rpx; display: flex; gap: 14rpx; opacity: 0; transform: translateY(18rpx); pointer-events: none; transition: all .24s ease; }
.popup-actions.show { opacity: 1; transform: translateY(0); pointer-events: auto; }
.feedback-btn { flex: 1; height: 84rpx; line-height: 84rpx; border-radius: 42rpx; color: #fff; font-size: 28rpx; font-weight: 600; }
.feedback-btn.forget { background: #ef4444; }
.feedback-btn.blur { background: #f59e0b; }
.feedback-btn.master { background: #16a34a; }
</style>
