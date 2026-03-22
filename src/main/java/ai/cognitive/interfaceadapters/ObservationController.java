package ai.cognitive.interfaceadapters;

import ai.cognitive.cognition.CognitionDecision;
import ai.cognitive.cognition.CognitionService;
import ai.cognitive.memory.CuratedMemoryService;
import ai.cognitive.perception.Observation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ObservationController {
    private final CuratedMemoryService curatedMemoryService;
    private final CognitionService cognitionService;

    public ObservationController(CuratedMemoryService curatedMemoryService, CognitionService cognitionService) {
        this.curatedMemoryService = curatedMemoryService;
        this.cognitionService = cognitionService;
    }

    @PostMapping("/observe")
    public ResponseEntity<?> observe(@Valid @RequestBody Observation observation) {
        curatedMemoryService.observe(observation);
        CognitionDecision decision = cognitionService.evaluate(observation);

        if (decision.type() == CognitionDecision.DecisionType.SILENCE) {
            return ResponseEntity.noContent().build();
        }

        // Minimal debug response: explain why it chose to speak.
        return ResponseEntity.ok(Map.of(
                "decision", decision.type().name(),
                "confidence", decision.confidence(),
                "reasons", decision.reasons()));
    }
}
