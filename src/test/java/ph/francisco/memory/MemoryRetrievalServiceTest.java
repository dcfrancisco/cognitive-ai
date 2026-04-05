package ph.francisco.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryRetrievalServiceTest {

    @Mock
    private EpisodicMemoryRepository repository;

    private MemoryRetrievalService service;

    @BeforeEach
    void setUp() {
        service = new MemoryRetrievalServiceImpl(repository);
    }

    @Test
    void findRelevant_callsMarkRecalledForEachResult() {
        var m1 = memory("preference for morning meetings");
        var m2 = memory("dislikes cold coffee");
        when(repository.findRelevant("morning", 5)).thenReturn(List.of(m1, m2));

        var results = service.findRelevant("morning", 5);

        assertThat(results).hasSize(2);
        verify(repository).markRecalled(m1.id());
        verify(repository).markRecalled(m2.id());
        verify(repository, never()).findRecent(anyInt());
    }

    @Test
    void findRelevant_fallsBackToRecentWhenKeywordReturnsEmpty() {
        var m1 = memory("recent important fact");
        when(repository.findRelevant("unknown topic", 5)).thenReturn(List.of());
        when(repository.findRecent(5)).thenReturn(List.of(m1));

        var results = service.findRelevant("unknown topic", 5);

        assertThat(results).hasSize(1);
        verify(repository).findRecent(5);
        verify(repository).markRecalled(m1.id());
    }

    @Test
    void findRelevant_returnsEmptyWhenBothPathsEmpty() {
        when(repository.findRelevant(anyString(), anyInt())).thenReturn(List.of());
        when(repository.findRecent(anyInt())).thenReturn(List.of());

        var results = service.findRelevant("anything", 5);

        assertThat(results).isEmpty();
        verify(repository, never()).markRecalled(any());
    }

    private static EpisodicMemory memory(String summary) {
        return new EpisodicMemory(UUID.randomUUID(), summary, "INTERACTION", 1,
                "system", Instant.now(), null, 0);
    }
}
