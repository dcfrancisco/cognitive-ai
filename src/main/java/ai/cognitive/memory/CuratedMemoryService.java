package ai.cognitive.memory;

import ai.cognitive.perception.Observation;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CuratedMemoryService {
    private final WorkingMemory workingMemory;
    private final MemoryCandidateRepository candidateRepository;
    private final EpisodicMemoryRepository episodicMemoryRepository;

    // Minimal recurrence heuristic: count normalized snippets over a short window.
    private final Map<String, Integer> recurrenceCounts = new ConcurrentHashMap<>();
    private final Deque<String> recentKeys = new ArrayDeque<>();
    private final int recurrenceWindow = 50;

    public CuratedMemoryService(MemoryCandidateRepository candidateRepository,
            EpisodicMemoryRepository episodicMemoryRepository) {
        this.workingMemory = new WorkingMemory(25);
        this.candidateRepository = candidateRepository;
        this.episodicMemoryRepository = episodicMemoryRepository;
    }

    public void observe(Observation observation) {
        workingMemory.add(observation);
        considerCandidate(observation);
    }

    public List<WorkingMemory.Item> workingSnapshot() {
        return workingMemory.snapshot();
    }

    private void considerCandidate(Observation observation) {
        var content = observation.content() == null ? "" : observation.content().trim();
        if (content.isBlank()) {
            return;
        }

        var tags = new ArrayList<String>();
        boolean explicitlyRemember = Boolean.TRUE.equals(observation.explicitRemember());
        if (explicitlyRemember) {
            tags.add("explicit_remember");
        }

        String key = normalizeKey(content);
        int count = bumpRecurrence(key);

        boolean recurring = count >= 3;
        if (recurring) {
            tags.add("recurring");
        }

        // Rule-based first: only create candidates if explicit remember OR recurring.
        if (!explicitlyRemember && !recurring) {
            return;
        }

        String summary = summarizeMeaning(content);
        String rationale = explicitlyRemember
                ? "User explicitly requested remembering"
                : "Observed recurrence (>=3) in recent window";

        candidateRepository.insertPending(UUID.randomUUID(), observation.source(), summary, rationale, tags);
    }

    private int bumpRecurrence(String key) {
        recurrenceCounts.merge(key, 1, Integer::sum);
        recentKeys.addLast(key);
        if (recentKeys.size() > recurrenceWindow) {
            String evicted = recentKeys.removeFirst();
            recurrenceCounts.computeIfPresent(evicted, (k, v) -> v <= 1 ? null : (v - 1));
        }
        return recurrenceCounts.getOrDefault(key, 0);
    }

    private static String normalizeKey(String content) {
        String c = content.toLowerCase();
        c = c.replaceAll("\\s+", " ").trim();
        // Bound length to avoid unbounded keys.
        if (c.length() > 256) {
            c = c.substring(0, 256);
        }
        // Add a tiny hash suffix to reduce collisions on truncation.
        byte[] bytes = c.getBytes(StandardCharsets.UTF_8);
        int h = 0;
        for (byte b : bytes) {
            h = 31 * h + b;
        }
        return c + "#" + Integer.toHexString(h);
    }

    private static String summarizeMeaning(String content) {
        // Intentionally conservative: do not keep raw transcript; keep a short
        // meaning-preserving paraphrase.
        // Rule-based placeholder; can be swapped to LLM-assisted summarization later.
        String c = content.replaceAll("\\s+", " ").trim();
        if (c.length() <= 160) {
            return c;
        }
        return c.substring(0, 157) + "...";
    }

    public List<MemoryCandidate> pendingCandidates(int limit) {
        return candidateRepository.findPending(limit);
    }

    public void acceptCandidate(UUID id, String reviewerNote) {
        // Minimal: mark accepted and immediately materialize into episodic memory using
        // the candidate row.
        // To keep the flow auditable, we re-read the pending list and match by id.
        var pending = candidateRepository.findPending(200);
        MemoryCandidate found = pending.stream().filter(c -> c.id().equals(id)).findFirst().orElse(null);
        candidateRepository.mark(id, MemoryCandidate.Status.ACCEPTED, reviewerNote);
        if (found != null) {
            episodicMemoryRepository.insert(UUID.randomUUID(), found.summary(), found.rationale(), found.tags());
        }
    }

    public int rejectCandidate(UUID id, String reviewerNote) {
        return candidateRepository.mark(id, MemoryCandidate.Status.REJECTED, reviewerNote);
    }
}
