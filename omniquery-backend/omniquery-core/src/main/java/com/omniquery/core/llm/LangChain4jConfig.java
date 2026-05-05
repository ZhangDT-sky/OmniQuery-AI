package com.omniquery.core.llm;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("llm")
public class LangChain4jConfig {

    @Bean
    ChatModel chatModel(
        @Value("${omniquery.llm.api-key}") String apiKey,
        @Value("${omniquery.llm.base-url}") String baseUrl,
        @Value("${omniquery.llm.model-name}") String modelName
    ) {
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(modelName)
            .temperature(0.0)
            .build();
    }

    @Bean
    Nl2SqlAssistant nl2SqlAssistant(ChatModel chatModel, SchemaTools schemaTools) {
        return AiServices.builder(Nl2SqlAssistant.class)
            .chatModel(chatModel)
            .tools(schemaTools)
            .build();
    }
}
