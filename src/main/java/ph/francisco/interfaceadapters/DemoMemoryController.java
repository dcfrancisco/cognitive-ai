package ph.francisco.interfaceadapters;

import ph.francisco.memory.CuratedMemoryService;
import ph.francisco.memory.ConversationMemoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoMemoryController {

    private final CuratedMemoryService curatedMemoryService;
    private final ConversationMemoryService conversationMemoryService;

    public DemoMemoryController(CuratedMemoryService curatedMemoryService,
            ConversationMemoryService conversationMemoryService) {
        this.curatedMemoryService = curatedMemoryService;
        this.conversationMemoryService = conversationMemoryService;
    }

    @GetMapping("/api/demo/working-memory")
    public Object workingMemory(
            @org.springframework.web.bind.annotation.RequestParam(name = "sessionId", required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return curatedMemoryService.workingSnapshot();
        }
        return curatedMemoryService.workingSnapshot(sessionId);
    }

    @GetMapping("/api/demo/conversation")
    public Object conversation(
            @org.springframework.web.bind.annotation.RequestParam(name = "limit", required = false) Integer limit) {
        int n = (limit == null || limit <= 0) ? 50 : limit;
        return conversationMemoryService.retrieveRecent(n);
    }
}
