package ph.francisco.perception;

import jakarta.validation.constraints.NotBlank;

public record Observation(
        @NotBlank String source,
        @NotBlank String content,
        Boolean explicitRemember,
        String sessionId,
        String timestamp,
        String location) {

    /** Backward-compatible constructor — timestamp and location default to null. */
    public Observation(String source, String content, Boolean explicitRemember, String sessionId) {
        this(source, content, explicitRemember, sessionId, null, null);
    }
}
