package in.yusr.dawaguard.check;

import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses the many ways Indian packs print expiry: "SEP.2027", "Sep-27", "09/2027", "9/27". */
public final class ExpiryDates {

    private static final List<String> MONTHS = List.of("JAN", "FEB", "MAR", "APR", "MAY", "JUN",
            "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");
    private static final Pattern NAMED = Pattern.compile("([A-Z]{3})[A-Z]*[^A-Z0-9]*(\\d{2,4})");
    private static final Pattern NUMERIC = Pattern.compile("(\\d{1,2})\\s*[/.\\-]\\s*(\\d{2,4})");

    private ExpiryDates() {
    }

    public static Optional<YearMonth> parse(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String s = text.toUpperCase(Locale.ROOT);
        Matcher named = NAMED.matcher(s);
        while (named.find()) {
            int month = MONTHS.indexOf(named.group(1)) + 1;
            if (month > 0) {
                return Optional.of(YearMonth.of(year(named.group(2)), month));
            }
        }
        Matcher numeric = NUMERIC.matcher(s);
        if (numeric.find()) {
            int month = Integer.parseInt(numeric.group(1));
            if (month >= 1 && month <= 12) {
                return Optional.of(YearMonth.of(year(numeric.group(2)), month));
            }
        }
        return Optional.empty();
    }

    private static int year(String digits) {
        int y = Integer.parseInt(digits);
        return digits.length() == 2 ? 2000 + y : y;
    }
}
