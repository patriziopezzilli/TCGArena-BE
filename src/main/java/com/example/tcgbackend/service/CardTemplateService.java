package com.example.tcgbackend.service;

import com.example.tcgbackend.model.CardTemplate;
import com.example.tcgbackend.model.Expansion;
import com.example.tcgbackend.model.TCGType;
import com.example.tcgbackend.repository.CardTemplateRepository;
import com.example.tcgbackend.repository.ExpansionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CardTemplateService {
    @Autowired
    private CardTemplateRepository cardTemplateRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    public Page<CardTemplate> getAllCardTemplates(Pageable pageable) {
        return cardTemplateRepository.findAll(pageable);
    }

    @Deprecated
    public List<CardTemplate> getAllCardTemplates() {
        return cardTemplateRepository.findAll();
    }

    public Optional<CardTemplate> getCardTemplateById(Long id) {
        return cardTemplateRepository.findById(id);
    }

    public List<CardTemplate> getCardTemplatesByTcgType(String tcgType) {
        return cardTemplateRepository.findByTcgType(tcgType);
    }

    public List<CardTemplate> getCardTemplatesByExpansion(Long expansionId) {
        return cardTemplateRepository.findByExpansionId(expansionId);
    }

    public Page<CardTemplate> getCardTemplatesBySetCode(String setCode, Pageable pageable) {
        return cardTemplateRepository.findBySetCode(setCode, pageable);
    }

    public List<CardTemplate> getCardTemplatesByRarity(String rarity) {
        return cardTemplateRepository.findByRarity(rarity);
    }

    public List<CardTemplate> searchCardTemplates(String query) {
        return cardTemplateRepository.searchByNameOrSetCode(query);
    }

    public CardTemplate saveCardTemplate(CardTemplate cardTemplate) {
        return cardTemplateRepository.save(cardTemplate);
    }

    public List<CardTemplate> saveAllCardTemplates(List<? extends CardTemplate> cardTemplates) {
        return cardTemplateRepository.saveAll(cardTemplates.stream().map(card -> (CardTemplate) card).toList());
    }

    public Optional<CardTemplate> updateCardTemplate(Long id, CardTemplate cardDetails) {
        return cardTemplateRepository.findById(id).map(card -> {
            card.setName(cardDetails.getName());
            card.setTcgType(cardDetails.getTcgType());
            card.setSetCode(cardDetails.getSetCode());
            card.setExpansion(cardDetails.getExpansion());
            card.setRarity(cardDetails.getRarity());
            card.setCardNumber(cardDetails.getCardNumber());
            card.setDescription(cardDetails.getDescription());
            card.setImageUrl(cardDetails.getImageUrl());
            card.setMarketPrice(cardDetails.getMarketPrice());
            card.setManaCost(cardDetails.getManaCost());
            return cardTemplateRepository.save(card);
        });
    }

    public boolean deleteCardTemplate(Long id) {
        if (cardTemplateRepository.existsById(id)) {
            cardTemplateRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public void deleteByTcgType(TCGType tcgType) {
        cardTemplateRepository.deleteByTcgType(tcgType);
    }

    public Expansion getExpansionByName(String name) {
        return expansionRepository.findByTitle(name);
    }

    public Expansion saveExpansion(Expansion expansion) {
        return expansionRepository.save(expansion);
    }
}