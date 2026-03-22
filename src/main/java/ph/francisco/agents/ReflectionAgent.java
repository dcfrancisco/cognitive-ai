package ph.francisco.agents;

import ph.francisco.perception.Observation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReflectionAgent implements CognitiveAgent {

    @Override
    public boolean supports(CognitiveIntent intent) {
        return intent == CognitiveIntent.REFLECTION
                || intent == CognitiveIntent.GENERAL_RESPONSE;
    }

    @Override
    public AgentResponse handle(Observation observation) {
        String content = observation.content() == null ? "" : observation.content().trim();

        String message;
        if (content.endsWith("?")) {
            message = "I believe you’re asking for a response rather than silent observation.";
        } else {
            message = "I noticed this may benefit from a light reflective response rather than silence.";
        }

        return new AgentResponse(
                "ReflectionAgent",
                message,
                List.of(
                        "Observation was routed to reflective/general response",
                        "System chose a restrained partner-style reply"
                )
        );
    }
}
