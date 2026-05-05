package com.omniquery.rag.vector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PgVectorLiteralTest {

    @Test
    void formatsVectorWithStableLocale() {
        assertEquals("[0.10000000,2.50000000,-3.00000000]", PgVectorLiteral.from(new float[]{0.1f, 2.5f, -3f}));
    }
}
