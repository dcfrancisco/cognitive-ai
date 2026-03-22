package ai.cognitive.interfaceadapters;

import ai.cognitive.cognition.CognitionDecision;
import ai.cognitive.cognition.CognitionService;
import ai.cognitive.memory.CuratedMemoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ObservationController.class)
class ObservationControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    CuratedMemoryService curatedMemoryService;

    @MockBean
    CognitionService cognitionService;

    @Test
    void returns204WhenDecisionIsSilence() throws Exception {
        doNothing().when(curatedMemoryService).observe(any());
        when(cognitionService.evaluate(any())).thenReturn(
                new CognitionDecision(CognitionDecision.DecisionType.SILENCE, 0.6, List.of("Defaulting")));

        mvc.perform(post("/api/observe")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"source\":\"test\"," +
                        "\"content\":\"Just noting this.\"" +
                        "}"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void returns200WithReasonsWhenDecisionIsSpeak() throws Exception {
        doNothing().when(curatedMemoryService).observe(any());
        when(cognitionService.evaluate(any())).thenReturn(
                new CognitionDecision(CognitionDecision.DecisionType.SPEAK, 0.9, List.of("Direct question detected")));

        mvc.perform(post("/api/observe")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"source\":\"test\"," +
                        "\"content\":\"What should I do?\"" +
                        "}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
