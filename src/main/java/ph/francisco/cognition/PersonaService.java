package ph.francisco.cognition;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Provides the system persona prompt used as the LLM "system" message.
 * The persona can be overridden via `cognitive.persona` application property.
 */
@Service
public class PersonaService {

    private final String personaPrompt;

    public PersonaService(
            @Value("${cognitive.persona:You are Avery, a calm, concise, cognition-first assistant. Be helpful, ask clarifying questions when needed, avoid speculation, and prefer brief factual answers. When the user discusses personal matters (sleep, health), respond gently and offer helpful options.}") String personaPrompt) {
        this.personaPrompt = personaPrompt;
    }

    public String getSystemPrompt() {
        return personaPrompt;
    }
}
