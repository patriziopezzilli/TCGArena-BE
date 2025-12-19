package com.tcg.arena.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeDashboardDTO {
    private long nearbyShopsCount;
    private long upcomingTournamentsCount;
    private long collectionCount;
    private BigDecimal totalCollectionValue;
    private long unreadNewsCount;
    private long pendingReservationsCount;
    private long activeRequestsCount;
}
