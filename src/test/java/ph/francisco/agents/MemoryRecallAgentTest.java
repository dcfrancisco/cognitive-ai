package ph.francisco.agents;

import ph.francisco.memory.CuratedMemoryService;
import ph.francisco.memory.EpisodicMemory;
import ph.francisco.memory.MemoryRetrievalService;
import ph.francisco.memory.WorkingMemory;
import ph.francisco.perception.Observation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoryRecallAgentTest {

    @Mock
    private CuratedMemoryService curatedMemoryService;
    @Mock
    private MemoryRetrievalService memoryRetrievalService;
    @Mock
    private SpringAiResponseService springAiResponseService;

    private MemoryRecallAgent agent;

    @BeforeEach
    void setUp() {
        agent = new MemoryRecallAgent(curatedMemoryService, memoryRetrievalService, springAiResponseService);
        // Default: no AI response so we can assert on the fallback text
        // lenient() because fallback test never invokes springAiResponseService
        lenient().when(springAiResponseService.generateRecallResponse(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void handle_combinesEpisodicAndWorkingMemory() {
        var obs = new Observation("console", "what do you remember?", null, "s1");
        when(curatedMemoryService.workingSnapshot("s1")).thenReturn(List.of(
                new WorkingMemory.Item(Instant.now(), "console", "I like evening walks")));
        when(memoryRetrievalService.findRelevant(eq("what do you remember?"), anyInt())).thenReturn(List.of(
                episodic("prefers no meetings before 9am")));

        var response = agent.handle(obs);

        assertThat(response.message()).contains("Recent:");
        assertThat(response.message()).contains("From what I remember:");
        assertThat(response.reasons()).anyMatch(r -> r.contains("working memory"));
        assertThat(response.reasons()).anyMatch(r -> r.contains("Episodic memory recalled"));
    }

    @Test
    void handle_usesEpisodicWhenWorkingIsEmpty() {
        var obs = new Observation("console", "what do you know about me?", null, "s2");
        when(curatedMemoryService.workingSnapshot("s2")).thenReturn(List.of());
        when(memoryRetrievalService.findRelevant(eq("what do you know about me?"), anyInt())).thenReturn(List.of(
                episodic("user mentioned they work remotely")));

        var response = agent.handle(obs);

        assertThat(response.message()).contains("From what I remember:");
        assertThat(response.message()).doesNotContain("Recent:");
        assertThat(response.message()).doesNotContain("I do not have any memory");
        assertThat(response.reasons()).anyMatch(r -> r.contains("Episodic memory recalled"));
    }

    @Test
    void handle_fallbackWhenBothEmpty() {
        var obs = new Observation("console", "tell me something", null, "s3");
        when(curatedMemoryService.workingSnapshot("s3")).thenReturn(List.of());
        when(memoryRetrievalService.findRelevant(anyString(), anyInt())).thenReturn(List.of());

        var response = agent.handle(obs);

        assertThat(response.message()).contains("I do not have any memory relevant to that yet.");
        assertThat(response.reasons()).contains("Working memory empty", "No episodic memory found");
    }

    private static EpisodicMemory episodic(String summary) {
        return new EpisodicMemory(UUID.randomUUID(), summary, "INTERACTION", 1,
                "system", Instant.now(), null, 0);
    }
}
