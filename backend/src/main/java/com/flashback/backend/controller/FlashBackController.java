package com.flashback.backend.controller;

import com.flashback.backend.dto.ApiResponse;
import com.flashback.backend.service.FlashBackService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FlashBackController {
    private final FlashBackService service;

    public FlashBackController(FlashBackService service) {
        this.service = service;
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile(@RequestParam(defaultValue = "local_user_001") String userId) {
        return ApiResponse.ok(service.ensureUser(userId));
    }

    @PutMapping("/profile")
    public ApiResponse<Map<String, Object>> updateProfile(@RequestParam(defaultValue = "local_user_001") String userId,
                                                          @RequestBody Map<String, Object> payload) {
        return ApiResponse.ok(service.updateUser(userId, payload));
    }

    @GetMapping("/decks")
    public ApiResponse<List<Map<String, Object>>> decks(@RequestParam(defaultValue = "local_user_001") String userId) {
        return ApiResponse.ok(service.getDecks(userId));
    }

    @PostMapping("/decks")
    public ApiResponse<Map<String, Object>> createDeck(@RequestParam(defaultValue = "local_user_001") String userId,
                                                        @RequestBody Map<String, Object> payload) {
        String name = String.valueOf(payload.getOrDefault("name", "")).trim();
        if (name.isEmpty()) return ApiResponse.fail("名称不能为空");
        return ApiResponse.ok(service.createDeck(userId, name));
    }

    @GetMapping("/decks/{deckId}")
    public ApiResponse<Map<String, Object>> deck(@RequestParam(defaultValue = "local_user_001") String userId,
                                                  @PathVariable String deckId) {
        Map<String, Object> deck = service.getDeckById(userId, deckId);
        if (deck == null) return ApiResponse.fail("卡片集不存在");
        return ApiResponse.ok(deck);
    }

    @PutMapping("/decks/{deckId}/rename")
    public ApiResponse<Map<String, Object>> rename(@RequestParam(defaultValue = "local_user_001") String userId,
                                                    @PathVariable String deckId,
                                                    @RequestBody Map<String, Object> payload) {
        String name = String.valueOf(payload.getOrDefault("name", "")).trim();
        if (name.isEmpty()) return ApiResponse.fail("名称不能为空");
        boolean ok = service.renameDeck(userId, deckId, name);
        return ok ? ApiResponse.ok(new HashMap<>()) : ApiResponse.fail("卡片集不存在");
    }

    @PutMapping("/decks/{deckId}/public")
    public ApiResponse<Map<String, Object>> togglePublic(@RequestParam(defaultValue = "local_user_001") String userId,
                                                          @PathVariable String deckId,
                                                          @RequestBody Map<String, Object> payload) {
        boolean value = Boolean.TRUE.equals(payload.get("value"));
        boolean ok = service.toggleDeckPublic(userId, deckId, value);
        return ok ? ApiResponse.ok(new HashMap<>()) : ApiResponse.fail("卡片集不存在");
    }

    @PostMapping("/decks/{deckId}/cards")
    public ApiResponse<Map<String, Object>> addCard(@RequestParam(defaultValue = "local_user_001") String userId,
                                                     @PathVariable String deckId,
                                                     @RequestBody Map<String, Object> payload) {
        Map<String, Object> card = service.addCard(userId, deckId, payload);
        return card == null ? ApiResponse.fail("卡片集不存在") : ApiResponse.ok(card);
    }

    @PutMapping("/decks/{deckId}/cards/{cardId}")
    public ApiResponse<Map<String, Object>> updateCard(@RequestParam(defaultValue = "local_user_001") String userId,
                                                        @PathVariable String deckId,
                                                        @PathVariable String cardId,
                                                        @RequestBody Map<String, Object> payload) {
        boolean ok = service.updateCard(userId, deckId, cardId, payload);
        return ok ? ApiResponse.ok(new HashMap<>()) : ApiResponse.fail("知识点不存在");
    }

    @DeleteMapping("/decks/{deckId}/cards/{cardId}")
    public ApiResponse<Map<String, Object>> deleteCard(@RequestParam(defaultValue = "local_user_001") String userId,
                                                        @PathVariable String deckId,
                                                        @PathVariable String cardId) {
        boolean ok = service.deleteCard(userId, deckId, cardId);
        return ok ? ApiResponse.ok(new HashMap<>()) : ApiResponse.fail("知识点不存在");
    }

    @PostMapping("/decks/{deckId}/cards/{cardId}/review")
    public ApiResponse<Map<String, Object>> reviewCard(@RequestParam(defaultValue = "local_user_001") String userId,
                                                        @PathVariable String deckId,
                                                        @PathVariable String cardId,
                                                        @RequestBody Map<String, Object> payload) {
        String feedback = String.valueOf(payload.getOrDefault("feedback", ""));
        Map<String, Object> card = service.reviewCard(userId, deckId, cardId, feedback);
        return card == null ? ApiResponse.fail("知识点不存在") : ApiResponse.ok(card);
    }

    @GetMapping("/review/due")
    public ApiResponse<List<Map<String, Object>>> due(@RequestParam(defaultValue = "local_user_001") String userId) {
        return ApiResponse.ok(service.getDueCards(userId));
    }

    @GetMapping("/market/decks")
    public ApiResponse<List<Map<String, Object>>> market(@RequestParam(defaultValue = "local_user_001") String userId) {
        return ApiResponse.ok(service.getPublicDecks(userId));
    }

    @PostMapping("/market/decks/{deckId}/clone")
    public ApiResponse<Map<String, Object>> cloneDeck(@RequestParam(defaultValue = "local_user_001") String userId,
                                                       @PathVariable String deckId) {
        Map<String, Object> deck = service.clonePublicDeck(userId, deckId);
        return deck == null ? ApiResponse.fail("公开卡片集不存在") : ApiResponse.ok(deck);
    }

    @GetMapping("/study/heatmap")
    public ApiResponse<List<Map<String, Object>>> heatmap(@RequestParam(defaultValue = "local_user_001") String userId,
                                                           @RequestParam(defaultValue = "120") int days) {
        return ApiResponse.ok(service.getHeatmap(userId, days));
    }
}
