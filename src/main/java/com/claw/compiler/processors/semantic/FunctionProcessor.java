// ==================== FunctionProcessor.java ====================
package com.claw.compiler.processors.semantic;

import com.claw.compiler.context.SemanticContext;
import com.claw.compiler.core.Token;
import com.claw.compiler.core.TokenType;
import com.claw.compiler.hierarchy.HierarchicalBlocks;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 函数处理器 - 处理 function, public, private, return
 */
@Slf4j
public class FunctionProcessor {

    @Getter
    private final Map<String, FunctionInfo> functions = new LinkedHashMap<>();

    /**
     * 从Token流中解析函数声明
     */
    public void processTokens(List<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).getType() == TokenType.KW_FUNCTION) {
                FunctionInfo func = parseFunctionDeclaration(tokens, i);
                if (func != null) {
                    functions.put(func.name, func);
                    log.debug("解析函数: {}", func);
                }
            }
        }
    }

    private FunctionInfo parseFunctionDeclaration(List<Token> tokens, int funcIndex) {
        // 查看前面的修饰符
        String flowType = "normal";
        String visibility = "public";

        for (int j = funcIndex - 1; j >= Math.max(0, funcIndex - 3); j--) {
            Token prev = tokens.get(j);
            switch (prev.getType()) {
                case KW_NORMAL -> flowType = "normal";
                case KW_EXCEPTION -> flowType = "exception";
                case KW_FLOW -> flowType = "flow";
                case KW_PUBLIC -> visibility = "public";
                case KW_PRIVATE -> visibility = "private";
            }
        }

        // 函数名
        if (funcIndex + 1 >= tokens.size()) return null;
        Token nameToken = tokens.get(funcIndex + 1);
        if (nameToken.getType() != TokenType.IDENTIFIER) return null;
        String name = nameToken.getValue();

        // 解析参数列表
        List<ParameterInfo> params = new ArrayList<>();
        int paramStart = findNext(tokens, funcIndex + 2, TokenType.OPEN_PAREN);
        if (paramStart >= 0) {
            int paramEnd = findNext(tokens, paramStart + 1, TokenType.CLOSE_PAREN);
            if (paramEnd > paramStart + 1) {
                params = parseParameters(tokens, paramStart + 1, paramEnd);
            }
        }

        // 解析返回类型
        String returnType = "Void";
        int arrowIdx = findNext(tokens, funcIndex, TokenType.OP_ARROW);
        if (arrowIdx >= 0 && arrowIdx + 1 < tokens.size()) {
            returnType = tokens.get(arrowIdx + 1).getValue();
        }

        return new FunctionInfo(name, flowType, visibility, params, returnType,
                nameToken.getLine());
    }

    private List<ParameterInfo> parseParameters(List<Token> tokens, int start, int end) {
        List<ParameterInfo> params = new ArrayList<>();
        int i = start;
        while (i < end) {
            Token t = tokens.get(i);
            if (t.getType() == TokenType.IDENTIFIER) {
                String pName = t.getValue();
                String pType = "Any";
                if (i + 1 < end && tokens.get(i + 1).getType() == TokenType.OP_COLON) {
                    if (i + 2 < end) {
                        pType = tokens.get(i + 2).getValue();
                        i += 2;
                    }
                }
                params.add(new ParameterInfo(pName, pType));
            }
            i++;
        }
        return params;
    }

    private int findNext(List<Token> tokens, int from, TokenType type) {
        for (int i = from; i < tokens.size(); i++) {
            if (tokens.get(i).getType() == type) return i;
        }
        return -1;
    }

    public record FunctionInfo(String name, String flowType, String visibility,
                               List<ParameterInfo> parameters, String returnType, int line) {}
    public record ParameterInfo(String name, String type) {}
    public Object process(List<Token> tokens, HierarchicalBlocks blocks, SemanticContext ctx) {
        for (FunctionInfo functionInfo : functions.values()) {
            SemanticContext.FunctionSignature.Visibility visibility =
                    "private".equalsIgnoreCase(functionInfo.visibility) ?
                            SemanticContext.FunctionSignature.Visibility.PRIVATE :
                            SemanticContext.FunctionSignature.Visibility.PUBLIC;
            SemanticContext.FunctionSignature.FlowType flowType = switch (functionInfo.flowType.toLowerCase()) {
                case "exception" -> SemanticContext.FunctionSignature.FlowType.EXCEPTION;
                case "flow" -> SemanticContext.FunctionSignature.FlowType.FLOW;
                default -> SemanticContext.FunctionSignature.FlowType.NORMAL;
            };
            String returnTypeName = functionInfo.returnType != null && !functionInfo.returnType.isBlank()
                    ? functionInfo.returnType : "Void";
            SemanticContext.TypeInfo returnType = resolveType(returnTypeName, ctx);
            SemanticContext.FunctionSignature signature = new SemanticContext.FunctionSignature(
                    functionInfo.name, visibility, flowType, returnType, functionInfo.line);
            for (ParameterInfo parameter : functionInfo.parameters) {
                String paramTypeName = parameter.type() != null && !parameter.type().isBlank()
                        ? parameter.type() : "Any";
                signature.addParameter(new SemanticContext.ParameterInfo(
                        parameter.name(), resolveType(paramTypeName, ctx)));
            }
            ctx.registerFunction(signature);
        }
        return ctx;
    }

    private SemanticContext.TypeInfo resolveType(String typeName, SemanticContext ctx) {
        if (ctx.hasType(typeName)) {
            return ctx.resolveType(typeName);
        }
        return new SemanticContext.TypeInfo(typeName, SemanticContext.TypeInfo.TypeKind.USER_DEFINED);
    }
}

