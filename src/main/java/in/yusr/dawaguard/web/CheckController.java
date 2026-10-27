package in.yusr.dawaguard.web;

import in.yusr.dawaguard.check.CheckResult;
import in.yusr.dawaguard.check.CheckService;
import in.yusr.dawaguard.check.Images;
import in.yusr.dawaguard.check.StripReading;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Runs a check and streams progress as newline-delimited JSON: one {"type":"step"} line per pipeline step,
 * then a final {"type":"result"} (or {"type":"error"}) line.
 */
@RestController
@RequestMapping("/api/check")
public class CheckController {

    private static final Logger log = LoggerFactory.getLogger(CheckController.class);
    private static final MediaType NDJSON = MediaType.parseMediaType("application/x-ndjson");
    private static final long TIMEOUT_MS = 120_000;

    private final CheckService checks;
    private final RateLimiter limiter;
    private final JsonMapper json;
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

    public CheckController(CheckService checks, RateLimiter limiter, JsonMapper json) {
        this.checks = checks;
        this.limiter = limiter;
        this.json = json;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseBodyEmitter> checkPhoto(@RequestPart("image") MultipartFile image,
                                                            @RequestParam(defaultValue = "en") String lang,
                                                            HttpServletRequest request) throws IOException {
        String contentType = image.getContentType() == null ? "" : image.getContentType();
        if (image.isEmpty() || !contentType.startsWith("image/")) {
            return error(HttpStatus.BAD_REQUEST, "Please upload a photo of the medicine strip.");
        }
        if (!limiter.tryAcquire(request.getRemoteAddr())) {
            return tooMany();
        }
        String dataUrl = Images.toDataUrl(image.getBytes(), contentType);
        return stream(events -> checks.checkPhoto(dataUrl, language(lang), events));
    }

    @PostMapping(path = "/typed", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBodyEmitter> checkTyped(@Valid @RequestBody TypedCheck body,
                                                            HttpServletRequest request) {
        if (!limiter.tryAcquire(request.getRemoteAddr())) {
            return tooMany();
        }
        StripReading reading = StripReading.manual(body.brandName(), body.genericName(), body.batchNo(), body.expiryDate());
        return stream(events -> checks.checkTyped(reading, language(body.lang()), events));
    }

    private ResponseEntity<ResponseBodyEmitter> stream(Function<Consumer<Map<String, Object>>, CheckResult> run) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(TIMEOUT_MS);
        pool.execute(() -> {
            try {
                CheckResult result = run.apply(event -> send(emitter, event));
                Map<String, Object> done = new LinkedHashMap<>();
                done.put("type", "result");
                done.put("result", result);
                send(emitter, done);
            } catch (RuntimeException e) {
                log.warn("Check failed", e);
                send(emitter, Map.of("type", "error",
                        "message", "The check could not finish. Try again in a minute."));
            } finally {
                emitter.complete();
            }
        });
        return ResponseEntity.ok().contentType(NDJSON).body(emitter);
    }

    /** Each event is one JSON line, flushed immediately. A client that went away just stops receiving. */
    private void send(ResponseBodyEmitter emitter, Map<String, Object> event) {
        try {
            emitter.send(json.writeValueAsString(event) + "\n", MediaType.TEXT_PLAIN);
        } catch (IOException | IllegalStateException e) {
            log.debug("Client disconnected: {}", e.getMessage());
        }
    }

    private ResponseEntity<ResponseBodyEmitter> tooMany() {
        return error(HttpStatus.TOO_MANY_REQUESTS,
                "This free demo limits checks to keep costs down. Try again in an hour.");
    }

    /** Errors use the same line format as the stream, so the client has one parser. */
    private ResponseEntity<ResponseBodyEmitter> error(HttpStatus status, String message) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(TIMEOUT_MS);
        send(emitter, Map.of("type", "error", "message", message));
        emitter.complete();
        return ResponseEntity.status(status).contentType(NDJSON).body(emitter);
    }

    private static String language(String lang) {
        return "hi".equalsIgnoreCase(lang) ? "hi" : "en";
    }

    public record TypedCheck(@NotBlank @Size(max = 40) String batchNo,
                             @Size(max = 120) String brandName,
                             @Size(max = 200) String genericName,
                             @Size(max = 20) String expiryDate,
                             @Size(max = 2) String lang) {
    }
}
