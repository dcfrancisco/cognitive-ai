package ph.francisco.agents;

import ph.francisco.perception.Observation;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentOrchestrator {

    private final List<CognitiveAgent> agents;

    public AgentOrchestrator(List<CognitiveAgent> agents) {
        this.agents = agents;
    }

    public AgentResponse handle(CognitiveIntent intent, Observation observation) {
        return agents.stream()
                .filter(agent -> agent.supports(intent))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No agent found for intent: " + intent))
                .handle(observation);
    }
}
