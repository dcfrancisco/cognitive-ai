package ph.francisco.agents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
public class VoiceService {

    private static final Logger log = LoggerFactory.getLogger(VoiceService.class);

    private final ObjectProvider<OpenAiAudioTranscriptionModel> transcriptionModelProvider;
    private final ObjectProvider<OpenAiAudioSpeechModel> speechModelProvider;

    public VoiceService(
            ObjectProvider<OpenAiAudioTranscriptionModel> transcriptionModelProvider,
            ObjectProvider<OpenAiAudioSpeechModel> speechModelProvider) {
        this.transcriptionModelProvider = transcriptionModelProvider;
        this.speechModelProvider = speechModelProvider;
    }

    public boolean isTranscriptionAvailable() {
        return transcriptionModelProvider.getIfAvailable() != null;
    }

    public boolean isSpeechAvailable() {
        return speechModelProvider.getIfAvailable() != null;
    }

    /**
     * Transcribes audio bytes using OpenAI Whisper.
     * Returns Optional.empty() if the transcription model is not configured.
     */
    public Optional<String> transcribe(MultipartFile audioFile) {
        OpenAiAudioTranscriptionModel model = transcriptionModelProvider.getIfAvailable();
        if (model == null) {
            log.debug("Transcription model not available — OpenAI key may not be configured");
            return Optional.empty();
        }
        try {
            byte[] bytes = audioFile.getBytes();
            String originalName = audioFile.getOriginalFilename();
            final String filename = (originalName != null && !originalName.isBlank()) ? originalName : "audio.webm";
            ByteArrayResource resource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
            String text = model.call(resource);
            log.debug("Transcribed {} bytes → \"{}\"", bytes.length, text);
            return Optional.ofNullable(text).filter(t -> !t.isBlank());
        } catch (IOException e) {
            log.error("Failed to read audio bytes for transcription", e);
            return Optional.empty();
        }
    }

    /**
     * Converts text to speech using OpenAI TTS.
     * Returns Optional.empty() if the speech model is not configured.
     */
    public Optional<byte[]> speak(String text) {
        OpenAiAudioSpeechModel model = speechModelProvider.getIfAvailable();
        if (model == null) {
            log.debug("Speech model not available — OpenAI key may not be configured");
            return Optional.empty();
        }
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        byte[] audio = model.call(text);
        log.debug("Generated {} bytes of TTS audio for text: \"{}\"", audio.length,
                text.length() > 60 ? text.substring(0, 60) + "…" : text);
        return Optional.of(audio);
    }
}
