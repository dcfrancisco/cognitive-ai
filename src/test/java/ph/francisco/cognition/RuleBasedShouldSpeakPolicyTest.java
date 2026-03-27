package ph.francisco.cognition;

import ph.francisco.perception.Observation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedShouldSpeakPolicyTest {

    private final RuleBasedShouldSpeakPolicy policy = new RuleBasedShouldSpeakPolicy();

    @Test
    void defaultsToSilenceWhenNoInvitation() {
        var decision = policy.decide(new Observation("test", "Just thinking out loud.", null, null));
        assertThat(decision.type()).isEqualTo(CognitionDecision.DecisionType.SILENCE);
        assertThat(decision.reasons()).isNotEmpty();
    }

    @Test
    void speaksOnExplicitRemember() {
        var decision = policy.decide(new Observation("test", "I prefer meetings after 10am", true, null));
        assertThat(decision.type()).isEqualTo(CognitionDecision.DecisionType.SPEAK);
        assertThat(decision.reasons()).anyMatch(r -> r.toLowerCase().contains("remember"));
    }

    @Test
    void speaksOnQuestion() {
        var decision = policy.decide(new Observation("test", "What time is it?", null, null));
        assertThat(decision.type()).isEqualTo(CognitionDecision.DecisionType.SPEAK);
        assertThat(decision.reasons()).anyMatch(r -> r.toLowerCase().contains("question"));
    }
}
