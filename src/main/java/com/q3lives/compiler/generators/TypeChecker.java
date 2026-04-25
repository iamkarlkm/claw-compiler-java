// ==================== TypeChecker.java ====================
package com.q3lives.compiler.generators;

import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.compiler.context.StructureContext;
import com.q3lives.compiler.frontend.ASTNode;
import com.q3lives.compiler.processors.semantic.TypeProcessor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 类型检查器 - 第4层验证生成处理器
 *
 * 职责：
 * 1. 类型验证 - 确保所有类型使用正确
 * 2. 类型推断 - 自动推断变量类型
 * 3. 语义错误检测 - 检测类型不匹配等错误
 */
@Slf4j
public class TypeChecker {


    private final TypeProcessor typeProcessor;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    /** 类型兼容性规则表 */
    private static final Map<String, Set<String>> COMPATIBLE_TYPES = new HashMap<>();

    static {
        // Int 可以隐式转换为 Float
        COMPATIBLE_TYPES.put("Int", Set.of("Int", "Float", "Any"));
        COMPATIBLE_TYPES.put("Float", Set.of("Float", "Any"));
        COMPATIBLE_TYPES.put("String", Set.of("String", "Any"));
        COMPATIBLE_TYPES.put("Bool", Set.of("Bool", "Any"));
        COMPATIBLE_TYPES.put("Void", Set.of("Void"));
        COMPATIBLE_TYPES.put("Any", Set.of("Any", "Int", "Float", "String", "Bool"));
    }

    public TypeChecker(TypeProcessor typeProcessor) {
        this.typeProcessor = typeProcessor;
    }

