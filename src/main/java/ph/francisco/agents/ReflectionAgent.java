package ph.francisco.agents;

import ph.francisco.perception.Observation;
import ph.francisco.memory.CuratedMemoryService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class ReflectionAgent implements CognitiveAgent {

    private final SpringAiResponseService springAiResponseService;
    private final CuratedMemoryService curatedMemoryService;

    public ReflectionAgent(SpringAiResponseService springAiResponseService, CuratedMemoryService curatedMemoryService) {
        this.springAiResponseService = springAiResponseService;
        this.curatedMemoryService = curatedMemoryService;
    }

    @Override
    public boolean supports(CognitiveIntent intent) {
        return intent == CognitiveIntent.REFLECTION
                || intent == CognitiveIntent.GENERAL_RESPONSE;
    }

    @Override
    public AgentResponse handle(Observation observation) {
        String content = observation.content() == null ? "" : observation.content().trim();

        var reasons = new ArrayList<String>();
        reasons.add("Observation was routed to reflective/general response");

        var working = curatedMemoryService.workingSnapshot(observation.sessionId());
        var aiResponse = springAiResponseService.generateReflection(observation, working);
        if (aiResponse.isPresent()) {
            reasons.add("Spring AI produced a response");
            reasons.add("System chose a restrained partner-style reply");
            return new AgentResponse("ReflectionAgent", aiResponse.get(), reasons);
        }

        String message;
        if (content.endsWith("?")) {
            message = "I believe you're asking for a response rather than silent observation.";
        } else {
            message = "I noticed this may benefit from a light reflective response rather than silence.";
        }

        reasons.add("Spring AI unavailable; using rule-based response");
        reasons.add("System chose a restrained partner-style reply");

        return new AgentResponse("ReflectionAgent", message, reasons);
    }
}
