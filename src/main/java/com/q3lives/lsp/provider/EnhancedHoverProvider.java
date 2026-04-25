package com.q3lives.lsp.provider;

import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.lsp.provider.CompletionProvider;

/**
 * 增强版悬停信息提供器
 */
public class EnhancedHoverProvider extends HoverProvider {

    public EnhancedHoverProvider(SemanticContext semanticContext, CompletionProvider completionProvider) {
        super(semanticContext, completionProvider);
    }
}
