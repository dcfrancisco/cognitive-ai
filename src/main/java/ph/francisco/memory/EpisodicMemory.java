package ph.francisco.memory;

import org.springframework.jdbc.core.RowMapper;

import java.time.Instant;
import java.util.UUID;

public record EpisodicMemory(
        UUID id,
        String summary,
        String memoryType,
        int importance,
        String source,
        Instant createdAt,
        Instant lastRecalledAt,
        int recallCount
) {
    static final RowMapper<EpisodicMemory> ROW_MAPPER = (rs, rowNum) -> new EpisodicMemory(
            rs.getObject("id", UUID.class),
            rs.getString("summary"),
            rs.getString("memory_type"),
            rs.getInt("importance"),
            rs.getString("source"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("last_recalled_at") != null
                    ? rs.getTimestamp("last_recalled_at").toInstant()
                    : null,
            rs.getInt("recall_count")
    );
}
