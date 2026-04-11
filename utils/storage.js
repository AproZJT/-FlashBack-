const API_BASE = (typeof process !== 'undefined' && process.env && process.env.VUE_APP_API_BASE)
  ? process.env.VUE_APP_API_BASE
  : 'http://127.0.0.1:8080/api';

const DEFAULT_USER_ID = (typeof process !== 'undefined' && process.env && process.env.VUE_APP_USER_ID)
  ? process.env.VUE_APP_USER_ID
  : 'local_user_001';

const TOKEN_KEY = 'flashback_auth_token_v1';
const USER_ID_KEY = 'flashback_user_id_v1';
const REVIEW_PROGRESS_KEY = 'flashback_review_progress_v1';

// Phase 2: offline-first
const DECKS_CACHE_KEY = 'flashback_cache_decks_v1';
const DUE_CACHE_KEY = 'flashback_cache_due_v1';
const OFFLINE_QUEUE_KEY = 'flashback_offline_queue_v1';
const OFFLINE_CONFLICT_KEY = 'flashback_offline_conflict_count_v1';

let loadingCount = 0;
let networkListenerBound = false;
let syncingQueue = false;

function beginLoading(withLoading = true) {
  if (!withLoading) return;
  loadingCount += 1;
  if (loadingCount === 1) uni.showLoading({ title: '加载中', mask: true });
}

function endLoading(withLoading = true) {
  if (!withLoading) return;
  loadingCount = Math.max(0, loadingCount - 1);
  if (loadingCount === 0) uni.hideLoading();
}

function getToken() {
  return uni.getStorageSync(TOKEN_KEY) || '';
}

function setToken(token) {
  uni.setStorageSync(TOKEN_KEY, token || '');
}

function clearToken() {
  uni.removeStorageSync(TOKEN_KEY);
}

function getCurrentUserId() {
  return uni.getStorageSync(USER_ID_KEY) || DEFAULT_USER_ID;
}

function setCurrentUserId(userId) {
  uni.setStorageSync(USER_ID_KEY, userId || DEFAULT_USER_ID);
}

function clearCurrentUserId() {
  uni.removeStorageSync(USER_ID_KEY);
}

function getOfflineQueue() {
  return uni.getStorageSync(OFFLINE_QUEUE_KEY) || [];
}

function setOfflineQueue(queue) {
  uni.setStorageSync(OFFLINE_QUEUE_KEY, queue || []);
}

function getOfflineConflictCount() {
  return Number(uni.getStorageSync(OFFLINE_CONFLICT_KEY) || 0);
}

function setOfflineConflictCount(value) {
  uni.setStorageSync(OFFLINE_CONFLICT_KEY, Number(value || 0));
}

function enqueueOfflineOperation(op) {
  const queue = getOfflineQueue();
  queue.push({
    id: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    createdAt: Date.now(),
    retries: 0,
    ...op
  });
  setOfflineQueue(queue);
}

function getDecksCache() {
  return uni.getStorageSync(DECKS_CACHE_KEY) || [];
}

function setDecksCache(decks) {
  uni.setStorageSync(DECKS_CACHE_KEY, decks || []);
}

function getDueCache() {
  return uni.getStorageSync(DUE_CACHE_KEY) || [];
}

function setDueCache(cards) {
  uni.setStorageSync(DUE_CACHE_KEY, cards || []);
}

function updateLocalCard(deckId, cardId, updater) {
  const decks = getDecksCache();
  const targetDeck = decks.find(d => d.id === deckId);
  if (!targetDeck || !Array.isArray(targetDeck.cards)) return;
  const idx = targetDeck.cards.findIndex(c => c.id === cardId);
  if (idx < 0) return;
  targetDeck.cards[idx] = updater({ ...targetDeck.cards[idx] });
  setDecksCache(decks);
}

