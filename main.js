// 导入根组件 App.vue
import App from './App'

// ============ Vue 2 配置 ============
// #ifndef VUE3
// 导入 Vue 框架
import Vue from 'vue'
// 导入 Promise 适配器（用于兼容旧版本的异步操作）
import './uni.promisify.adaptor'

// 关闭 Vue 生产提示
Vue.config.productionTip = false
// 标记应用类型为小程序应用
App.mpType = 'app'
// 创建 Vue 实例，使用 App 组件配置
const app = new Vue({
  ...App
})
// 将 Vue 实例挂载到 DOM（#app 元素）
app.$mount()
// #endif

// ============ Vue 3 配置 ============
// #ifdef VUE3
// 导入 Vue 3 的 SSR 应用创建函数
import { createSSRApp } from 'vue'
// 导出创建应用的工厂函数
export function createApp() {
  // 使用 App 组件创建 SSR 应用实例
  const app = createSSRApp(App)
  return {
    app
  }
}
// #endif