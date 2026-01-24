# Architecture Overview

This document describes the architecture of the cognitive AI system, which leverages Spring Boot, Spring AI, and Ollama. 

## Key Components:
- **Cognition Gate**: Acts as the interface between different layers.
- **Memory Layers**: 
  - **Working Memory**: Temporary storage for immediate tasks.
  - **Episodic Memory**: Storing personal experiences.
  - **Semantic Memory**: Storing general knowledge and facts.
  - **Values**: Storing prioritized values to guide decisions.