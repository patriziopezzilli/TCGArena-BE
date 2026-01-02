package com.tcg.arena.service;

import com.tcg.arena.dto.CommunityPullDTO;
import com.tcg.arena.dto.CreatePullRequest;
import com.tcg.arena.model.CommunityPull;
import com.tcg.arena.model.PullLike;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.CommunityPullRepository;
import com.tcg.arena.repository.PullLikeRepository;
import com.tcg.arena.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CommunityPullService {

    @Autowired
    private CommunityPullRepository pullRepository;

    @Autowired
    private PullLikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    public Page<CommunityPullDTO> getPulls(TCGType tcgType, Long currentUserId, Pageable pageable) {
        Page<CommunityPull> page;
        if (tcgType != null) {
            page = pullRepository.findByTcgTypeOrderByCreatedAtDesc(tcgType, pageable);
        } else {
            page = pullRepository.findByOrderByCreatedAtDesc(pageable);
        }

        // Fetch current user reference if logged in
        Optional<User> currentUserOpt = currentUserId != null ? userRepository.findById(currentUserId)
                : Optional.empty();

        return page.map(pull -> convertToDTO(pull, currentUserOpt.orElse(null)));
    }

    @Transactional
    public CommunityPullDTO createPull(CreatePullRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CommunityPull pull = new CommunityPull(user, request.getTcgType(), request.getImageBase64());
        pull = pullRepository.save(pull);

        return convertToDTO(pull, user);
    }

    @Transactional
    public CommunityPullDTO toggleLike(Long pullId, Long userId) {
        CommunityPull pull = pullRepository.findById(pullId)
                .orElseThrow(() -> new RuntimeException("Pull not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<PullLike> existingLike = likeRepository.findByPullAndUser(pull, user);
        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
        } else {
            PullLike like = new PullLike(pull, user);
            likeRepository.save(like);

            // Send notification if not liking own post
            if (!pull.getUser().getId().equals(user.getId())) {
                notificationService.sendPullLikeNotification(
                        pull.getUser().getId(),
                        user.getDisplayName(), // Using display name of the liker
                        pull.getTcgType().getDisplayName());
            }
        }

        // Refresh DTO
        return convertToDTO(pull, user);
    }

    private CommunityPullDTO convertToDTO(CommunityPull pull, User currentUser) {
        CommunityPullDTO dto = new CommunityPullDTO();
        dto.setId(pull.getId());
        dto.setUserId(pull.getUser().getId());
        dto.setUserDisplayName(pull.getUser().getDisplayName());
        dto.setUserAvatarUrl(pull.getUser().getProfileImageUrl());
        dto.setTcgType(pull.getTcgType());
        dto.setImageBase64(pull.getImageBase64());
        dto.setCreatedAt(pull.getCreatedAt());

        // Count likes directly from repo for accuracy or size if eagerly loaded
        // For performance in list view, better to rely on size() if managed correctly
        // or use a count query.
        // Assuming not massive number of likes per post for MVP, likes.size() is okay
        // if lazy loaded in transaction,
        // but here we might be outside transaction scope for lazy loading if not
        // careful.
        // Let's use the repository count for safety.
        dto.setLikesCount((int) likeRepository.countByPull(pull));

        if (currentUser != null) {
            dto.setLikedByCurrentUser(likeRepository.existsByPullAndUser(pull, currentUser));
        } else {
            dto.setLikedByCurrentUser(false);
        }

        return dto;
    }
}
