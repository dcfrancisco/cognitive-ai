package ph.francisco.memory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MemoryCandidate(
		UUID id,
		Instant createdAt,
		String source,
		String summary,
		String rationale,
		List<String> tags,
		Status status,
		String reviewerNote
) {
	public enum Status {
		PENDING,
		ACCEPTED,
		REJECTED
	}
}
