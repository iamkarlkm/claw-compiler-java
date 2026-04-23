package claw.compiler.example;

import claw.compiler.utils.Logger;

/**
 * AOP（面向切面编程）示例
 * 演示 Claw 编译器如何将 AOP 语法编译到 Java、Python 和 C
 */
public class AOPExample {

    public static void main(String[] args) {
        System.out.println("=== AOP (Aspect-Oriented Programming) Examples ===");
        System.out.println();

        // 示例 1: @Before 通知
        System.out.println("Example 1: @Before Advice");
        beforeAdviceExample();
        System.out.println();

        // 示例 2: @After 通知
        System.out.println("Example 2: @After Advice");
        afterAdviceExample();
        System.out.println();

        // 示例 3: @Around 通知
        System.out.println("Example 3: @Around Advice");
        aroundAdviceExample();
        System.out.println();

        // 示例 4: 多个切面组合
        System.out.println("Example 4: Multiple Aspects Combined");
        multipleAspectsExample();
        System.out.println();

        // 示例 5: 日志切面
        System.out.println("Example 5: Logging Aspect");
        loggingAspectExample();
        System.out.println();

        // 示例 6: 事务切面
        System.out.println("Example 6: Transactional Aspect");
        transactionalAspectExample();
    }

    /**
     * @Before 通知示例
     */
    private static void beforeAdviceExample() {
        Logger.beginBlock("Before Advice Example");

        System.out.println("  Claw 代码:");
        System.out.println("    aspect Logging {");
        System.out.println("      @Before(\"execution(* *(..))\")");
        System.out.println("      function logBefore(context: JoinPoint) {");
        System.out.println("        print(\"Entering: \" + context.methodName);");
        System.out.println("      }");
        System.out.println("    }");
        System.out.println();

        System.out.println("  生成的 Python 代码:");
        System.out.println("    import functools");
        System.out.println("    def log_before(func):");
        System.out.println("        @functools.wraps(func)");
        System.out.println("        def wrapper(*args, **kwargs):");
        System.out.println("            print(f\"Entering: {func.__name__}\")");
        System.out.println("            result = func(*args, **kwargs)");
        System.out.println("            print(f\"Exiting: {func.__name__}\")");
        System.out.println("            return result");
        System.out.println("        return wrapper");
        System.out.println();

        System.out.println("  生成的 Java 代码:");
        System.out.println("    @Aspect");
        System.out.println("    public class LoggingAspect {");
        System.out.println("        @Before(\"execution(* *(..))\")");
        System.out.println("        public void logBefore(JoinPoint jp) {");
        System.out.println("            System.out.println(\"Entering: \" + jp.getSignature().getName());");
        System.out.println("        }");
        System.out.println("    }");
        System.out.println();

        System.out.println("  生成的 C 代码:");
        System.out.println("    // log_before 宏定义");
        System.out.println("    void log_before() {");
        System.out.println("        printf(\"Entering: %s\\n\", __func__);");
        System.out.println("    }");
        System.out.println();

        Logger.endBlock("Before Advice Example");
    }

    /**
     * @After 通知示例
     */
    private static void afterAdviceExample() {
        Logger.beginBlock("After Advice Example");

        System.out.println("  Claw 代码:");
        System.out.println("    aspect Logging {");
        System.out.println("      @After(\"execution(* *(..))\")");
        System.out.println("      function logAfter(context: JoinPoint) {");
        System.out.println("        print(\"Exiting: \" + context.methodName);");
        System.out.println("      }");
        System.out.println("    }");
        System.out.println();

        System.out.println("  生成的 Python 代码:");
        System.out.println("    def log_after(func):");
        System.out.println("        @functools.wraps(func)");
        System.out.println("        def wrapper(*args, **kwargs):");
        System.out.println("            result = func(*args, **kwargs)");
        System.out.println("            print(f\"Exiting: {func.__name__}\")");
        System.out.println("            return result");
        System.out.println("        return wrapper");
        System.out.println();

        Logger.endBlock("After Advice Example");
    }

    /**
     * @Around 通知示例
     */
    private static void aroundAdviceExample() {
        Logger.beginBlock("Around Advice Example");

        System.out.println("  Claw 代码:");
        System.out.println("    aspect Transactional {");
        System.out.println("      @Around(\"execution(* *(..))\")");
        System.out.println("      function manageTransaction(context: JoinPoint) -> void {");
        System.out.println("        print(\"Transaction started\");");
        System.out.println("        context.proceed();");
        System.out.println("        print(\"Transaction committed\");");
        System.out.println("      }");
        System.out.println("    }");
        System.out.println();

        System.out.println("  生成的 Python 代码:");
        System.out.println("    def manage_transaction(func):");
        System.out.println("        @functools.wraps(func)");
        System.out.println("        def wrapper(*args, **kwargs):");
        System.out.println("            print(\"Transaction started\")");
        System.out.println("            result = func(*args, **kwargs)");
        System.out.println("            print(\"Transaction committed\")");
        System.out.println("            return result");
        System.out.println("        return wrapper");
        System.out.println();

        System.out.println("  生成的 Java 代码:");
        System.out.println("    @Aspect");
        System.out.println("    public class TransactionalAspect {");
        System.out.println("        @Around(\"execution(* *(..))\")");
        System.out.println("        public Object manageTransaction(ProceedingJoinPoint jp) throws Throwable {");
        System.out.println("            System.out.println(\"Transaction started\");");
        System.out.println("            Object result = jp.proceed();");
        System.out.println("            System.out.println(\"Transaction committed\");");
        System.out.println("            return result;");
        System.out.println("        }");
        System.out.println("    }");
        System.out.println();

        Logger.endBlock("Around Advice Example");
    }

