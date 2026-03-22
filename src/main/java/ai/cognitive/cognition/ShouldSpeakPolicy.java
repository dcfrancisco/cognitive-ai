package ai.cognitive.cognition;

import ai.cognitive.perception.Observation;

/**
 * Decides whether the system should speak at all.
 * Rule-based first; defaults to silence on uncertainty.
 */
public interface ShouldSpeakPolicy {
	CognitionDecision decide(Observation observation);
}
