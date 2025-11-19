package com.example.tcgbackend.service;

import com.example.tcgbackend.model.*;
import com.example.tcgbackend.repository.TournamentParticipantRepository;
import com.example.tcgbackend.repository.TournamentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TournamentService {
    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TournamentParticipantRepository participantRepository;

    public List<Tournament> getAllTournaments() {
        return tournamentRepository.findAll();
    }

    public Optional<Tournament> getTournamentById(Long id) {
        return tournamentRepository.findById(id);
    }

    public List<Tournament> getUpcomingTournaments() {
        return tournamentRepository.findUpcomingTournaments(LocalDateTime.now());
    }

    public List<Tournament> getNearbyTournaments(double latitude, double longitude, double radiusKm) {
        List<Tournament> allTournaments = tournamentRepository.findUpcomingTournaments(LocalDateTime.now());
        return allTournaments.stream()
            .filter(tournament -> {
                if (tournament.getLocation() == null) return false;
                double distance = calculateDistance(
                    latitude, longitude,
                    tournament.getLocation().getLatitude(),
                    tournament.getLocation().getLongitude()
                );
                return distance <= radiusKm;
            })
            .toList();
    }

    public Tournament saveTournament(Tournament tournament) {
        return tournamentRepository.save(tournament);
    }

    public Optional<Tournament> updateTournament(Long id, Tournament tournamentDetails) {
        return tournamentRepository.findById(id).map(tournament -> {
            tournament.setTitle(tournamentDetails.getTitle());
            tournament.setDescription(tournamentDetails.getDescription());
            tournament.setTcgType(tournamentDetails.getTcgType());
            tournament.setType(tournamentDetails.getType());
            tournament.setStatus(tournamentDetails.getStatus());
            tournament.setStartDate(tournamentDetails.getStartDate());
            tournament.setEndDate(tournamentDetails.getEndDate());
            tournament.setMaxParticipants(tournamentDetails.getMaxParticipants());
            tournament.setEntryFee(tournamentDetails.getEntryFee());
            tournament.setPrizePool(tournamentDetails.getPrizePool());
            tournament.setLocation(tournamentDetails.getLocation());
            return tournamentRepository.save(tournament);
        });
    }

    public boolean deleteTournament(Long id) {
        if (tournamentRepository.existsById(id)) {
            tournamentRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;
        return distance;
    }
}