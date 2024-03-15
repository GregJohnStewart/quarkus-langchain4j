package io.quarkiverse.langchain4j.bam.deployment;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.bam.BamRestApi;
import io.quarkiverse.langchain4j.bam.Message;
import io.quarkiverse.langchain4j.bam.Parameters;
import io.quarkiverse.langchain4j.bam.TextGenerationRequest;
import io.quarkiverse.langchain4j.bam.runtime.config.LangChain4jBamConfig;
import io.quarkus.test.QuarkusUnitTest;

public class AiChatServiceTest {

    static WireMockServer wireMockServer;
    static ObjectMapper mapper;
    static WireMockUtil mockServers;

    @RegisterExtension
    static QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .overrideRuntimeConfigKey("quarkus.langchain4j.bam.base-url", WireMockUtil.URL)
            .overrideRuntimeConfigKey("quarkus.langchain4j.bam.api-key", WireMockUtil.API_KEY)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(WireMockUtil.class));

    @BeforeAll
    static void beforeAll() {
        wireMockServer = new WireMockServer(options().port(WireMockUtil.PORT));
        wireMockServer.start();
        mapper = BamRestApi.objectMapper(new ObjectMapper());
        mockServers = new WireMockUtil(wireMockServer);
    }

    @AfterAll
    static void afterAll() {
        wireMockServer.stop();
    }

    @RegisterAiService
    @Singleton
    interface NewAIService {

        @SystemMessage("This is a systemMessage")
        @UserMessage("This is a userMessage {text}")
        String chat(String text);
    }

    @Inject
    NewAIService service;

    @Inject
    ChatLanguageModel chatModel;

    @Inject
    LangChain4jBamConfig langchain4jBamConfig;

    @Test
    void chat() throws Exception {
        var config = langchain4jBamConfig.defaultConfig();
        var modelId = config.chatModel().modelId();
        var parameters = Parameters.builder()
                .decodingMethod(config.chatModel().decodingMethod())
                .temperature(config.chatModel().temperature())
                .minNewTokens(config.chatModel().minNewTokens())
                .maxNewTokens(config.chatModel().maxNewTokens())
                .build();

        List<Message> messages = List.of(
                new Message("system", "This is a systemMessage"),
                new Message("user", "This is a userMessage Hello"));

        var body = new TextGenerationRequest(modelId, messages, parameters);

        mockServers
                .mockBuilder(WireMockUtil.URL_CHAT_API, 200)
                .body(mapper.writeValueAsString(body))
                .response("""
                        {
                            "results": [
                                {
                                    "generated_token_count": 20,
                                    "input_token_count": 146,
                                    "stop_reason": "max_tokens",
                                    "seed": 40268626,
                                    "generated_text": "AI Response"
                                }
                            ]
                        }
                        """)
                .build();

        assertEquals("AI Response", service.chat("Hello"));
    }

    @Test
    void chat_test_generate_1() throws Exception {
        var config = langchain4jBamConfig.defaultConfig();
        var modelId = config.chatModel().modelId();
        var parameters = Parameters.builder()
                .decodingMethod(config.chatModel().decodingMethod())
                .temperature(config.chatModel().temperature())
                .minNewTokens(config.chatModel().minNewTokens())
                .maxNewTokens(config.chatModel().maxNewTokens())
                .build();

        List<Message> messages = List.of(
                new Message("user", "Hello"));

        var body = new TextGenerationRequest(modelId, messages, parameters);

        mockServers
                .mockBuilder(WireMockUtil.URL_CHAT_API, 200)
                .body(mapper.writeValueAsString(body))
                .response("""
                        {
                            "results": [
                                {
                                    "generated_token_count": 20,
                                    "input_token_count": 146,
                                    "stop_reason": "max_tokens",
                                    "seed": 40268626,
                                    "generated_text": "AI Response"
                                }
                            ]
                        }
                        """)
                .build();

        assertEquals("AI Response", chatModel.generate("Hello"));
    }

    @Test
    void chat_test_generate_2() throws Exception {
        var config = langchain4jBamConfig.defaultConfig();
        var modelId = config.chatModel().modelId();
        var parameters = Parameters.builder()
                .decodingMethod(config.chatModel().decodingMethod())
                .temperature(config.chatModel().temperature())
                .minNewTokens(config.chatModel().minNewTokens())
                .maxNewTokens(config.chatModel().maxNewTokens())
                .build();

        List<Message> messages = List.of(
                new Message("system", "This is a systemMessage"),
                new Message("user", "This is a userMessage"),
                new Message("assistant", "This is a assistantMessage"));

        var body = new TextGenerationRequest(modelId, messages, parameters);

        mockServers
                .mockBuilder(WireMockUtil.URL_CHAT_API, 200)
                .body(mapper.writeValueAsString(body))
                .response("""
                        {
                            "results": [
                                {
                                    "generated_token_count": 20,
                                    "input_token_count": 146,
                                    "stop_reason": "max_tokens",
                                    "seed": 40268626,
                                    "generated_text": "AI Response"
                                }
                            ]
                        }
                        """)
                .build();

        var expected = Response.from(AiMessage.from("AI Response"), new TokenUsage(146, 20, 166), FinishReason.LENGTH);
        assertEquals(expected, chatModel.generate(List.of(
                new dev.langchain4j.data.message.SystemMessage("This is a systemMessage"),
                new dev.langchain4j.data.message.UserMessage("This is a userMessage"),
                new dev.langchain4j.data.message.AiMessage("This is a assistantMessage"))));
        assertEquals(expected, chatModel.generate(
                new dev.langchain4j.data.message.SystemMessage("This is a systemMessage"),
                new dev.langchain4j.data.message.UserMessage("This is a userMessage"),
                new dev.langchain4j.data.message.AiMessage("This is a assistantMessage")));
    }

    @Test
    void chat_test_tool_specification() throws Exception {

        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> chatModel.generate(List.of(), ToolSpecification.builder().build()));

        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> chatModel.generate(List.of(), List.of(ToolSpecification.builder().build())));
    }
}
