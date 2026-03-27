package ph.francisco.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SpringAiStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(SpringAiStartupLogger.class);

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final Environment env;

    public SpringAiStartupLogger(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider, Environment env) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.env = env;
    }

    @PostConstruct
    public void logStatus() {
        boolean clientPresent = chatClientBuilderProvider.getIfAvailable() != null;
        boolean apiKeySet = env.getProperty("spring.ai.openai.api-key") != null
                && !env.getProperty("spring.ai.openai.api-key").isBlank();
        String model = env.getProperty("spring.ai.openai.chat.options.model",
                env.getProperty("spring.ai.openai.chat.model", "unspecified"));

        if (clientPresent && apiKeySet) {
            log.info("Spring AI: ChatClient available and API key present. Model={}", model);
        } else if (clientPresent) {
            log.warn(
                    "Spring AI: ChatClient present but API key not set. Set spring.ai.openai.api-key to enable LLM responses.");
        } else {
            log.info(
                    "Spring AI: ChatClient not present (Spring AI not configured). Agents will use rule-based fallbacks.");
        }
    }
}
