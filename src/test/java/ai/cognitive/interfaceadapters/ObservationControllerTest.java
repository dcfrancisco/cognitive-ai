package ai.cognitive.interfaceadapters;

import ai.cognitive.agents.AgentOrchestrator;
import ai.cognitive.agents.AgentResponse;
import ai.cognitive.agents.CognitiveIntent;
import ai.cognitive.cognition.DecisionEngine;
import ai.cognitive.cognition.DecisionEngineResult;
import ai.cognitive.cognition.CognitionDecision;
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
        DecisionEngine decisionEngine;

        @MockBean
        AgentOrchestrator agentOrchestrator;

    @Test
    void returns204WhenDecisionIsSilence() throws Exception {
        doNothing().when(curatedMemoryService).observe(any());
        when(decisionEngine.evaluate(any())).thenReturn(
                new DecisionEngineResult(
                        new CognitionDecision(CognitionDecision.DecisionType.SILENCE, 0.6, List.of("Defaulting")),
                        CognitiveIntent.GENERAL_RESPONSE,
                        List.of("routed")
                ));

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
        when(decisionEngine.evaluate(any())).thenReturn(
                new DecisionEngineResult(
                        new CognitionDecision(CognitionDecision.DecisionType.SPEAK, 0.9, List.of("Direct question detected")),
                        CognitiveIntent.REFLECTION,
                        List.of("routed")
                ));

        when(agentOrchestrator.handle(any(), any())).thenReturn(
                new AgentResponse("ReflectionAgent", "I think you asked a question.", List.of("routed to reflection")));

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
