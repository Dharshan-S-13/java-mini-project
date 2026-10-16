import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class WindowsSpeechAssistant {
    private static final String RECOGNITION_SCRIPT = String.join("\n",
            "$ErrorActionPreference = 'Stop'",
            "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8",
            "Add-Type -AssemblyName System.Speech",
            "$recognizer = New-Object System.Speech.Recognition.SpeechRecognitionEngine",
            "$speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer",
            "try {",
            "  $speaker.Speak('Say a city name or ask for the weather in a city.')",
            "  $recognizer.LoadGrammar((New-Object System.Speech.Recognition.DictationGrammar))",
            "  $recognizer.SetInputToDefaultAudioDevice()",
            "  $result = $recognizer.Recognize([TimeSpan]::FromSeconds(12))",
            "  if ($null -ne $result) { [Console]::WriteLine($result.Text) }",
            "} finally {",
            "  $recognizer.Dispose()",
            "  $speaker.Dispose()",
            "}");
    private static final long RECOGNITION_TIMEOUT_SECONDS = 25;
    private static final long SPEECH_TIMEOUT_SECONDS = 35;

    private WindowsSpeechAssistant() {
    }

    public static String listenForCity() throws IOException, InterruptedException {
        requireWindows();
        return runPowerShell(RECOGNITION_SCRIPT, RECOGNITION_TIMEOUT_SECONDS).trim();
    }

    public static void speak(String text) throws IOException, InterruptedException {
        requireWindows();
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Speech text cannot be empty.");
        }

        String encodedText = Base64.getEncoder().encodeToString(
                text.getBytes(StandardCharsets.UTF_8));
        String script = String.join("\n",
                "$ErrorActionPreference = 'Stop'",
                "$text = [System.Text.Encoding]::UTF8.GetString("
                        + "[System.Convert]::FromBase64String('"
                        + encodedText + "'))",
                "Add-Type -AssemblyName System.Speech",
                "$speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer",
                "try { $speaker.Speak($text) } finally { $speaker.Dispose() }");
        runPowerShell(script, SPEECH_TIMEOUT_SECONDS);
    }

    private static String runPowerShell(String script, long timeoutSeconds)
            throws IOException, InterruptedException {
        String encodedScript = Base64.getEncoder().encodeToString(
                script.getBytes(StandardCharsets.UTF_16LE));
        Process process = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-EncodedCommand",
                encodedScript)
                .redirectErrorStream(true)
                .start();

        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor();
            throw new IOException("The Windows speech process timed out.");
        }

        String output = new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8).trim();
        if (process.exitValue() != 0) {
            String detail = output.isEmpty()
                    ? "PowerShell exited with code " + process.exitValue() + "."
                    : output;
            throw new IOException(detail);
        }
        return output;
    }

    private static void requireWindows() throws IOException {
        if (!System.getProperty("os.name").toLowerCase(Locale.ROOT)
                .contains("win")) {
            throw new IOException(
                    "Terminal voice input requires Windows speech recognition.");
        }
    }
}
