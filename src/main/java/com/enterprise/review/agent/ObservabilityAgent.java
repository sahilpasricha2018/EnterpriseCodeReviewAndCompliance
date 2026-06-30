package com.enterprise.review.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface ObservabilityAgent {
    @SystemMessage("Ensure the code has appropriate logging and does not suppress errors silently. Warn if metrics are missing.")
    String auditObservability(@UserMessage String gitDiff);
}
