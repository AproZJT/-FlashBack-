const API_BASE = (typeof process !== 'undefined' && process.env && process.env.VUE_APP_API_BASE)
  ? process.env.VUE_APP_API_BASE
  : 'http://127.0.0.1:8080/api';
const USER_ID = (typeof process !== 'undefined' && process.env && process.env.VUE_APP_USER_ID)
  ? process.env.VUE_APP_USER_ID
  : 'local_user_001';
const REVIEW_PROGRESS_KEY = 'flashback_review_progress_v1';

function request(url, method = 'GET', data) {
  return new Promise((resolve) => {
    uni.request({
      url: `${API_BASE}${url}${url.includes('?') ? '&' : '?'}userId=${encodeURIComponent(USER_ID)}`,
      method,
      data,
      timeout: 6000,
      success: (res) => {
        const body = res.data || {};
        if (body.ok === false) {
          resolve({ ok: false, message: body.message || '请求失败' });
          return;
        }
        resolve({ ok: true, data: body.data });
      },
      fail: () => resolve({ ok: false, message: '无法连接后端服务，请确认 Spring Boot 已启动' })
    });
  });
}

export async function getUserProfile() {
  const res = await request('/profile');
  return res.ok ? (res.data || { nickname: '', avatarText: '', goal: '' }) : { nickname: '', avatarText: '', goal: '' };
}

export async function updateUserProfile(payload) {
  const res = await request('/profile', 'PUT', payload);
  if (!res.ok) return { ok: false, message: res.message };
  return { ok: true };
}

export async function getDecks() {
  const res = await request('/decks');
  return res.ok ? (res.data || []) : [];
}

export async function getDueReviewCards() {
  const res = await request('/review/due');
  return res.ok ? (res.data || []) : [];
}

export async function createDeck(name) {
  const res = await request('/decks', 'POST', { name });
  if (!res.ok) return { ok: false, message: res.message };
  return { ok: true, deck: res.data };
}

export async function getDeckById(deckId) {
  const res = await request(`/decks/${deckId}`);
  return res.ok ? (res.data || null) : null;
}

export async function renameDeck(deckId, name) {
  const res = await request(`/decks/${deckId}/rename`, 'PUT', { name });
  if (!res.ok) return { ok: false, message: res.message };
  return { ok: true };
}

export async function toggleDeckPublic(deckId, value) {
  const res = await request(`/decks/${deckId}/public`, 'PUT', { value });
  if (!res.ok) return { ok: false, message: res.message };
  return { ok: true };
}

export async function getPublicDecks(page = 1, pageSize = 20) {
  const res = await request(`/market/decks?page=${page}&pageSize=${pageSize}`);
  return res.ok ? (res.data || []) : [];
}

export async function clonePublicDeck(sourceDeckId) {
  const res = await request(`/market/decks/${sourceDeckId}/clone`, 'POST', {});
  if (!res.ok) return { ok: false, message: res.message };
  return { ok: true, deckId: res.data?.id };
}

export async function addCard(deckId, payload) {
  const res = await request(`/decks/${deckId}/cards`, 'POST', payload);
  if (!res.ok) return { ok: false, message: res.message };
  return { ok: true };
}

export async function updateCard(deckId, cardId, payload) {
  const res = await request(`/decks/${deckId}/cards/${cardId}`, 'PUT', payload);
  if (!res.ok) return { ok: false, message: res.message };
  return { ok: true };
}

export async function deleteCard(deckId, cardId) {
  const res = await request(`/decks/${deckId}/cards/${cardId}`, 'DELETE');
  if (!res.ok) return { ok: false, message: res.message };
  return { ok: true };
}

export async function reviewCard(deckId, cardId, feedback) {
  const res = await request(`/decks/${deckId}/cards/${cardId}/review`, 'POST', { feedback });
  if (!res.ok) return { ok: false, message: res.message };
  return { ok: true, card: res.data };
}

export async function getStudyHeatmap(days = 120) {
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
