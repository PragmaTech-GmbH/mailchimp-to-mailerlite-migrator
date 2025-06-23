package digital.pragmatech.service.migration;

import digital.pragmatech.model.common.MigrationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class MigrationProgressTracker {
    
    private final AtomicReference<MigrationStatus> currentMigrationStatus = new AtomicReference<>();
    private final ConcurrentHashMap<String, Object> metrics = new ConcurrentHashMap<>();
    
    public void initializeMigration(String migrationId) {
        MigrationStatus status = MigrationStatus.builder()
                .id(migrationId)
                .phase(MigrationStatus.MigrationPhase.INITIALIZATION)
                .state(MigrationStatus.MigrationState.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .progress(MigrationStatus.Progress.builder()
                        .totalItems(0)
                        .processedItems(0)
                        .successfulItems(0)
                        .failedItems(0)
                        .percentComplete(0.0)
                        .build())
                .statistics(MigrationStatus.Statistics.builder()
                        .totalSubscribers(0)
                        .migratedSubscribers(0)
                        .totalTags(0)
                        .migratedGroups(0)
                        .totalProducts(0)
                        .migratedProducts(0)
                        .totalCategories(0)
                        .migratedCategories(0)
                        .estimatedTimeRemaining(0)
                        .build())
                .build();
        
        currentMigrationStatus.set(status);
        broadcastUpdate();
        log.info("Migration {} initialized", migrationId);
    }
    
    public void updatePhase(MigrationStatus.MigrationPhase phase) {
        MigrationStatus current = currentMigrationStatus.get();
        if (current != null) {
            MigrationStatus updated = MigrationStatus.builder()
                    .id(current.getId())
                    .phase(phase)
                    .state(current.getState())
                    .startedAt(current.getStartedAt())
                    .progress(current.getProgress())
                    .statistics(current.getStatistics())
                    .errors(current.getErrors())
                    .build();
            
            currentMigrationStatus.set(updated);
            broadcastUpdate();
            log.info("Migration phase updated to: {}", phase);
        }
    }
    
    public void updateProgress(int totalItems, int processedItems, int successfulItems, int failedItems) {
        MigrationStatus current = currentMigrationStatus.get();
        if (current != null) {
            MigrationStatus.Progress updatedProgress = MigrationStatus.Progress.builder()
                    .totalItems(totalItems)
                    .processedItems(processedItems)
                    .successfulItems(successfulItems)
                    .failedItems(failedItems)
                    .build();
            updatedProgress.updatePercentComplete();
            
            MigrationStatus updated = MigrationStatus.builder()
                    .id(current.getId())
                    .phase(current.getPhase())
                    .state(current.getState())
                    .startedAt(current.getStartedAt())
                    .progress(updatedProgress)
                    .statistics(current.getStatistics())
                    .errors(current.getErrors())
                    .build();
            
            currentMigrationStatus.set(updated);
            broadcastUpdate();
        }
    }
    
    public void updateStatistics(MigrationStatus.Statistics statistics) {
        MigrationStatus current = currentMigrationStatus.get();
        if (current != null) {
            MigrationStatus updated = MigrationStatus.builder()
                    .id(current.getId())
                    .phase(current.getPhase())
                    .state(current.getState())
                    .startedAt(current.getStartedAt())
                    .progress(current.getProgress())
                    .statistics(statistics)
                    .errors(current.getErrors())
                    .build();
            
            currentMigrationStatus.set(updated);
            broadcastUpdate();
        }
    }
    
    public void addError(String phase, String entity, String entityId, String errorMessage, String errorCode, boolean retryable) {
        MigrationStatus current = currentMigrationStatus.get();
        if (current != null) {
            MigrationStatus.MigrationError error = MigrationStatus.MigrationError.builder()
                    .timestamp(LocalDateTime.now())
                    .phase(phase)
                    .entity(entity)
                    .entityId(entityId)
                    .errorMessage(errorMessage)
                    .errorCode(errorCode)
                    .retryable(retryable)
                    .build();
            
            current.getErrors().add(error);
            broadcastUpdate();
            log.error("Migration error added: {} - {} ({})", phase, errorMessage, errorCode);
        }
    }
    
    public void completeMigration() {
        MigrationStatus current = currentMigrationStatus.get();
        if (current != null) {
            MigrationStatus updated = MigrationStatus.builder()
                    .id(current.getId())
                    .phase(MigrationStatus.MigrationPhase.COMPLETION)
                    .state(MigrationStatus.MigrationState.COMPLETED)
                    .startedAt(current.getStartedAt())
                    .completedAt(LocalDateTime.now())
                    .progress(current.getProgress())
                    .statistics(current.getStatistics())
                    .errors(current.getErrors())
                    .build();
            
            currentMigrationStatus.set(updated);
            broadcastUpdate();
            log.info("Migration {} completed", current.getId());
        }
    }
    
    public void failMigration(String errorMessage) {
        MigrationStatus current = currentMigrationStatus.get();
        if (current != null) {
            addError(current.getPhase().toString(), "Migration", current.getId(), errorMessage, "MIGRATION_FAILED", false);
            
            MigrationStatus updated = MigrationStatus.builder()
                    .id(current.getId())
                    .phase(current.getPhase())
                    .state(MigrationStatus.MigrationState.FAILED)
                    .startedAt(current.getStartedAt())
                    .completedAt(LocalDateTime.now())
                    .progress(current.getProgress())
                    .statistics(current.getStatistics())
                    .errors(current.getErrors())
                    .build();
            
            currentMigrationStatus.set(updated);
            broadcastUpdate();
            log.error("Migration {} failed: {}", current.getId(), errorMessage);
        }
    }
    
    public void pauseMigration() {
        MigrationStatus current = currentMigrationStatus.get();
        if (current != null) {
            MigrationStatus updated = MigrationStatus.builder()
                    .id(current.getId())
                    .phase(current.getPhase())
                    .state(MigrationStatus.MigrationState.PAUSED)
                    .startedAt(current.getStartedAt())
                    .progress(current.getProgress())
                    .statistics(current.getStatistics())
                    .errors(current.getErrors())
                    .build();
            
            currentMigrationStatus.set(updated);
            broadcastUpdate();
            log.info("Migration {} paused", current.getId());
        }
    }
    
    public void resumeMigration() {
        MigrationStatus current = currentMigrationStatus.get();
        if (current != null && current.getState() == MigrationStatus.MigrationState.PAUSED) {
            MigrationStatus updated = MigrationStatus.builder()
                    .id(current.getId())
                    .phase(current.getPhase())
                    .state(MigrationStatus.MigrationState.IN_PROGRESS)
                    .startedAt(current.getStartedAt())
                    .progress(current.getProgress())
                    .statistics(current.getStatistics())
                    .errors(current.getErrors())
                    .build();
            
            currentMigrationStatus.set(updated);
            broadcastUpdate();
            log.info("Migration {} resumed", current.getId());
        }
    }
    
    public MigrationStatus getCurrentStatus() {
        return currentMigrationStatus.get();
    }
    
    public boolean isMigrationInProgress() {
        MigrationStatus current = currentMigrationStatus.get();
        return current != null && current.getState() == MigrationStatus.MigrationState.IN_PROGRESS;
    }
    
    public void setMetric(String key, Object value) {
        metrics.put(key, value);
    }
    
    public Object getMetric(String key) {
        return metrics.get(key);
    }
    
    private void broadcastUpdate() {
        // No-op: WebSocket broadcasting removed, status is now polled via REST API
    }
}