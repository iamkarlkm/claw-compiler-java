package com.q3lives.lsp.provider;

import com.q3lives.compiler.context.SemanticContext;

/**
 * 增强版文档符号提供器
 */
public class EnhancedDocumentSymbolProvider extends DocumentSymbolProvider {

    public EnhancedDocumentSymbolProvider(SemanticContext semanticContext) {
        super(semanticContext);
    }
}
