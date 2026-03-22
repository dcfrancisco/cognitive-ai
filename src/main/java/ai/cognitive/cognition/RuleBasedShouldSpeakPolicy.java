package ai.cognitive.cognition;

import ai.cognitive.perception.Observation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Locale;

@Component
public class RuleBasedShouldSpeakPolicy implements ShouldSpeakPolicy {

    @Override
    public CognitionDecision decide(Observation observation) {
        var reasons = new ArrayList<String>();
        var content = observation.content() == null ? "" : observation.content().trim();
        var lower = content.toLowerCase(Locale.ROOT);

        // Explicit "remember this" implies user intent; we may acknowledge lightly.
        if (Boolean.TRUE.equals(observation.explicitRemember())) {
            reasons.add("Explicit remember request present");
            return new CognitionDecision(CognitionDecision.DecisionType.SPEAK, 0.9, reasons);
        }

        // If directly asked a question, we can respond.
        if (content.endsWith("?") || lower.startsWith("can you") || lower.startsWith("could you")
                || lower.startsWith("what ") || lower.startsWith("why ")) {
            reasons.add("Direct question detected");
            return new CognitionDecision(CognitionDecision.DecisionType.SPEAK, 0.75, reasons);
        }

        // If user explicitly addresses the system by name/role (basic heuristic).
        if (lower.contains("companion") || lower.contains("assistant") || lower.matches(".*\\bai\\b.*")) {
            reasons.add("Explicit address detected");
            return new CognitionDecision(CognitionDecision.DecisionType.SPEAK, 0.65, reasons);
        }

        reasons.add("Defaulting to silence (no clear invitation to intervene)");
        return new CognitionDecision(CognitionDecision.DecisionType.SILENCE, 0.6, reasons);
    }
}
