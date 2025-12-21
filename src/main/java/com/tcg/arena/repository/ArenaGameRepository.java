package com.tcg.arena.repository;

import com.tcg.arena.model.ArenaGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArenaGameRepository extends JpaRepository<ArenaGame, String> {

    List<ArenaGame> findAllByOrderByNameAsc();
}
