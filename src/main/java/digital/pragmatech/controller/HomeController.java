package digital.pragmatech.controller;

import digital.pragmatech.config.ApiConfiguration;
import digital.pragmatech.dto.request.ApiKeysRequest;
import digital.pragmatech.dto.response.ApiResponse;
import digital.pragmatech.service.mailchimp.MailchimpService;
import digital.pragmatech.service.migration.MigrationValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final ApiConfiguration apiConfiguration;
    private final MigrationValidator migrationValidator;
    private final MailchimpService mailchimpService;
    
    @GetMapping("/")
    public String index(Model model) {
        // Initialize API keys request with defaults if available
        ApiKeysRequest request = new ApiKeysRequest();
        if (apiConfiguration.getMailchimp().getDefaultApiKey() != null && 
            !apiConfiguration.getMailchimp().getDefaultApiKey().isEmpty()) {
            request.setMailchimpApiKey(apiConfiguration.getMailchimp().getDefaultApiKey());
            // Auto-configure from environment variable
            apiConfiguration.getMailchimp().setApiKey(apiConfiguration.getMailchimp().getDefaultApiKey());
        }
        if (apiConfiguration.getMailerlite().getDefaultApiToken() != null && 
            !apiConfiguration.getMailerlite().getDefaultApiToken().isEmpty()) {
            request.setMailerLiteApiToken(apiConfiguration.getMailerlite().getDefaultApiToken());
            // Auto-configure from environment variable
            apiConfiguration.getMailerlite().setApiToken(apiConfiguration.getMailerlite().getDefaultApiToken());
        }
        
        model.addAttribute("apiKeysRequest", request);
        
        // Check if API keys are already configured
        boolean hasMailchimpKey = apiConfiguration.getMailchimp().getApiKey() != null && 
                                 !apiConfiguration.getMailchimp().getApiKey().isEmpty();
        boolean hasMailerLiteToken = apiConfiguration.getMailerlite().getApiToken() != null && 
                                    !apiConfiguration.getMailerlite().getApiToken().isEmpty();
        
        model.addAttribute("hasMailchimpKey", hasMailchimpKey);
        model.addAttribute("hasMailerLiteToken", hasMailerLiteToken);
        model.addAttribute("canProceed", hasMailchimpKey && hasMailerLiteToken);
        model.addAttribute("preConfigured", hasMailchimpKey && hasMailerLiteToken);
        
        return "index";
    }
    
    @PostMapping("/api/configure")
    @ResponseBody
    public CompletableFuture<ApiResponse<String>> configureApiKeys(@Valid @RequestBody ApiKeysRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Set API keys in configuration
                apiConfiguration.getMailchimp().setApiKey(request.getMailchimpApiKey());
                apiConfiguration.getMailerlite().setApiToken(request.getMailerLiteApiToken());
                
                log.info("API keys configured successfully");
                return ApiResponse.success("API keys configured successfully");
                
            } catch (Exception e) {
                log.error("Failed to configure API keys", e);
                return ApiResponse.error("Failed to configure API keys: " + e.getMessage());
            }
        });
    }
    
    @PostMapping("/api/validate")
    @ResponseBody
    public CompletableFuture<ApiResponse<MigrationValidator.ValidationResult>> validateConnections() {
        return migrationValidator.validateApiConnections()
                .thenApply(result -> {
                    if (result.isValid()) {
                        return ApiResponse.success("API connections validated successfully", result);
                    } else {
                        return ApiResponse.<MigrationValidator.ValidationResult>error("API validation failed");
                    }
                })
                .exceptionally(throwable -> {
                    log.error("API validation failed", throwable);
                    return ApiResponse.error("Validation failed: " + throwable.getMessage());
                });
    }
    
    @PostMapping("/api/analyze")
    @ResponseBody
    public CompletableFuture<ApiResponse<MigrationValidator.PreMigrationAnalysis>> analyzeForMigration() {
        return migrationValidator.analyzeForMigration()
                .thenApply(analysis -> {
                    if (analysis.hasError()) {
                        return ApiResponse.<MigrationValidator.PreMigrationAnalysis>error("Analysis failed: " + analysis.getError());
                    } else {
                        return ApiResponse.success("Pre-migration analysis completed", analysis);
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Pre-migration analysis failed", throwable);
                    return ApiResponse.<MigrationValidator.PreMigrationAnalysis>error("Analysis failed: " + throwable.getMessage());
                });
    }
    
    @GetMapping("/api/keys-configured")
    @ResponseBody
    public ApiResponse<Map<String, Boolean>> checkApiKeysConfigured() {
        Map<String, Boolean> status = new HashMap<>();
        status.put("mailchimp", apiConfiguration.getMailchimp().getDefaultApiKey() != null && 
                                !apiConfiguration.getMailchimp().getDefaultApiKey().isEmpty());
        status.put("mailerlite", apiConfiguration.getMailerlite().getDefaultApiToken() != null && 
                                 !apiConfiguration.getMailerlite().getDefaultApiToken().isEmpty());
        return ApiResponse.success("API keys configuration status", status);
    }
    
    @GetMapping("/api/tags")
    @ResponseBody
    public CompletableFuture<ApiResponse<Map<String, List<String>>>> getMailchimpTags() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get all lists
                var lists = mailchimpService.getAllLists();
                Map<String, List<String>> tagsByList = new HashMap<>();
                
                for (var list : lists) {
                    List<String> tags = mailchimpService.getAllTags(list.getId());
                    tagsByList.put(list.getName(), tags);
                }
                
                return ApiResponse.success("Tags fetched successfully", tagsByList);
                
            } catch (Exception e) {
                log.error("Failed to fetch Mailchimp tags", e);
                return ApiResponse.error("Failed to fetch tags: " + e.getMessage());
            }
        });
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Check if API keys are configured
        boolean hasApiKeys = apiConfiguration.getMailchimp().getApiKey() != null &&
                            apiConfiguration.getMailerlite().getApiToken() != null;
        
        if (!hasApiKeys) {
            return "redirect:/?error=api-keys-required";
        }
        
        return "dashboard";
    }
    
    @GetMapping("/results")
    public String results(Model model, @RequestParam(required = false) String migrationId) {
        model.addAttribute("migrationId", migrationId);
        return "results";
    }
    
    @PostMapping("/configure")
    public String configureApiKeysForm(@Valid @ModelAttribute ApiKeysRequest request, 
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errors", bindingResult.getAllErrors());
            return "redirect:/?error=validation";
        }
        
        try {
            apiConfiguration.getMailchimp().setApiKey(request.getMailchimpApiKey());
            apiConfiguration.getMailerlite().setApiToken(request.getMailerLiteApiToken());
            
            redirectAttributes.addFlashAttribute("success", "API keys configured successfully");
            return "redirect:/dashboard";
            
        } catch (Exception e) {
            log.error("Failed to configure API keys via form", e);
            redirectAttributes.addFlashAttribute("error", "Failed to configure API keys: " + e.getMessage());
            return "redirect:/?error=configuration";
        }
    }
}