package ph.francisco.cognition;

import ph.francisco.perception.Observation;

import java.util.ArrayList;

/**
 * Temporary policy that forces the system to speak for all observations.
 *
 * This class is intentionally NOT registered as a Spring bean by default so
 * the `RuleBasedShouldSpeakPolicy` is used in normal operation. Re-enable
 * only for short-term manual testing.
 */
public class ForcedShouldSpeakPolicy implements ShouldSpeakPolicy {

    @Override
    public CognitionDecision decide(Observation observation) {
        var reasons = new ArrayList<String>();
        reasons.add("Forced policy: always speak (TODO: re-enable intelligent filtering)");
        return new CognitionDecision(CognitionDecision.DecisionType.SPEAK, 0.99, reasons);
    }
}
