<template>
  <view class="page" @touchstart="onEdgeSwipeStart" @touchmove="onEdgeSwipeMove" @touchend="onEdgeSwipeEnd">
    <view class="top-nav">
      <view class="nav-left" @tap="goHome">‹ 首页</view>
      <view class="nav-title">{{ deck.name || '卡片管理' }}</view>
      <view class="nav-right" @tap="createCard">+ 新建</view>
    </view>

    <view class="publish-row">
      <text class="publish-title">发布到集市</text>
      <switch class="publish-switch" color="#22c55e" :checked="!!deck.is_public" @change="togglePublic" />
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
            hover-class="none"
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
      <view class="nav-item" @tap="goMarket">集市</view>
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
            <rich-text class="face-rich" :nodes="activeCardHtml"></rich-text>
            <image v-if="activeCard.back_image_url" class="popup-image" :src="activeCard.back_image_url" mode="widthFix" />
            <view v-if="activeCard.audio_url" class="popup-audio" @tap.stop="playAudio(activeCard.audio_url)">▶ 播放音频</view>
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
import { getDeckById, addCard, updateCard, deleteCard, reviewCard, toggleDeckPublic } from '@/utils/storage.js';
import { renderMarkdownToHtml } from '@/utils/markdown.js';

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
      activeCardHtml: '',
      reviewQueue: [],
      reviewQueueIndex: -1,
      edgeSwipe: {
        tracking: false,
        startX: 0,
        startY: 0,
        shouldHandle: false
      }
    };
  },
  async onLoad(query) {
    this.deckId = query.deckId || '';
    await this.loadDeck();
  },
  async onShow() {
    await this.loadDeck();
  },
  methods: {
    async loadDeck() {
      const target = await getDeckById(this.deckId);
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
      this.activeCardHtml = renderMarkdownToHtml(card.back_text || '');
      this.popupVisible = true;
      this.popupFlipped = false;
    },
    playAudio(url) {
      if (!url) return;
      const audio = uni.createInnerAudioContext();
      audio.src = url;
      audio.play();
    },
    async togglePublic(event) {
      const value = !!(event && event.detail && event.detail.value);
      const res = await toggleDeckPublic(this.deckId, value);
      if (!res.ok) {
        uni.showToast({ title: res.message, icon: 'none' });
        await this.loadDeck();
        return;
      }
      this.deck.is_public = value;
      uni.showToast({ title: value ? '已发布到集市' : '已取消发布', icon: 'none' });
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
    async submitFeedback(type) {
      if (this.submitting || !this.activeCard.id) return;
      if (!this.popupFlipped) {
        uni.showToast({ title: '请先翻转查看答案', icon: 'none' });
        return;
      }
      this.submitting = true;
      const result = await reviewCard(this.deckId, this.activeCard.id, type, Number(this.activeCard.version || 0));
      if (!result.ok) {
        uni.showToast({ title: result.message, icon: 'none' });
        this.submitting = false;
        return;
      }
      await this.loadDeck();

      if (this.reviewQueue.length && this.reviewQueueIndex >= 0) {
        this.reviewQueueIndex += 1;
        if (this.reviewQueueIndex < this.reviewQueue.length) {
          const next = this.reviewQueue[this.reviewQueueIndex];
          setTimeout(() => {
            this.activeCard = next;
            this.activeCardHtml = renderMarkdownToHtml(next.back_text || '');
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
            confirmText: '下一步',
            success: ({ confirm: ok, content: answer }) => {
              if (!ok || this.submitting) return;
              uni.showModal({
                title: '媒体 URL（可空）',
                editable: true,
                placeholderText: '格式：frontImg|backImg|audio',
                confirmText: '保存',
                success: async ({ confirm: yes, content: mediaRaw }) => {
                  if (!yes || this.submitting) return;
                  const parts = String(mediaRaw || '').split('|');
                  const frontImageUrl = (parts[0] || '').trim();
                  const backImageUrl = (parts[1] || '').trim();
                  const audioUrl = (parts[2] || '').trim();

                  this.submitting = true;
                  const result = await addCard(this.deckId, { front, back: answer, frontImageUrl, backImageUrl, audioUrl });
                  if (!result.ok) {
                    uni.showToast({ title: result.message, icon: 'none' });
                    this.submitting = false;
                    return;
                  }
                  await this.loadDeck();
                  uni.showToast({ title: '添加成功', icon: 'success' });
                  setTimeout(() => {
                    this.submitting = false;
                  }, 300);
                }
              });
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
        success: async ({ confirm }) => {
          if (!confirm || this.submitting) return;
          this.submitting = true;
          const result = await deleteCard(this.deckId, cardId);
          if (!result.ok) {
            uni.showToast({ title: result.message, icon: 'none' });
            this.submitting = false;
            return;
          }
          await this.loadDeck();
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
            confirmText: '下一步',
            success: ({ confirm: ok, content: answer }) => {
              if (!ok || this.submitting) return;
              uni.showModal({
                title: '媒体 URL（可空）',
                editable: true,
                placeholderText: `${card.front_image_url || ''}|${card.back_image_url || ''}|${card.audio_url || ''}`,
                confirmText: '保存',
                success: async ({ confirm: yes, content: mediaRaw }) => {
                  if (!yes || this.submitting) return;
                  const parts = String(mediaRaw || '').split('|');
                  const frontImageUrl = (parts[0] || '').trim();
                  const backImageUrl = (parts[1] || '').trim();
                  const audioUrl = (parts[2] || '').trim();

                  this.submitting = true;
                  const result = await updateCard(this.deckId, card.id, {
                    front,
                    back: answer,
                    frontImageUrl,
                    backImageUrl,
                    audioUrl,
                    version: Number(card.version || 0)
                  });
                  if (!result.ok) {
                    uni.showToast({ title: result.message, icon: 'none' });
                    this.submitting = false;
                    return;
                  }
                  await this.loadDeck();
                  uni.showToast({ title: '已更新', icon: 'none' });
                  setTimeout(() => {
                    this.submitting = false;
                  }, 250);
                }
              });
            }
          });
        }
      });
    },
    onEdgeSwipeStart(event) {
      if (this.popupVisible) return;
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
        this.goHome();
      }
    },
    onEdgeSwipeEnd() {
      this.edgeSwipe.tracking = false;
      this.edgeSwipe.shouldHandle = false;
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
.page { min-height: 100vh; background: $fb-bg-page; padding: 24rpx 24rpx 128rpx; box-sizing: border-box; }
.top-nav { height: 86rpx; border-radius: $fb-radius-card; background: $fb-bg-surface; border: 1rpx solid $fb-border; display: flex; align-items: center; justify-content: space-between; padding: 0 22rpx; }
.nav-left,.nav-right { color: $fb-text-accent; font-size: 25rpx; font-weight: 600; }
.nav-title { color: $fb-text-primary; font-size: 28rpx; font-weight: 700; max-width: 360rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.publish-row {
  margin-top: 14rpx;
  border: 1rpx solid #d8ebff;
  border-radius: 18rpx;
  background: #ffffff;
  box-shadow: 0 8rpx 18rpx rgba(84, 136, 194, 0.08);
  padding: 16rpx 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.publish-title { color: #4d6d9b; font-size: 25rpx; }
.publish-switch { transform: scale(0.9); transform-origin: right center; }
.list-wrap { margin-top: 14rpx; max-height: calc(100vh - 300rpx); }
.list-inner { display: flex; flex-direction: column; gap: 10rpx; }
.swipe-row { position: relative; overflow: hidden; border-radius: 18rpx; }
.delete-action { position: absolute; right: 0; top: 0; width: 164rpx; height: 100%; background: linear-gradient(180deg, #ef5d6a, #d93f4e); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 28rpx; }
.card-item { position: relative; z-index: 2; background: #ffffff; border: 1rpx solid #d8ebff; border-radius: 18rpx; padding: 20rpx; box-shadow: 0 8rpx 18rpx rgba(84, 136, 194, 0.08); transition: transform .18s ease; }
.card-row { display: flex; gap: 12rpx; align-items: center; }
.card-front { flex: 1; color: #244f88; font-size: 28rpx; line-height: 1.55; }
.status-tag { padding: 8rpx 14rpx; border-radius: 14rpx; font-size: 20rpx; }
.s-master { background: #def9ec; color: #178a57; }
.s-blur { background: #fff1db; color: #b7791f; }
.s-forget { background: #ffe3e5; color: #b6404a; }
.s-none { background: #edf2ff; color: #6a7ea7; }
.card-tip { display: block; margin-top: 8rpx; color: #6f88ae; font-size: 22rpx; }
.empty-wrap { margin-top: 220rpx; text-align: center; }
.empty-title { color: #5376a8; font-size: 32rpx; }
.empty-tip { color: #86a2c7; font-size: 23rpx; margin-top: 8rpx; display: block; }
.bottom-main { position: fixed; left: 0; right: 0; bottom: 126rpx; display: flex; justify-content: center; }
.start-btn {
  width: 86%; height: 88rpx; line-height: 88rpx; border-radius: 44rpx;
  background: linear-gradient(180deg, #35c58e 0%, #239666 100%);
  color: #fff; font-size: 30rpx; font-weight: 700;
}
.start-btn[disabled] { background: #b9c9e4; color: #f8fbff; }
.bottom-nav { position: fixed; left: 24rpx; right: 24rpx; bottom: 24rpx; background: #ffffff; border: 1rpx solid #d8ebff; border-radius: 26rpx; box-shadow: 0 10rpx 24rpx rgba(88, 139, 193, 0.15); height: 94rpx; display: flex; align-items: center; }
.nav-item { flex: 1; text-align: center; color: #7b95b9; font-size: 26rpx; }
.nav-item.active { color: #5f41f6; font-weight: 700; }
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
.face-rich {
  margin-top: 16rpx;
  color: #1f2937;
  font-size: 26rpx;
  line-height: 1.7;
}
.face-rich :deep(h1),
.face-rich :deep(h2),
.face-rich :deep(h3) {
  margin: 10rpx 0;
  color: #111827;
  font-weight: 700;
}
.face-rich :deep(strong) { font-weight: 700; }
.face-rich :deep(.md-inline-code) {
  padding: 2rpx 8rpx;
  border-radius: 8rpx;
  background: #f3f4f6;
  color: #b91c1c;
  font-size: 24rpx;
}
.face-rich :deep(.md-code) {
  margin: 14rpx 0;
  border-radius: 14rpx;
  background: #0f172a;
  overflow: hidden;
}
.face-rich :deep(.md-code-lang) {
  padding: 8rpx 14rpx;
  font-size: 20rpx;
  color: #cbd5e1;
  background: #1e293b;
  text-transform: lowercase;
}
.face-rich :deep(.md-code code) {
  display: block;
  padding: 14rpx;
  color: #e2e8f0;
  font-size: 22rpx;
  line-height: 1.65;
  font-family: Menlo, Monaco, Consolas, 'Courier New', monospace;
}
.face-rich :deep(.md-k) { color: #93c5fd; }
.face-rich :deep(.md-s) { color: #86efac; }
.face-rich :deep(.md-n) { color: #fca5a5; }
.face-rich :deep(.md-c) { color: #94a3b8; font-style: italic; }
.face-rich :deep(.md-b) { color: #fcd34d; }
.popup-image { margin-top: 14rpx; width: 100%; border-radius: 12rpx; border: 1rpx solid #d8ebff; }
.popup-audio { margin-top: 12rpx; width: 220rpx; text-align: center; padding: 10rpx 14rpx; border-radius: 999rpx; background: #edf2ff; color: #365c95; font-size: 22rpx; }
</style>
