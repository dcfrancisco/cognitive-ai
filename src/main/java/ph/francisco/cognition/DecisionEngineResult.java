package ph.francisco.cognition;

import ph.francisco.agents.CognitiveIntent;

import java.util.List;

public record DecisionEngineResult(
        CognitionDecision cognitionDecision,
        CognitiveIntent intent,
        List<String> routingReasons
) {
}
