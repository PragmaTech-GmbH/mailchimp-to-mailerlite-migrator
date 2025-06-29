package digital.pragmatech.service.migration;

import digital.pragmatech.model.common.MigrationStatus;
import digital.pragmatech.service.mailchimp.MailchimpService;
import digital.pragmatech.service.mailerlite.MailerLiteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationValidator {
    
    private final MailchimpService mailchimpService;
    private final MailerLiteService mailerLiteService;
    
    public CompletableFuture<ValidationResult> validateApiConnections() {
        return CompletableFuture.supplyAsync(() -> {
            List<String> errors = new ArrayList<>();
            
            try {
                if (!mailchimpService.testConnection()) {
                    errors.add("Failed to connect to Mailchimp API. Please check your API key.");
                }
            } catch (Exception e) {
                log.error("Mailchimp connection validation failed", e);
                errors.add("Mailchimp API connection error: " + e.getMessage());
            }
            
            try {
                if (!mailerLiteService.testConnection()) {
                    errors.add("Failed to connect to MailerLite API. Please check your API token.");
                }
            } catch (Exception e) {
                log.error("MailerLite connection validation failed", e);
                errors.add("MailerLite API connection error: " + e.getMessage());
            }
            
            return ValidationResult.builder()
                    .valid(errors.isEmpty())
                    .errors(errors)
                    .validatedAt(LocalDateTime.now())
                    .build();
        });
    }
    
    public CompletableFuture<PreMigrationAnalysis> analyzeForMigration() {
        return CompletableFuture.supplyAsync(() -> {
            PreMigrationAnalysis.PreMigrationAnalysisBuilder analysisBuilder = 
                    PreMigrationAnalysis.builder()
                            .analyzedAt(LocalDateTime.now());
            
            try {
                // Analyze Mailchimp data
                var lists = mailchimpService.getAllLists();
                analysisBuilder.totalLists(lists.size());
                
                int totalSubscribers = 0;
                int totalTags = 0;
                
                for (var list : lists) {
                    var members = mailchimpService.getAllMembers(list.getId());
                    totalSubscribers += members.size();
                    
                    var tags = mailchimpService.getAllTags(list.getId());
                    totalTags += tags.size();
                }
                
                analysisBuilder.totalSubscribers(totalSubscribers);
                analysisBuilder.totalTags(totalTags);
                
                // Analyze e-commerce data
                var shops = mailchimpService.getAllEcommerceShops();
                analysisBuilder.totalEcommerceShops(shops.size());
                
                // Estimate migration time (rough calculation)
                long estimatedMinutes = calculateEstimatedTime(totalSubscribers, totalTags, shops.size());
                analysisBuilder.estimatedMigrationTimeMinutes(estimatedMinutes);
                
                return analysisBuilder.build();
                
            } catch (Exception e) {
                log.error("Pre-migration analysis failed", e);
                return analysisBuilder
                        .error("Analysis failed: " + e.getMessage())
                        .build();
            }
        });
    }
    
    private long calculateEstimatedTime(int subscribers, int tags, int shops) {
        // Rough estimation: 
        // - 1000 subscribers per minute
        // - 100 tags per minute
        // - 50 products per minute
        long subscriberTime = subscribers / 1000;
        long tagTime = tags / 100;
        long shopTime = shops * 2; // 2 minutes per shop setup
        
        return Math.max(subscriberTime + tagTime + shopTime, 1);
    }
    
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final LocalDateTime validatedAt;
        
        private ValidationResult(boolean valid, List<String> errors, LocalDateTime validatedAt) {
            this.valid = valid;
            this.errors = errors;
            this.validatedAt = validatedAt;
        }
        
        public static ValidationResultBuilder builder() {
            return new ValidationResultBuilder();
        }
        
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public LocalDateTime getValidatedAt() { return validatedAt; }
        
        public static class ValidationResultBuilder {
            private boolean valid;
            private List<String> errors;
            private LocalDateTime validatedAt;
            
            public ValidationResultBuilder valid(boolean valid) {
                this.valid = valid;
                return this;
            }
            
            public ValidationResultBuilder errors(List<String> errors) {
                this.errors = errors;
                return this;
            }
            
            public ValidationResultBuilder validatedAt(LocalDateTime validatedAt) {
                this.validatedAt = validatedAt;
                return this;
            }
            
            public ValidationResult build() {
                return new ValidationResult(valid, errors, validatedAt);
            }
        }
    }
    
    public static class PreMigrationAnalysis {
        private final int totalLists;
        private final int totalSubscribers;
        private final int totalTags;
        private final int totalEcommerceShops;
        private final long estimatedMigrationTimeMinutes;
        private final LocalDateTime analyzedAt;
        private final String error;
        
        private PreMigrationAnalysis(int totalLists, int totalSubscribers, int totalTags, 
                                    int totalEcommerceShops, long estimatedMigrationTimeMinutes,
                                    LocalDateTime analyzedAt, String error) {
            this.totalLists = totalLists;
            this.totalSubscribers = totalSubscribers;
            this.totalTags = totalTags;
            this.totalEcommerceShops = totalEcommerceShops;
            this.estimatedMigrationTimeMinutes = estimatedMigrationTimeMinutes;
            this.analyzedAt = analyzedAt;
            this.error = error;
        }
        
        public static PreMigrationAnalysisBuilder builder() {
            return new PreMigrationAnalysisBuilder();
        }
        
        // Getters
        public int getTotalLists() { return totalLists; }
        public int getTotalSubscribers() { return totalSubscribers; }
        public int getTotalTags() { return totalTags; }
        public int getTotalEcommerceShops() { return totalEcommerceShops; }
        public long getEstimatedMigrationTimeMinutes() { return estimatedMigrationTimeMinutes; }
        public LocalDateTime getAnalyzedAt() { return analyzedAt; }
        public String getError() { return error; }
        public boolean hasError() { return error != null; }
        
        public static class PreMigrationAnalysisBuilder {
            private int totalLists;
            private int totalSubscribers;
            private int totalTags;
            private int totalEcommerceShops;
            private long estimatedMigrationTimeMinutes;
            private LocalDateTime analyzedAt;
            private String error;
            
            public PreMigrationAnalysisBuilder totalLists(int totalLists) {
                this.totalLists = totalLists;
                return this;
            }
            
            public PreMigrationAnalysisBuilder totalSubscribers(int totalSubscribers) {
                this.totalSubscribers = totalSubscribers;
                return this;
            }
            
            public PreMigrationAnalysisBuilder totalTags(int totalTags) {
                this.totalTags = totalTags;
                return this;
            }
            
            public PreMigrationAnalysisBuilder totalEcommerceShops(int totalEcommerceShops) {
                this.totalEcommerceShops = totalEcommerceShops;
                return this;
            }
            
            public PreMigrationAnalysisBuilder estimatedMigrationTimeMinutes(long estimatedMigrationTimeMinutes) {
                this.estimatedMigrationTimeMinutes = estimatedMigrationTimeMinutes;
                return this;
            }
            
            public PreMigrationAnalysisBuilder analyzedAt(LocalDateTime analyzedAt) {
                this.analyzedAt = analyzedAt;
                return this;
            }
            
            public PreMigrationAnalysisBuilder error(String error) {
                this.error = error;
                return this;
            }
            
            public PreMigrationAnalysis build() {
                return new PreMigrationAnalysis(totalLists, totalSubscribers, totalTags, 
                        totalEcommerceShops, estimatedMigrationTimeMinutes, analyzedAt, error);
            }
        }
    }
}