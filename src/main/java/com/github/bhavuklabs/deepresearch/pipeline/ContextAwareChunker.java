package com.github.bhavuklabs.deepresearch.pipeline;

import java.util.*;
import java.util.logging.Logger;
import com.github.bhavuklabs.deepresearch.context.DeepResearchContext;
import com.github.bhavuklabs.deepresearch.models.NarrativeStructure;


public class ContextAwareChunker {

    private static final Logger logger = Logger.getLogger(ContextAwareChunker.class.getName());
    private static final double CHUNK_OVERLAP_RATIO = 0.15;

    private final int contextLimit;

    public ContextAwareChunker(int contextLimit) {
        this.contextLimit = contextLimit;
    }

    
    public List<ContentChunk> chunkContent(String content,
        DeepResearchContext context,
        NarrativeStructure structure) {
        List<ContentChunk> chunks = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return chunks;
        }

        
        int optimalChunkSize = calculateOptimalChunkSize(content, structure);
        int overlapSize = (int) (optimalChunkSize * CHUNK_OVERLAP_RATIO);

        String[] paragraphs = content.split("\n\n");
        StringBuilder currentChunk = new StringBuilder();
        int currentSize = 0;
        int chunkIndex = 0;

        for (String paragraph : paragraphs) {
            if (currentSize + paragraph.length() > optimalChunkSize && currentSize > 0) {
                
                String theme = determineChunkTheme(currentChunk.toString());
                chunks.add(new ContentChunk(currentChunk.toString(), theme,
                    chunkIndex * optimalChunkSize, chunkIndex * optimalChunkSize + currentSize));

                
                String overlap = extractOverlap(currentChunk.toString(), overlapSize);
                currentChunk = new StringBuilder(overlap);
                currentSize = overlap.length();
                chunkIndex++;
            }

            currentChunk.append(paragraph).append("\n\n");
            currentSize += paragraph.length() + 2;
        }

        
        if (currentSize > 0) {
            String theme = determineChunkTheme(currentChunk.toString());
            chunks.add(new ContentChunk(currentChunk.toString(), theme,
                chunkIndex * optimalChunkSize, chunkIndex * optimalChunkSize + currentSize));
        }

        logger.info("Content chunked into " + chunks.size() + " segments");
        return chunks;
    }

    
    public List<ContextChunk> chunkPrompt(String prompt) {
        List<ContextChunk> chunks = new ArrayList<>();

        if (countTokens(prompt) <= contextLimit) {
            chunks.add(new ContextChunk(prompt, countTokens(prompt)));
            return chunks;
        }

        
        int chunkSize = contextLimit - 500; 
        String[] sentences = prompt.split("\\. ");

        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;

        for (String sentence : sentences) {
            int sentenceTokens = countTokens(sentence);

            if (currentTokens + sentenceTokens > chunkSize && currentTokens > 0) {
                chunks.add(new ContextChunk(currentChunk.toString(), currentTokens));

                
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

    
    public List<ContextChunk> chunkNarrative(String narrative) {
        List<ContextChunk> chunks = new ArrayList<>();

        
        String[] sections = narrative.split("(?=##\\s)");

        for (String section : sections) {
            if (!section.trim().isEmpty()) {
                int tokens = countTokens(section);
                chunks.add(new ContextChunk(section.trim(), tokens));
            }
        }

        return chunks;
    }

    
    public String compressPrompt(String prompt, int targetTokens) {
        if (countTokens(prompt) <= targetTokens) {
            return prompt;
        }

        
        int targetChars = targetTokens * 4; 
        if (prompt.length() <= targetChars) {
            return prompt;
        }

        
        int breakPoint = findSentenceBreak(prompt, targetChars);
        return prompt.substring(0, breakPoint) + "...";
    }

    
    private int calculateOptimalChunkSize(String content, NarrativeStructure structure) {
        int baseChunkSize = 2000; 

        
        if (content.contains("```") || content.contains("http")) {
            baseChunkSize += 500; 
        }

        
        int sectionCount = structure.getSections().size();
        if (sectionCount > 6) {
            baseChunkSize -= 200; 
        }

        return Math.max(1000, Math.min(baseChunkSize, 3000));
    }

    
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

    
    private String extractOverlap(String content, int overlapSize) {
        if (content.length() <= overlapSize) {
            return content;
        }

        
        String suffix = content.substring(Math.max(0, content.length() - overlapSize * 2));
        int lastSentence = suffix.lastIndexOf(". ");

        if (lastSentence > overlapSize / 2) {
            return suffix.substring(lastSentence + 2);
        }

        return content.substring(content.length() - overlapSize);
    }

    
    private int findSentenceBreak(String text, int position) {
        for (int i = Math.min(position, text.length() - 1); i > position - 200 && i > 0; i--) {
            if (text.charAt(i) == '.' || text.charAt(i) == '!' || text.charAt(i) == '?') {
                return i + 1;
            }
        }
        return position;
    }

    
    private int countTokens(String text) {
        return text != null ? (int) Math.ceil(text.length() / 4.0) : 0;
    }

    
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