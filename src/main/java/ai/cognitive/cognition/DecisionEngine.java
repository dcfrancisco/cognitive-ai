package ai.cognitive.cognition;

import ai.cognitive.agents.IntentRouter;
import ai.cognitive.perception.Observation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class DecisionEngine {

    private final ShouldSpeakPolicy shouldSpeakPolicy;
    private final IntentRouter intentRouter;

    public DecisionEngine(ShouldSpeakPolicy shouldSpeakPolicy, IntentRouter intentRouter) {
        this.shouldSpeakPolicy = shouldSpeakPolicy;
        this.intentRouter = intentRouter;
    }

    public DecisionEngineResult evaluate(Observation observation) {
        CognitionDecision cognitionDecision = shouldSpeakPolicy.decide(observation);

        var routingReasons = new ArrayList<String>();
        var intent = intentRouter.route(observation);
        routingReasons.add("Intent routed to " + intent.name());

        return new DecisionEngineResult(cognitionDecision, intent, routingReasons);
    }
}
