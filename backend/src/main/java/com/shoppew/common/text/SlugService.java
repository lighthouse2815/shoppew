package com.shoppew.common.text;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class SlugService {

    public String normalize(String value) {
        String source = value.strip().toLowerCase(Locale.ROOT).replace('đ', 'd');
        String withoutMarks = Normalizer.normalize(source, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return withoutMarks
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-{2,}", "-");
    }
}
