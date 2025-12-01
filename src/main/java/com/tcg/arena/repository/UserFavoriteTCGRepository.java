package com.tcg.arena.repository;

import com.tcg.arena.model.UserFavoriteTCG;
import com.tcg.arena.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFavoriteTCGRepository extends JpaRepository<UserFavoriteTCG, Long> {
    List<UserFavoriteTCG> findByUser(User user);
    void deleteByUser(User user);
}

