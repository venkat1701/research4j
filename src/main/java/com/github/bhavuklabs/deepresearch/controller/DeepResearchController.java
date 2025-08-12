package com.github.bhavuklabs.deepresearch.controller;

import com.github.bhavuklabs.deepresearch.models.DeepResearchConfig;
import com.github.bhavuklabs.deepresearch.models.DeepResearchProgress;
import com.github.bhavuklabs.deepresearch.models.DeepResearchResult;
import com.github.bhavuklabs.pipeline.profile.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * REST API controller for tree-based deep research with WebSocket progress updates.
 * Provides endpoints for starting, monitoring, and managing deep research sessions.
 */
@RestController
@RequestMapping("/api/v1/research/deep")
@CrossOrigin(origins = "*") // Configure appropriately for production
public class DeepResearchController {

    private static final Logger logger = Logger.getLogger(DeepResearchController.class.getName());

    @Autowired
    private EnhancedDeepResearchEngine deepResearchEngine;

    /**
     * Request payload for starting deep research
     */
    public static class StartResearchRequest {
        private String query;
        private int maxDepth = 3;
        private int breadthPerLevel = 3;
        private boolean enableWebSocket = true;
        private String userProfileId;
        private Map<String, Object> config;

        // Constructors
        public StartResearchRequest() {}

        public StartResearchRequest(String query) {
            this.query = query;
        }

        // Getters and setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public int getMaxDepth() { return maxDepth; }
        public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }

        public int getBreadthPerLevel() { return breadthPerLevel; }
        public void setBreadthPerLevel(int breadthPerLevel) { this.breadthPerLevel = breadthPerLevel; }

        public boolean isEnableWebSocket() { return enableWebSocket; }
        public void setEnableWebSocket(boolean enableWebSocket) { this.enableWebSocket = enableWebSocket; }

