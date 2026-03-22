package ph.francisco.values;

import java.util.List;

/**
 * Explicit values & boundaries.
 * These are configured by humans and are NOT learned or embedded.
 */
public final class ValuesAndBoundaries {
	private ValuesAndBoundaries() {
	}

	public static List<String> rules() {
		return List.of(
				"Cognition before interaction: decide whether to speak; silence is valid.",
				"Memory is curated, not logged: store meaning only; never store raw transcripts by default.",
				"Non-hierarchical: partner stance; no commands, no judgments, defer to human agency.",
				"Explainability and restraint: if unsure, default to silence; if cannot explain why, do not remember."
		);
	}
}
