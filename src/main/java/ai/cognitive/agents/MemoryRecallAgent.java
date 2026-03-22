package ai.cognitive.agents;

import ai.cognitive.memory.CuratedMemoryService;
import ai.cognitive.perception.Observation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MemoryRecallAgent implements CognitiveAgent {

    private final CuratedMemoryService curatedMemoryService;

    public MemoryRecallAgent(CuratedMemoryService curatedMemoryService) {
        this.curatedMemoryService = curatedMemoryService;
    }

    @Override
    public boolean supports(CognitiveIntent intent) {
        return intent == CognitiveIntent.MEMORY_RECALL;
    }

    @Override
    public AgentResponse handle(Observation observation) {
        var working = curatedMemoryService.workingSnapshot();

        if (working.isEmpty()) {
            return new AgentResponse(
                    "MemoryRecallAgent",
                    "I don’t have enough recent working memory to answer that yet.",
                    List.of("Working memory snapshot was empty")
            );
        }

        String recent = working.stream()
                .skip(Math.max(0, working.size() - 3))
                .map(item -> "- " + item.content())
                .collect(Collectors.joining("\n"));

        return new AgentResponse(
                "MemoryRecallAgent",
                "From recent working memory, here’s what seems relevant:\n" + recent,
                List.of(
                        "User asked for recall",
                        "Response was grounded in recent working memory"
                )
        );
    }
}
