package ph.francisco.interfaceadapters;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiHealthController {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final Environment env;

    public AiHealthController(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider, Environment env) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.env = env;
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        boolean clientAvailable = chatClientBuilderProvider.getIfAvailable() != null;
        boolean apiKeySet = env.getProperty("spring.ai.openai.api-key") != null
                && !env.getProperty("spring.ai.openai.api-key").isBlank();
        String model = env.getProperty("spring.ai.openai.chat.options.model",
                env.getProperty("spring.ai.openai.chat.model", "unknown"));

        return ResponseEntity.ok(Map.of(
                "available", clientAvailable && apiKeySet,
                "clientPresent", clientAvailable,
                "apiKeySet", apiKeySet,
                "model", model));
    }
}
