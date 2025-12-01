package com.tcg.arena.service;

import com.tcg.arena.dto.WaitingListRequestDTO;
import com.tcg.arena.dto.WaitingListResponseDTO;
import com.tcg.arena.model.WaitingListEntry;
import com.tcg.arena.repository.WaitingListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WaitingListService {
    
    private final WaitingListRepository waitingListRepository;
    
    public WaitingListService(WaitingListRepository waitingListRepository) {
        this.waitingListRepository = waitingListRepository;
    }
    
    @Transactional
    public WaitingListResponseDTO addToWaitingList(WaitingListRequestDTO request) {
        // Check if email already exists
        if (waitingListRepository.existsByEmail(request.getEmail())) {
            return new WaitingListResponseDTO(
                "Sei già nella lista d'attesa! Ti contatteremo presto.",
                true
            );
        }
        
        // Create new entry
        WaitingListEntry entry = new WaitingListEntry();
        entry.setEmail(request.getEmail());
        entry.setCity(request.getCity());
        entry.setUserType(request.getUserType());
        entry.setContacted(false);
        
        waitingListRepository.save(entry);
        
        String message = request.getUserType() == WaitingListEntry.UserType.MERCHANT
            ? "Grazie per l'interesse! Ti contatteremo presto per attivare il tuo negozio."
            : "Grazie per esserti registrato! Ti avviseremo quando TCG Arena sarà disponibile.";
        
        return new WaitingListResponseDTO(message, true);
    }
    
    public List<WaitingListEntry> getAllEntries() {
        return waitingListRepository.findAll();
    }
    
    public List<WaitingListEntry> getUncontactedEntries() {
        return waitingListRepository.findAll().stream()
            .filter(entry -> !entry.getContacted())
            .toList();
    }
    
    @Transactional
    public void markAsContacted(Long id) {
        waitingListRepository.findById(id).ifPresent(entry -> {
            entry.setContacted(true);
            waitingListRepository.save(entry);
        });
    }
}
