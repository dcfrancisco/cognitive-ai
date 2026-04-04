package ph.francisco.agents;

import ph.francisco.perception.Observation;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class IntentRouter {

    public CognitiveIntent route(Observation observation) {
        String content = observation.content() == null ? "" : observation.content().trim();
        String lower = content.toLowerCase(Locale.ROOT);

        if (Boolean.TRUE.equals(observation.explicitRemember())) {
            return CognitiveIntent.MEMORY_CAPTURE;
        }

        if (lower.contains("remember this")
                || lower.contains("please remember")
                || lower.contains("note this")) {
            return CognitiveIntent.MEMORY_CAPTURE;
        }

        if (lower.contains("what do you remember")
                || lower.contains("what do i prefer")
                || lower.contains("remind me")
                || lower.contains("what did i say")
                || lower.contains("what do you know")
                || lower.contains("what do you know from")) {
            return CognitiveIntent.MEMORY_RECALL;
        }

        if (content.endsWith("?")
                || lower.startsWith("what ")
                || lower.startsWith("why ")
                || lower.startsWith("how ")
                || lower.startsWith("should ")) {
            return CognitiveIntent.REFLECTION;
        }

        return CognitiveIntent.GENERAL_RESPONSE;
    }
}
