package com.example.cms.domain.model.article;

import lombok.Value;

@Value
public class RenderedContent {
    String html;

    public static RenderedContent of(String html) {
        if (html == null || html.isBlank()) {
            return new RenderedContent("");
        }
        return new RenderedContent(html);
    }
}
