package claw.compiler.binding;

import java.util.List;
import java.util.Map;

/**
 * 目标语言运行时抽象接口
 * 
 * 解耦设计核心：Claw编译器的所有目标语言（Java/Python/C 等）都实现此接口。
 * 
 * 上游依赖：
 *   - IRGenerator 生成语言无关的 ClawIR
 *   - TargetCodeGenerator 遍历 ClawIR 指令，调用本接口方法生成目标代码
 * 
 * 下游实现：
 *   - JavaRuntime   (Java目标)
 *   - PythonRuntime (Python目标，未来)
 *   - CRuntime      (C目标，未来)
 * 
 * 设计映射：
 *   思想1 - 三层操作流（normal/exception/flow）
 *   思想5 - 4个程序注解（@BeforeName/@AfterName/@BeforeProps/@AfterProps）
 *           5个系统注解（@@description/@@param/@@return/@@example/@@deprecated）
 * 
 * @version 3.0
 * @date 2026-03-22
 */
public interface TargetRuntime {

    // ================================================================
    //  基础信息
    // ================================================================

    /**
     * 获取目标语言名称
     * @return 如 "Java", "Python", "C"
     */
    String getLanguageName();

    /**
     * 获取目标文件扩展名
     * @return 如 ".java", ".py", ".c"
     */
    String getFileExtension();

    // ================================================================
    //  类型系统映射
    // ================================================================

    /**
     * 将 Claw 类型映射为目标语言的类型字符串
     * 
     * Claw基本类型：Int, Float, String, Bool, Void, Any, type
     * 
     * 示例映射（Java）：
     *   "Int"    -> "int"
     *   "Float"  -> "double"
     *   "String" -> "String"
     *   "Bool"   -> "boolean"
     *   "Void"   -> "void"
     *   "Any"    -> "Object"
     *   自定义类型 -> 原名保留
     * 
     * @param clawType Claw类型名
     * @return 目标语言类型名
     */
    String mapType(String clawType);

    /**
     * 将 Claw 类型映射为包装/引用类型（泛型上下文使用）
     * 
     * 示例映射（Java）：
     *   "Int"  -> "Integer"
     *   "Bool" -> "Boolean"
     * 
     * 对于不区分原始/包装类型的语言（如Python），可直接委托给 mapType()
     * 
     * @param clawType Claw类型名
     * @return 目标语言包装类型名
     */
    String mapBoxedType(String clawType);

    /**
     * 获取目标语言的空值字面量
     * @return 如 "null" (Java), "None" (Python)
     */
    String getNullLiteral();

    /**
     * 获取目标语言的布尔值字面量
     * @param value 布尔值
     * @return 如 "true"/"false" (Java), "True"/"False" (Python)
     */
    String getBoolLiteral(boolean value);

    // ================================================================
    //  内存管理
    // ================================================================

    /**
     * 生成对象分配代码
     * 
     * @param typeName        类型名
     * @param constructorArgs 构造函数参数列表（可为null或空）
     * @return 如 "new UserData()" (Java), "UserData()" (Python)
     */
    String generateAllocation(String typeName, List<String> constructorArgs);

    /**
     * 生成对象释放代码
     * 
     * 对于有GC的语言（Java/Python），可返回注释或空操作。
     * 对于手动管理的语言（C），返回 "free(ptr)" 等。
     * 
     * @param varName 变量名
     * @return 释放代码或注释
     */
    String generateDeallocation(String varName);

    /**
     * 目标语言是否需要显式内存管理
     * @return Java/Python: false, C: true
     */
    boolean requiresExplicitMemoryManagement();

    // ================================================================
    //  构造/析构函数钩子 (思想5: @BeforeName / @AfterName)
    // ================================================================

    /**
     * 生成构造函数钩子代码
     * 
     * 对应 @BeforeName("method_name", "target")
     * 语义：系统在分配内存时自动调用指定方法
     * 
     * @param methodName 要调用的方法名（如 "initializeUser"）
     * @param target     目标对象（如 "this"）
     * @return 生成的钩子调用代码
     *         Java示例: "this.initializeUser();"
     */
    String generateConstructorHook(String methodName, String target);

    /**
     * 生成析构函数钩子代码
     * 
     * 对应 @AfterName("method_name", "target")
     * 语义：系统在回收内存时自动调用指定方法
     * 
     * @param methodName 要调用的方法名（如 "cleanupUser"）
     * @param target     目标对象（如 "this"）
     * @return 生成的钩子代码
     *         Java示例: 生成 AutoCloseable.close() 覆写方法
     */
    String generateDestructorHook(String methodName, String target);

    // ================================================================
    //  属性变更监听钩子 (思想5: @BeforeProps / @AfterProps)
    // ================================================================

