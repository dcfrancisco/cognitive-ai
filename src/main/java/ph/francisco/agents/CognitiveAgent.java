package ph.francisco.agents;

import ph.francisco.perception.Observation;

public interface CognitiveAgent {
    boolean supports(CognitiveIntent intent);
    AgentResponse handle(Observation observation);
}
