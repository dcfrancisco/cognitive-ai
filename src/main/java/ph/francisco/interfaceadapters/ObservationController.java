package ph.francisco.interfaceadapters;

import ph.francisco.agents.AgentOrchestrator;
import ph.francisco.agents.AgentResponse;
import ph.francisco.cognition.CognitionDecision;
import ph.francisco.cognition.DecisionEngine;
import ph.francisco.cognition.DecisionEngineResult;
import ph.francisco.memory.CuratedMemoryService;
import ph.francisco.memory.ConversationMemoryService;
import ph.francisco.perception.Observation;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ObservationController {
    private final CuratedMemoryService curatedMemoryService;
    private final ConversationMemoryService conversationMemoryService;
    private final DecisionEngine decisionEngine;
    private final AgentOrchestrator agentOrchestrator;

    public ObservationController(
            CuratedMemoryService curatedMemoryService,
            ConversationMemoryService conversationMemoryService,
            DecisionEngine decisionEngine,
            AgentOrchestrator agentOrchestrator) {
        this.curatedMemoryService = curatedMemoryService;
        this.conversationMemoryService = conversationMemoryService;
        this.decisionEngine = decisionEngine;
        this.agentOrchestrator = agentOrchestrator;
    }

    @PostMapping("/observe")
    public ResponseEntity<?> observe(@Valid @RequestBody Observation observation) {
        // Ensure a sessionId exists for multi-turn/demo scenarios
        String sessionId = observation.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
            observation = new Observation(observation.source(), observation.content(), observation.explicitRemember(),
                    sessionId);
        }

        curatedMemoryService.observe(observation);

        DecisionEngineResult result = decisionEngine.evaluate(observation);
        CognitionDecision decision = result.cognitionDecision();

        if (decision.type() == CognitionDecision.DecisionType.SILENCE) {
            return ResponseEntity.noContent().build();
        }

        AgentResponse agentResponse = agentOrchestrator.handle(result.intent(), observation);

        // persist conversational turn for demo/inspection
        try {
            conversationMemoryService.store(observation, agentResponse);
        } catch (Exception e) {
            // do not fail the REST call if memory persistence has an issue
            System.err.println("Failed to store conversation turn: " + e.getMessage());
        }

        var reasons = new ArrayList<String>();
        reasons.addAll(decision.reasons());
        reasons.addAll(result.routingReasons());
        reasons.addAll(agentResponse.reasons());

        var resp = Map.of(
                "decision", decision.type().name(),
                "confidence", decision.confidence(),
                "intent", result.intent().name(),
                "agent", agentResponse.agent(),
                "message", agentResponse.message(),
                "reasons", reasons,
                "sessionId", sessionId);

        return ResponseEntity.ok(resp);
    }
}
