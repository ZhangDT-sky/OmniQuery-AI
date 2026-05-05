package com.omniquery.rag.vector;

import java.util.Locale;

public final class PgVectorLiteral {

    private PgVectorLiteral() {
    }

    public static String from(float[] vector) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(String.format(Locale.ROOT, "%.8f", vector[i]));
        }
        return builder.append(']').toString();
    }
}
