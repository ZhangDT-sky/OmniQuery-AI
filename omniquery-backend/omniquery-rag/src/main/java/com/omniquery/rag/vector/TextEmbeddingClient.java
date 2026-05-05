package com.omniquery.rag.vector;

public interface TextEmbeddingClient {

    float[] embed(String text);
}
