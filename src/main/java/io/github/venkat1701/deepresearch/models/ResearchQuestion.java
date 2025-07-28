package io.github.venkat1701.deepresearch.models;

import java.time.Instant;
import java.util.*;

/**
 * Research Question - Represents a specific research question with metadata
 */
public class ResearchQuestion {

    public enum Priority {
        LOW(1), MEDIUM(2), HIGH(3);

        private final int value;

        Priority(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private String question;
    private Priority priority;
    private String category;
    private boolean researched;
    private Instant createdAt;
    private Instant researchedAt;
    private String rationale;
    private Map<String, Object> metadata;
    private List<String> keywords;
    private String expectedOutcome;

    // Constructors
    public ResearchQuestion(String question, Priority priority, String category) {
        this.question = question;
        this.priority = priority;
        this.category = category;
        this.researched = false;
        this.createdAt = Instant.now();
        this.rationale = "";
        this.metadata = new HashMap<>();
        this.keywords = extractKeywords(question);
        this.expectedOutcome = "";
    }

    public ResearchQuestion(String question, String category, String priorityStr) {
        this(question, parsePriority(priorityStr), category);
    }

    // Static helper method to parse priority from string
    private static Priority parsePriority(String priorityStr) {
        if (priorityStr == null) return Priority.MEDIUM;

        return switch (priorityStr.toLowerCase()) {
            case "high" -> Priority.HIGH;
            case "low" -> Priority.LOW;
            default -> Priority.MEDIUM;
        };
    }

    // Extract keywords from question text
    private List<String> extractKeywords(String questionText) {
        if (questionText == null || questionText.isEmpty()) {
            return new ArrayList<>();
        }

        // Simple keyword extraction - split on whitespace and filter
        String[] words = questionText.toLowerCase().split("\\s+");
        List<String> keywords = new ArrayList<>();

        for (String word : words) {
            // Clean word and filter out short words and common stop words
            String cleanWord = word.replaceAll("[^a-zA-Z0-9]", "");
            if (cleanWord.length() > 3 && !isStopWord(cleanWord)) {
                keywords.add(cleanWord);
            }
        }

        return keywords;
    }

    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of(
            "what", "how", "why", "when", "where", "who", "which", "that", "this", "these", "those",
            "the", "and", "for", "are", "but", "not", "you", "all", "can", "had", "her", "was", "one",
            "our", "out", "day", "get", "has", "him", "his", "how", "its", "may", "new", "now", "old",
            "see", "two", "way", "who", "boy", "did", "down", "each", "find", "good", "have", "like",
            "make", "said", "she", "they", "time", "very", "will", "with", "your"
        );
        return stopWords.contains(word.toLowerCase());
    }

    // Mark question as researched
    public void markAsResearched() {
        this.researched = true;
        this.researchedAt = Instant.now();
    }

    // Calculate question complexity score
    public double getComplexityScore() {
        double score = 0.0;

        // Length factor
        int wordCount = question.split("\\s+").length;
        score += Math.min(wordCount / 15.0, 1.0) * 0.3;

        // Complexity keywords
        String lowerQuestion = question.toLowerCase();
        if (lowerQuestion.contains("compare") || lowerQuestion.contains("analyze") ||
            lowerQuestion.contains("evaluate") || lowerQuestion.contains("assess")) {
            score += 0.4;
        }

        // Priority factor
        score += priority.getValue() / 3.0 * 0.3;

        return Math.min(score, 1.0);
    }

    // Check if question requires deep research
    public boolean requiresDeepResearch() {
        return getComplexityScore() > 0.6 || priority == Priority.HIGH;
    }

    // Get estimated research time in minutes
    public int getEstimatedResearchTime() {
        int baseTime = 5; // Base 5 minutes

        // Adjust based on complexity
        baseTime += (int) (getComplexityScore() * 15);

        // Adjust based on priority
        if (priority == Priority.HIGH) {
            baseTime += 10;
        } else if (priority == Priority.LOW) {
            baseTime -= 2;
        }

        return Math.max(baseTime, 3); // Minimum 3 minutes
    }

