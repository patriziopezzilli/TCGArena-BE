package com.tcg.arena.service;

import com.tcg.arena.dto.*;
import com.tcg.arena.model.*;
import com.tcg.arena.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommunityThreadService {

    @Autowired
    private CommunityThreadRepository threadRepository;

    @Autowired
    private ThreadResponseRepository responseRepository;

    @Autowired
    private PollOptionRepository pollOptionRepository;

    @Autowired
    private PollVoteRepository pollVoteRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get paginated list of threads, optionally filtered by TCG type
     */
    public Page<CommunityThreadDTO> getThreads(String tcgType, Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CommunityThread> threads;

        if (tcgType != null && !tcgType.isEmpty()) {
            threads = threadRepository.findByTcgTypeOrderByCreatedAtDesc(tcgType, pageable);
        } else {
            threads = threadRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return threads.map(thread -> toDTO(thread, currentUserId, false));
    }

    /**
     * Get single thread with all responses
     */
    public CommunityThreadDTO getThreadById(Long threadId, Long currentUserId) {
        CommunityThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread not found"));

        return toDTO(thread, currentUserId, true);
    }

    /**
     * Create a new thread
     */
    @Transactional
    public CommunityThreadDTO createThread(CreateThreadRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CommunityThread thread = new CommunityThread(
                creator,
                request.getTcgType(),
                request.getThreadType(),
                request.getTitle(),
                request.getContent());

        thread = threadRepository.save(thread);

        // Create poll options if it's a poll
        if (request.getThreadType() == ThreadType.POLL && request.getPollOptions() != null) {
            for (String optionText : request.getPollOptions()) {
                if (optionText != null && !optionText.trim().isEmpty()) {
                    PollOption pollOption = new PollOption(thread, optionText.trim());
                    pollOptionRepository.save(pollOption);
                }
            }
        }

        return toDTO(thread, creatorId, false);
    }

    /**
     * Add a response to a thread (one per user)
     */
    @Transactional
    public ThreadResponseDTO addResponse(Long threadId, CreateThreadResponseRequest request, Long responderId) {
        CommunityThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread not found"));

        User responder = userRepository.findById(responderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user is the thread creator (creators cannot respond to their own
        // thread)
        if (thread.getCreator().getId().equals(responderId)) {
            throw new RuntimeException("Il creatore del thread non può rispondere al proprio thread");
        }

        // Check if user has already responded
        if (responseRepository.existsByThreadAndResponder(thread, responder)) {
            throw new RuntimeException("Hai già risposto a questo thread. È consentita una sola risposta per utente.");
        }

        ThreadResponse response = new ThreadResponse(thread, responder, request.getContent());
        response = responseRepository.save(response);

        return toResponseDTO(response);
    }

    /**
     * Check if user can respond to a thread
     */
    public boolean canUserRespond(Long threadId, Long userId) {
        CommunityThread thread = threadRepository.findById(threadId).orElse(null);
        if (thread == null)
            return false;

        // Creator cannot respond
        if (thread.getCreator().getId().equals(userId))
            return false;

        User user = userRepository.findById(userId).orElse(null);
        if (user == null)
            return false;

        // Check if already responded
        return !responseRepository.existsByThreadAndResponder(thread, user);
    }

    /**
     * Convert entity to DTO
     */
    private CommunityThreadDTO toDTO(CommunityThread thread, Long currentUserId, boolean includeResponses) {
        CommunityThreadDTO dto = new CommunityThreadDTO();
        dto.setId(thread.getId());
        dto.setCreatorId(thread.getCreator().getId());
        dto.setCreatorUsername(thread.getCreator().getUsername());
        dto.setCreatorDisplayName(thread.getCreator().getDisplayName() != null
                ? thread.getCreator().getDisplayName()
                : thread.getCreator().getUsername());
        dto.setCreatorAvatarUrl(thread.getCreator().getProfileImageUrl());
        dto.setTcgType(thread.getTcgType());
        dto.setThreadType(thread.getThreadType());
        dto.setTitle(thread.getTitle());
        dto.setContent(thread.getContent());
        dto.setCreatedAt(thread.getCreatedAt());
        dto.setResponseCount(responseRepository.countByThread(thread));
        dto.setCreatedByCurrentUser(currentUserId != null && thread.getCreator().getId().equals(currentUserId));

        if (currentUserId != null) {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            if (currentUser != null) {
                dto.setHasCurrentUserResponded(responseRepository.existsByThreadAndResponder(thread, currentUser));
            }
        }

        if (includeResponses) {
            List<ThreadResponse> responses = responseRepository.findByThreadOrderByCreatedAtAsc(thread);
            dto.setResponses(responses.stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList()));
        }

        // Include poll options if it's a poll
        if (thread.getThreadType() == ThreadType.POLL) {
            List<PollOption> pollOptions = pollOptionRepository.findByThreadOrderByCreatedAtAsc(thread);
            dto.setPollOptions(pollOptions.stream()
                    .map(pollOption -> toPollOptionDTO(pollOption, currentUserId))
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    /**
     * Convert response entity to DTO
     */
    private ThreadResponseDTO toResponseDTO(ThreadResponse response) {
        ThreadResponseDTO dto = new ThreadResponseDTO();
        dto.setId(response.getId());
        dto.setResponderId(response.getResponder().getId());
        dto.setResponderUsername(response.getResponder().getUsername());
        dto.setResponderDisplayName(response.getResponder().getDisplayName() != null
                ? response.getResponder().getDisplayName()
                : response.getResponder().getUsername());
        dto.setResponderAvatarUrl(response.getResponder().getProfileImageUrl());
        dto.setContent(response.getContent());
        dto.setCreatedAt(response.getCreatedAt());
        return dto;
    }

    /**
     * Vote on a poll option
     */
    @Transactional
    public PollOptionDTO voteOnPoll(Long pollOptionId, Long voterId) {
        User voter = userRepository.findById(voterId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        PollOption pollOption = pollOptionRepository.findById(pollOptionId)
                .orElseThrow(() -> new RuntimeException("Poll option not found"));

        // Check if user already voted on this poll
        boolean hasVoted = pollVoteRepository.existsByUserAndPollOptionThread(voter, pollOption.getThread());
        if (hasVoted) {
            throw new RuntimeException("User has already voted on this poll");
        }

        // Create the vote
        PollVote vote = new PollVote(voter, pollOption);
        pollVoteRepository.save(vote);

        return toPollOptionDTO(pollOption, voterId);
    }

    /**
     * Check if user has voted on a poll
     */
    public boolean hasUserVotedOnPoll(Long threadId, Long userId) {
        CommunityThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return pollVoteRepository.existsByUserAndPollOptionThread(user, thread);
    }

    /**
     * Convert poll option to DTO
     */
    private PollOptionDTO toPollOptionDTO(PollOption pollOption, Long currentUserId) {
        int voteCount = pollVoteRepository.countByPollOption(pollOption);
        boolean hasCurrentUserVoted = false;

        if (currentUserId != null) {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            if (currentUser != null) {
                hasCurrentUserVoted = pollVoteRepository.existsByUserAndPollOption(currentUser, pollOption);
            }
        }

        return new PollOptionDTO(pollOption.getId(), pollOption.getOptionText(), voteCount, hasCurrentUserVoted);
    }

    /**
     * Delete a thread (only creator)
     */
    @Transactional
    public void deleteThread(Long threadId, Long requesterId) {
        CommunityThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread not found"));

        if (!thread.getCreator().getId().equals(requesterId)) {
            throw new RuntimeException("Only the creator can delete this thread");
        }

        // Delete all responses
        List<ThreadResponse> responses = responseRepository.findByThreadOrderByCreatedAtAsc(thread);
        responseRepository.deleteAll(responses);

        // Delete poll votes and options if exists
        if (thread.getThreadType() == ThreadType.POLL) {
            List<PollOption> options = pollOptionRepository.findByThreadOrderByCreatedAtAsc(thread);
            for (PollOption option : options) {
                // Delete votes for this option
                List<PollVote> votes = pollVoteRepository.findAll().stream()
                        .filter(v -> v.getPollOption().getId().equals(option.getId()))
                        .collect(Collectors.toList());
                pollVoteRepository.deleteAll(votes);
            }
            pollOptionRepository.deleteAll(options);
        }

        threadRepository.delete(thread);
    }
}
