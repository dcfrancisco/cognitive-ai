package ph.francisco.interfaceadapters;

import ph.francisco.memory.CuratedMemoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoMemoryController {

    private final CuratedMemoryService curatedMemoryService;

    public DemoMemoryController(CuratedMemoryService curatedMemoryService) {
        this.curatedMemoryService = curatedMemoryService;
    }

    @GetMapping("/api/demo/working-memory")
    public Object workingMemory(
            @org.springframework.web.bind.annotation.RequestParam(name = "sessionId", required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return curatedMemoryService.workingSnapshot();
        }
        return curatedMemoryService.workingSnapshot(sessionId);
    }
}
