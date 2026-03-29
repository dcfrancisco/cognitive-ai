package ph.francisco.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ph.francisco.perception.Observation;
import ph.francisco.memory.CuratedMemoryService;
import ph.francisco.memory.ConversationMemoryService;
import ph.francisco.cognition.DecisionEngine;
import ph.francisco.cognition.DecisionEngineResult;
import ph.francisco.agents.AgentOrchestrator;
import ph.francisco.agents.AgentResponse;

import java.util.Scanner;
import java.util.UUID;

@Component
public class CognitiveLoopRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CognitiveLoopRunner.class);

    private final CuratedMemoryService curatedMemoryService;
    private final ConversationMemoryService conversationMemoryService;
    private final DecisionEngine decisionEngine;
    private final AgentOrchestrator agentOrchestrator;

    public CognitiveLoopRunner(
            CuratedMemoryService curatedMemoryService,
            ConversationMemoryService conversationMemoryService,
            DecisionEngine decisionEngine,
            AgentOrchestrator agentOrchestrator) {
        this.curatedMemoryService = curatedMemoryService;
        this.conversationMemoryService = conversationMemoryService;
        this.decisionEngine = decisionEngine;
        this.agentOrchestrator = agentOrchestrator;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("CognitiveLoopRunner starting — interactive console enabled.");
        Scanner scanner = new Scanner(System.in);
        String sessionId = UUID.randomUUID().toString();

        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                Thread.sleep(100);
                continue;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty())
                continue;

            Observation observation = new Observation("console", line, null, sessionId);

            log.info("Observation received: {}", line);
            // persist observation in curated memory (existing memory pipeline)
            curatedMemoryService.observe(observation);

            DecisionEngineResult result = decisionEngine.evaluate(observation);
            log.info("Decision result: {}", result.cognitionDecision().type());

            if (result.cognitionDecision().type() == ph.francisco.cognition.CognitionDecision.DecisionType.SILENCE) {
                log.info("Decision is SILENCE — skipping response generation.");
                continue;
            }

            log.info("Routed intent: {}", result.intent().name());
            AgentResponse agentResponse = agentOrchestrator.handle(result.intent(), observation);
            log.info("Agent selected: {}", agentResponse.agent());
            log.info("Response generated: {}", agentResponse.message());

            System.out.println("User: " + line);
            System.out.println("Avery: " + agentResponse.message());

            // store conversation entry for quick retrieval
            conversationMemoryService.store(observation, agentResponse);
        }
    }
}
