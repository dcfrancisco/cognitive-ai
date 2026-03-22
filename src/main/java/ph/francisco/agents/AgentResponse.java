package ph.francisco.agents;

import java.util.List;

public record AgentResponse(
        String agent,
        String message,
        List<String> reasons
) {
}
