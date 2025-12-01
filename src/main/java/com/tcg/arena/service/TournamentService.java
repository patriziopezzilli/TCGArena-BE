package com.tcg.arena.service;

import com.tcg.arena.model.*;
import com.tcg.arena.repository.TournamentParticipantRepository;
import com.tcg.arena.repository.TournamentRepository;
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
        return tournamentRepository.findAllByOrderByStartDateDesc();
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
                // Check if latitude and longitude are not null before using them
                Double lat = tournament.getLocation().getLatitude();
                Double lon = tournament.getLocation().getLongitude();
                if (lat == null || lon == null) return false;
                
                double distance = calculateDistance(
                    latitude, longitude,
                    lat,
                    lon
                );
                return distance <= radiusKm;
            })
            .toList();
    }

    public Tournament saveTournament(Tournament tournament) {
        return tournamentRepository.save(tournament);
    }

    public TournamentParticipant registerForTournament(Long tournamentId, Long userId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new RuntimeException("Tournament not found"));

        // Check if user is already registered
        Optional<TournamentParticipant> existingParticipant = participantRepository
            .findByTournamentIdAndUserId(tournamentId, userId);

        if (existingParticipant.isPresent()) {
            throw new RuntimeException("User is already registered for this tournament");
        }

        // Count current registered participants
        long registeredCount = participantRepository.countByTournamentIdAndStatus(
            tournamentId, ParticipantStatus.REGISTERED);

        TournamentParticipant participant = new TournamentParticipant();
        participant.setTournamentId(tournamentId);
        participant.setUserId(userId);
        participant.setRegistrationDate(LocalDateTime.now());

        // Determine status based on available slots
        if (registeredCount < tournament.getMaxParticipants()) {
            participant.setStatus(ParticipantStatus.REGISTERED);
        } else {
            participant.setStatus(ParticipantStatus.WAITING_LIST);
        }

        return participantRepository.save(participant);
    }

    public boolean unregisterFromTournament(Long tournamentId, Long userId) {
        Optional<TournamentParticipant> participant = participantRepository
            .findByTournamentIdAndUserId(tournamentId, userId);

        if (participant.isPresent()) {
            participantRepository.delete(participant.get());

            // If the participant was registered and there are people on waiting list,
            // promote the first person from waiting list
            if (participant.get().getStatus() == ParticipantStatus.REGISTERED) {
                promoteFromWaitingList(tournamentId);
            }

            return true;
        }
        return false;
    }

    private void promoteFromWaitingList(Long tournamentId) {
        List<TournamentParticipant> waitingList = participantRepository
            .findByTournamentIdAndStatusOrderByRegistrationDateAsc(
                tournamentId, ParticipantStatus.WAITING_LIST);

        if (!waitingList.isEmpty()) {
            TournamentParticipant promoted = waitingList.get(0);
            promoted.setStatus(ParticipantStatus.REGISTERED);
            participantRepository.save(promoted);
        }
    }

    public List<TournamentParticipant> getTournamentParticipants(Long tournamentId) {
        return participantRepository.findByTournamentId(tournamentId);
    }

    public List<TournamentParticipant> getRegisteredParticipants(Long tournamentId) {
        return participantRepository.findByTournamentIdAndStatus(
            tournamentId, ParticipantStatus.REGISTERED);
    }

    public List<TournamentParticipant> getWaitingList(Long tournamentId) {
        return participantRepository.findByTournamentIdAndStatus(
            tournamentId, ParticipantStatus.WAITING_LIST);
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