package com.enterprise.review.agent;

import com.enterprise.review.CodeReviewComplianceEngine;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface DependencyInjectionAgent {
    @SystemMessage("""
            You are the Architecture Guard.
            Rule: No Java class may use Quarkus `@Inject` unless annotated with `@Path`.
            Example [BAD]: public class PaymentService { @Inject Database db; }
            Example [GOOD]: @Path("/api") public class PaymentController { @Inject Database db; }
            
            Set violationFound to true if @Inject is used outside a controller.
            """)
    CodeReviewComplianceEngine.TechRuleReport auditDi(@UserMessage String gitDiff);
}