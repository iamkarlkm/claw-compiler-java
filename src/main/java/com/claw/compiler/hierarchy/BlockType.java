// ==================== BlockType.java ====================
package com.claw.compiler.hierarchy;

/**
 * 18种代码块类型（思想4）
 * 
 * 6个维度分类，4个粒度分级
 */
public enum BlockType {
    // ====== 函数相关（3种）======
    FUNCTION_BLOCK("函数块", Dimension.FUNCTION, Granularity.COARSE),
    PARAMETER_BLOCK("参数块", Dimension.FUNCTION, Granularity.FINE),
    RETURN_BLOCK("返回块", Dimension.FUNCTION, Granularity.FINE),

    // ====== 控制流相关（3种）======
    CONTROL_FLOW_BLOCK("控制流块", Dimension.CONTROL_FLOW, Granularity.MEDIUM),
    CONDITION_BLOCK("条件块", Dimension.CONTROL_FLOW, Granularity.FINE),
    LOOP_BODY_BLOCK("循环体块", Dimension.CONTROL_FLOW, Granularity.MEDIUM),

    // ====== 表达式相关（3种）======
    EXPRESSION_BLOCK("表达式块", Dimension.EXPRESSION, Granularity.FINE),
    FUNCTION_CALL_BLOCK("函数调用块", Dimension.EXPRESSION, Granularity.FINE),
    ARRAY_BLOCK("数组块", Dimension.EXPRESSION, Granularity.FINE),

    // ====== 声明相关（2种）======
    VARIABLE_DECLARATION_BLOCK("变量声明块", Dimension.DECLARATION, Granularity.FINE),
    IMPORT_DECLARATION_BLOCK("导入声明块", Dimension.DECLARATION, Granularity.FINE),

    // ====== 范围相关（2种）======
    SCOPE_BLOCK("作用域块", Dimension.SCOPE, Granularity.COARSE),
    TYPE_INNER_BLOCK("类型内部块", Dimension.SCOPE, Granularity.MEDIUM),

    // ====== 赋值/类型/模块/注解（4种）======
    ASSIGNMENT_BLOCK("赋值块", Dimension.STATEMENT, Granularity.FINE),
    TYPE_DEFINITION_BLOCK("类型定义块", Dimension.SCOPE, Granularity.COARSE),
    MODULE_BLOCK("模块块", Dimension.SCOPE, Granularity.TOP),
    ANNOTATION_BLOCK("注解块", Dimension.STATEMENT, Granularity.FINE),

    // ====== 根块 ======
    ROOT_BLOCK("根块", Dimension.SCOPE, Granularity.TOP);

    private final String description;
    private final Dimension dimension;
    private final Granularity granularity;

    BlockType(String description, Dimension dimension, Granularity granularity) {
        this.description = description;
        this.dimension = dimension;
        this.granularity = granularity;
    }

    public String getDescription() { return description; }
    public Dimension getDimension() { return dimension; }
    public Granularity getGranularity() { return granularity; }

    /** 6个维度 */
    public enum Dimension {
        FUNCTION, CONTROL_FLOW, EXPRESSION, DECLARATION, SCOPE, STATEMENT
    }

    /** 4个粒度 */
    public enum Granularity {
        TOP,     // 顶层（模块）
        COARSE,  // 粗粒度（函数、类型定义）
        MEDIUM,  // 中粒度（控制流体）
        FINE     // 细粒度（表达式、声明）
    }
}