    /**
     * 生成属性变更前监听代码
     * 
     * 对应 @BeforeProps("property_list")
     * 语义：指定的字段被变更前，系统自动调用
     * 
     * @param propertyPath 属性路径（如 "user.age"）
     * @param newValue     即将设置的新值表达式
     * @return 前置监听代码
     *         Java示例: "this.__beforePropertyChange(\"user.age\", newValue);"
     */
    String generateBeforePropsHook(String propertyPath, String newValue);

    /**
     * 生成属性变更后监听代码
     * 
     * 对应 @AfterProps("property_list")
     * 语义：指定的字段被变更后，系统自动调用
     * 
     * @param propertyPath 属性路径（如 "user.email"）
     * @param oldValue     旧值表达式
     * @param newValue     新值表达式
     * @return 后置监听代码
     */
    String generateAfterPropsHook(String propertyPath, String oldValue, String newValue);

    /**
     * 生成带监听的属性赋值完整代码（包装整个赋值过程）
     * 
     * 编译器在检测到 assignment_block 的目标属性被 @BeforeProps/@AfterProps 
     * 注解监听时，调用此方法生成完整的赋值+监听代码。
     * 
     * @param propertyPath  属性路径
     * @param newValueExpr  新值表达式
     * @param hasBefore     是否有 @BeforeProps 监听
     * @param hasAfter      是否有 @AfterProps 监听
     * @return 完整的带监听赋值代码
     */
    String generateMonitoredPropertySet(String propertyPath, String newValueExpr,
                                         boolean hasBefore, boolean hasAfter);

    // ================================================================
    //  三层操作流 (思想1: normal / exception / flow)
    // ================================================================

    /**
     * 生成异常捕获代码
     * 
     * Claw异常流特性：去掉try和{}，保留catch和throws
     * catch与当前代码块边界一致，不生成堆栈信息，提高性能
     * 
     * 注意：如果目标语言要求 try-catch 配对（如Java），
     * 运行时实现应自动包裹 try 块。
     * 
     * @param exceptionType 异常类型名（Claw类型，会通过 mapType 转换）
     * @param varName       异常变量名
     * @param handlerBody   处理体代码字符串
     * @return 异常捕获代码
     */
    String generateCatchBlock(String exceptionType, String varName, String handlerBody);

    /**
     * 生成 throws 声明
     * 
     * @param exceptionTypes 异常类型列表
     * @return throws声明字符串（如 " throws IOException, ParseException"），
     *         如果目标语言不支持（如Python），返回空字符串
     */
    String generateThrowsDeclaration(List<String> exceptionTypes);
    
    String generateThrowStatement(String exceptionType, String messageExpr);
    
    String generateFunctionCallWithAssignment(String resultVar, String funcName, List<String> args);

    /**
     * 生成业务逻辑流转代码
     * 
     * Claw流转特性：flow to target
     * 直接向后向上跳转，不记录堆栈信息
     * 
     * 目标语言映射：
     *   Java: labeled break（因为Java无原生goto）
     *   C: goto
     *   Python: 函数调用链/异常跳转模拟
     * 
     * @param targetLabel 跳转目标标签
     * @return 流转代码
     */
    String generateFlowTo(String targetLabel);

    /**
     * 目标语言是否支持原生的流程跳转（goto/label）
     * 
     * @return Java: false, C: true, Python: false
     */
    boolean supportsNativeFlowJump();

    // ================================================================
    //  函数生成
    // ================================================================

    /**
     * 生成函数定义头部
     * 
     * Claw关键字映射：function, public, private, return
     * 
     * @param visibility  可见性修饰符（"public" / "private"）
     * @param returnType  返回类型（Claw类型，会通过 mapType 转换）
     * @param funcName    函数名
     * @param params      参数列表，每项为 (参数名, Claw类型) 的键值对
     * @param throwsTypes throws 声明的异常类型列表（可为null或空）
     * @return 函数头代码
     *         Java示例: "public int calculate(int x, String name) throws ValidationError {"
     */
    String generateFunctionHeader(String visibility, String returnType,
                                   String funcName, List<Map.Entry<String, String>> params,
                                   List<String> throwsTypes);

    /**
     * 生成函数定义尾部
     * @return Java: "}", Python: ""（靠缩进结束）
     */
    String generateFunctionFooter();

    /**
     * 生成函数调用表达式
     * 
     * @param funcName 函数名
     * @param args     参数表达式列表
     * @return 调用表达式（不含语句终止符）
     *         如 "processUser(userData, config)"
     */
    String generateFunctionCall(String funcName, List<String> args);

    /**
     * 生成 return 语句
     * 
     * @param expression 返回值表达式（null 表示 Void 返回）
     * @return Java: "return expression;" 或 "return;"
     */
    String generateReturn(String expression);

