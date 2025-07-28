package io.github.venkat1701.deepresearch.pipeline;

import java.util.*;
import java.util.logging.Logger;
import io.github.venkat1701.deepresearch.context.DeepResearchContext;
import io.github.venkat1701.deepresearch.models.NarrativeStructure;

/**
 * Context-Aware Chunker - Handles intelligent content chunking with overlap
 * Overcomes context limits through sophisticated content segmentation
 */
public class ContextAwareChunker {

    private static final Logger logger = Logger.getLogger(ContextAwareChunker.class.getName());
    private static final double CHUNK_OVERLAP_RATIO = 0.15;

    private final int contextLimit;

    public ContextAwareChunker(int contextLimit) {
        this.contextLimit = contextLimit;
    }

    /**
     * Chunk content with intelligent overlap and theme detection
     */
    public List<ContentChunk> chunkContent(String content,
        DeepResearchContext context,
        NarrativeStructure structure) {
        List<ContentChunk> chunks = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return chunks;
        }

        // Determine optimal chunk size based on content and structure
        int optimalChunkSize = calculateOptimalChunkSize(content, structure);
        int overlapSize = (int) (optimalChunkSize * CHUNK_OVERLAP_RATIO);

        String[] paragraphs = content.split("\n\n");
        StringBuilder currentChunk = new StringBuilder();
        int currentSize = 0;
        int chunkIndex = 0;

        for (String paragraph : paragraphs) {
            if (currentSize + paragraph.length() > optimalChunkSize && currentSize > 0) {
                // Create chunk with current content
                String theme = determineChunkTheme(currentChunk.toString());
                chunks.add(new ContentChunk(currentChunk.toString(), theme,
                    chunkIndex * optimalChunkSize, chunkIndex * optimalChunkSize + currentSize));

                // Start new chunk with overlap
                String overlap = extractOverlap(currentChunk.toString(), overlapSize);
                currentChunk = new StringBuilder(overlap);
                currentSize = overlap.length();
                chunkIndex++;
            }

            currentChunk.append(paragraph).append("\n\n");
            currentSize += paragraph.length() + 2;
        }

        // Add final chunk
        if (currentSize > 0) {
            String theme = determineChunkTheme(currentChunk.toString());
            chunks.add(new ContentChunk(currentChunk.toString(), theme,
                chunkIndex * optimalChunkSize, chunkIndex * optimalChunkSize + currentSize));
        }

