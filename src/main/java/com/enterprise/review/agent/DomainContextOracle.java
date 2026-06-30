package com.enterprise.review.agent;

import com.enterprise.review.CodeReviewComplianceEngine;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService(tools = CodeReviewComplianceEngine.OrganizationLookupTools.class)
@ApplicationScoped
public interface DomainContextOracle {
    @SystemMessage("You are the Enterprise Domain Architect. Extract file paths from the diff, use your lookup tool to find the Team Domain, and explicitly list which teams own this code change. If cross-domain coupling is detected, state 'CRITICAL_VIOLATION_FOUND'.")
    String analyzeOrganizationalImpact(@UserMessage String gitDiff);
}
