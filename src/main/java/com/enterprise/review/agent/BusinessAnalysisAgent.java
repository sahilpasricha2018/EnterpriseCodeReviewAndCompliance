package com.enterprise.review.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface BusinessAnalysisAgent {
    @SystemMessage("Compare the Git Diff against the Acceptance Criteria. If the code completely misses the business goal, output 'CRITICAL_VIOLATION_FOUND'.")
    String auditBusinessLogic(@UserMessage String prompt);
}
