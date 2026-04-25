package com.q3lives.lsp.provider;

import com.q3lives.compiler.context.SemanticContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import java.util.ArrayList;
import java.util.List;

/**
 * 增强版代码操作提供器
 */
public class EnhancedCodeActionProvider extends CodeActionProvider {

    public EnhancedCodeActionProvider(SemanticContext semanticContext) {
        super(semanticContext);
    }

    @Override
    public List<CodeAction> provideCodeActions(CodeActionParams params) {
        // 基础实现，返回空列表
        return new ArrayList<>();
    }
}
