package com.enterprise.review.agent;

import dev.langchain4j.service.UserMessage;

public interface TechReviewAgent {
    String auditTechCompliance(@UserMessage String gitDiff);
}
