package ai.cognitive.interfaceadapters;

import ai.cognitive.agents.AgentOrchestrator;
import ai.cognitive.agents.AgentResponse;
import ai.cognitive.cognition.CognitionDecision;
import ai.cognitive.cognition.DecisionEngine;
import ai.cognitive.cognition.DecisionEngineResult;
import ai.cognitive.memory.CuratedMemoryService;
import ai.cognitive.perception.Observation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ObservationController {
    private final CuratedMemoryService curatedMemoryService;
    private final DecisionEngine decisionEngine;
    private final AgentOrchestrator agentOrchestrator;

    public ObservationController(
            CuratedMemoryService curatedMemoryService,
            DecisionEngine decisionEngine,
            AgentOrchestrator agentOrchestrator) {
        this.curatedMemoryService = curatedMemoryService;
        this.decisionEngine = decisionEngine;
        this.agentOrchestrator = agentOrchestrator;
    }

    @PostMapping("/observe")
    public ResponseEntity<?> observe(@Valid @RequestBody Observation observation) {
        curatedMemoryService.observe(observation);

        DecisionEngineResult result = decisionEngine.evaluate(observation);
        CognitionDecision decision = result.cognitionDecision();

        if (decision.type() == CognitionDecision.DecisionType.SILENCE) {
            return ResponseEntity.noContent().build();
        }

        AgentResponse agentResponse = agentOrchestrator.handle(result.intent(), observation);

        var reasons = new ArrayList<String>();
        reasons.addAll(decision.reasons());
        reasons.addAll(result.routingReasons());
        reasons.addAll(agentResponse.reasons());

        return ResponseEntity.ok(Map.of(
                "decision", decision.type().name(),
                "confidence", decision.confidence(),
                "intent", result.intent().name(),
                "agent", agentResponse.agent(),
                "message", agentResponse.message(),
                "reasons", reasons
        ));
    }
}
