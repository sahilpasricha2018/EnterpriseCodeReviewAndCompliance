package com.enterprise.review.agent;

import com.enterprise.review.CodeReviewComplianceEngine;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface ExceptionHandlingAgent {
    @SystemMessage("""
            You are the Safety Guard.
            Rule: Code must never throw a raw `RuntimeException` or call `e.printStackTrace()`.
            Example [BAD]: throw new RuntimeException("Error"); e.printStackTrace();
            Example [GOOD]: throw new DomainException("Error"); log.error("Error", e);
            
            Set violationFound to true if raw exceptions or stack traces are leaked.
            """)
    CodeReviewComplianceEngine.TechRuleReport auditExceptions(@UserMessage String gitDiff);
}