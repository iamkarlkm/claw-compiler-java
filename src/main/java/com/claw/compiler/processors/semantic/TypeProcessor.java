// ==================== TypeProcessor.java ====================
package com.claw.compiler.processors.semantic;

import com.claw.compiler.context.SemanticContext;
import com.claw.compiler.core.Token;
import com.claw.compiler.core.TokenType;
import com.claw.compiler.hierarchy.HierarchicalBlocks;

import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 类型处理器 - 处理 Int, Float, String, Bool, Void, Any, type
 */
@Slf4j
public class TypeProcessor {

    /** 内置类型 */
    private static final Set<TokenType> TYPE_KEYWORDS = Set.of(
        TokenType.KW_INT, TokenType.KW_FLOAT, TokenType.KW_STRING,
        TokenType.KW_BOOL, TokenType.KW_VOID, TokenType.KW_ANY, TokenType.KW_TYPE
    );

    /** 已注册的自定义类型 */
    private final Map<String, TypeInfo> registeredTypes = new LinkedHashMap<>();

    public TypeProcessor() {
        // 注册内置类型
        registerBuiltinType("Int", "integer", true);
        registerBuiltinType("Float", "floating-point", true);
        registerBuiltinType("String", "string", false);
        registerBuiltinType("Bool", "boolean", true);
        registerBuiltinType("Void", "void", false);
        registerBuiltinType("Any", "any", false);
    }

    private void registerBuiltinType(String name, String description, boolean isPrimitive) {
        registeredTypes.put(name, new TypeInfo(name, description, true, isPrimitive));
    }

    /** 处理Token中的类型信息 */
    public void processTokens(List<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType() == TokenType.KW_TYPE && i + 1 < tokens.size()) {
                Token nameToken = tokens.get(i + 1);
                if (nameToken.getType() == TokenType.IDENTIFIER) {
                    registerCustomType(nameToken.getValue());
                }
            }
        }
    }

    /** 注册自定义类型 */
    public void registerCustomType(String name) {
        if (!registeredTypes.containsKey(name)) {
            registeredTypes.put(name, new TypeInfo(name, "user-defined", false, false));
            log.debug("注册自定义类型: {}", name);
        }
    }

    /** 检查类型是否存在 */
    public boolean isTypeExists(String name) {
        return registeredTypes.containsKey(name);
    }

    /** 获取类型信息 */
    public TypeInfo getTypeInfo(String name) {
        return registeredTypes.get(name);
    }

    /** 是否为类型关键字Token */
    public boolean isTypeKeyword(Token token) {
        return TYPE_KEYWORDS.contains(token.getType());
    }

    /** 类型信息 */
    public record TypeInfo(String name, String description, boolean builtin, boolean primitive) {}

    public Object process(List<Token> tokens, HierarchicalBlocks blocks, SemanticContext ctx) {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType() == TokenType.KW_TYPE && i + 1 < tokens.size()) {
                Token nameToken = tokens.get(i + 1);
                if (nameToken.getType() == TokenType.IDENTIFIER) {
                    registerCustomType(nameToken.getValue());
                    ctx.registerType(new SemanticContext.TypeInfo(
                            nameToken.getValue(), SemanticContext.TypeInfo.TypeKind.USER_DEFINED));
                }
            } else if (isTypeKeyword(token)) {
                String typeName = token.getValue();
                if (typeName != null && !typeName.isBlank() && !ctx.hasType(typeName)) {
                    ctx.registerType(new SemanticContext.TypeInfo(
                            typeName, SemanticContext.TypeInfo.TypeKind.PRIMITIVE));
                }
            }
        }
        return ctx;
    }
}

