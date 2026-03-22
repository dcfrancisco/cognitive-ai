package ph.francisco.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class OpenAiOptionalEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String[] OPENAI_AUTOCONFIGS = {
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration"
    };

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Enable OpenAI auto-config only when a key is present.
        // Spring's relaxed binding will map `SPRING_AI_OPENAI_API_KEY` to
        // `spring.ai.openai.api-key`.
        String apiKey = environment.getProperty("spring.ai.openai.api-key");
        String speechApiKey = environment.getProperty("spring.ai.openai.speech.api-key");
        if (StringUtils.hasText(apiKey) || StringUtils.hasText(speechApiKey)) {
            return;
        }

        String existing = environment.getProperty("spring.autoconfigure.exclude");
        Set<String> excludes = new LinkedHashSet<>();
        if (StringUtils.hasText(existing)) {
            Arrays.stream(existing.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(excludes::add);
        }
        excludes.addAll(Arrays.asList(OPENAI_AUTOCONFIGS));

        String value = String.join(",", excludes);
        environment.getPropertySources().addFirst(
                new MapPropertySource("openaiOptionalAutoConfigExcludes",
                        Map.of("spring.autoconfigure.exclude", value)));
    }

    @Override
    public int getOrder() {
        // Run early.
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