    // ================================================================
    //  变量声明
    // ================================================================

    /**
     * 生成变量声明语句
     * 
     * Claw关键字映射：const, var
     * 
     * @param isConst   是否为常量（const）
     * @param typeName  变量类型（Claw类型）
     * @param varName   变量名
     * @param initExpr  初始化表达式（可为null，表示仅声明）
     * @return 声明语句
     *         Java示例: "final int MAX_SIZE = 100;"
     *         Java示例: "String name;"
     */
    String generateVariableDeclaration(boolean isConst, String typeName,
                                        String varName, String initExpr);

    // ================================================================
    //  控制流语句
    // ================================================================

    /**
     * 生成 if 语句头
     * @param condition 条件表达式
     * @return Java: "if (condition) {"
     */
    String generateIf(String condition);

    /**
     * 生成 else 子句
     * @return Java: "} else {"
     */
    String generateElse();

    /**
     * 生成 else if 子句
     * @param condition 条件表达式
     * @return Java: "} else if (condition) {"
     */
    String generateElseIf(String condition);

    /**
     * 生成 while 循环头
     * @param condition 循环条件
     * @return Java: "while (condition) {"
     */
    String generateWhile(String condition);

    /**
     * 生成 for-each 循环头
     * @param varName   循环变量名
     * @param iterable  可迭代表达式
     * @return Java: "for (var varName : iterable) {"
     */
    String generateFor(String varName, String iterable);

    /**
     * 生成 break 语句
     * @return Java: "break;"
     */
    String generateBreak();

    /**
     * 生成 continue 语句
     * @return Java: "continue;"
     */
    String generateContinue();

    // ================================================================
    //  块界定符与语句终止符
    // ================================================================

    /**
     * 代码块的开始符号
     * @return Java/C: "{", Python: ":"
     */
    String getBlockOpen();

    /**
     * 代码块的结束符号
     * @return Java/C: "}", Python: ""（靠缩进）
     */
    String getBlockClose();

    /**
     * 语句终止符
     * @return Java/C: ";", Python: ""
     */
    String getStatementTerminator();

    // ================================================================
    //  导入/导出
    // ================================================================

    /**
     * 生成导入语句
     * 
     * Claw关键字映射：import
     * 
     * @param modulePath  模块路径
     * @param symbolName  导入的符号名（null表示导入整个模块）
     * @return Java: "import package.Class;"
     *         Python: "from module import name"
     */
    String generateImport(String modulePath, String symbolName);

    /**
     * 生成导出标记
     * 
     * Claw关键字映射：export
     * 
     * @param symbolName 导出的符号名
     * @return 对于Java，可见性通过修饰符控制，此方法可返回注释
     */
    String generateExport(String symbolName);

    // ================================================================
    //  类型定义
    // ================================================================

    /**
     * 生成类型定义头部
     * 
     * Claw关键字映射：type
     * 
     * @param typeName    类型名
     * @param visibility  可见性
     * @return Java: "public class TypeName implements AutoCloseable {"
     *         Python: "class TypeName:"
     */
    String generateTypeDefinitionHeader(String typeName, String visibility);

    /**
     * 生成类型字段声明
     * 
     * @param fieldName   字段名
     * @param fieldType   字段类型（Claw类型）
     * @param visibility  可见性
     * @return Java: "    private int age;"
     */
    String generateTypeField(String fieldName, String fieldType, String visibility);

    /**
     * 生成类型定义尾部
     * @return Java: "}"
     */
    String generateTypeDefinitionFooter();

    // ================================================================
    //  运算符表达式
    // ================================================================

    /**
     * 生成二元运算表达式
     * 
     * Claw运算符：+, -, *, /, %, &&, ||, ==, !=, <, >, <=, >=
     * 
     * @param left     左操作数
     * @param operator 运算符
     * @param right    右操作数
     * @return 如 "x + y", "a && b"
     */
    String generateBinaryExpression(String left, String operator, String right);

    /**
     * 生成一元运算表达式
     * 
     * Claw运算符：!, -(取负)
     * 
     * @param operator 运算符
     * @param operand  操作数
     * @return 如 "!flag", "-value"
     */
    String generateUnaryExpression(String operator, String operand);

    // ================================================================
    //  注释生成
    // ================================================================

    /**
     * 生成单行注释
     * @param comment 注释内容
     * @return Java: "// comment", Python: "# comment"
     */
    String generateComment(String comment);

