package ph.francisco.agents;

import ph.francisco.perception.Observation;
import ph.francisco.values.ValuesAndBoundaries;
import ph.francisco.memory.WorkingMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SpringAiResponseService {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    public SpringAiResponseService(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
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
            return Optional.empty();
        }

        try {
            ChatClient client = builder.build();
            String response = client.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            if (!StringUtils.hasText(response)) {
                return Optional.empty();
            }

            return Optional.of(response.trim());
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
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
