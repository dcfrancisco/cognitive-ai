package ph.francisco.agents;

import ph.francisco.perception.Observation;
import ph.francisco.values.ValuesAndBoundaries;
import ph.francisco.memory.WorkingMemory;
import ph.francisco.cognition.PersonaService;
import org.springframework.ai.chat.client.ChatClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SpringAiResponseService {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final PersonaService personaService;

    public SpringAiResponseService(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            PersonaService personaService) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.personaService = personaService;
    }

    public Optional<String> generateReflection(Observation observation, List<WorkingMemory.Item> items) {
        String content = safeContent(observation);
        if ((items == null || items.isEmpty()) && !org.springframework.util.StringUtils.hasText(content)) {
            return Optional.empty();
        }

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("User request:\n");
        userPrompt.append(content).append("\n\n");
        if (items != null && !items.isEmpty()) {
            userPrompt.append("Recent working memory:\n");
            items.stream()
                    .skip(Math.max(0, items.size() - 5))
                    .forEach(item -> userPrompt.append("- [").append(item.source()).append("] ").append(item.content())
                            .append("\n"));
        }

        return callModel(buildReflectionSystemPrompt(), userPrompt.toString());
    }

    public Optional<String> generateRecallResponse(Observation observation, List<WorkingMemory.Item> items) {
        if (items == null || items.isEmpty()) {
            return Optional.empty();
        }

        String content = safeContent(observation);
        if (!StringUtils.hasText(content)) {
            return Optional.empty();
        }

        String memoryLines = items.stream()
                .skip(Math.max(0, items.size() - 5))
                .map(item -> "- [" + item.source() + "] " + item.content())
                .collect(Collectors.joining("\n"));

        String userPrompt = "User request:\n" + content
                + "\n\nRecent working memory:\n" + memoryLines;

        return callModel(buildRecallSystemPrompt(), userPrompt);
    }

    public Optional<String> summarizeMemoryCandidate(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (!StringUtils.hasText(trimmed)) {
            return Optional.empty();
        }

        String userPrompt = "Observation:\n" + trimmed;
        return callModel(buildSummarizeSystemPrompt(), userPrompt)
                .map(this::truncateSummary);
    }

    private String safeContent(Observation observation) {
        if (observation == null) {
            return "";
        }
        return Optional.ofNullable(observation.content())
                .map(String::trim)
                .orElse("");
    }

    @SuppressWarnings("null")
    private Optional<String> callModel(String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(userPrompt)) {
            return Optional.empty();
        }

        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            // Fall back to direct OpenAI call if Spring ChatClient is not available
            return callOpenAiDirect(systemPrompt, userPrompt);
        }

        try {
            ChatClient client = builder.build();
            String combinedSystem = combinePersonaAndSystem(systemPrompt);
            String response = client.prompt()
                    .system(combinedSystem)
                    .user(userPrompt)
                    .call()
                    .content();

            if (!StringUtils.hasText(response)) {
                return Optional.empty();
            }

            return Optional.of(response.trim());
        } catch (RuntimeException ex) {
            // Try a direct OpenAI HTTP call as a fallback to work around provider
            // compatibility issues
            return callOpenAiDirect(systemPrompt, userPrompt);
        }
    }

    private Optional<String> callOpenAiDirect(String systemPrompt, String userPrompt) {
        try {
            String apiKey = System.getenv("SPRING_AI_OPENAI_API_KEY");
            if (!StringUtils.hasText(apiKey)) {
                apiKey = System.getenv("OPENAI_API_KEY");
            }
            if (!StringUtils.hasText(apiKey)) {
                return Optional.empty();
            }

            String model = System.getenv("SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL");
            if (!StringUtils.hasText(model)) {
                model = System.getenv("AI_MODEL");
            }
            if (!StringUtils.hasText(model)) {
                model = "gpt-4o-mini";
            }

            String prompt = combinePersonaAndSystem(systemPrompt) + "\n\n" + userPrompt;

            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());

            ObjectMapper mapper = new ObjectMapper();
            // Minimal request body for Responses API
            JsonNode body = mapper.createObjectNode()
                    .put("model", model)
                    .put("input", prompt);

            HttpEntity<String> req = new HttpEntity<>(mapper.writeValueAsString(body), headers);

            ResponseEntity<String> resp = rt.postForEntity("https://api.openai.com/v1/responses", req, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return Optional.empty();
            }

            JsonNode root = mapper.readTree(resp.getBody());
            // Try common response shapes
            // 1) output[0].content[0].text
            if (root.has("output") && root.get("output").isArray() && root.get("output").size() > 0) {
                JsonNode out0 = root.get("output").get(0);
                if (out0.has("content") && out0.get("content").isArray() && out0.get("content").size() > 0) {
                    for (JsonNode c : out0.get("content")) {
                        if (c.has("text")) {
                            String text = c.get("text").asText();
                            if (StringUtils.hasText(text))
                                return Optional.of(text.trim());
                        }
                        if (c.isTextual()) {
                            String text = c.asText();
                            if (StringUtils.hasText(text))
                                return Optional.of(text.trim());
                        }
                    }
                }
            }

            // 2) choices[0].message.content (chat/completions shape)
            if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                JsonNode choice = root.get("choices").get(0);
                if (choice.has("message") && choice.get("message").has("content")) {
                    String text = choice.get("message").get("content").asText();
                    if (StringUtils.hasText(text))
                        return Optional.of(text.trim());
                }
                if (choice.has("text")) {
                    String text = choice.get("text").asText();
                    if (StringUtils.hasText(text))
                        return Optional.of(text.trim());
                }
            }

            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String combinePersonaAndSystem(String systemPrompt) {
        String persona = personaService == null ? "" : personaService.getSystemPrompt();
        if (!StringUtils.hasText(persona)) {
            return systemPrompt == null ? "" : systemPrompt;
        }
        if (!StringUtils.hasText(systemPrompt)) {
            return persona;
        }
        return persona + "\n\n" + systemPrompt;
    }

    private String buildReflectionSystemPrompt() {
        String rules = ValuesAndBoundaries.rules().stream()
                .map(rule -> "- " + rule)
                .collect(Collectors.joining("\n"));

        return "You are a restrained cognitive companion. Follow these values:\n"
                + rules
                + "\nRespond in 1-3 sentences, avoid commands or judgments, and keep it calm and grounded.";
    }

    private String buildRecallSystemPrompt() {
        return "You answer recall requests using only the provided working memory. "
                + "Be honest about uncertainty, avoid inventing details, and respond in 1-3 sentences.";
    }

    private String buildSummarizeSystemPrompt() {
        return "Summarize the meaning of the observation in 160 characters or fewer. "
                + "Do not include quotes, prefixes, or extra commentary.";
    }

    private String truncateSummary(String summary) {
        String trimmed = summary == null ? "" : summary.trim();
        if (trimmed.length() <= 160) {
            return trimmed;
        }
        return trimmed.substring(0, 157) + "...";
    }
}