function normalizeFailure(res, rawMessage) {
  const statusCode = res?.statusCode;
  if (statusCode === 401) {
    clearToken();
    return { ok: false, code: 401, message: '登录已过期，请重新登录' };
  }
  if (statusCode >= 500) {
    return { ok: false, code: statusCode, message: '服务异常，请稍后重试' };
  }
  return { ok: false, code: statusCode || -1, message: rawMessage || '请求失败' };
}

function request(url, method = 'GET', data, options = {}) {
  const { withLoading = false, auth = true } = options;
  beginLoading(withLoading);
  return new Promise((resolve) => {
    const headers = {};
    if (auth) {
      const token = getToken();
      if (token) headers.Authorization = `Bearer ${token}`;
    }

    uni.request({
      url: `${API_BASE}${url}`,
      method,
      data,
      timeout: 8000,
      header: headers,
      success: (res) => {
        const body = res.data || {};
        if (res.statusCode >= 400) {
          resolve(normalizeFailure(res, body.message));
          return;
        }
        if (body.ok === false) {
          resolve(normalizeFailure(res, body.message || '请求失败'));
          return;
        }
        resolve({ ok: true, data: body.data, code: res.statusCode || 200 });
      },
      fail: () => resolve({ ok: false, code: -1, message: '网络异常，请检查后端服务是否启动' }),
      complete: () => endLoading(withLoading)
    });
  });
}

function normalizeReviewFeedback(feedback) {
  if (feedback === 'forget') return 'again';
  if (feedback === 'blur') return 'hard';
  if (feedback === 'master') return 'good';
  return feedback;
}

function bindNetworkListenerIfNeeded() {
  if (networkListenerBound || typeof uni?.onNetworkStatusChange !== 'function') return;
  networkListenerBound = true;
  uni.onNetworkStatusChange((res) => {
    if (res?.isConnected) {
      syncOfflineQueue();
    }
  });
}

async function isNetworkConnected() {
  return new Promise((resolve) => {
    if (typeof uni?.getNetworkType !== 'function') {
      resolve(true);
      return;
    }
    uni.getNetworkType({
      success: (res) => resolve(res?.networkType !== 'none'),
      fail: () => resolve(true)
    });
  });
}

export async function syncOfflineQueue() {
  if (syncingQueue) return { ok: true, synced: 0, conflicts: 0 };
  if (!getToken()) return { ok: false, synced: 0, conflicts: 0, message: '未登录' };

  const connected = await isNetworkConnected();
  if (!connected) return { ok: false, synced: 0, conflicts: 0, message: '离线状态' };

  syncingQueue = true;
  try {
    const queue = [...getOfflineQueue()];
    const remain = [];
    let synced = 0;
    let conflicts = 0;

    for (const op of queue) {
      let res;
      if (op.type === 'review') {
        res = await request(`/decks/${op.deckId}/cards/${op.cardId}/review`, 'POST', {
          feedback: op.feedback,
          version: op.version
        }, { withLoading: false });
      } else if (op.type === 'updateCard') {
        res = await request(`/decks/${op.deckId}/cards/${op.cardId}`, 'PUT', {
          front: op.front,
          back: op.back,
          version: op.version
        }, { withLoading: false });
      } else {
        continue;
      }

      if (res.ok) {
        synced += 1;
        continue;
      }

      // 网络仍异常：保留队列，等待下次恢复
      if (res.code === -1) {
        remain.push(op);
        continue;
      }

      // version 冲突：丢弃该离线操作并提示刷新
      if ((res.message || '').includes('其他地方更新')) {
        conflicts += 1;
        continue;
      }

      // 其他失败：有限重试
      const retry = (op.retries || 0) + 1;
      if (retry <= 3) {
        remain.push({ ...op, retries: retry });
      }
    }

    setOfflineQueue(remain);
    setOfflineConflictCount(getOfflineConflictCount() + conflicts);
    return { ok: true, synced, conflicts };
  } finally {
    syncingQueue = false;
  }
}

