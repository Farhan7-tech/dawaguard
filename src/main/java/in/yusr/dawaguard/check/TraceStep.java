package in.yusr.dawaguard.check;

import in.yusr.dawaguard.llm.NebiusClient;

/** One step of a check, shown in the UI's "which model did what" panel. */
public record TraceStep(String step, String description, String engine, long ms, int promptTokens,
                        int completionTokens) {

    static TraceStep of(String step, String description, NebiusClient.ChatResult chat) {
        return new TraceStep(step, description, chat.model(), chat.latencyMs(), chat.promptTokens(),
                chat.completionTokens());
    }
}
