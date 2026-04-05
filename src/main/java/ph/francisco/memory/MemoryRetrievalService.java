package ph.francisco.memory;

import java.util.List;

public interface MemoryRetrievalService {
    List<EpisodicMemory> findRelevant(String input, int limit);
}
