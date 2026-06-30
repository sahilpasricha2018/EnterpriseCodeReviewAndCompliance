package com.enterprise.review.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface GatekeeperAgent {
    @SystemMessage("Analyze the PR. If it ONLY changes markdown, txt, or documentation files, output 'BYPASS'. Otherwise, output 'PROCEED'.")
    String validateScope(@UserMessage String gitDiff);
}