        logger.info("Content chunked into " + chunks.size() + " segments");
        return chunks;
    }

    /**
     * Chunk prompts for context window management
     */
    public List<ContextChunk> chunkPrompt(String prompt) {
        List<ContextChunk> chunks = new ArrayList<>();

        if (countTokens(prompt) <= contextLimit) {
            chunks.add(new ContextChunk(prompt, countTokens(prompt)));
            return chunks;
        }

        // Split into manageable chunks with overlap
        int chunkSize = contextLimit - 500; // Leave room for response
        String[] sentences = prompt.split("\\. ");

        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;

        for (String sentence : sentences) {
            int sentenceTokens = countTokens(sentence);

            if (currentTokens + sentenceTokens > chunkSize && currentTokens > 0) {
                chunks.add(new ContextChunk(currentChunk.toString(), currentTokens));

                // Start new chunk with some overlap
                currentChunk = new StringBuilder();
                currentTokens = 0;
            }

            currentChunk.append(sentence).append(". ");
            currentTokens += sentenceTokens;
        }

        if (currentTokens > 0) {
            chunks.add(new ContextChunk(currentChunk.toString(), currentTokens));
        }

        logger.info("Prompt chunked into " + chunks.size() + " context windows");
        return chunks;
    }

    /**
     * Chunk narrative for coherence enhancement
     */
    public List<ContextChunk> chunkNarrative(String narrative) {
        List<ContextChunk> chunks = new ArrayList<>();

        // Split narrative into sections for coherence enhancement
        String[] sections = narrative.split("(?=##\\s)");

        for (String section : sections) {
            if (!section.trim().isEmpty()) {
                int tokens = countTokens(section);
                chunks.add(new ContextChunk(section.trim(), tokens));
            }
        }

        return chunks;
    }

    /**
     * Compress prompt to fit target token count
     */
    public String compressPrompt(String prompt, int targetTokens) {
        if (countTokens(prompt) <= targetTokens) {
            return prompt;
        }

        // Simple compression by truncating to target size
        int targetChars = targetTokens * 4; // Approximate
        if (prompt.length() <= targetChars) {
            return prompt;
        }

        // Find good break point
        int breakPoint = findSentenceBreak(prompt, targetChars);
        return prompt.substring(0, breakPoint) + "...";
    }

    /**
     * Calculate optimal chunk size based on content complexity
     */
    private int calculateOptimalChunkSize(String content, NarrativeStructure structure) {
        int baseChunkSize = 2000; // Base chunk size in characters

        // Adjust based on content complexity
        if (content.contains("```") || content.contains("http")) {
            baseChunkSize += 500; // More space for code/links
        }

        // Adjust based on structure complexity
        int sectionCount = structure.getSections().size();
        if (sectionCount > 6) {
            baseChunkSize -= 200; // Smaller chunks for complex structures
        }

        return Math.max(1000, Math.min(baseChunkSize, 3000));
    }

    /**
     * Determine thematic category of content chunk
     */
    private String determineChunkTheme(String chunk) {
        String lowerChunk = chunk.toLowerCase();

        if (lowerChunk.contains("implement") || lowerChunk.contains("code")) {
            return "implementation";
        }
        if (lowerChunk.contains("performance") || lowerChunk.contains("benchmark")) {
            return "performance";
        }
        if (lowerChunk.contains("example") || lowerChunk.contains("case")) {
            return "examples";
        }
        if (lowerChunk.contains("architecture") || lowerChunk.contains("design")) {
            return "architecture";
        }
        if (lowerChunk.contains("security") || lowerChunk.contains("secure")) {
            return "security";
        }

        return "general";
    }

    /**
     * Extract overlap content from end of chunk
     */
    private String extractOverlap(String content, int overlapSize) {
        if (content.length() <= overlapSize) {
            return content;
        }

        // Extract from end, looking for sentence boundaries
        String suffix = content.substring(Math.max(0, content.length() - overlapSize * 2));
        int lastSentence = suffix.lastIndexOf(". ");

        if (lastSentence > overlapSize / 2) {
            return suffix.substring(lastSentence + 2);
        }

        return content.substring(content.length() - overlapSize);
    }

    /**
     * Find appropriate sentence break point
     */
    private int findSentenceBreak(String text, int position) {
        for (int i = Math.min(position, text.length() - 1); i > position - 200 && i > 0; i--) {
            if (text.charAt(i) == '.' || text.charAt(i) == '!' || text.charAt(i) == '?') {
                return i + 1;
            }
        }
        return position;
    }

    /**
     * Approximate token counting
     */
    private int countTokens(String text) {
        return text != null ? (int) Math.ceil(text.length() / 4.0) : 0;
    }

    /**
     * Content Chunk - Represents a segment of content with metadata
     */
    public static class ContentChunk {
        private final String content;
        private final String theme;
        private final int startIndex;
        private final int endIndex;
        private final double relevanceScore;

        public ContentChunk(String content, String theme, int startIndex, int endIndex) {
            this.content = content;
            this.theme = theme;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.relevanceScore = 1.0;
        }

        public String getContent() { return content; }
        public String getTheme() { return theme; }
        public int getStartIndex() { return startIndex; }
        public int getEndIndex() { return endIndex; }
        public double getRelevanceScore() { return relevanceScore; }
    }

    /**
     * Context Chunk - Represents a prompt chunk with token management
     */
    public static class ContextChunk {
        private final String content;
        private final int tokenCount;

        public ContextChunk(String content, int tokenCount) {
            this.content = content;
            this.tokenCount = tokenCount;
        }

        public String getContent() { return content; }
        public int getTokenCount() { return tokenCount; }
    }
}