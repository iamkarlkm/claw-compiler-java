package com.q3lives.compiler.common;

/**
 * 编译器常量定义
 */
public final class CompilerConstants {

    // 私有构造函数，防止实例化
    private CompilerConstants() {}

    // Token 类型相关常量
    public static final int DOUBLE_AT_SIGN_LENGTH = 2;
    public static final int SINGLE_AT_SIGN_LENGTH = 1;
    public static final int ESCAPE_CHARACTER_LENGTH = 1;

    // 编译器版本信息
    public static final String COMPILER_VERSION = "3.0.0";
    public static final String COMPILER_NAME = "Claw Compiler Java";

    // 默认配置
    public static final int DEFAULT_BUFFER_SIZE = 8192;
    public static final int MAX_TOKEN_LENGTH = 1024;
    public static final int MAX_AST_DEPTH = 1000;

    // 错误代码
    public static final int ERROR_CODE_LEXICAL = 1001;
    public static final int ERROR_CODE_SYNTAX = 1002;
    public static final int ERROR_CODE_SEMANTIC = 1003;
    public static final int ERROR_CODE_TYPE = 1004;
    public static final int ERROR_CODE_RUNTIME = 1005;

    // 性能相关
    public static final int TOKENIZER_INITIAL_CAPACITY = 1000;
    public static final int AST_CHILDREN_INITIAL_CAPACITY = 10;

    // 字符串常量
    public static final String EMPTY_STRING = "";
    public static final String NEWLINE = "\n";
    public static final String TAB = "\t";
    public static final String SPACE = " ";

    // 包名
    public static final String CORE_PACKAGE = "com.q3lives.compiler";
    public static final String BINDING_PACKAGE = "com.q3lives.compiler.binding";

    // 文件扩展名
    public static final String CLAW_EXTENSION = ".claw";
    public static final String JAVA_EXTENSION = ".java";
    public static final String PYTHON_EXTENSION = ".py";
    public static final String C_EXTENSION = ".c";
    public static final String IR_EXTENSION = ".ir";
    public static final String PSEUDO_EXTENSION = ".pseudo";

    // 错误消息模板
    public static final String ERROR_MESSAGE_FILE_NOT_FOUND = "文件未找到: %s";
    public static final String ERROR_MESSAGE_INVALID_PATH = "无效的文件路径: %s";
    public static final String ERROR_MESSAGE_ACCESS_DENIED = "访问被拒绝: %s";
    public static final String ERROR_MESSAGE_COMPILATION_FAILED = "编译失败: %s";
    public static final String ERROR_MESSAGE_INTERNAL_ERROR = "内部编译错误: %s";
}