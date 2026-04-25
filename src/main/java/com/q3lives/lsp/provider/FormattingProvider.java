package com.q3lives.lsp.provider;

import com.q3lives.compiler.context.SemanticContext;
import org.eclipse.lsp4j.TextEdit;
import java.util.List;

/**
 * 文档格式化提供器基类
 */
public abstract class FormattingProvider {

    protected final SemanticContext semanticContext;

    public FormattingProvider(SemanticContext semanticContext) {
        this.semanticContext = semanticContext;
    }

    /**
     * 提供文档格式化
     */
    public abstract List<TextEdit> provideFormatting(String document);
}
