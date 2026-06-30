package com.enterprise.review;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.*;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@QuarkusMain
public class CodeReviewComplianceEngine {
    public static void main(String... args) {
        io.quarkus.runtime.Quarkus.run(EnterprisePipelineRunner.class, args);
    }

    // ============================================================================
    // 1. ENTERPRISE DATA STATE SCHEMA
    // ============================================================================
    public static class ReviewState extends AgentState {
        // Using Map.ofEntries because we have more than 10 channels mapped
        public static final Map<String, Channel<?>> SCHEMA = Map.ofEntries(
            Map.entry("rawGitDiff", Channels.base(() -> "")),
            Map.entry("acceptanceCriteria", Channels.base(() -> "")),
            Map.entry("domainContext", Channels.base(() -> "")),
            
            // Specialist Logging Channels
            Map.entry("gatekeeperLog", Channels.base(() -> "")),
            Map.entry("vulnerabilityLog", Channels.base(() -> "")),
            Map.entry("performanceLog", Channels.base(() -> "")),
            Map.entry("businessAnalysisLog", Channels.base(() -> "")),
            Map.entry("techReviewLog", Channels.base(() -> "")),
            Map.entry("observabilityLog", Channels.base(() -> "")),
            
            // Output & Control Channels
            Map.entry("violationRegistry", Channels.appender(ArrayList::new)),
            Map.entry("finalMergedReport", Channels.base(() -> "")),
            Map.entry("isApproved", Channels.base(() -> false))
        );

        public ReviewState(Map<String, Object> initData) { super(initData); }
        
        // Type-Safe Accessors
        public String rawGitDiff() { return this.<String>value("rawGitDiff").orElse(""); }
        public String acceptanceCriteria() { return this.<String>value("acceptanceCriteria").orElse(""); }
        public String domainContext() { return this.<String>value("domainContext").orElse(""); }
        
        public String gatekeeperLog() { return this.<String>value("gatekeeperLog").orElse(""); }
        public String vulnerabilityLog() { return this.<String>value("vulnerabilityLog").orElse(""); }
        public String performanceLog() { return this.<String>value("performanceLog").orElse(""); }
        public String businessAnalysisLog() { return this.<String>value("businessAnalysisLog").orElse(""); }
        public String techReviewLog() { return this.<String>value("techReviewLog").orElse(""); }
        public String observabilityLog() { return this.<String>value("observabilityLog").orElse(""); }
        
        public List<String> violationRegistry() { return this.<List<String>>value("violationRegistry").orElse(new ArrayList<>()); }
        public boolean isApproved() { return this.<Boolean>value("isApproved").orElse(false); }
    }

    // ============================================================================
    // 2. DOMAIN ORACLE & TOOLS
    // ============================================================================
    @RegisterAiService
    @ApplicationScoped
    public interface DomainContextOracle {
        @SystemMessage("You are a code analysis expert. Your only job is to extract all unique file paths from the provided git diff and list each path on a new line. Do not add any commentary.")
        String analyzeOrganizationalImpact(@UserMessage String gitDiff);
    }

    // ============================================================================
    // 3. THE COMPLIANCE SPECIALISTS (LangChain4j Agents)
    // ============================================================================
    @RegisterAiService
    @ApplicationScoped
    public interface GatekeeperAgent {
        @SystemMessage("Analyze the PR. If it ONLY changes markdown, txt, or documentation files, output 'BYPASS'. Otherwise, output 'PROCEED'.")
        String validateScope(@UserMessage String gitDiff);
    }

    @RegisterAiService
    @ApplicationScoped
    public interface SecurityGuardAgent {
        @SystemMessage("Hunt exclusively for OWASP vulnerabilities, SQL injection, and hardcoded secrets. If a threat is found, output 'CRITICAL_VIOLATION_FOUND' alongside your report.")
        String auditSecurity(@UserMessage String gitDiff);
    }

    @RegisterAiService
    @ApplicationScoped
    public interface PerformanceCriticAgent {
        @SystemMessage("Hunt for memory leaks, unclosed database connections, and O(n^2) loops. If a severe leak is found, output 'CRITICAL_VIOLATION_FOUND'.")
        String auditPerformance(@UserMessage String gitDiff);
    }

    @RegisterAiService
    @ApplicationScoped
    public interface BusinessAnalysisAgent {
        @SystemMessage("Compare the Git Diff against the Acceptance Criteria. If the code completely misses the business goal, output 'CRITICAL_VIOLATION_FOUND'.")
        String auditBusinessLogic(@UserMessage String prompt);
    }

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

