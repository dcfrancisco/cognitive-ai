package ph.francisco.agents;

import ph.francisco.perception.Observation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntentRouterTest {

    private final IntentRouter router = new IntentRouter();

    @Test
    void routesExplicitRememberToMemoryCapture() {
        var intent = router.route(new Observation("test", "I prefer tea in the morning", true));
        assertThat(intent).isEqualTo(CognitiveIntent.MEMORY_CAPTURE);
    }

    @Test
    void routesRecallQuestionToMemoryRecall() {
        var intent = router.route(new Observation("test", "What do you remember about me?", null));
        assertThat(intent).isEqualTo(CognitiveIntent.MEMORY_RECALL);
    }

    @Test
    void routesQuestionToReflection() {
        var intent = router.route(new Observation("test", "Why am I always tired?", null));
        assertThat(intent).isEqualTo(CognitiveIntent.REFLECTION);
    }
}
