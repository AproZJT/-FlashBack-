const STORAGE_KEY = 'flashback_data_v2';
const REVIEW_PROGRESS_KEY = 'flashback_review_progress_v1';
const USER_PROFILE_KEY = 'flashback_user_profile_v1';

const defaultProfile = {
  userId: 'local_user_001',
  nickname: '学习者',
  goal: '每天掌握 10 个知识点',
  avatarText: '学'
};

const defaultData = {
  decks: [
    {
      id: 'deck-os',
      user_id: 'local_user_001',
      name: '操作系统-进程状态',
      createdAt: Date.now(),
      cards: [
        {
          id: 'card-os-1',
          user_id: 'local_user_001',
          deck_id: 'deck-os',
          front_text: '简述进程的三种基本状态',
          back_text: '就绪、运行、阻塞。就绪表示等待CPU，运行表示正在执行，阻塞表示等待I/O或事件。',
          review_count: 0,
          mastery_level: 0,
          last_review_time: 0
        },
        {
          id: 'card-os-2',
          user_id: 'local_user_001',
          deck_id: 'deck-os',
          front_text: '线程与进程的核心区别是什么？',
          back_text: '进程是资源分配基本单位，线程是CPU调度基本单位；同一进程内线程共享地址空间。',
          review_count: 0,
          mastery_level: 0,
          last_review_time: 0
        },
        {
          id: 'card-os-3',
          user_id: 'local_user_001',
          deck_id: 'deck-os',
          front_text: '什么是临界区？',
          back_text: '临界区是访问共享资源的代码段，多个线程同时进入会导致数据不一致。',
          review_count: 0,
          mastery_level: 0,
          last_review_time: 0
        },
        {
          id: 'card-os-4',
          user_id: 'local_user_001',
          deck_id: 'deck-os',
          front_text: '死锁产生的四个必要条件是什么？',
          back_text: '互斥、请求与保持、不可剥夺、循环等待。四个条件同时满足才会死锁。',
          review_count: 0,
          mastery_level: 0,
          last_review_time: 0
        },
        {
          id: 'card-os-5',
          user_id: 'local_user_001',
          deck_id: 'deck-os',
          front_text: '时间片轮转调度的特点是什么？',
          back_text: '每个进程分配固定时间片，时间片用完就切换，提升响应性但切换开销增加。',
          review_count: 0,
          mastery_level: 0,
          last_review_time: 0
        },
        {
          id: 'card-os-6',
          user_id: 'local_user_001',
          deck_id: 'deck-os',
          front_text: '虚拟内存的核心思想是什么？',
          back_text: '通过页式管理把逻辑地址映射到物理内存，按需调页，让程序看到更大的连续地址空间。',
          review_count: 0,
          mastery_level: 0,
          last_review_time: 0
        },
        {
          id: 'card-os-7',
          user_id: 'local_user_001',
          deck_id: 'deck-os',
          front_text: '页面置换算法中 FIFO 和 LRU 的区别？',
          back_text: 'FIFO淘汰最早进入内存的页；LRU淘汰最长时间未被访问的页，通常命中率更高。',
          review_count: 0,
          mastery_level: 0,
          last_review_time: 0
        },
        {
          id: 'card-os-8',
          user_id: 'local_user_001',
          deck_id: 'deck-os',
          front_text: '什么是系统调用？',
          back_text: '系统调用是用户态程序请求内核服务的接口，例如文件读写、进程创建、网络通信。',
          review_count: 0,
          mastery_level: 0,
          last_review_time: 0
        }
      ]
    },
    {
      id: 'deck-net',
      user_id: 'local_user_001',
      name: 'TCP/IP协议簇',
      createdAt: Date.now() + 1,
      cards: [
        {
          id: 'card-net-1',
          user_id: 'local_user_001',
          deck_id: 'deck-net',
          front_text: 'TCP 三次握手的三个步骤是什么？',
          back_text: '客户端发送 SYN，服务端返回 SYN+ACK，客户端再发 ACK，连接建立。',
          review_count: 0,
          mastery_level: 0,
          last_review_time: 0
        },
        {
          id: 'card-net-2',
          user_id: 'local_user_001',
          deck_id: 'deck-net',
          front_text: '为什么 TCP 需要四次挥手？',
          back_text: 'TCP 是全双工，双方关闭发送通道需分别发送 FIN 与 ACK，因此通常为四次。',
          review_count: 0,
          mastery_level: 0,
          last_review_time: 0
        },
        {
          id: 'card-net-3',
          user_id: 'local_user_001',
          deck_id: 'deck-net',
          front_text: 'TIME_WAIT 状态的作用是什么？',
          back_text: '保证最后 ACK 可重传并让旧连接报文在网络中消失，避免影响新连接。',
          review_count: 0,
          mastery_level: 0,
          last_review_time: 0
        },
        {
          id: 'card-net-4',
          user_id: 'local_user_001',
          deck_id: 'deck-net',
          front_text: 'TCP 和 UDP 的核心区别？',
          back_text: 'TCP 面向连接、可靠、有序；UDP 无连接、开销小、时延低但不保证可靠。',
          review_count: 0,
          mastery_level: 0,
          last_review_time: 0
        },
        {
          id: 'card-net-5',
          user_id: 'local_user_001',
          deck_id: 'deck-net',
          front_text: 'IP 地址和端口分别标识什么？',
          back_text: 'IP 标识主机，端口标识主机上的具体进程或服务。',
          review_count: 0,
          mastery_level: 0,
          last_review_time: 0
        },
        {
          id: 'card-net-6',
          user_id: 'local_user_001',
          deck_id: 'deck-net',
          front_text: 'ARP 协议解决什么问题？',
          back_text: '在局域网中根据目标 IP 地址解析出目标主机的 MAC 地址。',
          review_count: 0,
          mastery_level: 0,
          last_review_time: 0
        },
        {
          id: 'card-net-7',
          user_id: 'local_user_001',
          deck_id: 'deck-net',
          front_text: 'DNS 的作用与基本流程？',
          back_text: '将域名解析为 IP。通常先查本地缓存，再递归/迭代查询权威 DNS。',
          review_count: 0,
          mastery_level: 0,
          last_review_time: 0
        },
        {
          id: 'card-net-8',
          user_id: 'local_user_001',
          deck_id: 'deck-net',
          front_text: 'HTTP 与 HTTPS 的主要区别？',
          back_text: 'HTTPS 在 HTTP 基础上加 TLS 加密，提供机密性、完整性与身份认证。',
          review_count: 0,
          mastery_level: 0,
          last_review_time: 0
        },
        {
          id: 'card-net-9',
          user_id: 'local_user_001',
          deck_id: 'deck-net',
          front_text: '什么是 TCP 流量控制？',
          back_text: '接收方通过滑动窗口通告可接收数据量，发送方据此调整发送速率。',
          review_count: 0,
          mastery_level: 0,
          last_review_time: 0
        },
        {
          id: 'card-net-10',
          user_id: 'local_user_001',
          deck_id: 'deck-net',
          front_text: '什么是 TCP 拥塞控制？',
          back_text: '通过慢启动、拥塞避免、快重传、快恢复等机制，避免网络过载。',
          review_count: 0,
          mastery_level: 0,
          last_review_time: 0
        }
      ]
    }
  ]
};

