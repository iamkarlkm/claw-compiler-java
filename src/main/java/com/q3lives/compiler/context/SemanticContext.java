package com.q3lives.compiler.context;

import java.util.*;

/**
 * 语义上下文 - 第2层语义处理器（6个处理器）的聚合输出
 * 
 * 包含：类型信息、函数签名、变量声明、控制流信息、字面量、运算符等语义数据。
 * 由6个语义处理器并行生成，供 IRGenerator 和 TypeChecker 使用。
 */
public class SemanticContext {

    // ==================== 类型信息 ====================

    /**
     * Claw类型描述
     */
    public static class TypeInfo {
        public enum TypeKind {
            PRIMITIVE,    // Int, Float, String, Bool, Void, Any
            USER_DEFINED, // type 关键字定义的自定义类型
            ARRAY,        // 数组类型
            FUNCTION,     // 函数类型
            GENERIC       // 泛型类型（未来扩展）
        }

        private final String typeName;
        private final TypeKind kind;
        private final Map<String, TypeInfo> fields;         // 自定义类型的字段
        private final TypeInfo elementType;                 // 数组元素类型
        private final List<TypeInfo> parameterTypes;        // 函数参数类型
        private final TypeInfo returnType;                  // 函数返回类型

        // 基本类型构造
        public TypeInfo(String typeName, TypeKind kind) {
            this.typeName = typeName;
            this.kind = kind;
            this.fields = new LinkedHashMap<>();
            this.elementType = null;
            this.parameterTypes = null;
            this.returnType = null;
        }

        // 数组类型构造
        public TypeInfo(String typeName, TypeInfo elementType) {
            this.typeName = typeName;
            this.kind = TypeKind.ARRAY;
            this.fields = Collections.emptyMap();
            this.elementType = elementType;
            this.parameterTypes = null;
            this.returnType = null;
        }

        // 函数类型构造
        public TypeInfo(String typeName, List<TypeInfo> parameterTypes, TypeInfo returnType) {
            this.typeName = typeName;
            this.kind = TypeKind.FUNCTION;
            this.fields = Collections.emptyMap();
            this.elementType = null;
            this.parameterTypes = new ArrayList<>(parameterTypes);
            this.returnType = returnType;
        }

        public void addField(String fieldName, TypeInfo fieldType) {
            fields.put(fieldName, fieldType);
        }

        public String getTypeName() { return typeName; }
        public TypeKind getKind() { return kind; }
        public Map<String, TypeInfo> getFields() { return Collections.unmodifiableMap(fields); }
        public TypeInfo getElementType() { return elementType; }
        public List<TypeInfo> getParameterTypes() {
            return parameterTypes != null ? Collections.unmodifiableList(parameterTypes) : null;
        }
        public TypeInfo getReturnType() { return returnType; }

        public boolean isPrimitive() { return kind == TypeKind.PRIMITIVE; }
        public boolean isUserDefined() { return kind == TypeKind.USER_DEFINED; }
        public boolean isArray() { return kind == TypeKind.ARRAY; }
        public boolean isFunction() { return kind == TypeKind.FUNCTION; }

        /**
         * 类型兼容性检查
         */
        public boolean isCompatibleWith(TypeInfo other) {
            if (other == null) return false;
            // Any类型与所有类型兼容
            if ("Any".equals(this.typeName) || "Any".equals(other.typeName)) return true;
            // 同名类型兼容
            if (this.typeName.equals(other.typeName)) return true;
            // 数值类型之间的隐式转换（Int -> Float）
            if ("Int".equals(this.typeName) && "Float".equals(other.typeName)) return true;
            return false;
        }

        @Override
        public String toString() {
            return typeName + "(" + kind + ")";
        }
    }

    // ==================== 函数签名 ====================

    /**
     * 函数签名信息
     */
    public static class FunctionSignature {
        public enum Visibility { PUBLIC, PRIVATE }
        public enum FlowType { NORMAL, EXCEPTION, FLOW }

        private final String functionName;
        private final Visibility visibility;
        private final FlowType flowType;               // 三层操作流类型
        private final List<ParameterInfo> parameters;
        private final TypeInfo returnType;
        private final String flowTarget;                // 仅 FLOW 类型使用
        private final int definedAtLine;
        private final List<String> throwsTypes;         // throws声明的异常类型

