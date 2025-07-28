package io.github.venkat1701.deepresearch.models;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.deepresearch.context.DeepResearchContext;

/**
 * Supporting Classes for Deep Research System
 * Contains data models, utilities, and helper classes
 */

/**
 * Narrative Structure - Represents the hierarchical structure of a research narrative
 */
public class NarrativeStructure {
    private final List<NarrativeSection> sections;
    private final int estimatedLength;
    private final String structureType;

    public NarrativeStructure(List<NarrativeSection> sections) {
        this.sections = new ArrayList<>(sections);
        this.estimatedLength = sections.stream()
            .mapToInt(NarrativeSection::getTargetLength)
            .sum();
        this.structureType = determineStructureType(sections);
    }

    public NarrativeStructure(List<NarrativeSection> sections, String structureType) {
        this.sections = new ArrayList<>(sections);
        this.estimatedLength = sections.stream()
            .mapToInt(NarrativeSection::getTargetLength)
            .sum();
        this.structureType = structureType;
    }

    private String determineStructureType(List<NarrativeSection> sections) {
        if (sections.size() <= 3) return "Simple";
        if (sections.size() <= 6) return "Standard";
        if (sections.size() <= 10) return "Comprehensive";
        return "Expert";
    }

    public List<NarrativeSection> getSections() {
        return new ArrayList<>(sections);
    }

    public int getEstimatedLength() {
        return estimatedLength;
    }

    public String getStructureType() {
        return structureType;
    }

    public void addSection(NarrativeSection section) {
        sections.add(section);
    }

    public void insertSection(int index, NarrativeSection section) {
        sections.add(index, section);
    }

    public boolean removeSection(String title) {
        return sections.removeIf(section -> section.getTitle().equals(title));
    }
}