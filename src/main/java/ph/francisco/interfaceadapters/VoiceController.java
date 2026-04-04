package ph.francisco.interfaceadapters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ph.francisco.agents.VoiceService;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/voice")
public class VoiceController {

    private static final Logger log = LoggerFactory.getLogger(VoiceController.class);

    private final VoiceService voiceService;

    public VoiceController(VoiceService voiceService) {
        this.voiceService = voiceService;
    }

    /**
     * Transcribes an audio file to text using OpenAI Whisper.
     * Accepts multipart/form-data with field name "audio".
     * Returns {"text": "..."} or 503 if not configured.
     */
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> transcribe(@RequestParam("audio") MultipartFile audio) {
        if (audio.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Audio file is empty"));
        }
        log.info("Transcription request: {} bytes, contentType={}", audio.getSize(), audio.getContentType());

        Optional<String> result = voiceService.transcribe(audio);
        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Transcription service not configured"));
        }
        return ResponseEntity.ok(Map.of("text", result.get()));
    }

    /**
     * Converts text to speech using OpenAI TTS.
     * Accepts {"text": "..."}, returns audio/mpeg bytes.
     * Returns 503 if not configured.
     */
    @PostMapping(value = "/speak", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> speak(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("TTS request: {} chars", text.length());

        Optional<byte[]> result = voiceService.speak(text);
        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(result.get());
    }
}