    /**
     * 多个切面组合示例
     */
    private static void multipleAspectsExample() {
        Logger.beginBlock("Multiple Aspects Combined");

        System.out.println("  Claw 代码:");
        System.out.println("    aspect Logging {");
        System.out.println("      @Before(\"execution(* *(..))\")");
        System.out.println("      function logBefore(context: JoinPoint) { ... }");
        System.out.println("      @After(\"execution(* *(..))\")");
        System.out.println("      function logAfter(context: JoinPoint) { ... }");
        System.out.println("    }");
        System.out.println("    aspect Validation {");
        System.out.println("      @Before(\"execution(* *(..))\")");
        System.out.println("      function validate(context: JoinPoint) { ... }");
        System.out.println("    }");
        System.out.println();

        System.out.println("  生成的 Python 代码:");
        System.out.println("    # Logging aspect");
        System.out.println("    def log_before(func):");
        System.out.println("        @functools.wraps(func)");
        System.out.println("        def wrapper(*args, **kwargs):");
        System.out.println("            print(f\"Entering: {func.__name__}\")");
        System.out.println("            result = func(*args, **kwargs)");
        System.out.println("            print(f\"Exiting: {func.__name__}\")");
        System.out.println("            return result");
        System.out.println("        return wrapper");
        System.out.println("    ");
        System.out.println("    # Validation aspect");
        System.out.println("    def validate(func):");
        System.out.println("        @functools.wraps(func)");
        System.out.println("        def wrapper(*args, **kwargs):");
        System.out.println("            print(f\"Validating: {func.__name__}\")");
        System.out.println("            result = func(*args, **kwargs)");
        System.out.println("            return result");
        System.out.println("        return wrapper");
        System.out.println();

        Logger.endBlock("Multiple Aspects Combined");
    }

    /**
     * 日志切面示例
     */
    private static void loggingAspectExample() {
        Logger.beginBlock("Logging Aspect Example");

        System.out.println("  使用场景：记录所有方法的调用日志");
        System.out.println();
        System.out.println("  Claw 代码:");
        System.out.println("    aspect Logging {");
        System.out.println("      @Before(\"execution(public *.*(..))\")");
        System.out.println("      function logBefore(context: JoinPoint) {");
        System.out.println("        print(format(\"[ENTER] {} called\", context.methodName));");
        System.out.println("      }");
        System.out.println("      @After(\"execution(public *.*(..))\")");
        System.out.println("      function logAfter(context: JoinPoint) {");
        System.out.println("        print(format(\"[EXIT] {} completed\", context.methodName));");
        System.out.println("      }");
        System.out.println("    }");
        System.out.println();

        System.out.println("  应用切面:");
        System.out.println("    @Aspect(Logging)");
        System.out.println("    function calculate(a: int, b: int) -> int {");
        System.out.println("      return a + b;  // 被日志切面自动记录");
        System.out.println("    }");
        System.out.println();

        System.out.println("  输出:");
        System.out.println("    [ENTER] calculate called");
        System.out.println("    [ENTER] add called");
        System.out.println("    [EXIT] add completed");
        System.out.println("    [EXIT] calculate completed");
        System.out.println();

        Logger.endBlock("Logging Aspect Example");
    }

    /**
     * 事务切面示例
     */
    private static void transactionalAspectExample() {
        Logger.beginBlock("Transactional Aspect Example");

        System.out.println("  使用场景：自动管理数据库事务");
        System.out.println();
        System.out.println("  Claw 代码:");
        System.out.println("    aspect Transactional {");
        System.out.println("      @Around(\"execution(* *.transaction*(..))\")");
        System.out.println("      function manageTransaction(context: JoinPoint) -> void {");
        System.out.println("        print(\"Opening transaction...\");");
        System.out.println("        begin_transaction();");
        System.out.println("        context.proceed();");
        System.out.println("        print(\"Committing transaction...\");");
        System.out.println("        commit_transaction();");
        System.out.println("      }");
        System.out.println("    }");
        System.out.println();

        System.out.println("  应用切面:");
        System.out.println("    @Aspect(Transactional)");
        System.out.println("    function saveUser(user: User) -> bool {");
        System.out.println("      // 事务自动管理，无需手动处理");
        System.out.println("      database.insert(user);");
        System.out.println("      return true;");
        System.out.println("    }");
        System.out.println();

        System.out.println("  输出:");
        System.out.println("    Opening transaction...");
        System.out.println("    Opening transaction...");
        System.out.println("    [INSERT INTO users ...]");
        System.out.println("    Committing transaction...");
        System.out.println("    Opening transaction...");
        System.println("    [INSERT INTO logs ...]");
        System.out.println("    Committing transaction...");
        System.out.println();

        Logger.endBlock("Transactional Aspect Example");
    }

    // ==================== 实际应用示例 ====================

    /**
     * 模拟 @Before 通知实现
     */
    private static Object logBefore(Object func) {
        System.out.println("  [BEFORE] " + func.getClass().getSimpleName() + " called");
        return func;
    }

    /**
     * 模拟 @Around 通知实现
     */
    private static Object manageTransaction(Object func) {
        System.out.println("  [BEGIN TRANSACTION]");
        try {
            Object result = func;
            System.out.println("  [COMMIT TRANSACTION]");
            return result;
        } catch (Exception e) {
            System.out.println("  [ROLLBACK TRANSACTION]");
            throw e;
        }
    }

    /**
     * 测试方法
     */
    private static int add(int a, int b) {
        return a + b;
    }

    /**
     * 测试方法（带注解）
     */
    @Deprecated
    private static void deprecatedMethod() {
        System.out.println("  This method is deprecated");
    }

    /**
     * 带事务的方法
     */
    private static void saveUser(String userName) {
        System.out.println("  Saving user: " + userName);
    }
}
