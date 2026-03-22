package ph.francisco.perception;

import jakarta.validation.constraints.NotBlank;

public record Observation(
        @NotBlank String source,
        @NotBlank String content,
        Boolean explicitRemember) {
}
