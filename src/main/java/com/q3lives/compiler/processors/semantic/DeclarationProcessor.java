// ==================== DeclarationProcessor.java ====================
package com.q3lives.compiler.processors.semantic;

import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.compiler.core.Token;
import com.q3lives.compiler.core.TokenType;
import com.q3lives.compiler.hierarchy.HierarchicalBlocks;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 声明处理器 - 处理 import, export, const, var
 */
@Slf4j
public class DeclarationProcessor {


    @Getter
    private final List<DeclarationInfo> declarations = new ArrayList<>();

    public void processTokens(List<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            switch (token.getType()) {
                case KW_IMPORT -> declarations.add(parseImport(tokens, i));
                case KW_EXPORT -> declarations.add(new DeclarationInfo("export", null, null, token.getLine()));
                case KW_VAR -> declarations.add(parseVarConst(tokens, i, false));
                case KW_CONST -> declarations.add(parseVarConst(tokens, i, true));
                default -> {}
            }
        }
        log.debug("识别 {} 个声明", declarations.size());
    }

    private DeclarationInfo parseImport(List<Token> tokens, int index) {
        StringBuilder path = new StringBuilder();
        int i = index + 1;
        while (i < tokens.size() && tokens.get(i).getType() != TokenType.NEWLINE &&
               tokens.get(i).getType() != TokenType.EOF) {
            path.append(tokens.get(i).getValue());
            i++;
        }
        return new DeclarationInfo("import", path.toString().trim(), null, tokens.get(index).getLine());
    }

    private DeclarationInfo parseVarConst(List<Token> tokens, int index, boolean isConst) {
        String kind = isConst ? "const" : "var";
        String name = null;
        String type = null;

        if (index + 1 < tokens.size() && tokens.get(index + 1).getType() == TokenType.IDENTIFIER) {
            name = tokens.get(index + 1).getValue();
        }
        // 检查类型注解
        if (index + 2 < tokens.size() && tokens.get(index + 2).getType() == TokenType.OP_COLON) {
            if (index + 3 < tokens.size()) {
                type = tokens.get(index + 3).getValue();
            }
        }

        return new DeclarationInfo(kind, name, type, tokens.get(index).getLine());
    }

    public record DeclarationInfo(String kind, String name, String type, int line) {}

    public Object process(List<Token> tokens, HierarchicalBlocks blocks, SemanticContext ctx) {
        for (DeclarationInfo declaration : declarations) {
            if (declaration.name() == null) {
                continue;
            }
            String typeName = declaration.type() != null && !declaration.type().isBlank()
                    ? declaration.type() : "Any";
            SemanticContext.TypeInfo typeInfo = ctx.hasType(typeName)
                    ? ctx.resolveType(typeName)
                    : new SemanticContext.TypeInfo(typeName, SemanticContext.TypeInfo.TypeKind.USER_DEFINED);
            SemanticContext.VariableDeclaration.DeclarationType declType =
                    "const".equalsIgnoreCase(declaration.kind()) ?
                            SemanticContext.VariableDeclaration.DeclarationType.CONST :
                            SemanticContext.VariableDeclaration.DeclarationType.VAR;
            ctx.registerVariable(new SemanticContext.VariableDeclaration(
                    declaration.name(), typeInfo, declType, null,
                    declaration.line(), "global"));
        }
        return ctx;
    }
}

