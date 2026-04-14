// ==================== ControlFlowProcessor.java ====================
package com.claw.compiler.processors.semantic;

import com.claw.compiler.context.SemanticContext;
import com.claw.compiler.core.Token;
import com.claw.compiler.core.TokenType;
import static com.claw.compiler.core.TokenType.KW_IF;
import com.claw.compiler.hierarchy.HierarchicalBlocks;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 控制流处理器 - 处理 if, else, for, while, break, continue
 */
@Slf4j
public class ControlFlowProcessor {

    @Getter
    private final List<ControlFlowInfo> controlFlows = new ArrayList<>();

    public void processTokens(List<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            switch (token.getType()) {
                case KW_IF -> controlFlows.add(parseIf(tokens, i));
                case KW_ELSE -> controlFlows.add(new ControlFlowInfo("else", token.getLine(), i));
                case KW_FOR -> controlFlows.add(parseFor(tokens, i));
                case KW_WHILE -> controlFlows.add(parseWhile(tokens, i));
                case KW_BREAK -> controlFlows.add(new ControlFlowInfo("break", token.getLine(), i));
                case KW_CONTINUE -> controlFlows.add(new ControlFlowInfo("continue", token.getLine(), i));
                default -> {}
            }
        }
        log.debug("识别 {} 个控制流结构", controlFlows.size());
    }

    private ControlFlowInfo parseIf(List<Token> tokens, int index) {
        ControlFlowInfo info = new ControlFlowInfo("if", tokens.get(index).getLine(), index);
        // 解析条件表达式
        int parenStart = index + 1;
        if (parenStart < tokens.size() && tokens.get(parenStart).getType() == TokenType.OPEN_PAREN) {
            int depth = 1;
            int j = parenStart + 1;
            StringBuilder condition = new StringBuilder();
            while (j < tokens.size() && depth > 0) {
                if (tokens.get(j).getType() == TokenType.OPEN_PAREN) depth++;
                else if (tokens.get(j).getType() == TokenType.CLOSE_PAREN) depth--;
                if (depth > 0) condition.append(tokens.get(j).getValue()).append(" ");
                j++;
            }
            info.setCondition(condition.toString().trim());
        }
        return info;
    }

    private ControlFlowInfo parseFor(List<Token> tokens, int index) {
        return new ControlFlowInfo("for", tokens.get(index).getLine(), index);
    }

    private ControlFlowInfo parseWhile(List<Token> tokens, int index) {
        return new ControlFlowInfo("while", tokens.get(index).getLine(), index);
    }

    @Getter
    public static class ControlFlowInfo {
        private final String type;
        private final int line;
        private final int tokenIndex;
        private String condition;

        public ControlFlowInfo(String type, int line, int tokenIndex) {
            this.type = type;
            this.line = line;
            this.tokenIndex = tokenIndex;
        }

        public void setCondition(String condition) { this.condition = condition; }
    }

    public Object process(List<Token> tokens, HierarchicalBlocks blocks, SemanticContext ctx) {
        for (ControlFlowInfo flow : controlFlows) {
            SemanticContext.ControlFlowInfo.ControlType controlType = switch (flow.getType()) {
                case "if" -> SemanticContext.ControlFlowInfo.ControlType.IF;
                case "else" -> SemanticContext.ControlFlowInfo.ControlType.ELSE;
                case "for" -> SemanticContext.ControlFlowInfo.ControlType.FOR;
                case "while" -> SemanticContext.ControlFlowInfo.ControlType.WHILE;
                case "break" -> SemanticContext.ControlFlowInfo.ControlType.BREAK;
                case "continue" -> SemanticContext.ControlFlowInfo.ControlType.CONTINUE;
                default -> SemanticContext.ControlFlowInfo.ControlType.IF;
            };
            ctx.addControlFlow(new SemanticContext.ControlFlowInfo(
                    controlType,
                    flow.getCondition() != null ? flow.getCondition() : "",
                    flow.getLine(),
                    "else".equalsIgnoreCase(flow.getType())
            ));
        }
        return ctx;
    }
}