    @RegisterAiService
    @ApplicationScoped
    public interface ObservabilityAgent {
        @SystemMessage("Ensure the code has appropriate logging and does not suppress errors silently. Warn if metrics are missing.")
        String auditObservability(@UserMessage String gitDiff);
    }

    @RegisterAiService
    @ApplicationScoped
    public interface DoneDefAgent {
        @SystemMessage("You are the Release Manager. Read all the specialist reports. Summarize them into a single Markdown report. End the report with either [FINAL_DECISION: APPROVED] or [FINAL_DECISION: REJECTED] based on the findings.")
        String generateFinalReport(@UserMessage String combinedReports);
    }

    // ============================================================================
    // 4. GRAPH ENGINE CONTROLLER
    // ============================================================================
    @ApplicationScoped
    public static class EnterprisePipelineRunner implements QuarkusApplication {

        @Inject GatekeeperAgent gatekeeper;
        @Inject DomainContextOracle domainOracle;
        @Inject SecurityGuardAgent security;
        @Inject PerformanceCriticAgent performance;
        @Inject BusinessAnalysisAgent ba;
        @Inject TechReviewAgent tech;
        @Inject ObservabilityAgent observability;
        @Inject DoneDefAgent doneDef;

        @Override
        public int run(String... args) throws Exception {
            System.out.println("\n=== BOOTING 8-NODE COMPLIANCE PIPELINE ===");

            StateGraph<ReviewState> workflow = new StateGraph<>(ReviewState.SCHEMA, ReviewState::new)
                .addNode("n_gatekeeper", node_async(this::runGatekeeper))
                .addNode("n_domain", node_async(this::runDomain))
                .addNode("n_security", node_async(this::runSecurity))
                .addNode("n_performance", node_async(this::runPerformance))
                .addNode("n_ba", node_async(this::runBA))
                .addNode("n_tech", node_async(this::runTech))
                .addNode("n_observability", node_async(this::runObservability))
                .addNode("n_donedef", node_async(this::runDoneDef))
                
                // Pipeline sequence
                .addEdge(START, "n_gatekeeper")
                .addConditionalEdges("n_gatekeeper", edge_async(this::evaluateGatekeeper), 
                    Map.of("bypass", END, "proceed", "n_domain")
                )
                .addEdge("n_domain", "n_security")
                .addEdge("n_security", "n_performance")
                .addEdge("n_performance", "n_ba")
                .addEdge("n_ba", "n_tech")
                .addEdge("n_tech", "n_observability")
                .addEdge("n_observability", "n_donedef")
                
                // Final Sign-off
                .addConditionalEdges("n_donedef", edge_async(this::evaluateFinalGate), 
                    Map.of("merge_approved", END, "merge_rejected", END)
                );

            var engine = workflow.compile();

            // Mock Payload containing PCI and Architecture violations
            String badCodePR = """
                diff --git a/src/main/java/com/billing/PaymentService.java
                +++ b/src/main/java/com/billing/PaymentService.java
                @@ -12,4 +12,14 @@ 
                + public class PaymentService {
                +    @Inject DatabaseConnection db; // ILLEGAL INJECT OUTSIDE CONTROLLER
                +
                +    public void processTransaction(String panNumber) {
                +        System.out.println("Processing raw payment for PAN: " + panNumber); // PCI BREACH!
                +        if(panNumber == null) {
                +            throw new RuntimeException("PAN Missing"); // ILLEGAL RTE!
                +        }
                +    }
                + }
                """;

            Map<String, Object> payload = Map.of(
                "rawGitDiff", badCodePR,
                "acceptanceCriteria", "Ensure payments process gracefully without exposing secure data."
            );

            System.out.println("--> Injecting PR into State Matrix...");
            ReviewState finalState = engine.invoke(payload).orElseThrow();

            System.out.println("\n==========================================================");
            if (finalState.isApproved()) {
                System.out.println("✅ STATUS: MERGE APPROVED");
            } else {
                System.out.println("❌ STATUS: MERGE REJECTED");
                System.out.println("Violations Found: " + finalState.violationRegistry().size());
            }
            System.out.println("==========================================================");
            return 0;
        }

        // --- Node Implementations ---
        private Map<String, Object> runGatekeeper(ReviewState state) {
            System.out.println("[NODE 1] Gatekeeper analyzing scope...");
            return Map.of("gatekeeperLog", gatekeeper.validateScope(state.rawGitDiff()));
        }

