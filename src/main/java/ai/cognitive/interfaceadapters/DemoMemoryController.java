package ai.cognitive.interfaceadapters;

import ai.cognitive.memory.CuratedMemoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoMemoryController {

    private final CuratedMemoryService curatedMemoryService;

    public DemoMemoryController(CuratedMemoryService curatedMemoryService) {
        this.curatedMemoryService = curatedMemoryService;
    }

    @GetMapping("/api/demo/working-memory")
    public Object workingMemory() {
        return curatedMemoryService.workingSnapshot();
    }
}
