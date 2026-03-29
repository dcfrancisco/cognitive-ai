package ph.francisco.cognition;

import ph.francisco.perception.Observation;

import java.util.ArrayList;

/**
 * Temporary policy that forces the system to speak for all observations.
 *
 * NOTE: This implementation is intentionally NOT registered as a Spring bean
 * by default to avoid overriding the normal `RuleBasedShouldSpeakPolicy`.
 * Use it only for short-term manual testing by registering it as a bean.
 * TODO: re-enable intelligent filtering if this file is registered again.
 */
public class ForcedShouldSpeakPolicy implements ShouldSpeakPolicy {

    @Override
    public CognitionDecision decide(Observation observation) {
        var reasons = new ArrayList<String>();
        reasons.add("Forced policy: always speak (TODO: re-enable intelligent filtering)");
        return new CognitionDecision(CognitionDecision.DecisionType.SPEAK, 0.99, reasons);
    }
}
