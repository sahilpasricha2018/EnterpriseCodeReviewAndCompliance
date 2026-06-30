package com.enterprise.review;

import com.enterprise.review.agent.*;
import dev.langchain4j.agent.tool.Tool;
import io.quarkiverse.langchain4j.RegisterAiService;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            
            // --- NEW: Splitting the Tech Review into isolated tracks ---
            Map.entry("pciComplianceLog", Channels.base(() -> "")),
            Map.entry("diComplianceLog", Channels.base(() -> "")),
            Map.entry("exceptionLog", Channels.base(() -> "")),
            Map.entry("stylingLog", Channels.base(() -> "")),
            
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
        
        // --- NEW: Type-Safe Accessors for Tech Sub-Agents ---
        public String pciComplianceLog() { return this.<String>value("pciComplianceLog").orElse(""); }
        public String diComplianceLog() { return this.<String>value("diComplianceLog").orElse(""); }
        public String exceptionLog() { return this.<String>value("exceptionLog").orElse(""); }
        public String stylingLog() { return this.<String>value("stylingLog").orElse(""); }
        
        public String observabilityLog() { return this.<String>value("observabilityLog").orElse(""); }
        
        public List<String> violationRegistry() { return this.<List<String>>value("violationRegistry").orElse(new ArrayList<>()); }
        public boolean isApproved() { return this.<Boolean>value("isApproved").orElse(false); }
    }

    // ============================================================================
    // 1.5 STRUCTURED OUTPUT POJOS
    // ============================================================================
    public static class TechRuleReport {
        public boolean violationFound;
        public String agentReasoning;
    }

    // ============================================================================
    // 2. DOMAIN ORACLE & TOOLS
    // ============================================================================
    @ApplicationScoped
    public static class OrganizationLookupTools {
        @Tool("Look up the team that owns a specific file path or module.")
        public String getTeamOwnershipForPath(String filePath) {
            if (filePath.contains("/billing/")) return "TEAM: Finance | DOMAIN: Billing";
            if (filePath.contains("/inventory/")) return "TEAM: Supply-Chain | DOMAIN: Inventory";
            return "TEAM: Platform | DOMAIN: Shared Services";
        }
    }


    // ============================================================================
    // 3. GRAPH ENGINE CONTROLLER
    // ============================================================================
    @ApplicationScoped
    public static class EnterprisePipelineRunner implements QuarkusApplication {

        @Inject GatekeeperAgent gatekeeper;
        @Inject DomainContextOracle domainOracle;
        @Inject SecurityGuardAgent security;
        @Inject PerformanceCriticAgent performance;
        @Inject BusinessAnalysisAgent ba;
        
        // --- NEW: Injecting the specialized Tech agents ---
        @Inject PciComplianceAgent pciAgent;
        @Inject ExceptionHandlingAgent exceptionAgent;
        @Inject StylingAgent stylingAgent;
        // --------------------------------------------------
        
        @Inject ObservabilityAgent observability;
        @Inject DoneDefAgent doneDef;

        @Override
        public int run(String... args) throws Exception {
            System.out.println("\n=== BOOTING 11-NODE COMPLIANCE PIPELINE ===");

            StateGraph<ReviewState> workflow = new StateGraph<>(ReviewState.SCHEMA, ReviewState::new)
                .addNode("n_gatekeeper", node_async(this::runGatekeeper))
                .addNode("n_domain", node_async(this::runDomain))
                .addNode("n_security", node_async(this::runSecurity))
                .addNode("n_performance", node_async(this::runPerformance))
                .addNode("n_ba", node_async(this::runBA))
                
                // --- NEW: Isolated Tech Nodes ---
                .addNode("n_pci", node_async(this::runPciCheck))
                .addNode("n_exceptions", node_async(this::runExceptionCheck))
                .addNode("n_styling", node_async(this::runStylingCheck))
                // --------------------------------
                
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
                
                // --- NEW: Wire the Tech agents sequentially ---
                .addEdge("n_ba", "n_pci")
                .addEdge("n_pci", "n_di")
                .addEdge("n_di", "n_exceptions")
                .addEdge("n_exceptions", "n_styling")
                .addEdge("n_styling", "n_observability")
                // ----------------------------------------------
                
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
                System.out.println("Failed Policies: " + finalState.violationRegistry());
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
            String log = domainOracle.analyzeOrganizationalImpact(state.rawGitDiff());
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

        // --- NEW: Sub-Node Executions for Tech Rules ---
        
        private Map<String, Object> runPciCheck(ReviewState state) {
            System.out.println("[NODE 6.1] PCI Agent scanning for unmasked PANs...");
            TechRuleReport report = pciAgent.auditPci(state.rawGitDiff());
            List<String> violations = new ArrayList<>();
            if (report.violationFound) violations.add("Tech Mandate: PCI PAN Masking Breach");
            return Map.of("pciComplianceLog", report.agentReasoning, "violationRegistry", violations);
        }

        private Map<String, Object> runExceptionCheck(ReviewState state) {
            System.out.println("[NODE 6.2] Exception Agent tracking stack trace leaks...");
            TechRuleReport report = exceptionAgent.auditExceptions(state.rawGitDiff());
            List<String> violations = new ArrayList<>();
            if (report.violationFound) violations.add("Tech Mandate: Illegal Exception Handling");
            return Map.of("exceptionLog", report.agentReasoning, "violationRegistry", violations);
        }

        private Map<String, Object> runStylingCheck(ReviewState state) {
            System.out.println("[NODE 6.3] Styling Agent reviewing code conventions...");
            TechRuleReport report = stylingAgent.auditStyling(state.rawGitDiff());
            List<String> violations = new ArrayList<>();
            if (report.violationFound) violations.add("Tech Mandate: Critical Formatting Failure");
            return Map.of("stylingLog", report.agentReasoning, "violationRegistry", violations);
        }
        // -----------------------------------------------

        private Map<String, Object> runObservability(ReviewState state) {
            System.out.println("[NODE 7] Observability tracking logs and metrics...");
            String log = observability.auditObservability(state.rawGitDiff());
            return checkForViolations("observabilityLog", log, "Observability Gap");
        }

        private Map<String, Object> runDoneDef(ReviewState state) {
            System.out.println("[NODE 8] DoneDef Manager synthesizing final decision...");
            
            // --- NEW: Combine all the specialized tech logs into the final payload ---
            String combined = String.format(
                "Domain: %s\nSecurity: %s\nPerf: %s\nBA: %s\nPCI: %s\nDI: %s\nExceptions: %s\nStyling: %s\nObs: %s",
                state.domainContext(), state.vulnerabilityLog(), state.performanceLog(),
                state.businessAnalysisLog(), state.pciComplianceLog(), state.diComplianceLog(),
                state.exceptionLog(), state.stylingLog(), state.observabilityLog()
            );
            
            String finalReport = doneDef.generateFinalReport(combined);
            boolean isApproved = state.violationRegistry().isEmpty();
            
            return Map.of("finalMergedReport", finalReport, "isApproved", isApproved);
        }

        private String evaluateFinalGate(ReviewState state) {
            return state.isApproved() ? "merge_approved" : "merge_rejected";
        }

        // Helper to DRY up violation checking for simple string logs
        private Map<String, Object> checkForViolations(String channelName, String logResult, String violationName) {
            if (logResult.contains("CRITICAL_VIOLATION_FOUND")) {
                // If a violation is found, update the channel AND append to the registry
                return Map.of(channelName, logResult, "violationRegistry", List.of(violationName));
            }
            return Map.of(channelName, logResult);
        }
    }
}