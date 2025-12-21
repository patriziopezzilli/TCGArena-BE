package com.tcg.arena.service;

import com.tcg.arena.model.Suggestion;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.SuggestionRepository;
import com.tcg.arena.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SuggestionService {

    @Autowired
    private SuggestionRepository suggestionRepository;

    @Autowired
    private UserRepository userRepository;

    public Suggestion createSuggestion(Long userId, String text) {
        User user = userRepository.findById(userId).orElse(null);
        String email = user != null ? user.getEmail() : "Unknown";

        Suggestion suggestion = new Suggestion(userId, text, email);
        return suggestionRepository.save(suggestion);
    }

    public List<Suggestion> getAllSuggestions() {
        return suggestionRepository.findAllByOrderByCreatedAtDesc();
    }

    public void markAsRead(Long id) {
        suggestionRepository.findById(id).ifPresent(s -> {
            s.setRead(true);
            suggestionRepository.save(s);
        });
    }
}