        public FunctionSignature(String functionName, Visibility visibility,
                                 FlowType flowType, TypeInfo returnType, int definedAtLine) {
            this.functionName = functionName;
            this.visibility = visibility;
            this.flowType = flowType;
            this.returnType = returnType;
            this.definedAtLine = definedAtLine;
            this.parameters = new ArrayList<>();
            this.flowTarget = null;
            this.throwsTypes = new ArrayList<>();
        }

        public FunctionSignature(String functionName, Visibility visibility,
                                 FlowType flowType, TypeInfo returnType,
                                 int definedAtLine, String flowTarget) {
            this.functionName = functionName;
            this.visibility = visibility;
            this.flowType = flowType;
            this.returnType = returnType;
            this.definedAtLine = definedAtLine;
            this.parameters = new ArrayList<>();
            this.flowTarget = flowTarget;
            this.throwsTypes = new ArrayList<>();
        }

        public void addParameter(ParameterInfo param) {
            parameters.add(param);
        }

        public void addThrowsType(String exceptionType) {
            throwsTypes.add(exceptionType);
        }

        public String getFunctionName() { return functionName; }
        public Visibility getVisibility() { return visibility; }
        public FlowType getFlowType() { return flowType; }
        public List<ParameterInfo> getParameters() { return Collections.unmodifiableList(parameters); }
        public TypeInfo getReturnType() { return returnType; }
        public String getFlowTarget() { return flowTarget; }
        public int getDefinedAtLine() { return definedAtLine; }
        public List<String> getThrowsTypes() { return Collections.unmodifiableList(throwsTypes); }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (flowType != FlowType.NORMAL) sb.append(flowType.name().toLowerCase()).append(" ");
            sb.append(visibility.name().toLowerCase()).append(" function ");
            sb.append(functionName).append("(");
            sb.append(parameters.stream()
                .map(p -> p.getName() + ": " + p.getType().getTypeName())
                .reduce((a, b) -> a + ", " + b).orElse(""));
            sb.append(") -> ").append(returnType.getTypeName());
            if (!throwsTypes.isEmpty()) {
                sb.append(" throws ").append(String.join(", ", throwsTypes));
            }
            return sb.toString();
        }
    }

    /**
     * 参数信息
     */
    public static class ParameterInfo {
        private final String name;
        private final TypeInfo type;
        private final String defaultValue;    // 可选默认值
        private final boolean isConst;

        public ParameterInfo(String name, TypeInfo type) {
            this(name, type, null, false);
        }

        public ParameterInfo(String name, TypeInfo type, String defaultValue, boolean isConst) {
            this.name = name;
            this.type = type;
            this.defaultValue = defaultValue;
            this.isConst = isConst;
        }

        public String getName() { return name; }
        public TypeInfo getType() { return type; }
        public String getDefaultValue() { return defaultValue; }
        public boolean isConst() { return isConst; }
    }

    // ==================== 变量声明 ====================

    /**
     * 变量声明信息
     */
    public static class VariableDeclaration {
        public enum DeclarationType { VAR, CONST }

        private final String name;
        private final TypeInfo type;
        private final DeclarationType declarationType;
        private final String initExpression;      // 初始化表达式（字符串形式）
        private final int declaredAtLine;
        private final String ownerScope;          // 所属作用域ID

        public VariableDeclaration(String name, TypeInfo type, DeclarationType declarationType,
                                   String initExpression, int declaredAtLine, String ownerScope) {
            this.name = name;
            this.type = type;
            this.declarationType = declarationType;
            this.initExpression = initExpression;
            this.declaredAtLine = declaredAtLine;
            this.ownerScope = ownerScope;
        }

        public String getName() { return name; }
        public TypeInfo getType() { return type; }
        public DeclarationType getDeclarationType() { return declarationType; }
        public String getInitExpression() { return initExpression; }
        public int getDeclaredAtLine() { return declaredAtLine; }
        public String getOwnerScope() { return ownerScope; }
        public boolean isConst() { return declarationType == DeclarationType.CONST; }
    }

    // ==================== 控制流信息 ====================

    /**
     * 控制流节点
     */
    public static class ControlFlowInfo {
        public enum ControlType { IF, ELSE, FOR, WHILE, BREAK, CONTINUE }

        private final ControlType controlType;
        private final String conditionExpression;
        private final int line;
        private final boolean hasElse;
        private final String loopVariable;         // for循环的迭代变量
        private final String loopRange;            // for循环的范围

        public ControlFlowInfo(ControlType controlType, String conditionExpression,
                               int line, boolean hasElse) {
            this(controlType, conditionExpression, line, hasElse, null, null);
        }

        public ControlFlowInfo(ControlType controlType, String conditionExpression,
                               int line, boolean hasElse,
                               String loopVariable, String loopRange) {
            this.controlType = controlType;
            this.conditionExpression = conditionExpression;
            this.line = line;
            this.hasElse = hasElse;
            this.loopVariable = loopVariable;
            this.loopRange = loopRange;
        }

        public ControlType getControlType() { return controlType; }
        public String getConditionExpression() { return conditionExpression; }
        public int getLine() { return line; }
        public boolean hasElse() { return hasElse; }
        public String getLoopVariable() { return loopVariable; }
        public String getLoopRange() { return loopRange; }
    }

    // ==================== 字面量信息 ====================

    /**
     * 字面量
     */
    public static class LiteralInfo {
        public enum LiteralType { INT, FLOAT, STRING, BOOL, NULL }

        private final LiteralType literalType;
        private final String rawValue;
        private final Object parsedValue;
        private final int line;

        public LiteralInfo(LiteralType literalType, String rawValue, Object parsedValue, int line) {
            this.literalType = literalType;
            this.rawValue = rawValue;
            this.parsedValue = parsedValue;
            this.line = line;
        }

        public LiteralType getLiteralType() { return literalType; }
        public String getRawValue() { return rawValue; }
        public Object getParsedValue() { return parsedValue; }
        public int getLine() { return line; }

        /**
         * 获取对应的Claw类型
         */
        public TypeInfo toTypeInfo() {
            switch (literalType) {
                case INT: return new TypeInfo("Int", TypeInfo.TypeKind.PRIMITIVE);
                case FLOAT: return new TypeInfo("Float", TypeInfo.TypeKind.PRIMITIVE);
                case STRING: return new TypeInfo("String", TypeInfo.TypeKind.PRIMITIVE);
                case BOOL: return new TypeInfo("Bool", TypeInfo.TypeKind.PRIMITIVE);
                case NULL: return new TypeInfo("Any", TypeInfo.TypeKind.PRIMITIVE);
                default: return new TypeInfo("Any", TypeInfo.TypeKind.PRIMITIVE);
            }
        }
    }

    // ==================== 运算符信息 ====================

    /**
     * 运算符
     */
    public static class OperatorInfo {
        public enum OperatorCategory { ARITHMETIC, COMPARISON, LOGICAL, ASSIGNMENT }

        private final String symbol;
        private final OperatorCategory category;
        private final int precedence;             // 优先级
        private final boolean isUnary;
        private final TypeInfo resultType;        // 运算结果类型

        public OperatorInfo(String symbol, OperatorCategory category,
                            int precedence, boolean isUnary, TypeInfo resultType) {
            this.symbol = symbol;
            this.category = category;
            this.precedence = precedence;
            this.isUnary = isUnary;
            this.resultType = resultType;
        }

        public String getSymbol() { return symbol; }
        public OperatorCategory getCategory() { return category; }
        public int getPrecedence() { return precedence; }
        public boolean isUnary() { return isUnary; }
        public TypeInfo getResultType() { return resultType; }
    }

    // ==================== SemanticContext 主体 ====================

    // 类型注册表（类型处理器输出）
    private final Map<String, TypeInfo> typeRegistry;

    // 函数签名表（函数处理器输出）
    private final Map<String, FunctionSignature> functionSignatures;

    // 变量声明表（声明处理器输出）— 按作用域组织
    private final Map<String, List<VariableDeclaration>> variablesByScope;

    // 控制流信息列表（控制流处理器输出）
    private final List<ControlFlowInfo> controlFlows;

    // 字面量列表（字面量处理器输出）
    private final List<LiteralInfo> literals;

    // 运算符注册表（运算符处理器输出）
    private final Map<String, OperatorInfo> operatorRegistry;

    // 错误收集
    private final List<SemanticError> errors;

    public SemanticContext() {
        this.typeRegistry = new LinkedHashMap<>();
        this.functionSignatures = new LinkedHashMap<>();
        this.variablesByScope = new LinkedHashMap<>();
        this.controlFlows = new ArrayList<>();
        this.literals = new ArrayList<>();
        this.operatorRegistry = new LinkedHashMap<>();
        this.errors = new ArrayList<>();

        // 注册内置原始类型
        registerBuiltinTypes();
        // 注册内置运算符
        registerBuiltinOperators();
    }

    // ==================== 内置类型注册 ====================

    private void registerBuiltinTypes() {
        typeRegistry.put("Int", new TypeInfo("Int", TypeInfo.TypeKind.PRIMITIVE));
        typeRegistry.put("Float", new TypeInfo("Float", TypeInfo.TypeKind.PRIMITIVE));
        typeRegistry.put("String", new TypeInfo("String", TypeInfo.TypeKind.PRIMITIVE));
        typeRegistry.put("Bool", new TypeInfo("Bool", TypeInfo.TypeKind.PRIMITIVE));
        typeRegistry.put("Void", new TypeInfo("Void", TypeInfo.TypeKind.PRIMITIVE));
        typeRegistry.put("Any", new TypeInfo("Any", TypeInfo.TypeKind.PRIMITIVE));
    }

    // ==================== 内置运算符注册 ====================

    private void registerBuiltinOperators() {
        TypeInfo intType = typeRegistry.get("Int");
        TypeInfo floatType = typeRegistry.get("Float");
        TypeInfo boolType = typeRegistry.get("Bool");
        TypeInfo stringType = typeRegistry.get("String");

        // 算术运算符
        operatorRegistry.put("+", new OperatorInfo("+", OperatorInfo.OperatorCategory.ARITHMETIC, 10, false, intType));
        operatorRegistry.put("-", new OperatorInfo("-", OperatorInfo.OperatorCategory.ARITHMETIC, 10, false, intType));
        operatorRegistry.put("*", new OperatorInfo("*", OperatorInfo.OperatorCategory.ARITHMETIC, 20, false, intType));
        operatorRegistry.put("/", new OperatorInfo("/", OperatorInfo.OperatorCategory.ARITHMETIC, 20, false, intType));
        operatorRegistry.put("%", new OperatorInfo("%", OperatorInfo.OperatorCategory.ARITHMETIC, 20, false, intType));

        // 比较运算符
        operatorRegistry.put("==", new OperatorInfo("==", OperatorInfo.OperatorCategory.COMPARISON, 5, false, boolType));
        operatorRegistry.put("!=", new OperatorInfo("!=", OperatorInfo.OperatorCategory.COMPARISON, 5, false, boolType));
        operatorRegistry.put("<", new OperatorInfo("<", OperatorInfo.OperatorCategory.COMPARISON, 5, false, boolType));
        operatorRegistry.put(">", new OperatorInfo(">", OperatorInfo.OperatorCategory.COMPARISON, 5, false, boolType));
        operatorRegistry.put("<=", new OperatorInfo("<=", OperatorInfo.OperatorCategory.COMPARISON, 5, false, boolType));
        operatorRegistry.put(">=", new OperatorInfo(">=", OperatorInfo.OperatorCategory.COMPARISON, 5, false, boolType));

        // 逻辑运算符
        operatorRegistry.put("&&", new OperatorInfo("&&", OperatorInfo.OperatorCategory.LOGICAL, 3, false, boolType));
        operatorRegistry.put("||", new OperatorInfo("||", OperatorInfo.OperatorCategory.LOGICAL, 2, false, boolType));
        operatorRegistry.put("!", new OperatorInfo("!", OperatorInfo.OperatorCategory.LOGICAL, 25, true, boolType));
    }

    // ==================== 类型注册（类型处理器调用） ====================

    public void registerType(TypeInfo typeInfo) {
        typeRegistry.put(typeInfo.getTypeName(), typeInfo);
    }

    public TypeInfo resolveType(String typeName) {
        return typeRegistry.get(typeName);
    }

    public boolean hasType(String typeName) {
        return typeRegistry.containsKey(typeName);
    }

    public Map<String, TypeInfo> getTypeRegistry() {
        return Collections.unmodifiableMap(typeRegistry);
    }

    // ==================== 函数签名注册（函数处理器调用） ====================

    public void registerFunction(FunctionSignature signature) {
        functionSignatures.put(signature.getFunctionName(), signature);
    }

    public FunctionSignature resolveFunction(String functionName) {
        return functionSignatures.get(functionName);
    }

    public boolean hasFunction(String functionName) {
        return functionSignatures.containsKey(functionName);
    }

    public Map<String, FunctionSignature> getFunctionSignatures() {
        return Collections.unmodifiableMap(functionSignatures);
    }

    /**
     * 获取指定操作流类型的所有函数
     */
    public List<FunctionSignature> getFunctionsByFlowType(FunctionSignature.FlowType flowType) {
        List<FunctionSignature> result = new ArrayList<>();
        for (FunctionSignature sig : functionSignatures.values()) {
            if (sig.getFlowType() == flowType) {
                result.add(sig);
            }
        }
        return result;
    }

    // ==================== 变量声明注册（声明处理器调用） ====================

    public void registerVariable(VariableDeclaration varDecl) {
        variablesByScope
            .computeIfAbsent(varDecl.getOwnerScope(), k -> new ArrayList<>())
            .add(varDecl);
    }

    public VariableDeclaration resolveVariable(String varName, String scopeId) {
        List<VariableDeclaration> vars = variablesByScope.get(scopeId);
        if (vars != null) {
            for (VariableDeclaration v : vars) {
                if (v.getName().equals(varName)) return v;
            }
        }
        return null;
    }

    public Map<String, List<VariableDeclaration>> getVariablesByScope() {
        return Collections.unmodifiableMap(variablesByScope);
    }

    // ==================== 控制流注册（控制流处理器调用） ====================

    public void addControlFlow(ControlFlowInfo cfInfo) {
        controlFlows.add(cfInfo);
    }

    public List<ControlFlowInfo> getControlFlows() {
        return Collections.unmodifiableList(controlFlows);
    }

    // ==================== 字面量注册（字面量处理器调用） ====================

    public void addLiteral(LiteralInfo literal) {
        literals.add(literal);
    }

    public List<LiteralInfo> getLiterals() {
        return Collections.unmodifiableList(literals);
    }

    // ==================== 运算符查询（运算符处理器调用） ====================

    public OperatorInfo resolveOperator(String symbol) {
        return operatorRegistry.get(symbol);
    }

    public void registerOperator(OperatorInfo operatorInfo) {
        operatorRegistry.put(operatorInfo.getSymbol(), operatorInfo);
    }

    public Map<String, OperatorInfo> getOperatorRegistry() {
        return Collections.unmodifiableMap(operatorRegistry);
    }

    // ==================== 错误管理 ====================

    /**
     * 语义错误
     */
    public static class SemanticError {
        public enum Severity { ERROR, WARNING, INFO }

        private final Severity severity;
        private final String message;
        private final int line;
        private final String context;

        public SemanticError(Severity severity, String message, int line, String context) {
            this.severity = severity;
            this.message = message;
            this.line = line;
            this.context = context;
        }

        public Severity getSeverity() { return severity; }
        public String getMessage() { return message; }
        public int getLine() { return line; }
        public String getContext() { return context; }

        @Override
        public String toString() {
            return "[" + severity + "] Line " + line + ": " + message +
                   (context != null ? " (in " + context + ")" : "");
        }
    }

    public void addError(SemanticError error) {
        errors.add(error);
    }

    public void addError(SemanticError.Severity severity, String message, int line, String context) {
        errors.add(new SemanticError(severity, message, line, context));
    }

    public List<SemanticError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public boolean hasErrors() {
        return errors.stream().anyMatch(e -> e.getSeverity() == SemanticError.Severity.ERROR);
    }

    public List<SemanticError> getErrorsBySeverity(SemanticError.Severity severity) {
        List<SemanticError> result = new ArrayList<>();
        for (SemanticError e : errors) {
            if (e.getSeverity() == severity) result.add(e);
        }
        return result;
    }

    // ==================== 调试输出 ====================

    @Override
    public String toString() {
        return "SemanticContext[types=" + typeRegistry.size() +
               ", functions=" + functionSignatures.size() +
               ", scopes=" + variablesByScope.size() +
               ", controlFlows=" + controlFlows.size() +
               ", literals=" + literals.size() +
               ", errors=" + errors.size() + "]";
    }

    /**
     * 输出完整的语义信息摘要
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Semantic Context Summary ===");

        sb.append("--- Types (").append(typeRegistry.size()).append(") ---");
        for (TypeInfo t : typeRegistry.values()) {
            sb.append("  ").append(t).append(" ");
        }

        sb.append("--- Functions (").append(functionSignatures.size()).append(") --- ");
        for (FunctionSignature f : functionSignatures.values()) {
            sb.append("  ").append(f).append(" ");
        }

        sb.append("--- Variables by Scope ---");
        for (Map.Entry<String, List<VariableDeclaration>> entry : variablesByScope.entrySet()) {
            sb.append("  Scope: ").append(entry.getKey()).append("");
            for (VariableDeclaration v : entry.getValue()) {
                sb.append("    ").append(v.getDeclarationType()).append(" ")
                  .append(v.getName()).append(": ").append(v.getType().getTypeName());
                if (v.getInitExpression() != null) {
                    sb.append(" = ").append(v.getInitExpression());
                }
                sb.append(" ");
            }
        }

        if (!errors.isEmpty()) {
            sb.append("--- Errors (").append(errors.size()).append(") --- ");
            for (SemanticError e : errors) {
                sb.append("  ").append(e).append(" ");
            }
        }

        return sb.toString();
    }
}


// package com.claw.compiler.context;

// import java.util.HashMap;
// import java.util.Map;

// import com.q3lives.compiler.processors.semantic.ControlFlowProcessor.ControlFlowInfo;
// import com.q3lives.compiler.processors.semantic.FunctionProcessor.FunctionInfo;
// import com.q3lives.compiler.processors.semantic.TypeProcessor.TypeInfo;
// import com.q3lives.compiler.processors.semantic.VariableInfo;
// import com.q3lives.compiler.processors.semantic.LiteralInfo;
// import com.q3lives.compiler.processors.semantic.OperatorInfo;

// /**
//  * 语义上下文 - 存储代码语义信息
//  */
// public class SemanticContext {
//     // 存储函数信息
//     private Map<String, FunctionInfo> functions = new HashMap<>();
    
