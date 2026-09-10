package com.engine.paper.template;

import com.engine.paper.domain.model.Document;

/**
 * High-level compiler converting templates and JSON data sets into fully bound Document ASTs.
 */
public class TemplateCompiler {

    private final JsonDataBinder dataBinder;
    private final MarkdownTemplateParser markdownParser;

    public TemplateCompiler() {
        this.dataBinder = new JsonDataBinder();
        this.markdownParser = new MarkdownTemplateParser();
    }

    public TemplateCompiler(JsonDataBinder dataBinder, MarkdownTemplateParser markdownParser) {
        this.dataBinder = dataBinder != null ? dataBinder : new JsonDataBinder();
        this.markdownParser = markdownParser != null ? markdownParser : new MarkdownTemplateParser();
    }

    /**
     * Compiles a template and JSON context into a renderable Document model.
     */
    public Document compile(String templateMarkdown, String jsonContext) {
        String boundMarkdown = dataBinder.bind(templateMarkdown, jsonContext);
        return markdownParser.parse(boundMarkdown);
    }
}
