package ph.francisco.memory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class MemoryCandidateRepository {
    private final JdbcTemplate jdbcTemplate;

    public MemoryCandidateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertPending(UUID id, String source, String summary, String rationale, List<String> tags) {
        jdbcTemplate.update(
                """
                        INSERT INTO memory_candidate (id, source, summary, rationale, tags, status)
                        VALUES (?, ?, ?, ?, ?, 'PENDING')
                        """,
                ps -> {
                    ps.setObject(1, id);
                    ps.setString(2, source);
                    ps.setString(3, summary);
                    ps.setString(4, rationale);
                    Array tagsArray = ps.getConnection().createArrayOf("text", tags.toArray());
                    ps.setArray(5, tagsArray);
                });
    }

    public List<MemoryCandidate> findPending(int limit) {
        return jdbcTemplate.query(
                """
                        SELECT id, created_at, source, summary, rationale, tags, status, reviewer_note
                        FROM memory_candidate
                        WHERE status = 'PENDING'
                        ORDER BY created_at DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> map(rs),
                limit);
    }

    public int mark(UUID id, MemoryCandidate.Status status, String reviewerNote) {
        return jdbcTemplate.update(
                """
                        UPDATE memory_candidate
                        SET status = ?, reviewer_note = ?
                        WHERE id = ?
                        """,
                status.name(), reviewerNote, id);
    }

    public MemoryCandidate findById(UUID id) {
        var list = jdbcTemplate.query(
                """
                        SELECT id, created_at, source, summary, rationale, tags, status, reviewer_note
                        FROM memory_candidate
                        WHERE id = ?
                        """,
                (rs, rowNum) -> map(rs),
                id);
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean pendingExistsBySummary(String summary) {
        try {
            Integer one = jdbcTemplate.queryForObject(
                    "SELECT 1 FROM memory_candidate WHERE status = 'PENDING' AND summary = ? LIMIT 1",
                    Integer.class,
                    summary);
            return one != null;
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return false;
        }
    }

    /**
     * Check for a pending candidate whose summary is similar to the provided summary
     * using Postgres pg_trgm similarity. Requires pg_trgm extension and a trigram
     * index on lower(summary) for performance.
     *
     * @param summary text to compare
     * @param similarityThreshold value in [0,1], higher means stricter match
     * @return true if a similar pending candidate exists
     */
    public boolean pendingSimilarExists(String summary, double similarityThreshold) {
        try {
            Integer one = jdbcTemplate.queryForObject(
                    "SELECT 1 FROM memory_candidate WHERE status = 'PENDING' AND similarity(lower(summary), lower(?)) >= ? LIMIT 1",
                    Integer.class,
                    summary,
                    similarityThreshold);
            return one != null;
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return false;
        }
    }

    private static MemoryCandidate map(ResultSet rs) throws SQLException {
        UUID id = (UUID) rs.getObject("id");
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        String source = rs.getString("source");
        String summary = rs.getString("summary");
        String rationale = rs.getString("rationale");
        String status = rs.getString("status");
        String reviewerNote = rs.getString("reviewer_note");
        Array arr = rs.getArray("tags");
        List<String> tags = arr == null ? List.of() : List.of((String[]) arr.getArray());
        return new MemoryCandidate(id, createdAt, source, summary, rationale, tags,
                MemoryCandidate.Status.valueOf(status), reviewerNote);
    }
}
