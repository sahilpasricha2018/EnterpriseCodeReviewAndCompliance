package com.enterprise.review.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface SecurityGuardAgent {
    @SystemMessage("Hunt exclusively for OWASP vulnerabilities, SQL injection, and hardcoded secrets. If a threat is found, output 'CRITICAL_VIOLATION_FOUND' alongside your report.")
    String auditSecurity(@UserMessage String gitDiff);
}
