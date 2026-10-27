package in.yusr.dawaguard.whatsapp;

import in.yusr.dawaguard.cabinet.CabinetMessages;
import in.yusr.dawaguard.cabinet.CabinetService;
import in.yusr.dawaguard.cdsco.BatchKeys;
import in.yusr.dawaguard.check.CheckResult;
import in.yusr.dawaguard.check.CheckService;
import in.yusr.dawaguard.check.Images;
import in.yusr.dawaguard.check.StripReading;
import in.yusr.dawaguard.check.Verdict;
import in.yusr.dawaguard.web.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * The WhatsApp conversation. A photo (or typed batch number) runs the same check as the web page; the reply is
 * the family message, and the medicine is saved to the family cabinet so it can be re-checked later.
 */
@Component
public class WhatsAppBot {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppBot.class);
    private static final long MAX_PHOTO_BYTES = 10L * 1024 * 1024;
    private static final int REMEMBERED_MESSAGE_IDS = 5_000;

    private final WhatsAppClient client;
    private final WhatsAppUserRepository users;
    private final CabinetService cabinet;
    private final CheckService checks;
    private final RateLimiter limiter;
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
    /** Meta redelivers a webhook it thinks failed; never answer the same message twice. */
    private final Set<String> seen = Collections.synchronizedSet(Collections.newSetFromMap(
            new LinkedHashMap<>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > REMEMBERED_MESSAGE_IDS;
                }
            }));

    public WhatsAppBot(WhatsAppClient client, WhatsAppUserRepository users, CabinetService cabinet,
                       CheckService checks, RateLimiter limiter) {
        this.client = client;
        this.users = users;
        this.cabinet = cabinet;
        this.checks = checks;
        this.limiter = limiter;
    }

    /** Returns at once; the webhook must answer Meta quickly, and a check takes 10-20 seconds. */
    void accept(WebhookPayload.Message message) {
        if (message == null || message.id() == null || message.from() == null || !seen.add(message.id())) {
            return;
        }
        pool.execute(() -> handle(message));
    }

    void handle(WebhookPayload.Message m) {
        WhatsAppUser user = touch(m);
        log.info("WhatsApp {} message from ...{}", m.type(), m.from().substring(Math.max(0, m.from().length() - 4)));
        try {
            client.markReadAndShowTyping(m.id());
            switch (m.type() == null ? "" : m.type()) {
                case "image" -> checkPhoto(user, m.image());
                case "document" -> {
                    if (m.document() != null && m.document().mimeType() != null
                            && m.document().mimeType().startsWith("image/")) {
                        checkPhoto(user, m.document());
                    } else {
                        reply(user, Replies.help(user.getLanguage()));
                    }
                }
                case "text" -> onText(user, m.text() == null ? "" : m.text().body());
                default -> reply(user, Replies.help(user.getLanguage()));
            }
        } catch (RuntimeException e) {
            log.warn("WhatsApp message {} failed", m.id(), e);
            try {
                reply(user, Replies.failed(user.getLanguage()));
            } catch (RuntimeException ignored) {
                // Nothing more to do if WhatsApp itself is unreachable.
            }
        }
    }

    private WhatsAppUser touch(WebhookPayload.Message m) {
        String firstText = m.text() != null ? m.text().body() : m.image() != null ? m.image().caption() : null;
        WhatsAppUser user = users.findById(m.from())
                .orElseGet(() -> new WhatsAppUser(m.from(), Replies.detectLanguage(firstText)));
        user.touch();
        return users.save(user);
    }

    private void onText(WhatsAppUser user, String text) {
        String lang = user.getLanguage();
        String phone = user.getPhone();
        switch (Replies.parse(text)) {
            case Replies.SetLanguage(String language) -> {
                user.setLanguage(language);
                users.save(user);
                reply(user, Replies.languageSet(language));
            }
            case Replies.ShowCabinet() -> reply(user, CabinetMessages.list(cabinet.list(phone), lang));
            case Replies.Remove(int position) -> cabinet.remove(phone, position).ifPresentOrElse(
                    item -> reply(user, Replies.removed(item.displayName(), lang)),
                    () -> reply(user, Replies.noSuchItem(position, lang)));
            case Replies.DeleteMyData() -> {
                cabinet.clear(phone);
                users.delete(user);
                reply(user, Replies.dataDeleted(lang));
            }
            case Replies.CheckBatch(String batchNo, String name) -> runCheck(user, null,
                    () -> checks.checkTyped(StripReading.manual(name, null, batchNo, null), lang, event -> { }));
            case Replies.Help() -> reply(user, Replies.help(lang));
        }
    }

    private void checkPhoto(WhatsAppUser user, WebhookPayload.Media media) {
        if (media == null || media.id() == null) {
            reply(user, Replies.help(user.getLanguage()));
            return;
        }
        runCheck(user, media.caption(), () -> {
            WhatsAppClient.Media photo = client.download(media.id(), MAX_PHOTO_BYTES);
            String type = photo.mimeType() == null ? "image/jpeg" : photo.mimeType();
            String dataUrl;
            try {
                dataUrl = Images.toDataUrl(photo.bytes(), type);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return checks.checkPhoto(dataUrl, user.getLanguage(), event -> { });
        });
    }

    private void runCheck(WhatsAppUser user, String label, Supplier<CheckResult> check) {
        String lang = user.getLanguage();
        if (!limiter.tryAcquire("wa:" + user.getPhone())) {
            reply(user, Replies.limitReached(lang));
            return;
        }
        reply(user, Replies.checking(lang));
        CheckResult result;
        try {
            result = check.get();
        } catch (WhatsAppClient.MediaTooLargeException | Images.ImageTooLargeException e) {
            reply(user, Replies.tooLarge(lang));
            return;
        }
        StripReading reading = result.reading();
        log.info("WhatsApp check finished: {}", result.verdict());
        String medicine = reading.brandName() != null ? reading.brandName() : reading.genericName();
        reply(user, result.whatsappMessage() != null ? result.whatsappMessage()
                : Replies.fallback(result.verdict(), medicine, reading.batchNo(), lang));

        if (result.verdict() != Verdict.UNREADABLE && !BatchKeys.strict(reading.batchNo()).isEmpty()) {
            cabinet.save(user.getPhone(), reading, label).ifPresentOrElse(
                    item -> reply(user, CabinetMessages.saved(item, lang)),
                    () -> reply(user, Replies.cabinetFull(lang)));
        }
    }

    private void reply(WhatsAppUser user, String text) {
        try {
            client.sendText(user.getPhone(), text);
        } catch (RestClientException e) {
            log.warn("Could not send WhatsApp reply: {}", e.getMessage());
            throw e;
        }
    }
}
