package ph.francisco.agents;

import ph.francisco.perception.Observation;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class IntentRouter {

    private final SpringAiResponseService springAiResponseService;

    public IntentRouter(SpringAiResponseService springAiResponseService) {
        this.springAiResponseService = springAiResponseService;
    }

    public CognitiveIntent route(Observation observation) {
        String content = observation.content() == null ? "" : observation.content().trim();
        String lower = content.toLowerCase(Locale.ROOT);

        // Explicit flag always wins — no LLM call needed
        if (Boolean.TRUE.equals(observation.explicitRemember())) {
            return CognitiveIntent.MEMORY_CAPTURE;
        }

        // Try LLM classification first
        return springAiResponseService.classifyIntent(content)
                .orElseGet(() -> keywordFallback(lower, content));
    }

    /**
     * Keyword-based fallback used when no API key is configured or LLM is
     * unavailable.
     */
    private CognitiveIntent keywordFallback(String lower, String content) {
        if (lower.contains("remember this")
                || lower.contains("please remember")
                || lower.contains("note this")) {
            return CognitiveIntent.MEMORY_CAPTURE;
        }

        if (lower.contains("what do you remember")
                || lower.contains("what do i prefer")
                || lower.contains("remind me")
                || lower.contains("what did i say")
                || lower.contains("what do you know")
                || lower.contains("what do you know from")) {
            return CognitiveIntent.MEMORY_RECALL;
        }

        if (content.endsWith("?")
                || lower.startsWith("what ")
                || lower.startsWith("why ")
                || lower.startsWith("how ")
                || lower.startsWith("should ")) {
            return CognitiveIntent.REFLECTION;
        }

        return CognitiveIntent.GENERAL_RESPONSE;
    }
}
