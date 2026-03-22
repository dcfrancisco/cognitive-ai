package ai.cognitive.interfaceadapters;

import ai.cognitive.memory.CuratedMemoryService;
import ai.cognitive.memory.MemoryCandidate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/memory")
public class MemoryReviewController {
    private final CuratedMemoryService curatedMemoryService;

    public MemoryReviewController(CuratedMemoryService curatedMemoryService) {
        this.curatedMemoryService = curatedMemoryService;
    }

    @GetMapping("/candidates")
    public List<MemoryCandidate> pending(@RequestParam(defaultValue = "25") @Min(1) @Max(200) int limit) {
        return curatedMemoryService.pendingCandidates(limit);
    }

    public record ReviewRequest(@NotBlank String note) {
    }

    @PostMapping("/candidates/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable UUID id, @RequestBody ReviewRequest request) {
        curatedMemoryService.acceptCandidate(id, request.note());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/candidates/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable UUID id, @RequestBody ReviewRequest request) {
        curatedMemoryService.rejectCandidate(id, request.note());
        return ResponseEntity.noContent().build();
    }
}
