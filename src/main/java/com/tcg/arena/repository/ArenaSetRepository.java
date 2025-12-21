package com.tcg.arena.repository;

import com.tcg.arena.model.ArenaSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArenaSetRepository extends JpaRepository<ArenaSet, String> {

    List<ArenaSet> findByGameIdOrderByReleaseDateDesc(String gameId);

    Page<ArenaSet> findByGameId(String gameId, Pageable pageable);

    Optional<ArenaSet> findByIdAndGameId(String id, String gameId);

    @Query("SELECT s FROM ArenaSet s WHERE s.gameId = :gameId ORDER BY s.releaseDate DESC")
    List<ArenaSet> findAllByGameOrderedByReleaseDate(@Param("gameId") String gameId);

    long countByGameId(String gameId);
}
