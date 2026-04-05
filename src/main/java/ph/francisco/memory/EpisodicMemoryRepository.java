package ph.francisco.memory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.util.List;
import java.util.UUID;

@Repository
public class EpisodicMemoryRepository {
    private final JdbcTemplate jdbcTemplate;

    public EpisodicMemoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(UUID id, String summary, String rationale, List<String> tags,
            String source, String memoryType) {
        jdbcTemplate.update(
                """
                        INSERT INTO episodic_memory (id, summary, rationale, tags, source, memory_type)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                ps -> {
                    ps.setObject(1, id);
                    ps.setString(2, summary);
                    ps.setString(3, rationale);
                    Array tagsArray = ps.getConnection().createArrayOf("text", tags.toArray());
                    ps.setArray(4, tagsArray);
                    ps.setString(5, source);
                    ps.setString(6, memoryType);
                });
    }

    public List<EpisodicMemory> findRelevant(String input, int limit) {
        return jdbcTemplate.query(
                """
                        SELECT id, summary, memory_type, importance, source, created_at, last_recalled_at, recall_count
                        FROM episodic_memory
                        WHERE summary ILIKE '%' || ? || '%'
                        ORDER BY importance DESC, last_recalled_at DESC NULLS LAST, created_at DESC
                        LIMIT ?
                        """,
                EpisodicMemory.ROW_MAPPER,
                input, limit);
    }

    public List<EpisodicMemory> findRecent(int limit) {
        return jdbcTemplate.query(
                """
                        SELECT id, summary, memory_type, importance, source, created_at, last_recalled_at, recall_count
                        FROM episodic_memory
                        ORDER BY importance DESC, created_at DESC
                        LIMIT ?
                        """,
                EpisodicMemory.ROW_MAPPER,
                limit);
    }

    public void markRecalled(UUID id) {
        jdbcTemplate.update(
                """
                        UPDATE episodic_memory
                        SET recall_count = recall_count + 1, last_recalled_at = NOW()
                        WHERE id = ?
                        """,
                id);
    }
}
