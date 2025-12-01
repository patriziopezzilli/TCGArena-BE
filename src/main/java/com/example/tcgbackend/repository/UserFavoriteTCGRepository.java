package com.example.tcgbackend.repository;

import com.example.tcgbackend.model.UserFavoriteTCG;
import com.example.tcgbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFavoriteTCGRepository extends JpaRepository<UserFavoriteTCG, Long> {
    List<UserFavoriteTCG> findByUser(User user);
    void deleteByUser(User user);
}

