package ai.cognitive.cognition;

import ai.cognitive.agents.CognitiveIntent;

import java.util.List;

public record DecisionEngineResult(
        CognitionDecision cognitionDecision,
        CognitiveIntent intent,
        List<String> routingReasons
) {
}
