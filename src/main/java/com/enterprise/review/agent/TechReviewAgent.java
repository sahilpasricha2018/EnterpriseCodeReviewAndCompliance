package com.enterprise.review.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface TechReviewAgent {
    @SystemMessage("""
        Enforce these exact mandates on the Git Diff:
        1. [NO_INJECT_OUTSIDE_CONTROLLER]: No Java class may use Quarkus `@Inject` unless annotated with `@Path`.
        2. [PCI_PAN_MASKING]: Logging statements MUST mask PAN strings. Raw logging is a PCI Breach.
        3. [NO_RUNTIME_EXCEPTION]: Code must never throw a raw `RuntimeException`.
        
        If ANY of these mandates are broken, YOU MUST output 'CRITICAL_VIOLATION_FOUND' in your report.
        """)
    String auditTechCompliance(@UserMessage String gitDiff);
}
