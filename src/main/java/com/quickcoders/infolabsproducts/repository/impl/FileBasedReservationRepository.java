package com.quickcoders.infolabsproducts.repository.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcoders.infolabsproducts.domain.Reservation;
import com.quickcoders.infolabsproducts.repository.ReservationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Repository
@Profile("file")
public class FileBasedReservationRepository implements ReservationRepository {
    
    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final String dataFile;
    
    public FileBasedReservationRepository(ObjectMapper objectMapper, 
                                        @Value("${app.data.file.reservations:data/reservations.json}") String dataFile) {
        this.objectMapper = objectMapper;
        this.dataFile = dataFile;
    }
    
    @PostConstruct
    public void loadData() {
        File file = new File(dataFile);
        if (file.exists()) {
            try {
                Map<String, Reservation> loadedReservations = objectMapper.readValue(file, new TypeReference<Map<String, Reservation>>() {});
                reservations.putAll(loadedReservations);
                log.info("Loaded {} reservations from file: {}", reservations.size(), dataFile);
            } catch (IOException e) {
                log.error("Failed to load reservations from file: {}", dataFile, e);
            }
        } else {
            log.info("Data file does not exist, starting with empty repository: {}", dataFile);
        }
    }
    
    @PreDestroy
    public void saveData() {
        try {
            File file = new File(dataFile);
            file.getParentFile().mkdirs();
            objectMapper.writeValue(file, reservations);
            log.info("Saved {} reservations to file: {}", reservations.size(), dataFile);
        } catch (IOException e) {
            log.error("Failed to save reservations to file: {}", dataFile, e);
        }
    }

    @Override
    public Reservation save(Reservation reservation) {
        reservations.put(reservation.getReservationId(), reservation);
        return reservation;
    }

    @Override
    public Optional<Reservation> findByReservationId(String reservationId) {
        return Optional.ofNullable(reservations.get(reservationId));
    }

    @Override
    public List<Reservation> findBySkuAndStoreId(String sku, String storeId) {
        return reservations.values().stream()
                .filter(reservation -> reservation.getSku().equals(sku) && 
                                     reservation.getStoreId().equals(storeId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findExpiredReservations(Instant before) {
        return reservations.values().stream()
                .filter(reservation -> reservation.getExpiresAt() != null && 
                                     reservation.getExpiresAt().isBefore(before))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByReservationId(String reservationId) {
        return reservations.containsKey(reservationId);
    }

    @Override
    public void deleteByReservationId(String reservationId) {
        reservations.remove(reservationId);
    }

    @Override
    public void deleteExpiredReservations(Instant before) {
        List<String> expiredIds = findExpiredReservations(before).stream()
                .map(Reservation::getReservationId)
                .collect(Collectors.toList());
        
        expiredIds.forEach(reservations::remove);
    }
}
