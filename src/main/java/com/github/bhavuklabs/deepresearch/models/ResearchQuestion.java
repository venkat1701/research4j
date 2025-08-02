package com.github.bhavuklabs.deepresearch.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ResearchQuestion {

    public enum Priority {
        LOW(1),
        MEDIUM(2),
        HIGH(3);

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

    private List<String> extractKeywords(String questionText) {
        if (questionText == null || questionText.isEmpty()) {
            return new ArrayList<>();
        }

        String[] words = questionText.toLowerCase()
            .split("\\s+");
        List<String> keywords = new ArrayList<>();

        for (String word : words) {

            String cleanWord = word.replaceAll("[^a-zA-Z0-9]", "");
            if (cleanWord.length() > 3 && !isStopWord(cleanWord)) {
                keywords.add(cleanWord);
            }
        }

        return keywords;
    }

    public ResearchQuestion(String question, Priority priority, String category) {
        if (question == null || question.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Question cannot be null or empty");
        }
        if (category == null || category.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Category cannot be null or empty");
        }

        this.question = question.trim();
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.category = category.trim();
        this.researched = false;
        this.createdAt = Instant.now();
        this.rationale = "";
        this.metadata = new HashMap<>();
        this.keywords = extractKeywordsSafely(this.question);
        this.expectedOutcome = "";
    }

    public ResearchQuestion(String question, String category, String priorityStr) {
        this(question, parsePriority(priorityStr), category);
    }

    private List<String> extractKeywordsSafely(String questionText) {
        List<String> keywords = new ArrayList<>();

        if (questionText == null || questionText.trim()
            .isEmpty()) {
            return keywords;
        }

        try {

            String cleanText = questionText.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

            if (cleanText.isEmpty()) {
                return keywords;
            }

            String[] words = cleanText.split("\\s+");

            Set<String> uniqueKeywords = new LinkedHashSet<>();

            for (String word : words) {
                if (word != null && !word.trim()
                    .isEmpty()) {
                    String cleanWord = word.trim();

                    if (cleanWord.length() > 3 && !isStopWordSafe(cleanWord)) {
                        uniqueKeywords.add(cleanWord);
                    }
                }
            }

            keywords.addAll(uniqueKeywords);

        } catch (Exception e) {

            System.err.println("Error extracting keywords from question: " + e.getMessage());

            keywords.clear();
        }

        return keywords;
    }

    private boolean isStopWordSafe(String word) {
        if (word == null || word.trim()
            .isEmpty()) {
            return true;
        }

        try {

            Set<String> stopWords = createSafeStopWordsSet();
            return stopWords.contains(word.toLowerCase()
                .trim());
        } catch (Exception e) {

            System.err.println("Error checking stop word: " + e.getMessage());
            return false;
        }
    }

    private Set<String> createSafeStopWordsSet() {
        Set<String> stopWords = new LinkedHashSet<>();

        try {

            String[] stopWordArray = { "what", "how", "why", "when", "where", "who", "which", "that", "this", "these", "those", "the", "and", "for", "are",
                "but", "not", "you", "all", "can", "had", "her", "was", "one", "our", "out", "day", "get", "has", "him", "his", "its", "may", "new", "now",
                "old", "see", "two", "way", "boy", "did", "down", "each", "find", "good", "have", "like", "make", "said", "she", "they", "time", "very", "will",
                "with", "your" };

            for (String word : stopWordArray) {
                if (word != null && !word.trim()
                    .isEmpty()) {
                    try {
                        stopWords.add(word.trim()
                            .toLowerCase());
                    } catch (Exception e) {

                        System.err.println("Error adding stop word '" + word + "': " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error creating stop words set: " + e.getMessage());

            return new LinkedHashSet<>();
        }

        return stopWords;
    }

    private static Priority parsePriority(String priorityStr) {
        if (priorityStr == null || priorityStr.trim()
            .isEmpty()) {
            return Priority.MEDIUM;
        }

        try {
            String cleanPriority = priorityStr.trim()
                .toLowerCase();
            return switch (cleanPriority) {
                case "high" -> Priority.HIGH;
                case "low" -> Priority.LOW;
                case "medium" -> Priority.MEDIUM;
                default -> Priority.MEDIUM;
            };
        } catch (Exception e) {
            System.err.println("Error parsing priority '" + priorityStr + "': " + e.getMessage());
            return Priority.MEDIUM;
        }
    }

    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of("what", "why", "when", "where", "who", "which", "that", "this", "these", "those", "the", "and", "for", "are", "but",
            "not", "you", "all", "can", "had", "her", "was", "one", "our", "out", "day", "get", "has", "him", "his", "how", "its", "may", "new", "now", "old",
            "see", "two", "way", "who", "boy", "did", "down", "each", "find", "good", "have", "like", "make", "said", "she", "they", "time", "very", "will",
            "with", "your");
        return stopWords.contains(word.toLowerCase());
    }

    public void markAsResearched() {
        this.researched = true;
        this.researchedAt = Instant.now();
    }

    public double getComplexityScore() {
        double score = 0.0;

        int wordCount = question.split("\\s+").length;
        score += Math.min(wordCount / 15.0, 1.0) * 0.3;

        String lowerQuestion = question.toLowerCase();
        if (lowerQuestion.contains("compare") || lowerQuestion.contains("analyze") || lowerQuestion.contains("evaluate") || lowerQuestion.contains("assess")) {
            score += 0.4;
        }

        score += priority.getValue() / 3.0 * 0.3;

        return Math.min(score, 1.0);
    }

    public boolean requiresDeepResearch() {
        return getComplexityScore() > 0.6 || priority == Priority.HIGH;
    }

    public int getEstimatedResearchTime() {
        int baseTime = 5;

        baseTime += (int) (getComplexityScore() * 15);

        if (priority == Priority.HIGH) {
            baseTime += 10;
        } else if (priority == Priority.LOW) {
            baseTime -= 2;
        }

        return Math.max(baseTime, 3);
    }

    public List<String> generateSearchQueries() {
        List<String> queries = new ArrayList<>();

        queries.add(question);

        if (keywords.size() >= 2) {
            StringBuilder keywordQuery = new StringBuilder();
            for (int i = 0; i < Math.min(keywords.size(), 4); i++) {
                keywordQuery.append(keywords.get(i))
                    .append(" ");
            }
            queries.add(keywordQuery.toString()
                .trim());
        }

        if (category != null && !category.isEmpty()) {
            queries.add(question + " " + category);
        }

        if (requiresDeepResearch()) {
            queries.add(question + " detailed analysis");
            queries.add(question + " comprehensive guide");
        }

        return queries;
    }

    public ValidationResult validateQuestion() {
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (question == null || question.trim()
            .isEmpty()) {
            issues.add("Question text is empty");
        } else {
            if (question.length() < 10) {
                warnings.add("Question is very short (< 10 characters)");
            }
            if (question.length() > 200) {
                warnings.add("Question is very long (> 200 characters)");
            }
            if (!question.contains("?") && !question.toLowerCase()
                .startsWith("how") && !question.toLowerCase()
                .startsWith("what") && !question.toLowerCase()
                .startsWith("why")) {
                warnings.add("Question may not be properly formatted");
            }
        }

        if (category == null || category.trim()
            .isEmpty()) {
            warnings.add("No category specified");
        }

        if (keywords.isEmpty()) {
            warnings.add("No keywords extracted from question");
        }

        return new ValidationResult(issues, warnings, issues.isEmpty());
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
        this.keywords = extractKeywords(question);
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

    @Override
    public String toString() {
        return String.format("ResearchQuestion{question='%s', priority=%s, category='%s', researched=%s}", question, priority, category, researched);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ResearchQuestion that = (ResearchQuestion) obj;
        return Objects.equals(question, that.question) && Objects.equals(category, that.category) && priority == that.priority;
    }

    @Override
    public int hashCode() {
        return Objects.hash(question, category, priority);
    }

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

    public static class ValidationResult {

        private final List<String> issues;
        private final List<String> warnings;
        private final boolean isValid;

        public ValidationResult(List<String> issues, List<String> warnings, boolean isValid) {
            this.issues = new ArrayList<>(issues);
            this.warnings = new ArrayList<>(warnings);
            this.isValid = isValid;
        }

        public List<String> getIssues() {
            return new ArrayList<>(issues);
        }

        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }

        public boolean isValid() {
            return isValid;
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

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
                    report.append("- ")
                        .append(issue)
                        .append("\n");
                }
            }

            if (!warnings.isEmpty()) {
                report.append("\nWarnings:\n");
                for (String warning : warnings) {
                    report.append("- ")
                        .append(warning)
                        .append("\n");
                }
            }

            return report.toString();
        }
    }
}