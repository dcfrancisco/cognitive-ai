package ph.francisco.memory;

import ph.francisco.perception.Observation;
import ph.francisco.agents.SpringAiResponseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CuratedMemoryService {
    private final Map<String, WorkingMemory> sessionMemory = new ConcurrentHashMap<>();
    private final MemoryCandidateRepository candidateRepository;
    private final double similarityThreshold;
    private final EpisodicMemoryRepository episodicMemoryRepository;
    private final SpringAiResponseService springAiResponseService;

    // Minimal recurrence heuristic: count normalized snippets over a short window.
    private final Map<String, Integer> recurrenceCounts = new ConcurrentHashMap<>();
    private final Deque<String> recentKeys = new ArrayDeque<>();
    private static final int RECURRENCE_WINDOW = 50;

    public CuratedMemoryService(MemoryCandidateRepository candidateRepository,
            EpisodicMemoryRepository episodicMemoryRepository,
            SpringAiResponseService springAiResponseService,
            @Value("${memory.duplicate.similarity.threshold:0.45}") double similarityThreshold) {
        // per-session working memory instances
        // default capacity 25 items per session
        // sessionMemory is populated on demand
        this.candidateRepository = candidateRepository;
        this.episodicMemoryRepository = episodicMemoryRepository;
        this.springAiResponseService = springAiResponseService;
        this.similarityThreshold = similarityThreshold;
    }

    public void observe(Observation observation) {
        String sessionId = observation.sessionId() == null ? "global" : observation.sessionId();
        getOrCreateWorkingMemory(sessionId).add(observation);
        considerCandidate(observation);
    }

    public List<WorkingMemory.Item> workingSnapshot() {
        // return a merged snapshot across sessions (for legacy/demo fallback)
        return sessionMemory.values().stream()
                .flatMap(wm -> wm.snapshot().stream())
                .toList();
    }

    public List<WorkingMemory.Item> workingSnapshot(String sessionId) {
        if (sessionId == null) {
            return workingSnapshot();
        }
        WorkingMemory wm = sessionMemory.get(sessionId);
        return wm == null ? List.of() : wm.snapshot();
    }

    private WorkingMemory getOrCreateWorkingMemory(String sessionId) {
        return sessionMemory.computeIfAbsent(sessionId, id -> new WorkingMemory(25));
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

        // Prevent duplicate pending candidates using exact match first, then
        // a trigram-based fuzzy similarity check when available.
        if (candidateRepository.pendingExistsBySummary(summary)) {
            return;
        }

        // Fuzzy duplicate prevention: if the database has a pg_trgm-based index
        // and the similarity threshold is configured, avoid inserting near-duplicates.
        if (similarityThreshold > 0 && candidateRepository.pendingSimilarExists(summary, similarityThreshold)) {
            return;
        }

        candidateRepository.insertPending(UUID.randomUUID(), observation.source(), summary, rationale, tags);
    }

    private int bumpRecurrence(String key) {
        recurrenceCounts.merge(key, 1, (current, delta) -> current + delta);
        recentKeys.addLast(key);
        if (recentKeys.size() > RECURRENCE_WINDOW) {
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

    private String summarizeMeaning(String content) {
        // Intentionally conservative: do not keep raw transcript; keep a short
        // meaning-preserving paraphrase. Use Spring AI when available.
        return springAiResponseService.summarizeMemoryCandidate(content)
                .orElseGet(() -> ruleBasedSummary(content));
    }

    private static String ruleBasedSummary(String content) {
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
        // Read the candidate by id; only mark and materialize if the candidate exists
        // and is pending.
        MemoryCandidate found = candidateRepository.findById(id);
        if (found == null) {
            // nothing to accept
            return;
        }

        // Only accept if it's currently pending to avoid accidental re-accepts.
        if (found.status() == MemoryCandidate.Status.PENDING) {
            candidateRepository.mark(id, MemoryCandidate.Status.ACCEPTED, reviewerNote);
            episodicMemoryRepository.insert(UUID.randomUUID(), found.summary(), found.rationale(), found.tags());
        }
    }

    public int rejectCandidate(UUID id, String reviewerNote) {
        return candidateRepository.mark(id, MemoryCandidate.Status.REJECTED, reviewerNote);
    }
}