    /**
     * 执行类型检查
     */
    public TypeCheckResult check(ASTNode ast) {
        log.info("开始类型检查...");
        errors.clear();
        warnings.clear();

        checkNode(ast, new TypeEnvironment());

        if (errors.isEmpty()) {
            log.info("类型检查通过 (警告: {})", warnings.size());
        } else {
            log.error("类型检查失败: {} 个错误, {} 个警告", errors.size(), warnings.size());
        }

        return new TypeCheckResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * 递归检查AST节点
     */
    private String checkNode(ASTNode node, TypeEnvironment env) {
        if (node == null) {
            return "Void";
        }

        switch (node.getType()) {
            case PROGRAM:
                return checkProgram(node, env);
            case FUNCTION_DECLARATION:
                return checkFunctionDeclaration(node, env);
            case VARIABLE_DECLARATION:
                return checkVariableDeclaration(node, env);
            case ASSIGNMENT:
                return checkAssignment(node, env);
            case BINARY_EXPRESSION:
                return checkBinaryExpression(node, env);
            case UNARY_EXPRESSION:
                return checkUnaryExpression(node, env);
            case FUNCTION_CALL:
                return checkFunctionCall(node, env);
            case IF_STATEMENT:
                return checkIfStatement(node, env);
            case FOR_STATEMENT:
            case WHILE_STATEMENT:
                return checkLoopStatement(node, env);
            case RETURN_STATEMENT:
                return checkReturnStatement(node, env);
            case IDENTIFIER:
            case IDENTIFIER_REF:
                return checkIdentifier(node, env);
            case LITERAL:
            case INTEGER_LITERAL:
            case FLOAT_LITERAL:
            case STRING_LITERAL:
            case BOOLEAN_LITERAL:
            case NULL_LITERAL:
                return checkLiteral(node);
            case TYPE_DEFINITION:
                return checkTypeDefinition(node, env);
            case BLOCK:
                return checkBlock(node, env);
            case AOP_ASPECT:
                return checkAspect(node, env);
            case AOP_ADVICE:
                return checkAdvice(node, env);
            default:
                // 递归检查子节点
                for (ASTNode child : node.getChildren()) {
                    checkNode(child, env);
                }
                return "Void";
        }
    }

    private String checkProgram(ASTNode node, TypeEnvironment env) {
        for (ASTNode child : node.getChildren()) {
            checkNode(child, env);
        }
        return "Void";
    }

    private String checkFunctionDeclaration(ASTNode node, TypeEnvironment env) {
        String funcName = node.getProperty("name");
        String declaredReturnType = node.getProperty("returnType");

        log.debug("检查函数: {} -> {}", funcName, declaredReturnType);

        // 创建函数作用域
        TypeEnvironment funcEnv = env.createChild();

        // 注册泛型类型参数
        String typeParams = node.getProperty("typeParams");
        if (typeParams != null && !typeParams.isEmpty()) {
            for (String tp : typeParams.split(",")) {
                funcEnv.defineType(tp.trim());
            }
        }

        // 注册参数类型
        ASTNode params = node.getChildByType(ASTNode.NodeType.PARAMETER_LIST);
        if (params != null) {
            for (ASTNode param : params.getChildren()) {
                String paramName = param.getProperty("name");
                String paramType = param.getProperty("type");
                if (paramType == null) {
                    paramType = "Any";
                    warnings.add(String.format("参数 '%s' 未指定类型，默认为 Any", paramName));
                }
                funcEnv.defineVariable(paramName, paramType);
            }
        }

        // 检查函数体
        ASTNode body = node.getChildByType(ASTNode.NodeType.BLOCK);
        String inferredReturnType = declaredReturnType;
        if (body != null) {
            if (declaredReturnType == null || declaredReturnType.isEmpty()) {
                // 未声明返回类型：从函数体推断
                inferredReturnType = inferReturnType(body, funcEnv);
                if (inferredReturnType == null) {
                    inferredReturnType = "Void";
                }
                funcEnv.setExpectedReturnType(inferredReturnType);
                // 将推断结果写回 AST，供代码生成器使用
                node.setAttribute("returnType", inferredReturnType);
                log.info("推断函数 '{}' 的返回类型为: {}", funcName, inferredReturnType);
            } else {
                funcEnv.setExpectedReturnType(declaredReturnType);
            }
            // 正式检查函数体（此时 expectedReturnType 已确定）
            checkNode(body, funcEnv);
        } else if (declaredReturnType == null || declaredReturnType.isEmpty()) {
            inferredReturnType = "Void";
            node.setAttribute("returnType", inferredReturnType);
        }

        // 注册函数到当前环境
        env.defineFunction(funcName, inferredReturnType);

        return "Void";
    }

    /**
     * 从函数体推断返回类型：收集所有 return 语句的类型并统一。
     */
    private String inferReturnType(ASTNode body, TypeEnvironment env) {
        List<String> returnTypes = new ArrayList<>();
        collectReturnTypes(body, env, returnTypes);

        if (returnTypes.isEmpty()) {
            return "Void";
        }

        String unified = returnTypes.get(0);
        for (int i = 1; i < returnTypes.size(); i++) {
            String next = returnTypes.get(i);
            if (unified.equals(next)) {
                continue;
            }
            // Int 与 Float 混用时提升为 Float
            if (("Int".equals(unified) && "Float".equals(next)) ||
                ("Float".equals(unified) && "Int".equals(next))) {
                unified = "Float";
                continue;
            }
            // Any 与任何类型混用视为 Any
            if ("Any".equals(unified) || "Any".equals(next)) {
                unified = "Any";
                continue;
            }
            // 其他不兼容类型组合
            errors.add(String.format("返回类型不一致: %s 与 %s", unified, next));
            return "Any";
        }
        return unified;
    }

    /**
     * 递归收集函数体内所有 return 语句的表达式类型。
     */
    private void collectReturnTypes(ASTNode node, TypeEnvironment env, List<String> returnTypes) {
        if (node == null) return;

        if (node.getType() == ASTNode.NodeType.RETURN_STATEMENT) {
            if (node.getChildren().isEmpty()) {
                returnTypes.add("Void");
            } else {
                String exprType = checkNode(node.getChildren().get(0), env);
                returnTypes.add(exprType);
            }
            return;
        }

        // 跳过嵌套函数定义（避免将其 return 计入外层函数）
        if (node.getType() == ASTNode.NodeType.FUNCTION_DECLARATION) {
            return;
        }

        for (ASTNode child : node.getChildren()) {
            collectReturnTypes(child, env, returnTypes);
        }
    }

    private String checkVariableDeclaration(ASTNode node, TypeEnvironment env) {
        String varName = node.getProperty("name");
        // Parser 存储变量类型为 "type"，保持向后兼容也检查 "varType"
        String declaredType = node.getProperty("varType");
        if (declaredType == null) {
            declaredType = node.getProperty("type");
        }
        boolean isConst = !"true".equals(node.getProperty("mutable"));

        // 如果有初始化表达式，检查类型
        String inferredType = null;
        if (!node.getChildren().isEmpty()) {
            ASTNode initializer = node.getChildren().get(0);
            inferredType = checkNode(initializer, env);
        }

        // 确定最终类型
        String finalType;
        if (declaredType != null && !declaredType.isEmpty()) {
            finalType = declaredType;
            // 如果有初始化表达式，检查类型兼容性
            if (inferredType != null && !isTypeCompatible(inferredType, declaredType)) {
                errors.add(String.format("变量 '%s': 类型不匹配，声明为 %s 但初始化为 %s",
                        varName, declaredType, inferredType));
            }
        } else if (inferredType != null) {
            finalType = inferredType;
        } else {
            finalType = "Any";
            warnings.add(String.format("变量 '%s' 未指定类型且无初始化值，默认为 Any", varName));
        }

        env.defineVariable(varName, finalType);
        if (isConst) {
            env.markConst(varName);
        }

        log.debug("声明变量: {} : {} (const={})", varName, finalType, isConst);
        return "Void";
    }

    private String checkAssignment(ASTNode node, TypeEnvironment env) {
        if (node.getChildren().size() < 2) {
            errors.add("赋值表达式缺少目标或值");
            return "Void";
        }

        ASTNode target = node.getChildren().get(0);
        ASTNode value = node.getChildren().get(1);

        String targetName = target.getValue();
        String targetType = env.lookupVariable(targetName);
        if (targetType == null) {
            errors.add(String.format("变量 '%s' 未定义", targetName));
            return "Void";
        }

        // 检查是否为 const
        if (env.isConst(targetName)) {
            errors.add(String.format("不能对常量 '%s' 赋值", targetName));
            return "Void";
        }

        String valueType = checkNode(value, env);
        if (!isTypeCompatible(valueType, targetType)) {
            errors.add(String.format("赋值类型不匹配: '%s' 是 %s 类型，不能赋值 %s 类型",
                    targetName, targetType, valueType));
        }

        return "Void";
    }

    private String checkBinaryExpression(ASTNode node, TypeEnvironment env) {
        if (node.getChildren().size() < 2) {
            errors.add("二元表达式缺少操作数");
            return "Any";
        }

        String leftType = checkNode(node.getChildren().get(0), env);
        String rightType = checkNode(node.getChildren().get(1), env);
        String operator = node.getProperty("operator");

        if (operator == null) {
            operator = node.getValue();
        }

        return inferBinaryResultType(leftType, rightType, operator);
    }

    private String checkUnaryExpression(ASTNode node, TypeEnvironment env) {
        if (node.getChildren().isEmpty()) {
            errors.add("一元表达式缺少操作数");
            return "Any";
        }

        String operandType = checkNode(node.getChildren().get(0), env);
        String operator = node.getProperty("operator");

        if ("!".equals(operator)) {
            if (!"Bool".equals(operandType) && !"Any".equals(operandType)) {
                errors.add(String.format("逻辑非运算符 '!' 需要 Bool 类型，但得到 %s", operandType));
            }
            return "Bool";
        } else if ("-".equals(operator)) {
            if (!"Int".equals(operandType) && !"Float".equals(operandType) && !"Any".equals(operandType)) {
                errors.add(String.format("负号运算符 '-' 需要数值类型，但得到 %s", operandType));
            }
            return operandType;
        }

        return operandType;
    }

    private String checkFunctionCall(ASTNode node, TypeEnvironment env) {
        String funcName = node.getProperty("name");
        if (funcName == null) {
            funcName = node.getValue();
        }

        String returnType = env.lookupFunction(funcName);
        if (returnType == null) {
            // 可能是内建函数
            returnType = getBuiltinFunctionType(funcName);
            if (returnType == null) {
                warnings.add(String.format("函数 '%s' 未声明（可能是外部函数）", funcName));
                return "Any";
            }
        }

        return returnType;
    }

    private String checkIfStatement(ASTNode node, TypeEnvironment env) {
        // 检查条件表达式
        ASTNode condition = node.getChildByType(ASTNode.NodeType.BINARY_EXPRESSION);
        if (condition == null) {
            condition = node.getChildByType(ASTNode.NodeType.IDENTIFIER);
        }
        if (condition != null) {
            String condType = checkNode(condition, env);
            if (!"Bool".equals(condType) && !"Any".equals(condType)) {
                errors.add(String.format("if 条件需要 Bool 类型，但得到 %s", condType));
            }
        }

        // 检查 then/else 块
        for (ASTNode child : node.getChildren()) {
            if (child.getType() == ASTNode.NodeType.BLOCK) {
                checkNode(child, env.createChild());
            }
        }

        return "Void";
    }

    private String checkLoopStatement(ASTNode node, TypeEnvironment env) {
        TypeEnvironment loopEnv = env.createChild();

        for (ASTNode child : node.getChildren()) {
            checkNode(child, loopEnv);
        }

        return "Void";
    }

    private String checkReturnStatement(ASTNode node, TypeEnvironment env) {
        String expectedReturn = env.getExpectedReturnType();

        if (node.getChildren().isEmpty()) {
            if (expectedReturn != null && !"Void".equals(expectedReturn)) {
                errors.add(String.format("函数期望返回 %s，但 return 语句没有返回值", expectedReturn));
            }
            return "Void";
        }

        String actualReturn = checkNode(node.getChildren().get(0), env);

        if (expectedReturn != null && !isTypeCompatible(actualReturn, expectedReturn)) {
            errors.add(String.format("返回类型不匹配: 期望 %s，实际 %s",
                    expectedReturn, actualReturn));
        }

        return actualReturn;
    }

    private String checkIdentifier(ASTNode node, TypeEnvironment env) {
        String name = node.getValue();
        String type = env.lookupVariable(name);
        if (type == null) {
            type = env.lookupFunction(name);
        }
        if (type == null) {
            // 可能是属性访问
            if (name.contains(".")) {
                return "Any";
            }
            warnings.add(String.format("标识符 '%s' 未在当前作用域定义", name));
            return "Any";
        }
        return type;
    }

    private String checkLiteral(ASTNode node) {
        String literalType = node.getProperty("literalType");
        if (literalType != null) {
            return literalType;
        }

        // 从值推断
        String value = node.getValue();
        if (value == null) return "Any";

        if ("true".equals(value) || "false".equals(value)) {
            return "Bool";
        }
        if ("null".equals(value)) {
            return "Any";
        }
        if (value.startsWith("\"") || value.startsWith("'")) {
            return "String";
        }
        if (value.contains(".")) {
            try {
                Double.parseDouble(value);
                return "Float";
            } catch (NumberFormatException e) {
                return "Any";
            }
        }
        try {
            Long.parseLong(value);
            return "Int";
        } catch (NumberFormatException e) {
            return "Any";
        }
    }

    private String checkTypeDefinition(ASTNode node, TypeEnvironment env) {
        String typeName = node.getProperty("name");
        log.debug("注册自定义类型: {}", typeName);
        env.defineType(typeName);

        // 检查类型体
        TypeEnvironment typeEnv = env.createChild();

        // 注册泛型类型参数
        String typeParams = node.getProperty("typeParams");
        if (typeParams != null && !typeParams.isEmpty()) {
            for (String tp : typeParams.split(",")) {
                typeEnv.defineType(tp.trim());
            }
        }

        for (ASTNode child : node.getChildren()) {
            checkNode(child, typeEnv);
        }

        return "Void";
    }

    private String checkBlock(ASTNode node, TypeEnvironment env) {
        TypeEnvironment blockEnv = env.createChild();
        String lastType = "Void";
        for (ASTNode child : node.getChildren()) {
            lastType = checkNode(child, blockEnv);
        }
        return lastType;
    }

    private String checkAspect(ASTNode node, TypeEnvironment env) {
        String aspectName = node.getProperty("name");
        log.debug("检查切面定义: {}", aspectName);
        env.defineType(aspectName);

        TypeEnvironment aspectEnv = env.createChild();
        for (ASTNode child : node.getChildren()) {
            checkNode(child, aspectEnv);
        }
        return "Void";
    }

    private String checkAdvice(ASTNode node, TypeEnvironment env) {
        String adviceType = node.getProperty("adviceType");
        log.debug("检查通知: {} - {}", adviceType, node.getProperty("pointcut"));

        for (ASTNode child : node.getChildren()) {
            checkNode(child, env);
        }
        return "Void";
    }

    /**
     * 推断二元运算结果类型
     */
    private String inferBinaryResultType(String left, String right, String operator) {
        if (operator == null) return "Any";

        // 比较运算符和逻辑运算符总是返回 Bool
        if (operator.matches("[<>]=?|==|!=|&&|\\|\\|")) {
            return "Bool";
        }

        // 字符串连接
        if ("+".equals(operator) && ("String".equals(left) || "String".equals(right))) {
            return "String";
        }

        // 数值运算
        if ("Int".equals(left) && "Int".equals(right)) {
            return "/".equals(operator) ? "Float" : "Int";
        }
        if (("Int".equals(left) || "Float".equals(left)) &&
            ("Int".equals(right) || "Float".equals(right))) {
            return "Float";
        }

        // 取模
        if ("%".equals(operator)) {
            return "Int";
        }

        return "Any";
    }

    /**
     * 检查类型兼容性
     */
    private boolean isTypeCompatible(String sourceType, String targetType) {
        if (sourceType == null || targetType == null) return true;
        if (sourceType.equals(targetType)) return true;
        if ("Any".equals(sourceType) || "Any".equals(targetType)) return true;

        Set<String> compatible = COMPATIBLE_TYPES.get(sourceType);
        return compatible != null && compatible.contains(targetType);
    }

    /**
     * 获取内建函数返回类型
     */
    private String getBuiltinFunctionType(String funcName) {
        return switch (funcName) {
            case "print", "println" -> "Void";
            case "toString" -> "String";
            case "toInt", "parseInt" -> "Int";
            case "toFloat", "parseFloat" -> "Float";
            case "len", "length", "size" -> "Int";
            case "typeof" -> "String";
            case "input", "readLine" -> "String";
            default -> null;
        };
    }

    // ==================== 内部类 ====================

    /**
     * 类型检查结果
     */
    @Getter
    public static class TypeCheckResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public TypeCheckResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = Collections.unmodifiableList(errors);
            this.warnings = Collections.unmodifiableList(warnings);
        }

