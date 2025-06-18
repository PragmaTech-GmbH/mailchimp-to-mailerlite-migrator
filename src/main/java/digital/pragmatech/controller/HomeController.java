package digital.pragmatech.controller;

import digital.pragmatech.config.ApiConfiguration;
import digital.pragmatech.dto.request.ApiKeysRequest;
import digital.pragmatech.dto.response.ApiResponse;
import digital.pragmatech.service.migration.MigrationValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final ApiConfiguration apiConfiguration;
    private final MigrationValidator migrationValidator;
    
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("apiKeysRequest", new ApiKeysRequest());
        
        // Check if API keys are already configured
        boolean hasMailchimpKey = apiConfiguration.getMailchimp().getApiKey() != null;
        boolean hasMailerLiteToken = apiConfiguration.getMailerlite().getApiToken() != null;
        
        model.addAttribute("hasMailchimpKey", hasMailchimpKey);
        model.addAttribute("hasMailerLiteToken", hasMailerLiteToken);
        model.addAttribute("canProceed", hasMailchimpKey && hasMailerLiteToken);
        
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