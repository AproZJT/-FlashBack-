package com.flashback.backend.controller;

import com.flashback.backend.auth.CurrentUserContext;
import com.flashback.backend.auth.JwtService;
import com.flashback.backend.dto.ApiResponse;
import com.flashback.backend.dto.request.CreateDeckRequest;
import com.flashback.backend.dto.request.LoginRequest;
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
    private final JwtService jwtService;

    public FlashBackController(FlashBackService service, JwtService jwtService) {
        this.service = service;
        this.jwtService = jwtService;
    }

    @PostMapping("/auth/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> user = service.ensureUser(request.userId());
        String token = jwtService.createToken(request.userId());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return ApiResponse.ok(result);
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile() {
        return ApiResponse.ok(service.ensureUser(currentUserId()));
    }

    @PutMapping("/profile")
    public ApiResponse<Map<String, Object>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("nickname", request.nickname());
        payload.put("goal", request.goal());
        return ApiResponse.ok(service.updateUser(currentUserId(), payload));
    }

    @GetMapping("/decks")
    public ApiResponse<List<Map<String, Object>>> decks() {
        return ApiResponse.ok(service.getDecks(currentUserId()));
    }

    @PostMapping("/decks")
    public ApiResponse<Map<String, Object>> createDeck(@Valid @RequestBody CreateDeckRequest request) {
        return ApiResponse.ok(service.createDeck(currentUserId(), request.name().trim()));
    }

    @GetMapping("/decks/{deckId}")
    public ApiResponse<Map<String, Object>> deck(@PathVariable String deckId) {
        Map<String, Object> deck = service.getDeckById(currentUserId(), deckId);
        if (deck == null) throw new BizException("卡片集不存在");
        return ApiResponse.ok(deck);
    }

    @PutMapping("/decks/{deckId}/rename")
    public ApiResponse<Map<String, Object>> rename(@PathVariable String deckId,
                                                    @Valid @RequestBody RenameDeckRequest request) {
        boolean ok = service.renameDeck(currentUserId(), deckId, request.name().trim());
        if (!ok) throw new BizException("卡片集不存在");
        return ApiResponse.ok(new HashMap<>());
    }

    @PutMapping("/decks/{deckId}/public")
    public ApiResponse<Map<String, Object>> togglePublic(@PathVariable String deckId,
                                                          @Valid @RequestBody ToggleDeckPublicRequest request) {
        boolean ok = service.toggleDeckPublic(currentUserId(), deckId, request.value());
        if (!ok) throw new BizException("卡片集不存在");
        return ApiResponse.ok(new HashMap<>());
    }

    @PostMapping("/decks/{deckId}/cards")
    public ApiResponse<Map<String, Object>> addCard(@PathVariable String deckId,
                                                     @Valid @RequestBody UpsertCardRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("front", request.front());
        payload.put("back", request.back());
        Map<String, Object> card = service.addCard(currentUserId(), deckId, payload);
        if (card == null) throw new BizException("卡片集不存在");
        return ApiResponse.ok(card);
    }

    @PutMapping("/decks/{deckId}/cards/{cardId}")
    public ApiResponse<Map<String, Object>> updateCard(@PathVariable String deckId,
                                                        @PathVariable String cardId,
                                                        @Valid @RequestBody UpsertCardRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("front", request.front());
        payload.put("back", request.back());
        boolean ok = service.updateCard(currentUserId(), deckId, cardId, payload);
        if (!ok) throw new BizException("知识点不存在");
        return ApiResponse.ok(new HashMap<>());
    }

    @DeleteMapping("/decks/{deckId}/cards/{cardId}")
    public ApiResponse<Map<String, Object>> deleteCard(@PathVariable String deckId,
                                                        @PathVariable String cardId) {
        boolean ok = service.deleteCard(currentUserId(), deckId, cardId);
        if (!ok) throw new BizException("知识点不存在");
        return ApiResponse.ok(new HashMap<>());
    }

    @PostMapping("/decks/{deckId}/cards/{cardId}/review")
    public ApiResponse<Map<String, Object>> reviewCard(@PathVariable String deckId,
                                                        @PathVariable String cardId,
                                                        @Valid @RequestBody ReviewCardRequest request) {
        Map<String, Object> card = service.reviewCard(currentUserId(), deckId, cardId, request.feedback());
        if (card == null) throw new BizException("知识点不存在");
        return ApiResponse.ok(card);
    }

    @GetMapping("/review/due")
    public ApiResponse<List<Map<String, Object>>> due() {
        return ApiResponse.ok(service.getDueCards(currentUserId()));
    }

    @GetMapping("/market/decks")
    public ApiResponse<List<Map<String, Object>>> market(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(service.getPublicDecks(currentUserId(), page, pageSize));
    }

    @PostMapping("/market/decks/{deckId}/clone")
    public ApiResponse<Map<String, Object>> cloneDeck(@PathVariable String deckId) {
        Map<String, Object> deck = service.clonePublicDeck(currentUserId(), deckId);
        if (deck == null) throw new BizException("公开卡片集不存在");
        return ApiResponse.ok(deck);
    }

    @GetMapping("/study/heatmap")
    public ApiResponse<List<Map<String, Object>>> heatmap(@RequestParam(defaultValue = "120") int days) {
        return ApiResponse.ok(service.getHeatmap(currentUserId(), days));
    }

    private String currentUserId() {
        String userId = CurrentUserContext.get();
        if (userId == null || userId.isBlank()) {
            throw new BizException("未登录");
        }
        return userId;
    }
}