export async function login(userId = DEFAULT_USER_ID) {
  bindNetworkListenerIfNeeded();
  const res = await request('/auth/login', 'POST', { userId }, { withLoading: true, auth: false });
  if (!res.ok) return { ok: false, message: res.message };
  const token = res.data?.token || '';
  if (!token) return { ok: false, message: '登录失败：token 缺失' };
  setToken(token);
  setCurrentUserId(userId);
  await syncOfflineQueue();
  return { ok: true, user: res.data?.user || null };
}

export async function ensureAuthed() {
  bindNetworkListenerIfNeeded();
  if (getToken()) {
    syncOfflineQueue();
    return { ok: true };
  }
  return login(getCurrentUserId());
}

export function logout() {
  clearToken();
  clearCurrentUserId();
}

export async function getUserProfile() {
  await ensureAuthed();
  const res = await request('/profile');
  return res.ok ? (res.data || { nickname: '', avatarText: '', goal: '' }) : { nickname: '', avatarText: '', goal: '' };
}

export async function updateUserProfile(payload) {
  await ensureAuthed();
  const res = await request('/profile', 'PUT', payload, { withLoading: true });
  if (!res.ok) return { ok: false, message: res.message };
  return { ok: true };
}

export async function getDecks() {
  await ensureAuthed();
  const res = await request('/decks');
  if (res.ok) {
    setDecksCache(res.data || []);
    return res.data || [];
  }
  return getDecksCache();
}

export async function getDueReviewCards() {
  await ensureAuthed();
  const res = await request('/review/due');
  if (res.ok) {
    setDueCache(res.data || []);
    return res.data || [];
  }
  return getDueCache();
}

export async function createDeck(name) {
  await ensureAuthed();
  const res = await request('/decks', 'POST', { name }, { withLoading: true });
  if (!res.ok) return { ok: false, message: res.message };
  await getDecks();
  return { ok: true, deck: res.data };
}

export async function getDeckById(deckId) {
  await ensureAuthed();
  const res = await request(`/decks/${deckId}`);
  if (res.ok) return res.data || null;
  const decks = getDecksCache();
  return decks.find(d => d.id === deckId) || null;
}

export async function renameDeck(deckId, name) {
  await ensureAuthed();
  const res = await request(`/decks/${deckId}/rename`, 'PUT', { name }, { withLoading: true });
  if (!res.ok) return { ok: false, message: res.message };
  await getDecks();
  return { ok: true };
}

export async function toggleDeckPublic(deckId, value) {
  await ensureAuthed();
  const res = await request(`/decks/${deckId}/public`, 'PUT', { value }, { withLoading: true });
  if (!res.ok) return { ok: false, message: res.message };
  return { ok: true };
}

export async function getPublicDecks(page = 1, pageSize = 20) {
  await ensureAuthed();
  const res = await request(`/market/decks?page=${page}&pageSize=${pageSize}`);
  return res.ok ? (res.data || []) : [];
}

export async function seedDemoData() {
  await ensureAuthed();
  const res = await request('/dev/seed-demo', 'POST', {}, { withLoading: true });
  if (!res.ok) return { ok: false, message: res.message };
  await getDecks();
  return { ok: true, data: res.data };
}

export async function clonePublicDeck(sourceDeckId) {
  await ensureAuthed();
  const res = await request(`/market/decks/${sourceDeckId}/clone`, 'POST', {}, { withLoading: true });
  if (!res.ok) return { ok: false, message: res.message };
  await getDecks();
  return { ok: true, deckId: res.data?.id };
}

export async function addCard(deckId, payload) {
  await ensureAuthed();
  const req = {
    front: payload.front,
    back: payload.back,
    frontImageUrl: payload.frontImageUrl || '',
    backImageUrl: payload.backImageUrl || '',
    audioUrl: payload.audioUrl || ''
  };
  const res = await request(`/decks/${deckId}/cards`, 'POST', req, { withLoading: true });
  if (!res.ok) return { ok: false, message: res.message };
  await getDecks();
  return { ok: true };
}

