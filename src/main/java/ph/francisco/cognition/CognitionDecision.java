package ph.francisco.cognition;

import java.util.List;

public record CognitionDecision(
		DecisionType type,
		double confidence,
		List<String> reasons
) {
	public enum DecisionType {
		SPEAK,
		SILENCE
	}
}
