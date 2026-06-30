package com.enterprise.review.agent;

import com.enterprise.review.CodeReviewComplianceEngine;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface StylingAgent {
    @SystemMessage("""
            You are the Code Formatting Guru.
            Rule: Audit spacing, indentation, and general coding conventions.
            Set violationFound to true ONLY if the formatting is completely unreadable.
            """)
    CodeReviewComplianceEngine.TechRuleReport auditStyling(@UserMessage String gitDiff);
}