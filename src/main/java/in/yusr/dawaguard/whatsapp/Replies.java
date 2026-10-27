package in.yusr.dawaguard.whatsapp;

import in.yusr.dawaguard.check.Explainer;
import in.yusr.dawaguard.check.Verdict;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The bot's fixed replies, in English and Hindi, and the parsing of typed commands. */
final class Replies {

    private static final Pattern DEVANAGARI = Pattern.compile("\\p{IsDevanagari}");
    private static final Pattern REMOVE = Pattern.compile("^(?:remove|delete|हटाएँ|हटाओ|हटाएं)\\s+(\\d{1,2})$");
    private static final Pattern BATCH_WORD = Pattern.compile("(?i)^(batch|b\\.?\\s?no\\.?|बैच)[:#]?$");

    private Replies() {
    }

    sealed interface Command {
    }

    record Help() implements Command {
    }

    record SetLanguage(String language) implements Command {
    }

    record ShowCabinet() implements Command {
    }

    record Remove(int position) implements Command {
    }

    record DeleteMyData() implements Command {
    }

    /** A typed batch number, with whatever else was typed taken as the medicine name. */
    record CheckBatch(String batchNo, String medicineName) implements Command {
    }

    static Command parse(String text) {
        String t = text == null ? "" : text.strip();
        String lower = t.toLowerCase(Locale.ROOT);
        switch (lower) {
            case "hindi", "हिंदी", "हिन्दी" -> {
                return new SetLanguage("hi");
            }
            case "english", "अंग्रेज़ी", "अंग्रेजी" -> {
                return new SetLanguage("en");
            }
            case "cabinet", "my cabinet", "my medicines", "medicines", "list", "अलमारी", "दवाइयाँ", "दवाइयां" -> {
                return new ShowCabinet();
            }
            case "delete my data", "stop" -> {
                return new DeleteMyData();
            }
            default -> {
            }
        }
        Matcher remove = REMOVE.matcher(lower);
        if (remove.matches()) {
            return new Remove(Integer.parseInt(remove.group(1)));
        }
        return batch(t).map(Command.class::cast).orElseGet(Help::new);
    }

    /**
     * Finds a batch number in free text such as "TG25-1814A Pansid-40" or "batch: TG25-1814A of Pansid".
     * After a "batch" keyword, the next word is the batch; otherwise the word with the most digits is.
     */
    static Optional<CheckBatch> batch(String text) {
        List<String> words = new ArrayList<>(Arrays.asList(text.strip().split("\\s+")));
        int pick = -1;
        for (int i = 0; i < words.size() - 1; i++) {
            if (BATCH_WORD.matcher(words.get(i)).matches()) {
                words.remove(i);
                pick = i;
                break;
            }
        }
        if (pick < 0) {
            int bestDigits = 0;
            for (int i = 0; i < words.size(); i++) {
                int digits = (int) words.get(i).chars().filter(Character::isDigit).count();
                if (digits > bestDigits) {
                    bestDigits = digits;
                    pick = i;
                }
            }
        }
        if (pick < 0) {
            return Optional.empty();
        }
        String batch = words.get(pick).replaceAll("^[^A-Za-z0-9]+|[^A-Za-z0-9]+$", "");
        if (batch.length() < 3 || batch.length() > 30 || batch.chars().noneMatch(Character::isDigit)) {
            return Optional.empty();
        }
        words.remove(pick);
        String name = String.join(" ", words).replaceAll("(?i)\\b(of|for|the)\\b", " ").replaceAll("\\s+", " ").strip();
        return Optional.of(new CheckBatch(batch, name.isEmpty() ? null : name));
    }

    static String detectLanguage(String text) {
        return text != null && DEVANAGARI.matcher(text).find() ? "hi" : "en";
    }

    static String help(String lang) {
        if ("hi".equals(lang)) {
            return """
                    नमस्ते 🙏 मैं *DawaGuard* हूँ।

                    दवा की स्ट्रिप की साफ़ फ़ोटो भेजें, जिसमें बैच नंबर दिखे। मैं उसे CDSCO के अलर्ट से मिलाऊँगा, \
                    यानी वे दवाइयाँ जो सरकारी गुणवत्ता जाँच में फ़ेल हुईं या जिनके नकली होने का शक है, \
                    और उसे आपकी परिवार की दवा अलमारी में सेव कर दूँगा।

                    फ़ोटो नहीं है? बैच नंबर लिखें, जैसे: *TG25-1814A Pansid-40*

                    सेव की गई दवाइयाँ देखने के लिए *cabinet* लिखें। अंग्रेज़ी के लिए *English* लिखें।""";
        }
        return """
                Namaste 🙏 I'm *DawaGuard*.

                Send me a clear photo of a medicine strip that shows the batch number. I'll check it against \
                CDSCO's alerts for medicines that failed government quality tests or are suspected fakes, \
                and save it to your family medicine cabinet.

                No photo? Type the batch number, for example: *TG25-1814A Pansid-40*

                Reply *cabinet* to see saved medicines, or *हिंदी* for Hindi.""";
    }

