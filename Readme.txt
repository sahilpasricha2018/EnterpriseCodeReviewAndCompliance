Enterprise AI Code Review Engine

This project utilizes Quarkus, LangChain4j, and LangGraph4j to build an enterprise-grade AI compliance gate. It evaluates Git pull requests against strict business and technical rules (including PCI compliance, architectural limits, and performance) before allowing a merge.

Prerequisites

Java 17+ and Maven installed.

Ollama installed and running on your local machine (e.g., M4 Mac).

Qwen 3.5 model downloaded (or your preferred local model).

Startup Sequence

Run the following commands in your terminal to boot the engine:

# 1. Start the local LLM engine in the background
ollama serve &

# 2. Ensure the reasoning model is available
ollama pull qwen3.5

# 3. Compile the Quarkus application and launch the graph
mvn clean compile quarkus:dev


How It Works

The Java code contains a mocked Git Pull Request payload that intentionally violates standard Tech Review Rules (it contains an unmasked credit card PAN and an illegal @Inject outside of a controller).

Watch the terminal as the agents asynchronously isolate the code, flag the violations, write to the LangGraph state channels, and explicitly REJECT the pipeline merge based on your corporate guidelines.