const API_BASE = (typeof process !== 'undefined' && process.env && process.env.VUE_APP_API_BASE)
  ? process.env.VUE_APP_API_BASE
  : 'http://127.0.0.1:8080/api';

const DEFAULT_USER_ID = (typeof process !== 'undefined' && process.env && process.env.VUE_APP_USER_ID)
  ? process.env.VUE_APP_USER_ID
  : 'local_user_001';

const TOKEN_KEY = 'flashback_auth_token_v1';
const USER_ID_KEY = 'flashback_user_id_v1';
const REVIEW_PROGRESS_KEY = 'flashback_review_progress_v1';

let loadingCount = 0;

function beginLoading(withLoading = true) {
  if (!withLoading) return;
  loadingCount += 1;
  if (loadingCount === 1) {
    uni.showLoading({ title: '加载中', mask: true });
  }
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

export async function login(userId = DEFAULT_USER_ID) {
  const res = await request('/auth/login', 'POST', { userId }, { withLoading: true, auth: false });
  if (!res.ok) return { ok: false, message: res.message };
  const token = res.data?.token || '';
  if (!token) return { ok: false, message: '登录失败：token 缺失' };
  setToken(token);
  setCurrentUserId(userId);
  return { ok: true, user: res.data?.user || null };
}

export async function ensureAuthed() {
  if (getToken()) return { ok: true };
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
  return res.ok ? (res.data || []) : [];
}

export async function getDueReviewCards() {
  await ensureAuthed();
  const res = await request('/review/due');
  return res.ok ? (res.data || []) : [];
}

export async function createDeck(name) {
  await ensureAuthed();
  const res = await request('/decks', 'POST', { name }, { withLoading: true });
  if (!res.ok) return { ok: false, message: res.message };
  return { ok: true, deck: res.data };
}

export async function getDeckById(deckId) {
  await ensureAuthed();
  const res = await request(`/decks/${deckId}`);
  return res.ok ? (res.data || null) : null;
}

export async function renameDeck(deckId, name) {
  await ensureAuthed();
  const res = await request(`/decks/${deckId}/rename`, 'PUT', { name }, { withLoading: true });
  if (!res.ok) return { ok: false, message: res.message };
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

export async function clonePublicDeck(sourceDeckId) {
  await ensureAuthed();
  const res = await request(`/market/decks/${sourceDeckId}/clone`, 'POST', {}, { withLoading: true });
  if (!res.ok) return { ok: false, message: res.message };
  return { ok: true, deckId: res.data?.id };
}

export async function addCard(deckId, payload) {
  await ensureAuthed();
  const res = await request(`/decks/${deckId}/cards`, 'POST', payload, { withLoading: true });
  if (!res.ok) return { ok: false, message: res.message };
  return { ok: true };
}

export async function updateCard(deckId, cardId, payload) {
  await ensureAuthed();
  const res = await request(`/decks/${deckId}/cards/${cardId}`, 'PUT', payload, { withLoading: true });
  if (!res.ok) return { ok: false, message: res.message };
  return { ok: true };
}

export async function deleteCard(deckId, cardId) {
  await ensureAuthed();
  const res = await request(`/decks/${deckId}/cards/${cardId}`, 'DELETE', null, { withLoading: true });
  if (!res.ok) return { ok: false, message: res.message };
  return { ok: true };
}

export async function reviewCard(deckId, cardId, feedback) {
  await ensureAuthed();
  const res = await request(`/decks/${deckId}/cards/${cardId}/review`, 'POST', { feedback });
  if (!res.ok) return { ok: false, message: res.message };
  return { ok: true, card: res.data };
}

export async function getStudyHeatmap(days = 120) {
  await ensureAuthed();
  const res = await request(`/study/heatmap?days=${days}`);
  return res.ok ? (res.data || []) : [];
}

function getProgressMap() {
  return uni.getStorageSync(REVIEW_PROGRESS_KEY) || {};
}

export function saveReviewProgress(deckId, progress) {
  const map = getProgressMap();
  map[deckId] = {
    index: progress.index || 0,
    stats: progress.stats || { master: 0, blur: 0 },
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