//     // 存储类型信息
//     private Map<String, TypeInfo> types = new HashMap<>();
    
//     // 存储变量信息
//     private Map<String, VariableInfo> variables = new HashMap<>();
    
//     // 存储字面量信息
//     private Map<String, LiteralInfo> literals = new HashMap<>();
    
//     // 存储运算符信息
//     private Map<String, OperatorInfo> operators = new HashMap<>();
    
//     // 存储控制流信息
//     private Map<String, ControlFlowInfo> controlFlows = new HashMap<>();
    
//     // 添加函数
//     public void addFunction(String name, FunctionInfo info) {
//         functions.put(name, info);
//     }
    
//     // 获取函数
//     public FunctionInfo getFunction(String name) {
//         return functions.get(name);
//     }
    
//     // 添加类型
//     public void addType(String name, TypeInfo info) {
//         types.put(name, info);
//     }
    
//     // 获取类型
//     public TypeInfo getType(String name) {
//         return types.get(name);
//     }
    
//     // 添加变量
//     public void addVariable(String name, VariableInfo info) {
//         variables.put(name, info);
//     }
    
//     // 获取变量
//     public VariableInfo getVariable(String name) {
//         return variables.get(name);
//     }
    
//     // 添加字面量
//     public void addLiteral(String name, LiteralInfo info) {
//         literals.put(name, info);
//     }
    
//     // 获取字面量
//     public LiteralInfo getLiteral(String name) {
//         return literals.get(name);
//     }
    
//     // 添加运算符
//     public void addOperator(String name, OperatorInfo info) {
//         operators.put(name, info);
//     }
    
//     // 获取运算符
//     public OperatorInfo getOperator(String name) {
//         return operators.get(name);
//     }
    
//     // 添加控制流
//     public void addControlFlow(String name, ControlFlowInfo info) {
//         controlFlows.put(name, info);
//     }
    
//     // 获取控制流
//     public ControlFlowInfo getControlFlow(String name) {
//         return controlFlows.get(name);
//     }
    
//     // 其他辅助方法...
// }
