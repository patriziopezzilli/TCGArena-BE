package com.tcg.arena.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Service for managing scheduler locks to prevent duplicate executions
 */
@Service
public class SchedulerLockService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerLockService.class);

    private final JdbcTemplate jdbcTemplate;

    public SchedulerLockService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        createLockTableIfNotExists();
    }

    /**
     * Create the scheduler_locks table if it doesn't exist
     */
    private void createLockTableIfNotExists() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS scheduler_locks (
                    lock_key VARCHAR(255) PRIMARY KEY,
                    acquired_at TIMESTAMP NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    instance_id VARCHAR(255)
                )
                """);
            logger.info("Scheduler locks table created or already exists");
        } catch (Exception e) {
            logger.error("Failed to create scheduler locks table", e);
        }
    }

    /**
     * Try to acquire a lock for the given key
     * @param lockKey The unique key for the lock
     * @param duration How long the lock should be held
     * @return true if lock was acquired, false if already locked
     */
    @Transactional
    public boolean acquireLock(String lockKey, Duration duration) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plus(duration);

            // Try to insert a new lock
            int rowsAffected = jdbcTemplate.update("""
                INSERT INTO scheduler_locks (lock_key, acquired_at, expires_at, instance_id)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (lock_key) DO NOTHING
                """,
                lockKey,
                now,
                expiresAt,
                getInstanceId()
            );

            if (rowsAffected > 0) {
                logger.info("Successfully acquired lock for key: {}", lockKey);
                return true;
            }

            // Check if existing lock is expired
            boolean lockExpired = jdbcTemplate.queryForObject("""
                SELECT expires_at < ? FROM scheduler_locks WHERE lock_key = ?
                """,
                Boolean.class,
                now,
                lockKey
            );

            if (lockExpired) {
                // Try to update the expired lock
                rowsAffected = jdbcTemplate.update("""
                    UPDATE scheduler_locks
                    SET acquired_at = ?, expires_at = ?, instance_id = ?
                    WHERE lock_key = ? AND expires_at < ?
                    """,
                    now,
                    expiresAt,
                    getInstanceId(),
                    lockKey,
                    now
                );

                if (rowsAffected > 0) {
                    logger.info("Successfully acquired expired lock for key: {}", lockKey);
                    return true;
                }
            }

            logger.warn("Failed to acquire lock for key: {} - already locked", lockKey);
            return false;

        } catch (DataIntegrityViolationException e) {
            logger.warn("Lock already exists for key: {}", lockKey);
            return false;
        } catch (Exception e) {
            logger.error("Error acquiring lock for key: {}", lockKey, e);
            return false;
        }
    }

    /**
     * Release a lock
     * @param lockKey The lock key to release
     */
    @Transactional
    public void releaseLock(String lockKey) {
        try {
            int rowsAffected = jdbcTemplate.update(
                "DELETE FROM scheduler_locks WHERE lock_key = ?",
                lockKey
            );

            if (rowsAffected > 0) {
                logger.info("Successfully released lock for key: {}", lockKey);
            } else {
                logger.warn("No lock found to release for key: {}", lockKey);
            }
        } catch (Exception e) {
            logger.error("Error releasing lock for key: {}", lockKey, e);
        }
    }

    /**
     * Clean up expired locks (can be called periodically)
     */
    @Transactional
    public void cleanupExpiredLocks() {
        try {
            int rowsAffected = jdbcTemplate.update(
                "DELETE FROM scheduler_locks WHERE expires_at < ?",
                LocalDateTime.now()
            );

            if (rowsAffected > 0) {
                logger.info("Cleaned up {} expired locks", rowsAffected);
            }
        } catch (Exception e) {
            logger.error("Error cleaning up expired locks", e);
        }
    }

    /**
     * Get current lock status for debugging
     * @param lockKey The lock key to check
     * @return Lock status information
     */
    public String getLockStatus(String lockKey) {
        try {
            return jdbcTemplate.queryForObject("""
                SELECT CONCAT('Lock held by instance: ', instance_id, ', acquired at: ', acquired_at, ', expires at: ', expires_at)
                FROM scheduler_locks
                WHERE lock_key = ?
                """,
                String.class,
                lockKey
            );
        } catch (Exception e) {
            return "Lock not found or error: " + e.getMessage();
        }
    }

    /**
     * Scheduled cleanup of expired locks (runs every hour)
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void scheduledCleanupExpiredLocks() {
        cleanupExpiredLocks();
    }

    /**
     * Get a unique instance ID for this application instance
     */
    private String getInstanceId() {
        try {
            return System.getProperty("instance.id",
                java.net.InetAddress.getLocalHost().getHostName() + "-" + System.currentTimeMillis());
        } catch (Exception e) {
            // Fallback if hostname cannot be determined
            return "unknown-" + System.currentTimeMillis();
        }
    }
}