    static String languageSet(String lang) {
        return "hi".equals(lang) ? "ठीक है, अब मैं हिंदी में जवाब दूँगा।\n\n" + help(lang)
                : "Okay, I'll reply in English from now on.\n\n" + help(lang);
    }

    static String checking(String lang) {
        return "hi".equals(lang) ? "🔍 इस दवा को CDSCO अलर्ट से मिला रहा हूँ। लगभग 15 सेकंड लगेंगे।"
                : "🔍 Checking this medicine against CDSCO alerts. This takes about 15 seconds.";
    }

    static String failed(String lang) {
        return "hi".equals(lang) ? "यह जाँच पूरी नहीं हो पाई। एक मिनट बाद फिर कोशिश करें।"
                : "I couldn't finish this check. Please try again in a minute.";
    }

    static String tooLarge(String lang) {
        return "hi".equals(lang) ? "यह फ़ोटो बहुत बड़ी है। स्ट्रिप की सामान्य फ़ोन फ़ोटो भेजें।"
                : "This photo is too large. Send a normal phone photo of the strip.";
    }

    static String limitReached(String lang) {
        return "hi".equals(lang) ? "इस मुफ़्त डेमो की जाँच सीमा अभी पूरी हो गई है। एक घंटे बाद फिर कोशिश करें।"
                : "This free demo has reached its limit of checks for now. Please try again in an hour.";
    }

    static String removed(String name, String lang) {
        return "hi".equals(lang) ? name + " को आपकी दवा अलमारी से हटा दिया।" : "Removed " + name + " from your cabinet.";
    }

    static String noSuchItem(int position, String lang) {
        return "hi".equals(lang) ? "आपकी अलमारी में " + position + " नंबर की कोई दवा नहीं है। सूची देखने के लिए *cabinet* लिखें।"
                : "There is no medicine number " + position + " in your cabinet. Reply *cabinet* to see the list.";
    }

    static String cabinetFull(String lang) {
        return "hi".equals(lang) ? "आपकी दवा अलमारी भर गई है। नई दवा सेव करने के लिए *remove 1* जैसे किसी पुरानी दवा को हटाएँ।"
                : "Your cabinet is full. Remove an old medicine (for example *remove 1*) to save new ones.";
    }

    static String dataDeleted(String lang) {
        return "hi".equals(lang) ? "आपकी सेव की गई दवाइयाँ और सेटिंग्स हटा दी गईं। फिर से शुरू करने के लिए कभी भी फ़ोटो भेजें।"
                : "Deleted your saved medicines and settings. Send a photo any time to start again.";
    }

    /** Used only if the message model failed: the verdict and next step are still exact. */
    static String fallback(Verdict verdict, String medicine, String batchNo, String lang) {
        boolean hi = "hi".equals(lang);
        String subject = (medicine == null ? "" : medicine + ", ") + (batchNo == null ? "" : (hi ? "बैच " : "batch ") + batchNo);
        String finding = switch (verdict) {
            case SUSPECTED_FAKE -> hi ? "CDSCO की संदिग्ध नकली दवा सूची में है।" : "is on CDSCO's suspected fake (spurious) list.";
            case FAILED_QUALITY_TEST -> hi ? "CDSCO की गुणवत्ता जाँच में फ़ेल हुआ।" : "failed a CDSCO quality test.";
            case POSSIBLE_MATCH -> hi ? "CDSCO अलर्ट वाले एक बैच से बहुत मिलता-जुलता है।" : "is very close to a batch on a CDSCO alert.";
            case EXPIRED -> hi ? "एक्सपायर हो चुकी है।" : "is past its expiry date.";
            case NEEDS_BATCH_NUMBER, UNREADABLE -> hi ? "जाँच नहीं हो सकी, बैच नंबर पढ़ा नहीं गया।"
                    : "could not be checked: no batch number was readable.";
            case NOT_FLAGGED -> hi ? "किसी CDSCO अलर्ट में नहीं है।" : "is not on any CDSCO alert.";
        };
        String body = subject.isEmpty() ? (hi ? "यह दवा " : "This medicine ") + finding
                : subject + (hi ? ": " : " ") + finding;
        return Explainer.emojiFor(verdict) + " " + body + "\n" + Explainer.Facts.messageNextStep(verdict, lang);
    }
}
