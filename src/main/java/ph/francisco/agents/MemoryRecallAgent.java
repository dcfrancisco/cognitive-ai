package ph.francisco.agents;

import ph.francisco.memory.CuratedMemoryService;
import ph.francisco.memory.EpisodicMemory;
import ph.francisco.memory.MemoryRetrievalService;
import ph.francisco.perception.Observation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MemoryRecallAgent implements CognitiveAgent {

    private static final Logger log = LoggerFactory.getLogger(MemoryRecallAgent.class);
    private static final String AGENT_NAME = "MemoryRecallAgent";

    private final CuratedMemoryService curatedMemoryService;
    private final MemoryRetrievalService memoryRetrievalService;
    private final SpringAiResponseService springAiResponseService;

    public MemoryRecallAgent(CuratedMemoryService curatedMemoryService,
                             MemoryRetrievalService memoryRetrievalService,
                             SpringAiResponseService springAiResponseService) {
        this.curatedMemoryService = curatedMemoryService;
        this.memoryRetrievalService = memoryRetrievalService;
        this.springAiResponseService = springAiResponseService;
    }

    @Override
    public boolean supports(CognitiveIntent intent) {
        return intent == CognitiveIntent.MEMORY_RECALL;
    }

    @Override
    public AgentResponse handle(Observation observation) {
        var working = curatedMemoryService.workingSnapshot(observation.sessionId());
        var episodic = memoryRetrievalService.findRelevant(observation.content(), 5);

        log.debug("MemoryRecallAgent working={} episodic={}", working.size(), episodic.size());

        // Case C - both empty
        if (working.isEmpty() && episodic.isEmpty()) {
            return new AgentResponse(
                    AGENT_NAME,
                    "I do not have any memory relevant to that yet.",
                    List.of("Working memory empty", "No episodic memory found"));
        }

        var reasons = new ArrayList<String>();
        var contextParts = new ArrayList<String>();

        // Working memory context
        if (working.isEmpty() == false) {
            String workingContext = working.stream()
                    .skip(Math.max(0, working.size() - 3))
                    .map(item -> "- " + item.content())
                    .collect(Collectors.joining("\n"));
            contextParts.add("Recent:\n" + workingContext);
            reasons.add("Response grounded in recent working memory");
        }

        // Episodic memory context
        if (episodic.isEmpty() == false) {
            String episodicContext = episodic.stream()
                    .map(EpisodicMemory::summary)
                    .map(s -> "- " + s)
                    .collect(Collectors.joining("\n"));
            contextParts.add("From what I remember:\n" + episodicContext);
            reasons.add("Episodic memory recalled (" + episodic.size() + " item(s))");
        }

        String combinedContext = String.join("\n\n", contextParts);

        // Try Spring AI with combined context
        var aiResponse = springAiResponseService.generateRecallResponse(observation, working);
        if (aiResponse.isPresent()) {
            reasons.add("Spring AI produced a response");
            return new AgentResponse(AGENT_NAME, aiResponse.get(), reasons);
        }

        // Fallback: plain-text combined response
        return new AgentResponse(AGENT_NAME, combinedContext, reasons);
    }
}