export async function updateCard(deckId, cardId, payload) {
  await ensureAuthed();
  const req = {
    front: payload.front,
    back: payload.back,
    frontImageUrl: payload.frontImageUrl || '',
    backImageUrl: payload.backImageUrl || '',
    audioUrl: payload.audioUrl || '',
    version: Number(payload.version || 0)
  };

  const res = await request(`/decks/${deckId}/cards/${cardId}`, 'PUT', req, { withLoading: true });
  if (res.ok) {
    await getDecks();
    return { ok: true };
  }

  if (res.code === -1) {
    enqueueOfflineOperation({ type: 'updateCard', deckId, cardId, ...req });
    updateLocalCard(deckId, cardId, (card) => ({ ...card, front_text: req.front, back_text: req.back, version: Number(card.version || 0) + 1 }));
    return { ok: true, offlineQueued: true };
  }

  return { ok: false, message: res.message };
}

export async function deleteCard(deckId, cardId) {
  await ensureAuthed();
  const res = await request(`/decks/${deckId}/cards/${cardId}`, 'DELETE', null, { withLoading: true });
  if (!res.ok) return { ok: false, message: res.message };
  await getDecks();
  return { ok: true };
}

export async function reviewCard(deckId, cardId, feedback, version) {
  await ensureAuthed();
  const normalized = normalizeReviewFeedback(feedback);
  const req = { feedback: normalized, version: Number(version || 0) };

  const res = await request(`/decks/${deckId}/cards/${cardId}/review`, 'POST', req, { withLoading: false });
  if (res.ok) {
    await getDecks();
    return { ok: true, card: res.data };
  }

  if (res.code === -1) {
    enqueueOfflineOperation({ type: 'review', deckId, cardId, ...req });
    updateLocalCard(deckId, cardId, (card) => {
      const next = { ...card, version: Number(card.version || 0) + 1 };
      if (normalized === 'again') next.mastery_level = 1;
      else if (normalized === 'hard') next.mastery_level = 1;
      else if (normalized === 'good') next.mastery_level = 2;
      else if (normalized === 'easy') next.mastery_level = 3;
      next.last_review_grade = ({ again: 0, hard: 1, good: 2, easy: 3 })[normalized] ?? 2;
      return next;
    });
    return { ok: true, offlineQueued: true };
  }

  return { ok: false, message: res.message };
}

export async function getStudyHeatmap(days = 120) {
  await ensureAuthed();
  const res = await request(`/study/heatmap?days=${days}`);
  return res.ok ? (res.data || []) : [];
}

export async function importCardsByCsv(csvContent, defaultDeckName = 'CSV导入') {
  await ensureAuthed();
  const res = await request('/import/csv', 'POST', { csvContent, defaultDeckName }, { withLoading: true });
  if (!res.ok) return { ok: false, message: res.message };
  await getDecks();
  return { ok: true, report: res.data };
}

function getProgressMap() {
  return uni.getStorageSync(REVIEW_PROGRESS_KEY) || {};
}

export function saveReviewProgress(deckId, progress) {
  const map = getProgressMap();
  map[deckId] = {
    index: progress.index || 0,
    stats: progress.stats || { again: 0, hard: 0, good: 0, easy: 0 },
    updatedAt: Date.now()
  };
  uni.setStorageSync(REVIEW_PROGRESS_KEY, map);
}

export function getReviewProgress(deckId) {
  return getProgressMap()[deckId];
}

export function clearReviewProgress(deckId) {
  const map = getProgressMap();
  delete map[deckId];
  uni.setStorageSync(REVIEW_PROGRESS_KEY, map);
}

export function getOfflineQueueMeta() {
  return {
    pending: getOfflineQueue().length,
    conflicts: getOfflineConflictCount()
  };
}

export function clearOfflineConflicts() {
  setOfflineConflictCount(0);
}
