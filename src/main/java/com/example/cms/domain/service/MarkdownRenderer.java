package com.example.cms.domain.service;

import com.example.cms.domain.model.article.RenderedContent;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import org.springframework.stereotype.Service;

@Service
public class MarkdownRenderer {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownRenderer() {
        this.parser = Parser.builder().build();
        this.renderer = HtmlRenderer.builder().build();
    }

    public RenderedContent render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return RenderedContent.of("");
        }

        Document document = parser.parse(markdown);
        String html = renderer.render(document);

        // 基本的 HTML 清理
        html = sanitizeHtml(html);

        return RenderedContent.of(html);
    }

    private String sanitizeHtml(String html) {
        // 移除潜在危险的标签和属性（简化版）
        html = html.replaceAll("<script[^>]*>.*?</script>", "");
        html = html.replaceAll("on\\w+\\s*=\\s*[\"'][^\"']*[\"']", "");
        return html;
    }
}
