// ==================== EntityType.java ====================
package com.claw.compiler.decomposer;

/**
 * 实体类型定义
 */
public enum EntityType {
    FUNCTION("函数"),
    TYPE_DEFINITION("类型定义"),
    VARIABLE("变量"),
    CONSTANT("常量"),
    PARAMETER("参数"),
    IMPORT("导入"),
    ANNOTATION("注解"),
    EXPRESSION("表达式"),
    STATEMENT("语句");

    private final String description;

    EntityType(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}