function uid(prefix = 'id') {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

function clone(data) {
  return JSON.parse(JSON.stringify(data));
}

function normalizeText(text, maxLen = 80) {
  return String(text || '')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, maxLen);
}

function getUserProfileRaw() {
  const local = uni.getStorageSync(USER_PROFILE_KEY);
  if (!local || !local.userId) {
    uni.setStorageSync(USER_PROFILE_KEY, defaultProfile);
    return clone(defaultProfile);
  }
  if (!local.avatarText) {
    local.avatarText = (local.nickname || '学').slice(0, 1);
    uni.setStorageSync(USER_PROFILE_KEY, local);
  }
  return local;
}

function getCurrentUserId() {
  return getUserProfileRaw().userId;
}

function normalizeCard(card, deckId, userId, index = 0) {
  return {
    id: card.id || uid('card'),
    user_id: card.user_id || userId,
    deck_id: card.deck_id || deckId,
    front_text: card.front_text || card.front || '',
    back_text: card.back_text || card.back || '',
    review_count: Number(card.review_count || 0),
    mastery_level: Number(card.mastery_level || 0),
    last_review_time: Number(card.last_review_time || card.lastReviewedAt || 0),
    created_at: Number(card.created_at || card.createdAt || Date.now() + index)
  };
}

function migrateData(data) {
  const userId = getCurrentUserId();
  const decks = (data.decks || []).map(deck => {
    const deckId = deck.id || uid('deck');
    return {
      id: deckId,
      user_id: deck.user_id || userId,
      name: deck.name || '未命名卡片集',
      createdAt: deck.createdAt || Date.now(),
      cards: (deck.cards || []).map((card, index) => normalizeCard(card, deckId, deck.user_id || userId, index))
    };
  });
  return { decks };
}

function readData() {
  const local = uni.getStorageSync(STORAGE_KEY);
  if (!local || !local.decks) {
    const migrated = migrateData(defaultData);
    writeData(migrated);
    return migrated;
  }
  const migrated = migrateData(local);
  writeData(migrated);
  return migrated;
}

function writeData(data) {
  uni.setStorageSync(STORAGE_KEY, data);
}

function sortCardsByReviewPriority(cards) {
  return [...cards].sort((a, b) => {
    const levelPriority = level => {
      if (level === 0) return 4;
      if (level === 1) return 3;
      if (level === 2) return 2;
      return 1;
    };

    const p = levelPriority(Number(b.mastery_level || 0)) - levelPriority(Number(a.mastery_level || 0));
    if (p !== 0) return p;

    if (Number(a.mastery_level || 0) === 0 && Number(b.mastery_level || 0) === 0) {
      return Number(a.created_at || 0) - Number(b.created_at || 0);
    }

    return Number(a.last_review_time || 0) - Number(b.last_review_time || 0);
  });
}

export function getUserProfile() {
  return getUserProfileRaw();
}

