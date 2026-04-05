package ph.francisco.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemoryRetrievalServiceImpl implements MemoryRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(MemoryRetrievalServiceImpl.class);
    private static final int MAX_RESULTS = 5;

    private final EpisodicMemoryRepository repository;

    public MemoryRetrievalServiceImpl(EpisodicMemoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<EpisodicMemory> findRelevant(String input, int limit) {
        int effectiveLimit = Math.min(limit, MAX_RESULTS);

        List<EpisodicMemory> results = repository.findRelevant(input, effectiveLimit);
        log.debug("Keyword retrieval for input='{}': {} result(s)", input, results.size());

        if (results.isEmpty()) {
            results = repository.findRecent(effectiveLimit);
            log.debug("Keyword returned nothing — falling back to recency: {} result(s)", results.size());
        }

        results.forEach(m -> repository.markRecalled(m.id()));
        return results;
    }
}
