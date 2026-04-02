<template>
  <view class="page">
    <view class="top-nav">
      <view class="nav-left" @tap="goBack">‹ 返回</view>
      <view class="nav-title">CSV 导入（MVP）</view>
      <view class="nav-right" @tap="goHome">首页</view>
    </view>

    <view class="panel">
      <text class="label">目标卡片集</text>
      <picker :range="decks" range-key="name" @change="onDeckChange">
        <view class="picker">{{ selectedDeckName }}</view>
      </picker>

      <text class="label">CSV 内容（front,back）</text>
      <textarea
        v-model="csvText"
        class="editor"
        placeholder="front,back\n进程和线程区别,线程是调度单位"
      />

      <view class="actions">
        <button class="btn" @tap="fillTemplate">填充示例</button>
        <button class="btn primary" @tap="doImport">开始导入</button>
      </view>

      <view v-if="report" class="report">
        <text>成功：{{ report.success }}</text>
        <text>失败：{{ report.fail }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getDecks, addCard } from '@/utils/storage.js';

export default {
  data() {
    return {
      decks: [],
      selectedDeckIndex: 0,
      csvText: '',
      report: null,
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
    goBack() {
      uni.navigateBack();
    },
    goHome() {
      uni.reLaunch({ url: '/pages/index/index' });
    },
    onDeckChange(e) {
      this.selectedDeckIndex = Number(e.detail.value || 0);
    },
    fillTemplate() {
      this.csvText = 'front,back\n什么是 TCP 三次握手,用于建立可靠连接并同步序列号\n哈希冲突怎么处理,链地址法与开放地址法';
    },
    parseCSV(raw) {
      const rows = (raw || '').split(/\r?\n/).map(s => s.trim()).filter(Boolean);
      if (!rows.length) return [];
      const body = rows.slice(1);
      return body.map((line) => {
        const idx = line.indexOf(',');
        if (idx <= 0) return null;
        const front = line.slice(0, idx).trim();
        const back = line.slice(idx + 1).trim();
        if (!front || !back) return null;
        return { front, back };
      }).filter(Boolean);
    },
    async doImport() {
      if (this.submitting) return;
      if (!this.decks.length) {
        uni.showToast({ title: '请先创建卡片集', icon: 'none' });
        return;
      }
      const deckId = this.decks[this.selectedDeckIndex]?.id;
      const list = this.parseCSV(this.csvText);
      if (!list.length) {
        uni.showToast({ title: 'CSV 内容格式不正确', icon: 'none' });
        return;
      }
      this.submitting = true;
      let success = 0;
      let fail = 0;
      for (const item of list) {
        const res = await addCard(deckId, item);
        if (res.ok) success += 1;
        else fail += 1;
      }
      this.report = { success, fail };
      this.submitting = false;
      uni.showToast({ title: `导入完成：${success}/${list.length}`, icon: 'none' });
    }
  }
};
</script>

<style lang="scss">
.page { min-height: 100vh; background: #f8fbff; padding: 24rpx; box-sizing: border-box; }
.top-nav { height: 86rpx; border-radius: 20rpx; background: #fff; border: 1rpx solid #d8ebff; display:flex; align-items:center; justify-content:space-between; padding:0 20rpx; }
.nav-left,.nav-right { color:#6b4dfb; font-size:24rpx; }
.nav-title { color:#2362b2; font-size:28rpx; font-weight:700; }
.panel { margin-top: 16rpx; background:#fff; border:1rpx solid #d8ebff; border-radius:18rpx; padding:20rpx; }
.label { display:block; color:#4d6d9b; font-size:24rpx; margin-top:10rpx; }
.picker { margin-top:8rpx; height:76rpx; line-height:76rpx; border:1rpx solid #d8ebff; border-radius:14rpx; padding:0 14rpx; color:#244f88; }
.editor { width:100%; height:360rpx; border:1rpx solid #d8ebff; border-radius:14rpx; margin-top:8rpx; padding:12rpx; box-sizing:border-box; color:#244f88; }
.actions { margin-top: 16rpx; display:flex; gap:12rpx; }
.btn { flex:1; height:82rpx; line-height:82rpx; border-radius:40rpx; background:#e5eefc; color:#35598f; }
.btn.primary { background:#16a34a; color:#fff; }
.report { margin-top:14rpx; display:flex; gap:16rpx; color:#5f7ca8; }
</style>
