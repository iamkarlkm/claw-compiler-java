package com.q3lives.lsp.provider;

import com.q3lives.compiler.context.SemanticContext;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import java.util.List;

/**
 * 增强版引用提供器
 */
public class EnhancedReferenceProvider extends ReferenceProvider {

    public EnhancedReferenceProvider(SemanticContext semanticContext) {
        super(semanticContext);
    }

    /**
     * 提供引用（与LanguageServer调用的方法签名匹配）
     */
    public List<? extends Location> provideReferences(String document, Position position) {
        return findReferences(document, position);
    }
}
