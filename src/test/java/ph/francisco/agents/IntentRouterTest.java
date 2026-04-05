package ph.francisco.agents;

import ph.francisco.perception.Observation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntentRouterTest {

    // Use a mock that returns empty so the keyword fallback is exercised
    private final SpringAiResponseService aiService = mock(SpringAiResponseService.class);
    private final IntentRouter router = new IntentRouter(aiService);

    {
        when(aiService.classifyIntent(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void routesExplicitRememberToMemoryCapture() {
        var intent = router.route(new Observation("test", "I prefer tea in the morning", true, null));
        assertThat(intent).isEqualTo(CognitiveIntent.MEMORY_CAPTURE);
    }

    @Test
    void routesRecallQuestionToMemoryRecall() {
        var intent = router.route(new Observation("test", "What do you remember about me?", null, null));
        assertThat(intent).isEqualTo(CognitiveIntent.MEMORY_RECALL);
    }

    @Test
    void routesQuestionToReflection() {
        var intent = router.route(new Observation("test", "Why am I always tired?", null, null));
        assertThat(intent).isEqualTo(CognitiveIntent.REFLECTION);
    }
}