        public String getUserProfileId() { return userProfileId; }
        public void setUserProfileId(String userProfileId) { this.userProfileId = userProfileId; }

        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }
    }

    /**
     * Response payload for research session start
     */
    public static class StartResearchResponse {
        private String sessionId;
        private String message;
        private boolean webSocketEnabled;
        private String webSocketUrl;

        public StartResearchResponse(String sessionId, boolean webSocketEnabled) {
            this.sessionId = sessionId;
            this.webSocketEnabled = webSocketEnabled;
            this.message = "Deep research started successfully";
            if (webSocketEnabled) {
                this.webSocketUrl = "/ws/research/" + sessionId;
            }
        }

        // Getters and setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public boolean isWebSocketEnabled() { return webSocketEnabled; }
        public void setWebSocketEnabled(boolean webSocketEnabled) { this.webSocketEnabled = webSocketEnabled; }

        public String getWebSocketUrl() { return webSocketUrl; }
        public void setWebSocketUrl(String webSocketUrl) { this.webSocketUrl = webSocketUrl; }
    }

    /**
     * Start a new tree-based deep research session
     */
    @PostMapping("/start")
    public CompletableFuture<ResponseEntity<StartResearchResponse>> startResearch(
        @RequestBody @Valid StartResearchRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting deep research for query: " + request.getQuery());

                // Create user profile (simplified - you might want to fetch from user service)
                UserProfile userProfile = createUserProfile(request.getUserProfileId());

                // Create research configuration
                DeepResearchConfig config = createResearchConfig(request);

                // Start the research
                CompletableFuture<DeepResearchResult> researchFuture =
                    deepResearchEngine.startTreeBasedResearch(request.getQuery(), userProfile, config);

                // For async operation, we don't wait for completion here
                // The client will monitor progress via WebSocket or polling

                // Extract session ID from the future (this is simplified)
                String sessionId = generateSessionId();

                StartResearchResponse response = new StartResearchResponse(sessionId, request.isEnableWebSocket());
                return ResponseEntity.ok(response);

            } catch (Exception e) {
                logger.severe("Failed to start deep research: " + e.getMessage());
                return ResponseEntity.internalServerError()
                    .body(new StartResearchResponse(null, false));
            }
        });
    }

    /**
     * Get the current status and progress of a research session
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getResearchStatus(@PathVariable String sessionId) {
        try {
            DeepResearchProgress progress = deepResearchEngine.getResearchProgress(sessionId);

            if (progress == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = Map.of(
                "sessionId", progress.getSessionId(),
                "currentPhase", progress.getCurrentPhase(),
                "progressPercentage", progress.getProgressPercentage(),
                "currentActivity", progress.getCurrentActivity(),
                "isCompleted", progress.isCompleted(),
                "isCancelled", progress.isCancelled(),
                "totalDuration", progress.getTotalDuration().toString(),
                "estimatedTimeRemaining", progress.getEstimatedTimeRemaining().toString(),
                "errors", progress.getErrors()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.warning("Failed to get research status for session " + sessionId + ": " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get the complete research tree structure
     */
    @GetMapping("/{sessionId}/tree")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getResearchTree(@PathVariable String sessionId) {
        return deepResearchEngine.getResearchTree(sessionId)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                logger.warning("Failed to get research tree for session " + sessionId + ": " + throwable.getMessage());
                return ResponseEntity.notFound().build();
            });
    }

    /**
     * Retry a failed node in the research tree
     */
    @PostMapping("/{sessionId}/retry/{nodeId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> retryNode(
        @PathVariable String sessionId,
        @PathVariable String nodeId) {

        return deepResearchEngine.retryNode(sessionId, nodeId)
            .thenApply(success -> {
                Map<String, Object> response = Map.of(
                    "success", success,
                    "message", success ? "Node retry initiated" : "Failed to retry node",
                    "sessionId", sessionId,
                    "nodeId", nodeId
                );
                return ResponseEntity.ok(response);
            })
            .exceptionally(throwable -> {
                logger.warning("Failed to retry node " + nodeId + " in session " + sessionId + ": " + throwable.getMessage());
                Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", throwable.getMessage(),
                    "sessionId", sessionId,
                    "nodeId", nodeId
                );
                return ResponseEntity.internalServerError().body(errorResponse);
            });
    }

    /**
     * Cancel an active research session
     */
    @DeleteMapping("/{sessionId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> cancelResearch(@PathVariable String sessionId) {
        return deepResearchEngine.cancelResearch(sessionId)
            .thenApply(success -> {
                Map<String, Object> response = Map.of(
                    "success", success,
                    "message", success ? "Research session cancelled" : "Failed to cancel research session",
                    "sessionId", sessionId
                );
                return ResponseEntity.ok(response);
            })
            .exceptionally(throwable -> {
                logger.warning("Failed to cancel research session " + sessionId + ": " + throwable.getMessage());
                Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", throwable.getMessage(),
                    "sessionId", sessionId
                );
                return ResponseEntity.internalServerError().body(errorResponse);
            });
    }

    /**
     * Export research results in various formats
     */
    @GetMapping("/{sessionId}/export/{format}")
    public CompletableFuture<ResponseEntity<Object>> exportResults(
        @PathVariable String sessionId,
        @PathVariable String format) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get the research tree
                Map<String, Object> tree = deepResearchEngine.getResearchTree(sessionId).join();

                return switch (format.toLowerCase()) {
                    case "json" -> ResponseEntity.ok()
                        .header("Content-Type", "application/json")
                        .header("Content-Disposition", "attachment; filename=research-" + sessionId + ".json")
                        .body(tree);

                    case "markdown" -> {
                        String markdown = convertTreeToMarkdown(tree);
                        yield ResponseEntity.ok()
                            .header("Content-Type", "text/markdown")
                            .header("Content-Disposition", "attachment; filename=research-" + sessionId + ".md")
                            .body(markdown);
                    }

                    default -> ResponseEntity.badRequest()
                        .body(Map.of("error", "Unsupported export format: " + format,
                            "supportedFormats", new String[]{"json", "markdown"}));
                };

            } catch (Exception e) {
                logger.warning("Failed to export results for session " + sessionId + ": " + e.getMessage());
                return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
            }
        });
    }

    /**
     * Get all active research sessions
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveResearch() {
        try {
            Map<String, DeepResearchProgress> activeSessions = deepResearchEngine.getAllActiveResearch();

            Map<String, Object> response = Map.of(
                "activeSessionCount", activeSessions.size(),
                "sessions", activeSessions.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Map.of(
                            "sessionId", entry.getValue().getSessionId(),
                            "currentPhase", entry.getValue().getCurrentPhase(),
                            "progressPercentage", entry.getValue().getProgressPercentage(),
                            "isCompleted", entry.getValue().isCompleted(),
                            "startTime", entry.getValue().getStartTime()
                        )
                    ))
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.warning("Failed to get active research sessions: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get engine statistics and health information
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getEngineStatistics() {
        try {
            Map<String, Object> stats = deepResearchEngine.getEngineStatistics();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.warning("Failed to get engine statistics: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            boolean webSocketEnabled = deepResearchEngine.isWebSocketEnabled();
            int activeConnections = deepResearchEngine.getProgressBroadcaster().getActiveConnectionCount();
            int activeSessions = deepResearchEngine.getAllActiveResearch().size();

            Map<String, Object> health = Map.of(
                "status", "healthy",
                "webSocketEnabled", webSocketEnabled,
                "activeWebSocketConnections", activeConnections,
                "activeResearchSessions", activeSessions,
                "timestamp", java.time.Instant.now().toString()
            );

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            logger.severe("Health check failed: " + e.getMessage());
            return ResponseEntity.status(503)
                .body(Map.of(
                    "status", "unhealthy",
                    "error", e.getMessage(),
                    "timestamp", java.time.Instant.now().toString()
                ));
        }
    }

    // Helper methods

    private UserProfile createUserProfile(String userProfileId) {
        // Simplified user profile creation
        // In a real implementation, you'd fetch from a user service
        if (userProfileId != null) {
            return UserProfile.builder()
                .userId(userProfileId)
                .build();
        } else {
            return UserProfile.builder()
                .userId("anonymous")
                .build();
        }
    }

    private DeepResearchConfig createResearchConfig(StartResearchRequest request) {
        DeepResearchConfig.Builder configBuilder = DeepResearchConfig.builder();

        // Map request parameters to config
        DeepResearchConfig.ResearchDepth depth = mapToResearchDepth(request.getMaxDepth());
        configBuilder.researchDepth(depth);

        configBuilder.maxQuestions(request.getBreadthPerLevel() * request.getMaxDepth());
        configBuilder.enableWebSocket(request.isEnableWebSocket());

        // Set reasonable defaults
        configBuilder.maxProcessingTime(java.time.Duration.ofMinutes(15));
        configBuilder.maxSources(50);
        configBuilder.enableCrossValidation(true);

        // Apply custom config if provided
        if (request.getConfig() != null) {
            applyCustomConfig(configBuilder, request.getConfig());
        }

        return configBuilder.build();
    }

    private DeepResearchConfig.ResearchDepth mapToResearchDepth(int maxDepth) {
        return switch (maxDepth) {
            case 1, 2 -> DeepResearchConfig.ResearchDepth.BASIC;
            case 3 -> DeepResearchConfig.ResearchDepth.STANDARD;
            case 4 -> DeepResearchConfig.ResearchDepth.COMPREHENSIVE;
            default -> DeepResearchConfig.ResearchDepth.EXTENSIVE;
        };
    }

    private void applyCustomConfig(DeepResearchConfig.Builder configBuilder, Map<String, Object> customConfig) {
        // Apply custom configuration parameters
        customConfig.forEach((key, value) -> {
            switch (key) {
                case "maxSources" -> {
                    if (value instanceof Integer) {
                        configBuilder.maxSources((Integer) value);
                    }
                }
                case "maxProcessingTimeMinutes" -> {
                    if (value instanceof Integer) {
                        configBuilder.maxProcessingTime(java.time.Duration.ofMinutes((Integer) value));
                    }
                }
                case "enableCrossValidation" -> {
                    if (value instanceof Boolean) {
                        configBuilder.enableCrossValidation((Boolean) value);
                    }
                }
                case "preferredDomains" -> {
                    if (value instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<String> domains = (java.util.List<String>) value;
                        configBuilder.preferredDomains(domains);
                    }
                }
            }
        });
    }

    private String generateSessionId() {
        // Simplified session ID generation
        // In a real implementation, this would be handled by the research engine
        return java.util.UUID.randomUUID().toString();
    }

    private String convertTreeToMarkdown(Map<String, Object> tree) {
        StringBuilder markdown = new StringBuilder();

        // Add header
        String sessionId = (String) tree.get("sessionId");
        String status = (String) tree.get("status");
        markdown.append("# Deep Research Report\n\n");
        markdown.append("**Session ID:** ").append(sessionId).append("\n");
        markdown.append("**Status:** ").append(status).append("\n\n");

        // Add statistics
        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) tree.get("statistics");
        if (statistics != null) {
            markdown.append("## Research Statistics\n\n");
            statistics.forEach((key, value) -> {
                markdown.append("- **").append(formatKey(key)).append(":** ").append(value).append("\n");
            });
            markdown.append("\n");
        }

        // Add tree structure
        @SuppressWarnings("unchecked")
        Map<String, Object> rootNode = (Map<String, Object>) tree.get("rootNode");
        if (rootNode != null) {
            markdown.append("## Research Tree\n\n");
            appendNodeToMarkdown(markdown, rootNode, 0);
        }

        return markdown.toString();
    }

    @SuppressWarnings("unchecked")
    private void appendNodeToMarkdown(StringBuilder markdown, Map<String, Object> node, int depth) {
        String indent = "  ".repeat(depth);
        String header = "#".repeat(Math.min(6, depth + 3));

        String label = (String) node.get("label");
        String status = (String) node.get("status");
        String researchGoal = (String) node.get("researchGoal");

        markdown.append(indent).append(header).append(" ").append(label).append("\n\n");

        if (researchGoal != null && !researchGoal.isEmpty()) {
            markdown.append(indent).append("**Research Goal:** ").append(researchGoal).append("\n\n");
        }

        markdown.append(indent).append("**Status:** ").append(status).append("\n\n");

        // Add learnings count and search results count
        Integer learningsCount = (Integer) node.get("learningsCount");
        Integer searchResultsCount = (Integer) node.get("searchResultsCount");

        if (learningsCount != null && learningsCount > 0) {
            markdown.append(indent).append("**Learnings Found:** ").append(learningsCount).append("\n\n");
        }

        if (searchResultsCount != null && searchResultsCount > 0) {
            markdown.append(indent).append("**Sources Searched:** ").append(searchResultsCount).append("\n\n");
        }

        // Add children
        java.util.List<Map<String, Object>> children = (java.util.List<Map<String, Object>>) node.get("children");
        if (children != null && !children.isEmpty()) {
            for (Map<String, Object> child : children) {
                appendNodeToMarkdown(markdown, child, depth + 1);
            }
        }

        markdown.append("\n");
    }

    private String formatKey(String key) {
        // Convert camelCase to Title Case
        return key.replaceAll("([a-z])([A-Z])", "$1 $2")
            .replaceAll("^.", String.valueOf(Character.toUpperCase(key.charAt(0))));
    }
}