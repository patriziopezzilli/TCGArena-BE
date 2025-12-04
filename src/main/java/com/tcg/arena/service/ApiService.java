package com.tcg.arena.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ApiService {

    private final WebClient webClient = WebClient.create();

    public Mono<String> fetchPokemonCards() {
        return webClient.get()
                .uri("https://api.pokemontcg.io/v2/cards")
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> fetchMagicCards() {
        return webClient.get()
                .uri("https://api.scryfall.com/cards")
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> fetchOnePieceCards() {
        return webClient.get()
                .uri("https://onepiececardgame.dev/api/v2/cards")
                .retrieve()
                .bodyToMono(String.class);
    }

    // Methods to parse and save cards would go here
}