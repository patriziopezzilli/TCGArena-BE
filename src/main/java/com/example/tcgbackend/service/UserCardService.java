package com.example.tcgbackend.service;

import com.example.tcgbackend.model.CardCondition;
import com.example.tcgbackend.model.CardTemplate;
import com.example.tcgbackend.model.User;
import com.example.tcgbackend.model.UserCard;
import com.example.tcgbackend.repository.CardTemplateRepository;
import com.example.tcgbackend.repository.UserCardRepository;
import com.example.tcgbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserCardService {
    @Autowired
    private UserCardRepository userCardRepository;

    @Autowired
    private CardTemplateRepository cardTemplateRepository;

    @Autowired
    private UserRepository userRepository;

    public List<UserCard> getAllUserCards() {
        return userCardRepository.findAll();
    }

    public Optional<UserCard> getUserCardById(Long id) {
        return userCardRepository.findById(id);
    }

    public List<UserCard> getUserCardsByUserId(Long userId) {
        return userCardRepository.findByOwnerId(userId);
    }

    public List<UserCard> getUserCardsByCardTemplateId(Long cardTemplateId) {
        return userCardRepository.findByCardTemplateId(cardTemplateId);
    }

    public UserCard saveUserCard(UserCard userCard) {
        return userCardRepository.save(userCard);
    }

    public Optional<UserCard> updateUserCard(Long id, UserCard userCardDetails) {
        return userCardRepository.findById(id).map(userCard -> {
            userCard.setCondition(userCardDetails.getCondition());
            userCard.setIsGraded(userCardDetails.getIsGraded());
            userCard.setGradeService(userCardDetails.getGradeService());
            userCard.setGradeScore(userCardDetails.getGradeScore());
            userCard.setPurchasePrice(userCardDetails.getPurchasePrice());
            userCard.setDateAcquired(userCardDetails.getDateAcquired());
            return userCardRepository.save(userCard);
        });
    }

    public boolean deleteUserCard(Long id) {
        if (userCardRepository.existsById(id)) {
            userCardRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public UserCard addCardToUserCollection(CardTemplate cardTemplate, User owner, CardCondition condition) {
        UserCard userCard = new UserCard();
        userCard.setCardTemplate(cardTemplate);
        userCard.setOwner(owner);
        userCard.setCondition(condition);
        userCard.setIsGraded(false);
        userCard.setDateAdded(LocalDateTime.now());
        return userCardRepository.save(userCard);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<CardTemplate> getCardTemplateById(Long id) {
        return cardTemplateRepository.findById(id);
    }
}