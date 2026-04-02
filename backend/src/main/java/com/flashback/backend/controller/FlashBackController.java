package com.flashback.backend.controller;

import com.flashback.backend.dto.ApiResponse;
import com.flashback.backend.dto.request.CreateDeckRequest;
import com.flashback.backend.dto.request.RenameDeckRequest;
import com.flashback.backend.dto.request.ReviewCardRequest;
import com.flashback.backend.dto.request.ToggleDeckPublicRequest;
import com.flashback.backend.dto.request.UpdateProfileRequest;
import com.flashback.backend.dto.request.UpsertCardRequest;
import com.flashback.backend.exception.BizException;
import com.flashback.backend.service.FlashBackService;
import jakarta.validation.Valid;
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
                                                          @Valid @RequestBody UpdateProfileRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("nickname", request.nickname());
        payload.put("goal", request.goal());
        return ApiResponse.ok(service.updateUser(userId, payload));
    }

    @GetMapping("/decks")
    public ApiResponse<List<Map<String, Object>>> decks(@RequestParam(defaultValue = "local_user_001") String userId) {
        return ApiResponse.ok(service.getDecks(userId));
    }

    @PostMapping("/decks")
    public ApiResponse<Map<String, Object>> createDeck(@RequestParam(defaultValue = "local_user_001") String userId,
                                                        @Valid @RequestBody CreateDeckRequest request) {
        return ApiResponse.ok(service.createDeck(userId, request.name().trim()));
    }

    @GetMapping("/decks/{deckId}")
    public ApiResponse<Map<String, Object>> deck(@RequestParam(defaultValue = "local_user_001") String userId,
                                                  @PathVariable String deckId) {
        Map<String, Object> deck = service.getDeckById(userId, deckId);
        if (deck == null) throw new BizException("卡片集不存在");
        return ApiResponse.ok(deck);
    }

    @PutMapping("/decks/{deckId}/rename")
    public ApiResponse<Map<String, Object>> rename(@RequestParam(defaultValue = "local_user_001") String userId,
                                                    @PathVariable String deckId,
                                                    @Valid @RequestBody RenameDeckRequest request) {
        boolean ok = service.renameDeck(userId, deckId, request.name().trim());
        if (!ok) throw new BizException("卡片集不存在");
        return ApiResponse.ok(new HashMap<>());
    }

    @PutMapping("/decks/{deckId}/public")
    public ApiResponse<Map<String, Object>> togglePublic(@RequestParam(defaultValue = "local_user_001") String userId,
                                                          @PathVariable String deckId,
                                                          @Valid @RequestBody ToggleDeckPublicRequest request) {
        boolean ok = service.toggleDeckPublic(userId, deckId, request.value());
        if (!ok) throw new BizException("卡片集不存在");
        return ApiResponse.ok(new HashMap<>());
    }

    @PostMapping("/decks/{deckId}/cards")
    public ApiResponse<Map<String, Object>> addCard(@RequestParam(defaultValue = "local_user_001") String userId,
                                                     @PathVariable String deckId,
                                                     @Valid @RequestBody UpsertCardRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("front", request.front());
        payload.put("back", request.back());
        Map<String, Object> card = service.addCard(userId, deckId, payload);
        if (card == null) throw new BizException("卡片集不存在");
        return ApiResponse.ok(card);
    }

    @PutMapping("/decks/{deckId}/cards/{cardId}")
    public ApiResponse<Map<String, Object>> updateCard(@RequestParam(defaultValue = "local_user_001") String userId,
                                                        @PathVariable String deckId,
                                                        @PathVariable String cardId,
                                                        @Valid @RequestBody UpsertCardRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("front", request.front());
        payload.put("back", request.back());
        boolean ok = service.updateCard(userId, deckId, cardId, payload);
        if (!ok) throw new BizException("知识点不存在");
        return ApiResponse.ok(new HashMap<>());
    }

    @DeleteMapping("/decks/{deckId}/cards/{cardId}")
    public ApiResponse<Map<String, Object>> deleteCard(@RequestParam(defaultValue = "local_user_001") String userId,
                                                        @PathVariable String deckId,
                                                        @PathVariable String cardId) {
        boolean ok = service.deleteCard(userId, deckId, cardId);
        if (!ok) throw new BizException("知识点不存在");
        return ApiResponse.ok(new HashMap<>());
    }

    @PostMapping("/decks/{deckId}/cards/{cardId}/review")
    public ApiResponse<Map<String, Object>> reviewCard(@RequestParam(defaultValue = "local_user_001") String userId,
                                                        @PathVariable String deckId,
                                                        @PathVariable String cardId,
                                                        @Valid @RequestBody ReviewCardRequest request) {
        Map<String, Object> card = service.reviewCard(userId, deckId, cardId, request.feedback());
        if (card == null) throw new BizException("知识点不存在");
        return ApiResponse.ok(card);
    }

    @GetMapping("/review/due")
    public ApiResponse<List<Map<String, Object>>> due(@RequestParam(defaultValue = "local_user_001") String userId) {
        return ApiResponse.ok(service.getDueCards(userId));
    }

    @GetMapping("/market/decks")
    public ApiResponse<List<Map<String, Object>>> market(@RequestParam(defaultValue = "local_user_001") String userId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(service.getPublicDecks(userId, page, pageSize));
    }

    @PostMapping("/market/decks/{deckId}/clone")
    public ApiResponse<Map<String, Object>> cloneDeck(@RequestParam(defaultValue = "local_user_001") String userId,
                                                       @PathVariable String deckId) {
        Map<String, Object> deck = service.clonePublicDeck(userId, deckId);
        if (deck == null) throw new BizException("公开卡片集不存在");
        return ApiResponse.ok(deck);
    }

    @GetMapping("/study/heatmap")
    public ApiResponse<List<Map<String, Object>>> heatmap(@RequestParam(defaultValue = "local_user_001") String userId,
                                                           @RequestParam(defaultValue = "120") int days) {
        return ApiResponse.ok(service.getHeatmap(userId, days));
    }
}
