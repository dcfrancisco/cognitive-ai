package ph.francisco.memory;

import ph.francisco.perception.Observation;
import ph.francisco.agents.AgentResponse;
import org.springframework.stereotype.Service;
import ph.francisco.memory.CuratedMemoryService;

import java.time.Instant;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Service
public class ConversationMemoryService {

    private final Deque<ConversationEntry> entries = new ConcurrentLinkedDeque<>();
    private final CuratedMemoryService curatedMemoryService;

    public ConversationMemoryService(CuratedMemoryService curatedMemoryService) {
        this.curatedMemoryService = curatedMemoryService;
    }

    public static class ConversationEntry {
        public final Instant timestamp;
        public final String input;
        public final String response;

        public ConversationEntry(Instant timestamp, String input, String response) {
            this.timestamp = timestamp;
            this.input = input;
            this.response = response;
        }
    }

    public void store(Observation observation, AgentResponse response) {
        String in = observation == null ? "" : observation.content();
        String out = response == null ? "" : response.message();
        entries.addFirst(new ConversationEntry(Instant.now(), in, out));
        // keep bounded
        while (entries.size() > 1000) {
            entries.removeLast();
        }

        // Persist into core episodic memory for later recall
        try {
            curatedMemoryService.storeInteraction(observation, response);
        } catch (Exception e) {
            // avoid crashing interactive loop on DB errors; log via stderr
            System.err.println("Failed to persist interaction to curated memory: " + e.getMessage());
        }
    }

    public List<ConversationEntry> retrieveRecent(int n) {
        return entries.stream().limit(n).collect(Collectors.toList());
    }
}