export function updateUserProfile(payload) {
  const nickname = normalizeText(payload.nickname, 20);
  const goal = normalizeText(payload.goal, 32);
  if (!nickname) return { ok: false, message: '昵称不能为空' };
  if (!goal) return { ok: false, message: '学习目标不能为空' };

  const local = getUserProfileRaw();
  local.nickname = nickname;
  local.goal = goal;
  local.avatarText = nickname.slice(0, 1);
  uni.setStorageSync(USER_PROFILE_KEY, local);
  return { ok: true };
}

export function getDecks() {
  const userId = getCurrentUserId();
  return readData().decks.filter(item => item.user_id === userId);
}

export function createDeck(name) {
  const normalizedName = normalizeText(name, 32);
  if (!normalizedName) return { ok: false, message: '名称不能为空' };

  const data = readData();
  const userId = getCurrentUserId();
  const exists = data.decks.some(item => item.user_id === userId && item.name === normalizedName);
  if (exists) return { ok: false, message: '卡片集名称已存在' };

  data.decks.unshift({
    id: uid('deck'),
    user_id: userId,
    name: normalizedName,
    createdAt: Date.now(),
    cards: []
  });
  writeData(data);
  return { ok: true };
}

export function getDeckById(deckId) {
  const userId = getCurrentUserId();
  const data = readData();
  const deck = data.decks.find(item => item.id === deckId && item.user_id === userId);
  if (!deck) return null;
  return {
    ...deck,
    cards: sortCardsByReviewPriority(deck.cards)
  };
}

export function renameDeck(deckId, name) {
  const normalizedName = normalizeText(name, 32);
  if (!normalizedName) return { ok: false, message: '名称不能为空' };

  const data = readData();
  const userId = getCurrentUserId();
  const target = data.decks.find(item => item.id === deckId && item.user_id === userId);
  if (!target) return { ok: false, message: '卡片集不存在' };

  const exists = data.decks.some(item => item.id !== deckId && item.user_id === userId && item.name === normalizedName);
  if (exists) return { ok: false, message: '卡片集名称已存在' };

  target.name = normalizedName;
  writeData(data);
  return { ok: true };
}

export function addCard(deckId, payload) {
  const front = normalizeText(payload.front, 120);
  const back = normalizeText(payload.back, 500);
  if (!front) return { ok: false, message: '问题不能为空' };
  if (!back) return { ok: false, message: '解析不能为空' };

  const data = readData();
  const userId = getCurrentUserId();
  const target = data.decks.find(item => item.id === deckId && item.user_id === userId);
  if (!target) return { ok: false, message: '卡片集不存在' };

  target.cards.unshift({
    id: uid('card'),
    user_id: userId,
    deck_id: deckId,
    front_text: front,
    back_text: back,
    review_count: 0,
    mastery_level: 0,
    last_review_time: 0,
    created_at: Date.now()
  });
  writeData(data);
  return { ok: true };
}

export function updateCard(deckId, cardId, payload) {
  const front = normalizeText(payload.front, 120);
  const back = normalizeText(payload.back, 500);
  if (!front) return { ok: false, message: '问题不能为空' };
  if (!back) return { ok: false, message: '解析不能为空' };

  const data = readData();
  const userId = getCurrentUserId();
  const target = data.decks.find(item => item.id === deckId && item.user_id === userId);
  if (!target) return { ok: false, message: '卡片集不存在' };

  const card = target.cards.find(item => item.id === cardId && item.user_id === userId);
  if (!card) return { ok: false, message: '知识点不存在' };

  card.front_text = front;
  card.back_text = back;
  writeData(data);
  return { ok: true };
}

export function deleteCard(deckId, cardId) {
  const data = readData();
  const userId = getCurrentUserId();
  const target = data.decks.find(item => item.id === deckId && item.user_id === userId);
  if (!target) return { ok: false, message: '卡片集不存在' };

  target.cards = target.cards.filter(item => !(item.id === cardId && item.user_id === userId));
  writeData(data);
  return { ok: true };
}

export function reviewCard(deckId, cardId, feedback) {
  const data = readData();
  const userId = getCurrentUserId();
  const target = data.decks.find(item => item.id === deckId && item.user_id === userId);
  if (!target) return { ok: false, message: '卡片集不存在' };

  const card = target.cards.find(item => item.id === cardId && item.user_id === userId);
  if (!card) return { ok: false, message: '知识点不存在' };

  card.review_count = Number(card.review_count || 0) + 1;
  card.last_review_time = Date.now();

  if (feedback === 'forget') {
    card.mastery_level = 1;
  } else if (feedback === 'blur') {
    if (card.mastery_level === 3) card.mastery_level = 2;
    else card.mastery_level = 2;
  } else if (feedback === 'master') {
    card.mastery_level = 3;
  }

  writeData(data);
  return { ok: true, card: clone(card) };
}

export function getProgressMap() {
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
  const map = getProgressMap();
  return map[deckId];
}

export function clearReviewProgress(deckId) {
  const map = getProgressMap();
  delete map[deckId];
  uni.setStorageSync(REVIEW_PROGRESS_KEY, map);
}
