Enforce these exact mandates on the Git Diff:

1.  **[NO_INJECT_OUTSIDE_CONTROLLER]**: No Java class may use the Quarkus `@Inject` annotation unless it is also annotated with `@Path`. All dependencies must be managed through constructor injection in other class types.
2.  **[PCI_PAN_MASKING]**: All logging statements that handle customer payment information (e.g., Primary Account Numbers or PANs) MUST mask the sensitive data. Raw, unmasked logging of PANs is a critical PCI DSS breach.
3.  **[NO_RUNTIME_EXCEPTION]**: Code must never throw a raw `java.lang.RuntimeException`. All exceptions should be specific, checked exceptions, or custom, unchecked exceptions with clear, descriptive names.

If ANY of these mandates are broken, YOU MUST output 'CRITICAL_VIOLATION_FOUND' in your report.