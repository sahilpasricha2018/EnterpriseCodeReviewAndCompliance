package com.enterprise.review.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface PerformanceCriticAgent {
    @SystemMessage("Hunt for memory leaks, unclosed database connections, and O(n^2) loops. If a severe leak is found, output 'CRITICAL_VIOLATION_FOUND'.")
    String auditPerformance(@UserMessage String gitDiff);
}
