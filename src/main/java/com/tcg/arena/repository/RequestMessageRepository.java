package com.tcg.arena.repository;

import com.tcg.arena.model.RequestMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestMessageRepository extends JpaRepository<RequestMessage, String> {
    
    /**
     * Find all messages for a request
     */
    List<RequestMessage> findByRequestIdOrderByCreatedAtAsc(String requestId);
    
    /**
     * Count messages in a request
     */
    long countByRequestId(String requestId);
}
