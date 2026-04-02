package com.flashback.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.backend.exception.BizException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class FlashBackService {
    private static final long HOUR = 60L * 60L * 1000L;
    private static final long DAY = 24L * HOUR;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public FlashBackService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    private String userKey(String userId) { return "fb:user:" + userId; }
    private String userDeckSetKey(String userId) { return "fb:user:decks:" + userId; }
    private String deckKey(String deckId) { return "fb:deck:" + deckId; }
    private String deckCardSetKey(String deckId) { return "fb:deck:cards:" + deckId; }
    private String cardKey(String cardId) { return "fb:card:" + cardId; }
    private String reviewZsetKey(String userId) { return "fb:review:zset:" + userId; }
    private String heatBitmapKey(String userId, int year) { return "fb:study:bitmap:" + userId + ":" + year; }
    private String heatCountHashKey(String userId, int yearMonth) { return "fb:study:count:" + userId + ":" + yearMonth; }
    private String marketDeckZsetKey() { return "fb:market:decks"; }

    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private Map<String, Object> fromJsonMap(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try { return objectMapper.readValue(json, new TypeReference<Map<String, Object>>(){}); }
        catch (Exception e) { return new HashMap<>(); }
    }

    private List<Map<String, Object>> cardsByDeck(String deckId) {
        Set<String> cardIds = redis.opsForSet().members(deckCardSetKey(deckId));
        if (cardIds == null || cardIds.isEmpty()) return new ArrayList<>();

        List<String> keys = cardIds.stream().map(this::cardKey).toList();
        List<String> raws = redis.opsForValue().multiGet(keys);
        if (raws == null || raws.isEmpty()) return new ArrayList<>();

        List<Map<String, Object>> cards = raws.stream()
                .filter(Objects::nonNull)
                .map(this::fromJsonMap)
                .filter(map -> !map.isEmpty())
                .sorted(Comparator.comparingLong(c -> ((Number) c.getOrDefault("created_at", 0)).longValue()))
                .toList();

        return new ArrayList<>(cards);
    }

    public Map<String, Object> ensureUser(String userId) {
        String raw = redis.opsForValue().get(userKey(userId));
        if (raw != null) return fromJsonMap(raw);
        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("nickname", "学习者");
        user.put("goal", "每天掌握 10 个知识点");
        user.put("avatarText", "学");
        redis.opsForValue().set(userKey(userId), toJson(user));
        seedIfEmpty(userId);
        return user;
    }

    public Map<String, Object> updateUser(String userId, Map<String, Object> payload) {
        Map<String, Object> user = ensureUser(userId);
        String nickname = String.valueOf(payload.getOrDefault("nickname", "")).trim();
        String goal = String.valueOf(payload.getOrDefault("goal", "")).trim();
        if (!nickname.isEmpty()) {
            user.put("nickname", nickname);
            user.put("avatarText", nickname.substring(0, 1));
        }
        if (!goal.isEmpty()) user.put("goal", goal);
        redis.opsForValue().set(userKey(userId), toJson(user));
        return user;
    }

    public List<Map<String, Object>> getDecks(String userId) {
        ensureUser(userId);
        Set<String> deckIds = redis.opsForSet().members(userDeckSetKey(userId));
        List<Map<String, Object>> decks = new ArrayList<>();
        if (deckIds == null) return decks;
        for (String deckId : deckIds) {
            String raw = redis.opsForValue().get(deckKey(deckId));
            if (raw == null) continue;
            Map<String, Object> deck = fromJsonMap(raw);
            deck.put("cards", cardsByDeck(deckId));
            decks.add(deck);
        }
        decks.sort((a, b) -> Long.compare(((Number)b.getOrDefault("createdAt",0)).longValue(), ((Number)a.getOrDefault("createdAt",0)).longValue()));
        return decks;
    }

    public Map<String, Object> createDeck(String userId, String name) {
        long createdAt = System.currentTimeMillis();
        String deckId = "deck_" + createdAt + "_" + UUID.randomUUID().toString().substring(0, 6);
        Map<String, Object> deck = new HashMap<>();
        deck.put("id", deckId);
        deck.put("user_id", userId);
        deck.put("name", name);
        deck.put("is_public", false);
        deck.put("createdAt", createdAt);
        redis.opsForValue().set(deckKey(deckId), toJson(deck));
        redis.opsForSet().add(userDeckSetKey(userId), deckId);
        return deck;
    }

    public Map<String, Object> getDeckById(String userId, String deckId) {
        String raw = redis.opsForValue().get(deckKey(deckId));
        if (raw == null) return null;
        Map<String, Object> deck = fromJsonMap(raw);
        if (!userId.equals(deck.get("user_id"))) return null;
        deck.put("cards", cardsByDeck(deckId));
        return deck;
    }

    public boolean renameDeck(String userId, String deckId, String name) {
        Map<String, Object> deck = getDeckById(userId, deckId);
        if (deck == null) return false;
        deck.put("name", name);
        deck.remove("cards");
        redis.opsForValue().set(deckKey(deckId), toJson(deck));
        return true;
    }

    public boolean toggleDeckPublic(String userId, String deckId, boolean value) {
        Map<String, Object> deck = getDeckById(userId, deckId);
        if (deck == null) return false;
        deck.put("is_public", value);
        deck.remove("cards");
        redis.opsForValue().set(deckKey(deckId), toJson(deck));
        if (value) {
            long score = ((Number) deck.getOrDefault("createdAt", System.currentTimeMillis())).longValue();
            redis.opsForZSet().add(marketDeckZsetKey(), deckId, score);
        } else {
            redis.opsForZSet().remove(marketDeckZsetKey(), deckId);
        }
        return true;
    }

    public Map<String, Object> addCard(String userId, String deckId, Map<String, Object> payload) {
        Map<String, Object> deck = getDeckById(userId, deckId);
        if (deck == null) return null;
        long now = System.currentTimeMillis();
        String cardId = "card_" + now + "_" + UUID.randomUUID().toString().substring(0, 6);
        Map<String, Object> card = new HashMap<>();
        card.put("id", cardId);
        card.put("user_id", userId);
        card.put("deck_id", deckId);
        card.put("front_text", String.valueOf(payload.getOrDefault("front", "")));
        card.put("back_text", String.valueOf(payload.getOrDefault("back", "")));
        card.put("review_count", 0);
        card.put("mastery_level", 0);
        card.put("last_review_time", 0);
        card.put("next_review_time", 0);
        card.put("version", 0);
        card.put("created_at", now);
        redis.opsForValue().set(cardKey(cardId), toJson(card));
        redis.opsForSet().add(deckCardSetKey(deckId), cardId);
        redis.opsForZSet().add(reviewZsetKey(userId), cardId, 0);
        return card;
    }

    public boolean updateCard(String userId, String deckId, String cardId, Map<String, Object> payload, long expectedVersion) {
        Map<String, Object> card = fromJsonMap(redis.opsForValue().get(cardKey(cardId)));
        if (card.isEmpty()) return false;
        if (!userId.equals(card.get("user_id")) || !deckId.equals(card.get("deck_id"))) return false;

        long currentVersion = ((Number) card.getOrDefault("version", 0)).longValue();
        if (currentVersion != expectedVersion) {
            throw new BizException("知识点已在其他地方更新，请刷新后重试");
        }

        card.put("front_text", String.valueOf(payload.getOrDefault("front", card.getOrDefault("front_text", ""))));
        card.put("back_text", String.valueOf(payload.getOrDefault("back", card.getOrDefault("back_text", ""))));
        card.put("version", currentVersion + 1);
        redis.opsForValue().set(cardKey(cardId), toJson(card));
        return true;
    }

    public boolean deleteCard(String userId, String deckId, String cardId) {
        Map<String, Object> card = fromJsonMap(redis.opsForValue().get(cardKey(cardId)));
        if (card.isEmpty()) return false;
        if (!userId.equals(card.get("user_id")) || !deckId.equals(card.get("deck_id"))) return false;
        redis.delete(cardKey(cardId));
        redis.opsForSet().remove(deckCardSetKey(deckId), cardId);
        redis.opsForZSet().remove(reviewZsetKey(userId), cardId);
        return true;
    }

    public Map<String, Object> reviewCard(String userId, String deckId, String cardId, String feedback, long expectedVersion) {
        Map<String, Object> card = fromJsonMap(redis.opsForValue().get(cardKey(cardId)));
        if (card.isEmpty()) return null;
        if (!userId.equals(card.get("user_id")) || !deckId.equals(card.get("deck_id"))) return null;

        long currentVersion = ((Number) card.getOrDefault("version", 0)).longValue();
        if (currentVersion != expectedVersion) {
            throw new BizException("知识点已在其他地方更新，请刷新后重试");
        }

        long now = System.currentTimeMillis();
        int level = ((Number) card.getOrDefault("mastery_level", 0)).intValue();
        if ("forget".equals(feedback)) level = 1;
        if ("blur".equals(feedback)) level = 2;
        if ("master".equals(feedback)) level = 3;

        long nextReview = calcNextReviewTime(level, feedback, now);
        card.put("mastery_level", level);
        card.put("review_count", ((Number) card.getOrDefault("review_count", 0)).intValue() + 1);
        card.put("last_review_time", now);
        card.put("next_review_time", nextReview);
        card.put("version", currentVersion + 1);
        redis.opsForValue().set(cardKey(cardId), toJson(card));

        redis.opsForZSet().add(reviewZsetKey(userId), cardId, nextReview);
        markStudyToday(userId);
        return card;
    }

    public List<Map<String, Object>> getDueCards(String userId) {
        long now = System.currentTimeMillis();
        Set<String> ids = redis.opsForZSet().rangeByScore(reviewZsetKey(userId), 0, now);
        if (ids == null) return List.of();
        List<Map<String, Object>> list = new ArrayList<>();
        for (String id : ids) {
            Map<String, Object> card = fromJsonMap(redis.opsForValue().get(cardKey(id)));
            if (card.isEmpty()) continue;
            String deckId = String.valueOf(card.get("deck_id"));
            Map<String, Object> deck = fromJsonMap(redis.opsForValue().get(deckKey(deckId)));
            card.put("deckId", deckId);
            card.put("deckName", String.valueOf(deck.getOrDefault("name", "")));
            list.add(card);
        }
        return list;
    }

    public List<Map<String, Object>> getPublicDecks(String userId, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.min(Math.max(1, pageSize), 50);
        long start = (long) (safePage - 1) * safePageSize;
        long end = start + safePageSize - 1;

        Set<String> deckIds = redis.opsForZSet().reverseRange(marketDeckZsetKey(), start, end);
        if (deckIds == null || deckIds.isEmpty()) return List.of();

        List<Map<String, Object>> list = new ArrayList<>();
        for (String deckId : deckIds) {
            Map<String, Object> deck = fromJsonMap(redis.opsForValue().get(deckKey(deckId)));
            if (deck.isEmpty()) {
                redis.opsForZSet().remove(marketDeckZsetKey(), deckId);
                continue;
            }
            if (!Boolean.TRUE.equals(deck.get("is_public"))) {
                redis.opsForZSet().remove(marketDeckZsetKey(), deckId);
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("id", deckId);
            item.put("name", deck.get("name"));
            item.put("user_id", deck.get("user_id"));
            item.put("card_count", Optional.ofNullable(redis.opsForSet().size(deckCardSetKey(deckId))).orElse(0L));
            item.put("is_mine", userId.equals(deck.get("user_id")));
            list.add(item);
        }
        return list;
    }

    public Map<String, Object> clonePublicDeck(String userId, String sourceDeckId) {
        Map<String, Object> source = fromJsonMap(redis.opsForValue().get(deckKey(sourceDeckId)));
        if (source.isEmpty() || !Boolean.TRUE.equals(source.get("is_public"))) return null;

        Map<String, Object> target = createDeck(userId, source.get("name") + "-副本");
        String targetDeckId = String.valueOf(target.get("id"));
        List<Map<String, Object>> cards = cardsByDeck(sourceDeckId);
        for (Map<String, Object> c : cards) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("front", c.getOrDefault("front_text", ""));
            payload.put("back", c.getOrDefault("back_text", ""));
            addCard(userId, targetDeckId, payload);
        }
        return target;
    }

    public List<Map<String, Object>> getHeatmap(String userId, int days) {
        List<Map<String, Object>> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            int year = d.getYear();
            int offset = d.getDayOfYear() - 1;
            int yearMonth = year * 100 + d.getMonthValue();
            String dayField = String.format("%02d", d.getDayOfMonth());

            Boolean done = redis.opsForValue().getBit(heatBitmapKey(userId, year), offset);
            Object rawCount = redis.opsForHash().get(heatCountHashKey(userId, yearMonth), dayField);
            int count = 0;
            if (rawCount != null) {
                try {
                    count = Integer.parseInt(String.valueOf(rawCount));
                } catch (NumberFormatException ignored) {
                    count = 0;
                }
            } else if (Boolean.TRUE.equals(done)) {
                count = 1;
            }

            Map<String, Object> item = new HashMap<>();
            item.put("date", d.toString());
            item.put("count", count);
            list.add(item);
        }
        return list;
    }

    private long calcNextReviewTime(int mastery, String feedback, long now) {
        if ("forget".equals(feedback)) return now + 12 * HOUR;
        if ("blur".equals(feedback)) return now + DAY;
        return switch (mastery) {
            case 1 -> now + 2 * DAY;
            case 2 -> now + 4 * DAY;
            case 3 -> now + 7 * DAY;
            default -> now + DAY;
        };
    }

    private void markStudyToday(String userId) {
        LocalDate now = LocalDate.now();
        int dayOffset = now.getDayOfYear() - 1;
        int yearMonth = now.getYear() * 100 + now.getMonthValue();
        String dayField = String.format("%02d", now.getDayOfMonth());

        redis.opsForValue().setBit(heatBitmapKey(userId, now.getYear()), dayOffset, true);
        redis.expire(heatBitmapKey(userId, now.getYear()), 400, TimeUnit.DAYS);

        Long count = redis.opsForHash().increment(heatCountHashKey(userId, yearMonth), dayField, 1);
        if (count != null) {
            redis.expire(heatCountHashKey(userId, yearMonth), 400, TimeUnit.DAYS);
        }
    }

    private void seedIfEmpty(String userId) {
        Long size = redis.opsForSet().size(userDeckSetKey(userId));
        if (size != null && size > 0) return;

        // ========== 我的卡组：覆盖核心学习场景 ==========
        Map<String, Object> osDeck = createDeck(userId, "操作系统-进程与线程");
        toggleDeckPublic(userId, String.valueOf(osDeck.get("id")), true);
        String osDeckId = String.valueOf(osDeck.get("id"));

        Map<String, Object> os1 = addCard(userId, osDeckId, Map.of(
                "front", "简述进程的三种基本状态",
                "back", "就绪、运行、阻塞。\\n\\n```text\\n就绪: 等待 CPU\\n运行: 占用 CPU\\n阻塞: 等待事件\\n```"
        ));
        Map<String, Object> os2 = addCard(userId, osDeckId, Map.of(
                "front", "进程与线程的核心区别是什么？",
                "back", "进程是资源分配基本单位，线程是 CPU 调度基本单位；同一进程线程共享地址空间。"
        ));
        Map<String, Object> os3 = addCard(userId, osDeckId, Map.of(
                "front", "死锁的四个必要条件是什么？",
                "back", "**互斥**、**请求并保持**、**不可剥夺**、**循环等待**。"
        ));

        Map<String, Object> algoDeck = createDeck(userId, "算法基础-数组/哈希/双指针");
        String algoDeckId = String.valueOf(algoDeck.get("id"));
        Map<String, Object> al1 = addCard(userId, algoDeckId, Map.of(
                "front", "两数之和的常见优化思路是什么？",
                "back", "使用哈希表记录已访问数字和下标。\\n\\n```js\\nconst map = new Map();\\nfor (let i = 0; i < nums.length; i += 1) {\\n  const target = total - nums[i];\\n  if (map.has(target)) return [map.get(target), i];\\n  map.set(nums[i], i);\\n}\\n```"
        ));
        Map<String, Object> al2 = addCard(userId, algoDeckId, Map.of(
                "front", "哈希冲突常见处理方式有哪些？",
                "back", "**链地址法** 与 **开放地址法**。"
        ));
        Map<String, Object> al3 = addCard(userId, algoDeckId, Map.of(
                "front", "双指针适合哪类问题？",
                "back", "有序数组、回文串、滑动窗口、原地去重等可通过左右指针线性推进优化复杂度。"
        ));

        Map<String, Object> feDeck = createDeck(userId, "前端工程化-构建与发布");
        toggleDeckPublic(userId, String.valueOf(feDeck.get("id")), true);
        String feDeckId = String.valueOf(feDeck.get("id"));
        Map<String, Object> fe1 = addCard(userId, feDeckId, Map.of(
                "front", "Tree Shaking 生效的核心前提是什么？",
                "back", "依赖 ESM 静态分析。\\n\\n```js\\nexport function used() {}\\nexport function unused() {}\\n```"
        ));
        Map<String, Object> fe2 = addCard(userId, feDeckId, Map.of(
                "front", "为什么资源文件需要加 hash 指纹？",
                "back", "用于**长期缓存**与精准失效，提升线上加载稳定性。"
        ));
        Map<String, Object> fe3 = addCard(userId, feDeckId, Map.of(
                "front", "CI/CD 的基本价值是什么？",
                "back", "自动化构建、测试、部署，降低人工发布风险并提升迭代速度。"
        ));

        Map<String, Object> netDeck = createDeck(userId, "计算机网络-HTTP/TCP");
        String netDeckId = String.valueOf(netDeck.get("id"));
        Map<String, Object> n1 = addCard(userId, netDeckId, Map.of(
                "front", "HTTP/1.1 与 HTTP/2 的关键差异？",
                "back", "HTTP/2 支持多路复用、头部压缩、二进制分帧，减少队头阻塞。"
        ));
        Map<String, Object> n2 = addCard(userId, netDeckId, Map.of(
                "front", "TCP 三次握手的目的？",
                "back", "同步双方初始序列号，确认收发能力，建立可靠连接。"
        ));
        Map<String, Object> n3 = addCard(userId, netDeckId, Map.of(
                "front", "为什么会出现 TIME_WAIT？",
                "back", "确保被动关闭方收到最后 ACK，并让旧连接报文在网络中自然消亡。"
        ));

        Map<String, Object> dbDeck = createDeck(userId, "数据库系统-索引与事务");
        toggleDeckPublic(userId, String.valueOf(dbDeck.get("id")), true);
        String dbDeckId = String.valueOf(dbDeck.get("id"));
        Map<String, Object> db1 = addCard(userId, dbDeckId, Map.of(
                "front", "B+ 树为什么更适合数据库索引？",
                "back", "树高低、范围查询友好、磁盘 IO 次数少。"
        ));
        Map<String, Object> db2 = addCard(userId, dbDeckId, Map.of(
                "front", "事务的 ACID 分别是什么？",
                "back", "Atomicity / Consistency / Isolation / Durability"
        ));
        Map<String, Object> db3 = addCard(userId, dbDeckId, Map.of(
                "front", "可重复读与读已提交的区别？",
                "back", "可重复读避免不可重复读；读已提交只保证读取到已提交数据。"
        ));

        // ========== 复习行为：构造 forget / blur / master 混合轨迹 ==========
        reviewCard(userId, osDeckId, String.valueOf(os1.get("id")), "forget", 0);
        reviewCard(userId, osDeckId, String.valueOf(os2.get("id")), "blur", 0);
        reviewCard(userId, osDeckId, String.valueOf(os3.get("id")), "master", 0);

        reviewCard(userId, algoDeckId, String.valueOf(al1.get("id")), "master", 0);
        reviewCard(userId, algoDeckId, String.valueOf(al2.get("id")), "blur", 0);
        reviewCard(userId, algoDeckId, String.valueOf(al3.get("id")), "forget", 0);

        reviewCard(userId, feDeckId, String.valueOf(fe1.get("id")), "master", 0);
        reviewCard(userId, feDeckId, String.valueOf(fe2.get("id")), "blur", 0);
        reviewCard(userId, feDeckId, String.valueOf(fe3.get("id")), "master", 0);

        reviewCard(userId, netDeckId, String.valueOf(n1.get("id")), "blur", 0);
        reviewCard(userId, netDeckId, String.valueOf(n2.get("id")), "forget", 0);
        reviewCard(userId, netDeckId, String.valueOf(n3.get("id")), "master", 0);

        reviewCard(userId, dbDeckId, String.valueOf(db1.get("id")), "master", 0);
        reviewCard(userId, dbDeckId, String.valueOf(db2.get("id")), "blur", 0);
        reviewCard(userId, dbDeckId, String.valueOf(db3.get("id")), "forget", 0);

        // 手动调整部分卡片到期时间，确保首页“待复习”有明显分层
        long now = System.currentTimeMillis();
        redis.opsForZSet().add(reviewZsetKey(userId), String.valueOf(os1.get("id")), now - 4 * HOUR);    // 已到期
        redis.opsForZSet().add(reviewZsetKey(userId), String.valueOf(al3.get("id")), now - 2 * HOUR);   // 已到期
        redis.opsForZSet().add(reviewZsetKey(userId), String.valueOf(n2.get("id")), now - HOUR);        // 已到期
        redis.opsForZSet().add(reviewZsetKey(userId), String.valueOf(db3.get("id")), now + 6 * HOUR);   // 即将到期
        redis.opsForZSet().add(reviewZsetKey(userId), String.valueOf(fe3.get("id")), now + DAY);        // 明天到期
        redis.opsForZSet().add(reviewZsetKey(userId), String.valueOf(db1.get("id")), now + 4 * DAY);    // 中期
        redis.opsForZSet().add(reviewZsetKey(userId), String.valueOf(os3.get("id")), now + 7 * DAY);    // 长期

        // ========== 热力图：最近 120 天生成更有节奏的学习记录 ==========
        for (int i = 1; i <= 120; i++) {
            LocalDate d = LocalDate.now().minusDays(i);
            // 周中高频 + 周末低频 + 偶发冲刺
            if (i % 2 == 0 || i % 6 == 0 || i % 11 == 0 || i % 17 == 0) {
                redis.opsForValue().setBit(heatBitmapKey(userId, d.getYear()), d.getDayOfYear() - 1, true);
            }
        }

        // ========== 集市：额外构造两个公开来源用户 ==========
        String guestId = "guest_user_001";
        Map<String, Object> guest = new HashMap<>();
        guest.put("userId", guestId);
        guest.put("nickname", "算法同学");
        guest.put("goal", "冲刺 408");
        guest.put("avatarText", "算");
        redis.opsForValue().set(userKey(guestId), toJson(guest));

        Map<String, Object> guestDeck = createDeck(guestId, "高频算法题-二分与贪心");
        String guestDeckId = String.valueOf(guestDeck.get("id"));
        toggleDeckPublic(guestId, guestDeckId, true);
        addCard(guestId, guestDeckId, Map.of(
                "front", "二分查找的适用前提是什么？",
                "back", "序列具有单调性或可转化为单调判定函数。"
        ));
        addCard(guestId, guestDeckId, Map.of(
                "front", "贪心算法为什么需要证明？",
                "back", "需要证明局部最优选择可扩展为全局最优，常见方法是交换论证。"
        ));
        addCard(guestId, guestDeckId, Map.of(
                "front", "二分模板的边界该如何处理？",
                "back", "明确闭区间/半开区间不变量，并保持循环收敛条件一致。"
        ));

        String guestId2 = "guest_user_002";
        Map<String, Object> guest2 = new HashMap<>();
        guest2.put("userId", guestId2);
        guest2.put("nickname", "前端同学");
        guest2.put("goal", "准备秋招前端面试");
        guest2.put("avatarText", "前");
        redis.opsForValue().set(userKey(guestId2), toJson(guest2));

        Map<String, Object> guestDeck2 = createDeck(guestId2, "Vue3 与性能优化");
        String guestDeck2Id = String.valueOf(guestDeck2.get("id"));
        toggleDeckPublic(guestId2, guestDeck2Id, true);
        addCard(guestId2, guestDeck2Id, Map.of(
                "front", "为什么要合理拆分响应式状态？",
                "back", "减少无关组件的重渲染，降低依赖追踪开销。"
        ));
        addCard(guestId2, guestDeck2Id, Map.of(
                "front", "虚拟列表解决什么问题？",
                "back", "仅渲染可视区域元素，降低大列表 DOM 数量与内存占用。"
        ));
    }
}