        private String evaluateGatekeeper(ReviewState state) {
            return state.gatekeeperLog().contains("BYPASS") ? "bypass" : "proceed";
        }

        private Map<String, Object> runDomain(ReviewState state) {
            System.out.println("[NODE 2] Domain Oracle mapping team boundaries...");
            // Step 1: AI extracts file paths
            String filePathsList = domainOracle.analyzeOrganizationalImpact(state.rawGitDiff());
            
            // Step 2: Java logic processes paths
            String[] filePaths = filePathsList.split("\n");
            Set<String> domains = new HashSet<>();
            for (String path : filePaths) {
                if (path != null && !path.trim().isEmpty()) {
                    domains.add(getTeamOwnershipForPath(path.trim()));
                }
            }

            // Step 3: Check for cross-domain coupling and build log
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("File-to-Domain Mapping:\n");
            for (String path : filePaths) {
                if (path != null && !path.trim().isEmpty()) {
                    logBuilder.append(String.format("- %s -> %s\n", path.trim(), getTeamOwnershipForPath(path.trim())));
                }
            }

            boolean crossDomain = domains.size() > 1;
            if (crossDomain) {
                logBuilder.append("\nCRITICAL_VIOLATION_FOUND: Cross-domain coupling detected between: ").append(domains);
            } else {
                logBuilder.append("\nNo cross-domain coupling detected. All changes within domain: ").append(domains);
            }

            String log = logBuilder.toString();
            return checkForViolations("domainContext", log, "Domain Violation");
        }

        private Map<String, Object> runSecurity(ReviewState state) {
            System.out.println("[NODE 3] Security Guard hunting vulnerabilities...");
            String log = security.auditSecurity(state.rawGitDiff());
            return checkForViolations("vulnerabilityLog", log, "Security Vulnerability");
        }

        private Map<String, Object> runPerformance(ReviewState state) {
            System.out.println("[NODE 4] Performance Critic checking memory patterns...");
            String log = performance.auditPerformance(state.rawGitDiff());
            return checkForViolations("performanceLog", log, "Performance Issue");
        }

        private Map<String, Object> runBA(ReviewState state) {
            System.out.println("[NODE 5] BA Agent verifying Acceptance Criteria...");
            String prompt = "Criteria: " + state.acceptanceCriteria() + "\nDiff: " + state.rawGitDiff();
            String log = ba.auditBusinessLogic(prompt);
            return checkForViolations("businessAnalysisLog", log, "Business Logic Failure");
        }

        private Map<String, Object> runTech(ReviewState state) {
            System.out.println("[NODE 6] Tech Reviewer enforcing structural mandates...");
            String log = tech.auditTechCompliance(state.rawGitDiff());
            return checkForViolations("techReviewLog", log, "Tech Mandate Violation");
        }

        private Map<String, Object> runObservability(ReviewState state) {
            System.out.println("[NODE 7] Observability tracking logs and metrics...");
            String log = observability.auditObservability(state.rawGitDiff());
            return checkForViolations("observabilityLog", log, "Observability Gap");
        }

        private Map<String, Object> runDoneDef(ReviewState state) {
            System.out.println("[NODE 8] DoneDef Manager synthesizing final decision...");
            String combined = String.format(
                "Domain: %s\nSecurity: %s\nPerf: %s\nBA: %s\nTech: %s\nObs: %s",
                state.domainContext(), state.vulnerabilityLog(), state.performanceLog(),
                state.businessAnalysisLog(), state.techReviewLog(), state.observabilityLog()
            );
            
            String finalReport = doneDef.generateFinalReport(combined);
            boolean isApproved = state.violationRegistry().isEmpty();
            
            return Map.of("finalMergedReport", finalReport, "isApproved", isApproved);
        }

        private String evaluateFinalGate(ReviewState state) {
            return state.isApproved() ? "merge_approved" : "merge_rejected";
        }

        // Helper to DRY up violation checking
        private Map<String, Object> checkForViolations(String channelName, String logResult, String violationName) {
            if (logResult.contains("CRITICAL_VIOLATION_FOUND")) {
                // If a violation is found, update the channel AND append to the registry
                return Map.of(channelName, logResult, "violationRegistry", List.of(violationName));
            }
            return Map.of(channelName, logResult);
        }
        
        private String getTeamOwnershipForPath(String filePath) {
            if (filePath.contains("/billing/")) return "Billing";
            if (filePath.contains("/inventory/")) return "Inventory";
            return "Shared Services";
        }
    }
}
