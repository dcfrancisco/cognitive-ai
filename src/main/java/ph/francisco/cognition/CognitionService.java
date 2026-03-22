package ph.francisco.cognition;

import ph.francisco.perception.Observation;
import org.springframework.stereotype.Service;

@Service
public class CognitionService {
	private final ShouldSpeakPolicy shouldSpeakPolicy;

	public CognitionService(ShouldSpeakPolicy shouldSpeakPolicy) {
		this.shouldSpeakPolicy = shouldSpeakPolicy;
	}

	public CognitionDecision evaluate(Observation observation) {
		return shouldSpeakPolicy.decide(observation);
	}
}
