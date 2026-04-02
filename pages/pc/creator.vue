<template>
  <view class="page">
    <view class="top-nav">
      <text class="title">PC 轻量制卡台</text>
      <view class="actions-right">
        <view class="action" @tap="goImport">CSV 导入</view>
        <view class="action" @tap="goHome">返回首页</view>
      </view>
    </view>

    <view class="toolbar">
      <picker :range="decks" range-key="name" @change="onDeckChange">
        <view class="picker">{{ selectedDeckName }}</view>
      </picker>
      <button class="btn" @tap="addRow">+ 新增一行</button>
      <button class="btn primary" :disabled="submitting" @tap="batchCreate">批量创建</button>
    </view>

    <scroll-view class="table-wrap" scroll-y>
      <view class="thead row">
        <text class="col idx">#</text>
        <text class="col front">Front</text>
        <text class="col back">Back</text>
        <text class="col media">Image URL</text>
        <text class="col media">Audio URL</text>
      </view>

      <view class="row" v-for="(row, i) in rows" :key="i">
        <text class="col idx">{{ i + 1 }}</text>
        <input class="col front input" v-model="row.front" placeholder="问题" />
        <input class="col back input" v-model="row.back" placeholder="答案" />
        <input class="col media input" v-model="row.imageUrl" placeholder="https://..." />
        <input class="col media input" v-model="row.audioUrl" placeholder="https://..." />
      </view>
    </scroll-view>

    <view class="foot">
      <text class="tip">支持快速编辑 + 批量创建 + CSV 导入</text>
    </view>
  </view>
</template>

<script>
import { getDecks, addCard } from '@/utils/storage.js';

function newRow() {
  return { front: '', back: '', imageUrl: '', audioUrl: '' };
}

export default {
  data() {
    return {
      decks: [],
      selectedDeckIndex: 0,
      rows: [newRow(), newRow(), newRow(), newRow(), newRow()],
      submitting: false
    };
  },
  computed: {
    selectedDeckName() {
      if (!this.decks.length) return '暂无卡片集';
      return this.decks[this.selectedDeckIndex]?.name || this.decks[0].name;
    }
  },
  async onShow() {
    this.decks = await getDecks();
    if (this.selectedDeckIndex >= this.decks.length) this.selectedDeckIndex = 0;
  },
  methods: {
    goHome() {
      uni.reLaunch({ url: '/pages/index/index' });
    },
    goImport() {
      uni.navigateTo({ url: '/pages/import/csv' });
    },
    onDeckChange(e) {
      this.selectedDeckIndex = Number(e.detail.value || 0);
    },
    addRow() {
      this.rows.push(newRow());
    },
    async batchCreate() {
      if (this.submitting) return;
      if (!this.decks.length) {
        uni.showToast({ title: '请先创建卡片集', icon: 'none' });
        return;
      }
      const deckId = this.decks[this.selectedDeckIndex]?.id;
      const list = this.rows.filter(r => (r.front || '').trim() && (r.back || '').trim());
      if (!list.length) {
        uni.showToast({ title: '请至少填写一行 front/back', icon: 'none' });
        return;
      }
      this.submitting = true;
      let success = 0;
      for (const row of list) {
        const res = await addCard(deckId, {
          front: row.front.trim(),
          back: row.back.trim(),
          frontImageUrl: row.imageUrl.trim(),
          backImageUrl: row.imageUrl.trim(),
          audioUrl: row.audioUrl.trim()
        });
        if (res.ok) success += 1;
      }
      this.submitting = false;
      uni.showToast({ title: `已创建 ${success}/${list.length}`, icon: 'none' });
    }
  }
};
</script>

<style lang="scss">
.page { min-height: 100vh; background: $fb-bg-page; padding: 16rpx; box-sizing: border-box; }
.top-nav { height: 82rpx; background: $fb-bg-surface; border: 1rpx solid $fb-border; border-radius: 16rpx; display:flex; align-items:center; justify-content:space-between; padding: 0 16rpx; }
.title { color:$fb-text-primary; font-size:30rpx; font-weight:700; }
.actions-right { display:flex; gap: 12rpx; }
.action { color:$fb-text-accent; font-size:24rpx; }
.toolbar { margin-top: 12rpx; background:$fb-bg-surface; border:1rpx solid $fb-border; border-radius:16rpx; padding:12rpx; display:flex; gap:10rpx; align-items:center; }
.picker { width: 280rpx; height: 70rpx; line-height:70rpx; border:1rpx solid $fb-border; border-radius:12rpx; padding:0 12rpx; color:$fb-text-primary; }
.btn { height:70rpx; line-height:70rpx; border-radius:36rpx; background:#e5eefc; color:#35598f; padding: 0 20rpx; }
.btn.primary { background:$fb-review-easy; color:#fff; }
.table-wrap { margin-top: 12rpx; height: calc(100vh - 290rpx); background:$fb-bg-surface; border:1rpx solid $fb-border; border-radius:16rpx; }
.row { display:flex; align-items:center; border-bottom:1rpx solid #edf4ff; padding: 8rpx 10rpx; }
.thead { background:#f3f8ff; position: sticky; top: 0; z-index: 3; }
.col { color:$fb-text-primary; font-size:22rpx; }
.idx { width: 56rpx; }
.front { width: 24%; }
.back { width: 30%; }
.media { width: 23%; }
.input { height: 62rpx; border:1rpx solid $fb-border; border-radius:10rpx; padding: 0 10rpx; box-sizing:border-box; background:$fb-bg-surface; color:$fb-text-primary; }
.foot { margin-top: 10rpx; }
.tip { color:$fb-text-secondary; font-size:22rpx; }
</style>
