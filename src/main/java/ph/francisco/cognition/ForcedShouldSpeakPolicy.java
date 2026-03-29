package ph.francisco.cognition;

import ph.francisco.perception.Observation;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Temporary policy that forces the system to speak for all observations.
 *
 * NOTE: This is intentionally registered as the primary Spring bean while
 * working interactively. TODO: re-enable intelligent filtering and remove
 * 
 * @Primary before committing to long-running environments.
 */
@Component
@Primary
public class ForcedShouldSpeakPolicy implements ShouldSpeakPolicy {

    @Override
    public CognitionDecision decide(Observation observation) {
        var reasons = new ArrayList<String>();
        reasons.add("Forced policy: always speak (TODO: re-enable intelligent filtering)");
        return new CognitionDecision(CognitionDecision.DecisionType.SPEAK, 0.99, reasons);
    }
}
