package com.omniquery.core.session;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class CorrectionDetector {

    private static final Pattern CORRECTION_PATTERN = Pattern.compile(
        "(刚才|上一个|上一条|不是|改成|换成|只看|仅看|加上|去掉|按.*排序|sort by|only|instead|change to)",
        Pattern.CASE_INSENSITIVE
    );

    public boolean isCorrection(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        return CORRECTION_PATTERN.matcher(question.toLowerCase(Locale.ROOT)).find();
    }
}
