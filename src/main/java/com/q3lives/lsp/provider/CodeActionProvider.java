package com.q3lives.lsp.provider;

import com.q3lives.compiler.context.SemanticContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import java.util.List;

/**
 * 代码操作提供器基类
 */
public abstract class CodeActionProvider {

    protected final SemanticContext semanticContext;

    public CodeActionProvider(SemanticContext semanticContext) {
        this.semanticContext = semanticContext;
    }

    /**
     * 提供代码操作
     */
    public abstract List<CodeAction> provideCodeActions(CodeActionParams params);
}
