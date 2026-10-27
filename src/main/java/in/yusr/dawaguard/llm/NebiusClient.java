package in.yusr.dawaguard.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import in.yusr.dawaguard.config.DawaGuardProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin client for Nebius Token Factory's OpenAI-compatible chat API.
 * Every call records which model answered, so the UI can show "which model did what".
 */
@Component
public class NebiusClient {

    private final RestClient http;

    public NebiusClient(DawaGuardProperties props, RestClient.Builder builder) {
        this.http = builder
                .baseUrl(props.nebius().baseUrl())
                .defaultHeader("Authorization", "Bearer " + props.nebius().apiKey())
                .build();
    }

    public ChatResult chat(String model, String system, String user, int maxTokens) {
        return chat(model, system, user, null, maxTokens);
    }

    /** imageDataUrl is optional, e.g. "data:image/jpeg;base64,...". Only vision models accept it. */
    public ChatResult chat(String model, String system, String user, String imageDataUrl, int maxTokens) {
        return chat(model, system, user, imageDataUrl, maxTokens, true);
    }

    /**
     * thinking=false turns off Nemotron's reasoning phase. Use it for short, simple outputs: without it,
     * Lightning writes its thinking into the reply and spends hundreds of tokens before answering.
     */
    public ChatResult chat(String model, String system, String user, String imageDataUrl, int maxTokens,
                           boolean thinking) {
        Object userContent = user;
        if (imageDataUrl != null) {
            userContent = List.of(
                    Map.of("type", "text", "text", user),
                    Map.of("type", "image_url", "image_url", Map.of("url", imageDataUrl)));
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        if (system != null) {
            messages.add(Map.of("role", "system", "content", system));
        }
        messages.add(Map.of("role", "user", "content", userContent));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_tokens", maxTokens);
        if (!thinking) {
            body.put("chat_template_kwargs", Map.of("enable_thinking", false));
        }

        long started = System.currentTimeMillis();
        ChatResponse response = http.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ChatResponse.class);
        long latencyMs = System.currentTimeMillis() - started;

        String content = response.choices().isEmpty() ? "" : response.choices().get(0).message().content();
        Usage usage = response.usage() == null ? new Usage(0, 0) : response.usage();
        return new ChatResult(model, content == null ? "" : content.trim(),
                usage.promptTokens(), usage.completionTokens(), latencyMs);
    }

    public record ChatResult(String model, String content, int promptTokens, int completionTokens, long latencyMs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices, Usage usage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(@JsonProperty("prompt_tokens") int promptTokens,
                 @JsonProperty("completion_tokens") int completionTokens) {
    }
}