    // Generate search queries for this question
    public List<String> generateSearchQueries() {
        List<String> queries = new ArrayList<>();

        // Base query
        queries.add(question);

        // Keyword-based queries
        if (keywords.size() >= 2) {
            StringBuilder keywordQuery = new StringBuilder();
            for (int i = 0; i < Math.min(keywords.size(), 4); i++) {
                keywordQuery.append(keywords.get(i)).append(" ");
            }
            queries.add(keywordQuery.toString().trim());
        }

        // Category-specific queries
        if (category != null && !category.isEmpty()) {
            queries.add(question + " " + category);
        }

        // Complexity-based queries
        if (requiresDeepResearch()) {
            queries.add(question + " detailed analysis");
            queries.add(question + " comprehensive guide");
        }

        return queries;
    }

    // Validate question quality
    public ValidationResult validateQuestion() {
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (question == null || question.trim().isEmpty()) {
            issues.add("Question text is empty");
        } else {
            if (question.length() < 10) {
                warnings.add("Question is very short (< 10 characters)");
            }
            if (question.length() > 200) {
                warnings.add("Question is very long (> 200 characters)");
            }
            if (!question.contains("?") && !question.toLowerCase().startsWith("how") &&
                !question.toLowerCase().startsWith("what") && !question.toLowerCase().startsWith("why")) {
                warnings.add("Question may not be properly formatted");
            }
        }

        if (category == null || category.trim().isEmpty()) {
            warnings.add("No category specified");
        }

        if (keywords.isEmpty()) {
            warnings.add("No keywords extracted from question");
        }

        return new ValidationResult(issues, warnings, issues.isEmpty());
    }

    // Getters and Setters
    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
        this.keywords = extractKeywords(question); // Regenerate keywords
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isResearched() {
        return researched;
    }

    public void setResearched(boolean researched) {
        this.researched = researched;
        if (researched && researchedAt == null) {
            this.researchedAt = Instant.now();
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResearchedAt() {
        return researchedAt;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    public void setMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public List<String> getKeywords() {
        return new ArrayList<>(keywords);
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public void setExpectedOutcome(String expectedOutcome) {
        this.expectedOutcome = expectedOutcome;
    }

    // Utility methods
    @Override
    public String toString() {
        return String.format("ResearchQuestion{question='%s', priority=%s, category='%s', researched=%s}",
            question, priority, category, researched);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ResearchQuestion that = (ResearchQuestion) obj;
        return Objects.equals(question, that.question) &&
            Objects.equals(category, that.category) &&
            priority == that.priority;
    }

    @Override
    public int hashCode() {
        return Objects.hash(question, category, priority);
    }

    // Create a copy of this question
    public ResearchQuestion copy() {
        ResearchQuestion copy = new ResearchQuestion(question, priority, category);
        copy.researched = this.researched;
        copy.researchedAt = this.researchedAt;
        copy.rationale = this.rationale;
        copy.expectedOutcome = this.expectedOutcome;
        copy.metadata = new HashMap<>(this.metadata);
        copy.keywords = new ArrayList<>(this.keywords);
        return copy;
    }

    // Supporting Classes
    public static class ValidationResult {
        private final List<String> issues;
        private final List<String> warnings;
        private final boolean isValid;

        public ValidationResult(List<String> issues, List<String> warnings, boolean isValid) {
            this.issues = new ArrayList<>(issues);
            this.warnings = new ArrayList<>(warnings);
            this.isValid = isValid;
        }

        public List<String> getIssues() { return new ArrayList<>(issues); }
        public List<String> getWarnings() { return new ArrayList<>(warnings); }
        public boolean isValid() { return isValid; }
        public boolean hasWarnings() { return !warnings.isEmpty(); }

        public String getReport() {
            StringBuilder report = new StringBuilder();

            if (isValid) {
                report.append("✅ Question is valid\n");
            } else {
                report.append("❌ Question has issues\n");
            }

            if (!issues.isEmpty()) {
                report.append("\nIssues:\n");
                for (String issue : issues) {
                    report.append("- ").append(issue).append("\n");
                }
            }

            if (!warnings.isEmpty()) {
                report.append("\nWarnings:\n");
                for (String warning : warnings) {
                    report.append("- ").append(warning).append("\n");
                }
            }

            return report.toString();
        }
    }
}