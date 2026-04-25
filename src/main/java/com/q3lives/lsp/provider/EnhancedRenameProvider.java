package com.q3lives.lsp.provider;

import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.lsp.provider.CompletionProvider;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.WorkspaceEdit;

/**
 * 增强版重命名提供器
 */
public class EnhancedRenameProvider extends RenameProvider {

    public EnhancedRenameProvider(SemanticContext semanticContext, CompletionProvider completionProvider) {
        super(semanticContext, completionProvider);
    }

    /**
     * 提供重命名（与LanguageServer调用的方法签名匹配）
     */
    public WorkspaceEdit provideRename(String document, Position position, String newName) {
        return rename(document, position.getLine(), position.getCharacter(), newName);
    }
}
