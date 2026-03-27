package ph.francisco.agents;

import ph.francisco.memory.CuratedMemoryService;
import ph.francisco.perception.Observation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MemoryRecallAgent implements CognitiveAgent {

    private static final String AGENT_NAME = "MemoryRecallAgent";

    private final CuratedMemoryService curatedMemoryService;
    private final SpringAiResponseService springAiResponseService;

    public MemoryRecallAgent(CuratedMemoryService curatedMemoryService,
            SpringAiResponseService springAiResponseService) {
        this.curatedMemoryService = curatedMemoryService;
        this.springAiResponseService = springAiResponseService;
    }

    @Override
    public boolean supports(CognitiveIntent intent) {
        return intent == CognitiveIntent.MEMORY_RECALL;
    }

    @Override
    public AgentResponse handle(Observation observation) {
        var working = curatedMemoryService.workingSnapshot(observation.sessionId());

        if (working.isEmpty()) {
            return new AgentResponse(
                    AGENT_NAME,
                    "I don’t have enough recent working memory to answer that yet.",
                    List.of("Working memory snapshot was empty"));
        }

        var aiResponse = springAiResponseService.generateRecallResponse(observation, working);
        if (aiResponse.isPresent()) {
            return new AgentResponse(
                    AGENT_NAME,
                    aiResponse.get(),
                    List.of(
                            "User asked for recall",
                            "Spring AI produced a response",
                            "Response was grounded in recent working memory"));
        }

        String recent = working.stream()
                .skip(Math.max(0, working.size() - 3))
                .map(item -> "- " + item.content())
                .collect(Collectors.joining("\n"));

        return new AgentResponse(
                AGENT_NAME,
                "From recent working memory, here’s what seems relevant:\n" + recent,
                List.of(
                        "User asked for recall",
                        "Response was grounded in recent working memory"));
    }
}
