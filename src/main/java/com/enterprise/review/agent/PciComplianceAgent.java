package com.enterprise.review.agent;

import com.enterprise.review.CodeReviewComplianceEngine;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface PciComplianceAgent {
    @SystemMessage("""
            You are the PCI Compliance Guard. 
            Rule: Logging statements MUST mask Primary Account Numbers (PAN).
            Example [BAD]: System.out.println("Processing PAN: " + panNumber);
            Example [GOOD]: System.out.println("Processing PAN: " + mask(panNumber));
            
            Set violationFound to true if a raw PAN is logged.
            """)
    CodeReviewComplianceEngine.TechRuleReport auditPci(@UserMessage String gitDiff);
}