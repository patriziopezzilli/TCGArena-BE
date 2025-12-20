package com.tcg.arena.controller;

import com.tcg.arena.dto.TradeMatchDTO;
import com.tcg.arena.model.TradeListEntry;
import com.tcg.arena.model.TradeListType;
import com.tcg.arena.model.User;
import com.tcg.arena.service.TradeService;
import com.tcg.arena.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trade")
public class TradeController {

    @Autowired
    private TradeService tradeService;

    @Autowired
    private UserService userService;

    @PostMapping("/list/add")
    public ResponseEntity<?> addCardToList(
            Authentication authentication,
            @RequestBody Map<String, Object> payload) {
        
        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Long cardTemplateId = Long.valueOf(payload.get("cardTemplateId").toString());
        String typeStr = payload.get("type").toString();
        TradeListType type = TradeListType.valueOf(typeStr);

        tradeService.addCardToList(user.getId(), cardTemplateId, type);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/list/remove")
    public ResponseEntity<?> removeCardFromList(
            Authentication authentication,
            @RequestBody Map<String, Object> payload) {
        
        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Long cardTemplateId = Long.valueOf(payload.get("cardTemplateId").toString());
        String typeStr = payload.get("type").toString();
        TradeListType type = TradeListType.valueOf(typeStr);

        tradeService.removeCardFromList(user.getId(), cardTemplateId, type);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<com.tcg.arena.dto.TradeListEntryDTO>> getMyList(
            Authentication authentication,
            @RequestParam TradeListType type) {
        
        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(tradeService.getUserList(user.getId(), type));
    }

    @GetMapping("/matches")
    public ResponseEntity<List<TradeMatchDTO>> findMatches(
            Authentication authentication,
            @RequestParam(defaultValue = "50") double radius) {
        
        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(tradeService.findMatches(user.getId(), radius));
    }

    @GetMapping("/chat/{matchId}")
    public ResponseEntity<com.tcg.arena.dto.TradeChatResponseDTO> getMessages(
            Authentication authentication,
            @PathVariable Long matchId) {
        
        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(tradeService.getMessages(matchId, user.getId()));
    }

    @PostMapping("/chat/{matchId}")
    public ResponseEntity<?> sendMessage(
            Authentication authentication,
            @PathVariable Long matchId,
            @RequestBody Map<String, String> payload) {
        
        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String content = payload.get("content");
        
        tradeService.sendMessage(matchId, user.getId(), content);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/complete/{matchId}")
    public ResponseEntity<?> completeTrade(
            Authentication authentication,
            @PathVariable Long matchId) {
        
        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        tradeService.completeTrade(matchId, user.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel/{matchId}")
    public ResponseEntity<?> cancelTrade(
            Authentication authentication,
            @PathVariable Long matchId) {
        
        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        tradeService.cancelTrade(matchId, user.getId());
        return ResponseEntity.ok().build();
    }
}