    /**
     * 生成多行文档注释
     * 
     * 用于将5个系统注解（@@description, @@param, @@return, @@example, @@deprecated）
     * 转换为目标语言的文档格式。
     * 
     * @param metadata 系统注解键值对
     *   期望的键：
     *     "description" -> 函数描述（来自 @@description 第一个参数）
     *     "io_spec"     -> IO规格（来自 @@description 第二个参数）
     *     "param.xxx"   -> 参数描述（来自 @@param）
     *     "return"      -> 返回值描述（来自 @@return）
     *     "example"     -> 使用示例（来自 @@example）
     *     "deprecated"  -> 废弃原因（来自 @@deprecated）
     *     "alternative" -> 替代方案（来自 @@deprecated 第二个参数）
     * 
     * @return Java: JavaDoc 格式 ("/** ... * /")
     *         Python: docstring 格式 ('"""..."""')
     */
    String generateDocComment(Map<String, String> metadata);

    // ================================================================
    //  数组操作
    // ================================================================

    /**
     * 生成数组创建代码
     * 
     * @param elementType 元素类型（Claw类型）
     * @param size        数组大小表达式
     * @return Java: "new int[10]"
     *         Python: "[None] * 10"
     */
    String generateArrayCreation(String elementType, String size);

    /**
     * 生成数组元素读取代码
     * 
     * @param arrayName 数组名
     * @param index     索引表达式
     * @return Java/Python: "arrayName[index]"
     */
    String generateArrayGet(String arrayName, String index);

    /**
     * 生成数组元素设置代码
     * 
     * @param arrayName 数组名
     * @param index     索引表达式
     * @param value     值表达式
     * @return Java: "arrayName[index] = value;"
     */
    String generateArraySet(String arrayName, String index, String value);

    // ================================================================
    //  属性访问
    // ================================================================

    /**
     * 生成属性读取代码
     * 
     * @param objectExpr 对象表达式
     * @param fieldName  字段名
     * @return Java: "object.getFieldName()" 或 "object.fieldName"
     *         Python: "object.field_name"
     */
    String generatePropertyGet(String objectExpr, String fieldName);

    /**
     * 生成属性设置代码（不带监听，纯赋值）
     * 
     * @param objectExpr 对象表达式
     * @param fieldName  字段名
     * @param valueExpr  值表达式
     * @return Java: "object.setFieldName(value);"
     */
    String generatePropertySet(String objectExpr, String fieldName, String valueExpr);

    // ================================================================
    //  作用域管理
    // ================================================================

    /**
     * 生成进入作用域的代码（如果目标语言需要）
     * 
     * @param scopeName 作用域名称（用于调试和标签生成）
     * @return 对于Java，通常是 "{"；对于C可能包含变量作用域声明
     */
    String generateScopeEnter(String scopeName);

    /**
     * 生成退出作用域的代码
     * 
     * @param scopeName 作用域名称
     * @return 对于Java，通常是 "}"
     */
    String generateScopeExit(String scopeName);

    // ================================================================
    //  运行时辅助
    // ================================================================

    /**
     * 获取运行时需要的额外导入列表
     * 
     * 例如Java中使用属性监听需要导入：
     *   java.beans.PropertyChangeSupport
     *   java.beans.PropertyChangeListener
     * 
     * @return 需要导入的完整路径列表
     */
    List<String> getRequiredImports();

    /**
     * 生成运行时辅助代码
     * 
     * 为目标语言生成Claw特性所需的基础设施代码，包括：
     * - 属性监听基类（支持 @BeforeProps/@AfterProps）
     * - 构造/析构钩子接口
     * - 业务流转辅助工具
     * 
     * 此代码通常在编译结果的开头或作为独立的运行时库输出。
     * 
     * @return 运行时辅助代码字符串
     */
    String generateRuntimeHelpers();

    // ================================================================
    //  字符串处理
    // ================================================================

    /**
     * 生成字符串字面量
     * 
     * @param value 原始字符串值
     * @return Java: "\"value\"", Python: "'value'" 或 "\"value\""
     */
    String generateStringLiteral(String value);

    /**
     * 生成字符串拼接表达式
     * 
     * @param parts 待拼接的表达式列表
     * @return Java: "part1 + part2 + part3"
     *         Python: "f\"{part1}{part2}{part3}\"" 或 "part1 + part2"
     */
    String generateStringConcat(List<String> parts);

    // ================================================================
    //  异常/错误创建
    // ================================================================

    /**
     * 生成抛出异常的代码
     * 
     * @param exceptionType 异常类型
     * @param message       错误消息表达式
     * @return Java: "throw new ExceptionType(message);"
     *         Python: "raise ExceptionType(message)"
     */
    String generateThrow(String exceptionType, String message);

    /**
     * 生成异常类型定义（如果Claw源码中定义了自定义异常类型）
     * 
     * @param exceptionName  异常类型名
     * @param parentType     父异常类型（null表示默认基类）
     * @return 完整的异常类定义代码
     */
    String generateExceptionTypeDefinition(String exceptionName, String parentType);
}
