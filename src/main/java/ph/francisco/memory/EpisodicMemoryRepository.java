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

    public void insert(UUID id, String summary, String rationale, List<String> tags) {
        jdbcTemplate.update(
                """
                        INSERT INTO episodic_memory (id, summary, rationale, tags)
                        VALUES (?, ?, ?, ?)
                        """,
                ps -> {
                    ps.setObject(1, id);
                    ps.setString(2, summary);
                    ps.setString(3, rationale);
                    Array tagsArray = ps.getConnection().createArrayOf("text", tags.toArray());
                    ps.setArray(4, tagsArray);
                });
    }
}
