package ai.cognitive.agents;

import ai.cognitive.perception.Observation;

public interface CognitiveAgent {
    boolean supports(CognitiveIntent intent);
    AgentResponse handle(Observation observation);
}