        public boolean isValid() {
            return valid;
        }
    }

    /**
     * 类型环境（作用域链）
     */
    private static class TypeEnvironment {
        private final TypeEnvironment parent;
        private final Map<String, String> variables = new HashMap<>();
        private final Map<String, String> functions = new HashMap<>();
        private final Set<String> constVars = new HashSet<>();
        private final Set<String> definedTypes = new HashSet<>();
        private String expectedReturnType;

        TypeEnvironment() {
            this.parent = null;
            // 注册内建类型
            definedTypes.addAll(Set.of("Int", "Float", "String", "Bool", "Void", "Any"));
        }

        TypeEnvironment(TypeEnvironment parent) {
            this.parent = parent;
        }

        TypeEnvironment createChild() {
            TypeEnvironment child = new TypeEnvironment(this);
            child.expectedReturnType = this.expectedReturnType;
            return child;
        }

        void defineVariable(String name, String type) {
            variables.put(name, type);
        }

        void defineFunction(String name, String returnType) {
            functions.put(name, returnType);
        }

        void defineType(String name) {
            definedTypes.add(name);
        }

        void markConst(String name) {
            constVars.add(name);
        }

        boolean isConst(String name) {
            if (constVars.contains(name)) return true;
            return parent != null && parent.isConst(name);
        }

        String lookupVariable(String name) {
            String type = variables.get(name);
            if (type != null) return type;
            return parent != null ? parent.lookupVariable(name) : null;
        }

        String lookupFunction(String name) {
            String type = functions.get(name);
            if (type != null) return type;
            return parent != null ? parent.lookupFunction(name) : null;
        }

        String getExpectedReturnType() {
            if (expectedReturnType != null) return expectedReturnType;
            return parent != null ? parent.getExpectedReturnType() : null;
        }

        void setExpectedReturnType(String type) {
            this.expectedReturnType = type;
        }
    }

    public List<String> check(SemanticContext semanticCtx, StructureContext structureCtx) {
        List<String> feedback = new ArrayList<>();
        if (semanticCtx == null) {
            feedback.add("SemanticContext is null");
            return feedback;
        }
        if (semanticCtx.hasErrors()) {
            for (SemanticContext.SemanticError error : semanticCtx.getErrors()) {
                feedback.add(error.toString());
            }
        }
        for (List<SemanticContext.VariableDeclaration> decls : semanticCtx.getVariablesByScope().values()) {
            for (SemanticContext.VariableDeclaration decl : decls) {
                if (!semanticCtx.hasType(decl.getType().getTypeName())) {
                    feedback.add(String.format(
                            "Unknown type '%s' for variable '%s'",
                            decl.getType().getTypeName(), decl.getName()));
                }
            }
        }
        if (structureCtx == null) {
            return feedback;
        }
        if (structureCtx.getGlobalScope() == null) {
            feedback.add("StructureContext missing global scope");
        }
        return feedback;
    }
}
