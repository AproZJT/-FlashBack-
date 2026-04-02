<template>
  <view class="page">
    <view class="top-nav">
      <view class="nav-left" @tap="goBack">‹ 返回</view>
      <view class="nav-title">CSV 导入（后端解析）</view>
      <view class="nav-right" @tap="goHome">首页</view>
    </view>

    <view class="panel">
      <text class="label">默认卡片集名称（CSV 未填写 deckName 时使用）</text>
      <input v-model="defaultDeckName" class="input" placeholder="例如：CSV导入" />

      <text class="label">CSV 内容（支持列：front,back,deckName,imageUrl,audioUrl）</text>
      <textarea
        v-model="csvText"
        class="editor"
        placeholder="front,back,deckName,imageUrl,audioUrl\nTCP 三次握手,用于建立可靠连接,网络基础,https://img.com/a.png,https://audio.com/a.mp3"
      />

      <view class="actions">
        <button class="btn" @tap="fillTemplate">填充示例</button>
        <button class="btn primary" :disabled="submitting" @tap="doImport">开始导入</button>
      </view>

      <view v-if="report" class="report">
        <text>成功：{{ report.success }}</text>
        <text>失败：{{ report.fail }}</text>
        <text>总计：{{ report.total }}</text>
        <text>涉及卡组：{{ report.deckCount }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { importCardsByCsv } from '@/utils/storage.js';

export default {
  data() {
    return {
      csvText: '',
      defaultDeckName: 'CSV导入',
      report: null,
      submitting: false
    };
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    goHome() {
      uni.reLaunch({ url: '/pages/index/index' });
    },
    fillTemplate() {
      this.csvText = [
        'front,back,deckName,imageUrl,audioUrl',
        '什么是 TCP 三次握手,用于建立可靠连接并同步序列号,网络基础,https://example.com/front1.png,https://example.com/a1.mp3',
        '哈希冲突怎么处理,链地址法与开放地址法,算法基础,https://example.com/front2.png,https://example.com/a2.mp3'
      ].join('\n');
    },
    async doImport() {
      if (this.submitting) return;
      if (!this.csvText.trim()) {
        uni.showToast({ title: '请填写 CSV 内容', icon: 'none' });
        return;
      }
      this.submitting = true;
      const res = await importCardsByCsv(this.csvText, this.defaultDeckName || 'CSV导入');
      this.submitting = false;
      if (!res.ok) {
        uni.showToast({ title: res.message || '导入失败', icon: 'none' });
        return;
      }
      this.report = res.report || null;
      uni.showToast({ title: '导入完成', icon: 'success' });
    }
  }
};
</script>

<style lang="scss">
.page { min-height: 100vh; background: $fb-bg-page; padding: 24rpx; box-sizing: border-box; }
.top-nav { height: 86rpx; border-radius: $fb-radius-card; background: $fb-bg-surface; border: 1rpx solid $fb-border; display:flex; align-items:center; justify-content:space-between; padding:0 20rpx; }
.nav-left,.nav-right { color:$fb-text-accent; font-size:24rpx; }
.nav-title { color:$fb-text-primary; font-size:28rpx; font-weight:700; }
.panel { margin-top: 16rpx; background:$fb-bg-surface; border:1rpx solid $fb-border; border-radius:18rpx; padding:20rpx; }
.label { display:block; color:$fb-text-secondary; font-size:24rpx; margin-top:10rpx; }
.input { height:76rpx; border:1rpx solid $fb-border; border-radius:14rpx; margin-top:8rpx; padding:0 14rpx; color:$fb-text-primary; background:$fb-bg-surface; }
.editor { width:100%; height:380rpx; border:1rpx solid $fb-border; border-radius:14rpx; margin-top:8rpx; padding:12rpx; box-sizing:border-box; color:$fb-text-primary; }
.actions { margin-top: 16rpx; display:flex; gap:12rpx; }
.btn { flex:1; height:82rpx; line-height:82rpx; border-radius:40rpx; background:#e5eefc; color:#35598f; }
.btn.primary { background:$fb-review-easy; color:#fff; }
.btn[disabled] { background:#b9c9e4; color:#eef4ff; }
.report { margin-top:14rpx; display:flex; flex-wrap:wrap; gap:16rpx; color:$fb-text-secondary; }
</style>
