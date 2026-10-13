import java.util.regex.Pattern;

public final class WeatherVoiceInput {
    private static final Pattern CITY_PREFIX = Pattern.compile(
            "^(?:(?:what is|what's|tell me|show me|get|check)\\s+)?"
                    + "(?:the\\s+)?"
                    + "(?:weather(?:\\s+(?:like|forecast))?|forecast|temperature)"
                    + "\\s+(?:in|for|at)\\s+",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CITY_SUFFIX = Pattern.compile(
            "\\s+(?:(?:weather|forecast)(?:\\s+(?:for\\s+)?today)?"
                    + "|today|tomorrow|please)$",
            Pattern.CASE_INSENSITIVE);

    private WeatherVoiceInput() {
    }

    public static String normalizeCityRequest(String request) {
        if (request == null) {
            return "";
        }
        String city = request.trim()
                .replaceAll("^[\\s,.;:!?]+|[\\s,.;:!?]+$", "");
        city = CITY_PREFIX.matcher(city).replaceFirst("");
        city = CITY_SUFFIX.matcher(city).replaceFirst("");
        return city.replaceAll("^[\\s,.;:!?]+|[\\s,.;:!?]+$", "").trim();
    }
}
