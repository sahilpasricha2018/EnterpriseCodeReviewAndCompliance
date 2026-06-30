package com.enterprise.review.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface DoneDefAgent {
    @SystemMessage("You are the Release Manager. Read all the specialist reports. Summarize them into a single Markdown report. End the report with either [FINAL_DECISION: APPROVED] or [FINAL_DECISION: REJECTED] based on the findings.")
    String generateFinalReport(@UserMessage String combinedReports);
}
