package com.enterprise.review.agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@ApplicationScoped
public class TechReviewAgentImpl implements TechReviewAgent {

    @Inject
    ChatLanguageModel chatModel;

    private final String systemMessage;

    public TechReviewAgentImpl() {
        try {
            String persona = loadResource("tech-reviewer-persona.md");
            String rules = loadResource("tech-reviewer-rules.md");
            this.systemMessage = persona + "\n\n" + rules;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load system messages for TechReviewAgent", e);
        }
    }

    private String loadResource(String resourceName) throws Exception {
        try (InputStream inputStream = TechReviewAgentImpl.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new RuntimeException("Failed to find '" + resourceName + "' in classpath resources.");
            }
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }

    @Override
    public String auditTechCompliance(String gitDiff) {
        String userMessage = "Here is the git diff to review:\n\n" + gitDiff;

        PromptTemplate promptTemplate = PromptTemplate.from(systemMessage + "\n\n" + userMessage);
        Prompt prompt = promptTemplate.apply(Map.of()); // No variables needed in this combined template

        return chatModel.generate(prompt.text());
    }
}
