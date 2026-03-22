package ai.cognitive.agents;

import ai.cognitive.perception.Observation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemoryCaptureAgent implements CognitiveAgent {

    @Override
    public boolean supports(CognitiveIntent intent) {
        return intent == CognitiveIntent.MEMORY_CAPTURE;
    }

    @Override
    public AgentResponse handle(Observation observation) {
        return new AgentResponse(
                "MemoryCaptureAgent",
                "I’ll treat that as something worth remembering and reviewing.",
                List.of(
                        "Observation was routed to memory capture",
                        "This supports curated memory rather than raw logging"
                )
        );
    }
}
