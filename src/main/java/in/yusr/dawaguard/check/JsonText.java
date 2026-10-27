package in.yusr.dawaguard.check;

/** Models sometimes wrap JSON in prose or code fences; pull out the first top-level object. */
final class JsonText {

    private JsonText() {
    }

    static String firstObject(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Model returned no content");
        }
        int start = text.indexOf('{');
        if (start < 0) {
            throw new IllegalArgumentException("Model returned no JSON object");
        }
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return text.substring(start, i + 1);
            }
        }
        throw new IllegalArgumentException("Model returned an incomplete JSON object");
    }
}
