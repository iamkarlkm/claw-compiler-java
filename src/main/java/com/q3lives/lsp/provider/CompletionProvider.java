package com.q3lives.lsp.provider;

import com.q3lives.ir.ClawIR;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import com.q3lives.lsp.protocol.CompletionItem;
import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.compiler.hierarchy.CodeBlock;
// import com.q3lives.lsp.protocol.CompletionItem;
// import com.q3lives.lsp.utils.CacheManager;
// import com.q3lives.lsp.utils.PerformanceMonitor;
import org.eclipse.lsp4j.CompletionList;

/**
 * LSP 代码补全提供器（优化版）
 *
 * 负责为 Claw 语言提供代码补全功能，包括：
 * - 基础类型补全（Int, Float, String, Bool）
 * - 函数补全
 * - 变量补全
 * - 注解补全
 * - 修饰符补全
 *
 * 特性：
 * - 缓存优化
 * - 性能监控
 * - 智能上下文识别
 */
public class CompletionProvider {

    private final SemanticContext semanticContext;
    private final ClawIR ir;
    // private final CacheManager<String, List<com.q3lives.lsp.protocol.CompletionItem>> completionCache;
    // private final PerformanceMonitor performanceMonitor;
    private static final String OPERATION_NAME = "completion";

    // 缓存键格式: document:position:context
    private static final String CACHE_KEY_FORMAT = "%s:%d:%d:%s";


    // 补全触发字符
    private static final List<String> TRIGGER_CHARS = List.of(".", "(", " ", "@");

    // 类型补全列表
    private static final List<com.q3lives.lsp.protocol.CompletionItem> TYPE_COMPLETIONS = List.of(
        com.q3lives.lsp.protocol.CompletionItem.createTypeCompletion("Int", "整数类型 (int)"),
        com.q3lives.lsp.protocol.CompletionItem.createTypeCompletion("Float", "浮点数类型 (float)"),
        com.q3lives.lsp.protocol.CompletionItem.createTypeCompletion("String", "字符串类型"),
        com.q3lives.lsp.protocol.CompletionItem.createTypeCompletion("Bool", "布尔类型"),
        com.q3lives.lsp.protocol.CompletionItem.createTypeCompletion("Void", "空类型 (void)"),
        com.q3lives.lsp.protocol.CompletionItem.createTypeCompletion("Any", "任意类型")
    );

    // 函数补全列表
    private static final List<com.q3lives.lsp.protocol.CompletionItem> FUNCTION_COMPLETIONS = List.of(
        com.q3lives.lsp.protocol.CompletionItem.createFunctionCompletion("print", "String", "Void", "打印输出"),
        com.q3lives.lsp.protocol.CompletionItem.createFunctionCompletion("input", "String", "String", "用户输入"),
        com.q3lives.lsp.protocol.CompletionItem.createFunctionCompletion("parseInt", "String", "Int", "字符串转整数"),
        com.q3lives.lsp.protocol.CompletionItem.createFunctionCompletion("parseFloat", "String", "Float", "字符串转浮点数"),
        com.q3lives.lsp.protocol.CompletionItem.createFunctionCompletion("len", "Any", "Int", "获取长度/数量")
    );

    // 注解补全列表
    private static final List<com.q3lives.lsp.protocol.CompletionItem> ANNOTATION_COMPLETIONS = List.of(
        com.q3lives.lsp.protocol.CompletionItem.createAnnotationCompletion("@Before", "@Before - 前置通知"),
        com.q3lives.lsp.protocol.CompletionItem.createAnnotationCompletion("@After", "@After - 后置通知"),
        com.q3lives.lsp.protocol.CompletionItem.createAnnotationCompletion("@Around", "@Around - 环绕通知"),
        com.q3lives.lsp.protocol.CompletionItem.createAnnotationCompletion("@AfterReturning", "@AfterReturning - 返回值后通知"),
        com.q3lives.lsp.protocol.CompletionItem.createAnnotationCompletion("@AfterThrowing", "@AfterThrowing - 异常后通知"),
        com.q3lives.lsp.protocol.CompletionItem.createAnnotationCompletion("@Aspect", "@Aspect - 切面定义")
    );

    // 关键字补全列表
    private static final List<com.q3lives.lsp.protocol.CompletionItem> KEYWORD_COMPLETIONS = List.of(
        com.q3lives.lsp.protocol.CompletionItem.createTypeCompletion("if", "if - 条件语句"),
        com.q3lives.lsp.protocol.CompletionItem.createTypeCompletion("else", "else - 条件语句"),
        com.q3lives.lsp.protocol.CompletionItem.createTypeCompletion("while", "while - 循环语句"),
        com.q3lives.lsp.protocol.CompletionItem.createTypeCompletion("for", "for - 循环语句"),
        com.q3lives.lsp.protocol.CompletionItem.createTypeCompletion("function", "function - 函数定义"),
        com.q3lives.lsp.protocol.CompletionItem.createTypeCompletion("aspect", "aspect - 切面定义"),
        com.q3lives.lsp.protocol.CompletionItem.createTypeCompletion("return", "return - 返回语句"),
        com.q3lives.lsp.protocol.CompletionItem.createTypeCompletion("break", "break - 跳出循环"),
        com.q3lives.lsp.protocol.CompletionItem.createTypeCompletion("continue", "continue - 继续循环")
    );

    public CompletionProvider(SemanticContext semanticContext, ClawIR ir) {
        this.semanticContext = semanticContext;
        this.ir = ir;
        // this.completionCache = CacheManager.completionCache();
        // this.performanceMonitor = PerformanceMonitor.getInstance();
    }

    /**
     * 提供代码补全
     *
     * @param document 文档内容
     * @param position 光标位置
     * @return 补全列表
     */
    public CompletionList provideCompletion(String document, Position position) {
        // long startTime = performanceMonitor.start(OPERATION_NAME);

        try {
            // 生成缓存键
            String contextStr = position.getCharacter() > 0 ? "unknown" : "all";
            String cacheKey = String.format(CACHE_KEY_FORMAT,
                document.hashCode(), position.getLine(), position.getCharacter(), contextStr);

            // 尝试从缓存获取
            // List<com.q3lives.lsp.protocol.CompletionItem> cached = completionCache.get(cacheKey);
            // if (cached != null) {
            //     performanceMonitor.end(OPERATION_NAME, startTime);
            //     return new CompletionList(false, convertToLsp4j(cached));
            // }

            // 识别当前上下文
            Context context = analyzeContext(document, position);

            // 根据上下文提供补全
            List<com.q3lives.lsp.protocol.CompletionItem> completions = new ArrayList<>();

            switch (context) {
                case TYPE:
                    completions.addAll(TYPE_COMPLETIONS);
                    break;
                case FUNCTION:
                    completions.addAll(FUNCTION_COMPLETIONS);
                    break;
                case ANNOTATION:
                    completions.addAll(ANNOTATION_COMPLETIONS);
                    break;
                case KEYWORD:
                    completions.addAll(KEYWORD_COMPLETIONS);
                    break;
                case VARIABLE:
                    completions.addAll(getVariableCompletions());
                    break;
                case MODIFIER:
                    completions.addAll(getModifierCompletions());
                    break;
                case DOT:
                    completions.addAll(getDotCompletions());
                    break;
                case UNKNOWN:
                    // 返回所有补全
                    completions.addAll(TYPE_COMPLETIONS);
                    completions.addAll(FUNCTION_COMPLETIONS);
                    completions.addAll(ANNOTATION_COMPLETIONS);
                    completions.addAll(KEYWORD_COMPLETIONS);
                    break;
            }

            // 按名称排序
            completions.sort((a, b) -> a.getLabel().compareToIgnoreCase(b.getLabel()));

            // 缓存结果
            // completionCache.put(cacheKey, completions);

            // performanceMonitor.end(OPERATION_NAME, startTime);
            return new CompletionList(false, convertToLsp4j(completions));

        } catch (Exception e) {
            // performanceMonitor.end(OPERATION_NAME, startTime);
            System.err.println("Error providing completion: " + e.getMessage());
            return new CompletionList(false, Collections.<org.eclipse.lsp4j.CompletionItem>emptyList());
        }
    }

    /**
     * 分析当前上下文
     *
     * @param document 文档内容
     * @param position 光标位置
     * @return 上下文类型
     */
    private Context analyzeContext(String document, Position position) {
        // 获取光标前的一个字符
        int line = position.getLine();
        int charIndex = position.getCharacter();

        if (line < 0 || charIndex < 0) {
            return Context.UNKNOWN;
        }

        // 获取当前行
        String[] lines = document.split("\n");
        if (line >= lines.length) {
            return Context.UNKNOWN;
        }

        String currentLine = lines[line];

        // 检查是否是开头
        if (charIndex <= 0) {
            return Context.UNKNOWN;
        }

        char prevChar = currentLine.charAt(charIndex - 1);

        // 检查是否是 @ 符号
        if (prevChar == '@') {
            return Context.ANNOTATION;
        }

        // 检查是否是 . 符号
        if (prevChar == '.') {
            return Context.DOT;
        }

        // 检查是否是 (
        if (prevChar == '(') {
            return Context.FUNCTION;
        }

        // 检查是否是空格
        if (Character.isWhitespace(prevChar)) {
            return Context.UNKNOWN;
        }

        // 检查是否是修饰符
        if (isModifierStart(prevChar)) {
            return Context.MODIFIER;
        }

        // 检查是否是类型
        if (isTypeStart(prevChar)) {
            return Context.TYPE;
        }

        // 检查是否是变量
        if (isVariableStart(prevChar)) {
            return Context.VARIABLE;
        }

        return Context.UNKNOWN;
    }

    /**
     * 检查是否是修饰符开始字符
     */
    private boolean isModifierStart(char c) {
        return c == ' ' || c == '\t';
    }

    /**
     * 检查是否是类型开始字符
     */
    private boolean isTypeStart(char c) {
        return Character.isUpperCase(c);
    }

    /**
     * 检查是否是变量开始字符
     */
    private boolean isVariableStart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /**
     * 获取变量补全列表
     */
    private List<com.q3lives.lsp.protocol.CompletionItem> getVariableCompletions() {
        List<com.q3lives.lsp.protocol.CompletionItem> completions = new ArrayList<>();

        // TODO: 从语义上下文中获取变量列表
        if (semanticContext != null) {
            // 这里应该从 semanticContext 中提取变量信息
        }

        return completions;
    }

    /**
     * 获取修饰符补全列表
     */
    private List<com.q3lives.lsp.protocol.CompletionItem> getModifierCompletions() {
        List<com.q3lives.lsp.protocol.CompletionItem> completions = new ArrayList<>();

        // TODO: 从语义上下文中获取修饰符列表
        completions.add(com.q3lives.lsp.protocol.CompletionItem.createTypeCompletion("public", "public 修饰符"));
        completions.add(com.q3lives.lsp.protocol.CompletionItem.createTypeCompletion("private", "private 修饰符"));
        completions.add(com.q3lives.lsp.protocol.CompletionItem.createTypeCompletion("protected", "protected 修饰符"));

        return completions;
    }

    /**
     * 获取点操作符补全列表
     */
    private List<com.q3lives.lsp.protocol.CompletionItem> getDotCompletions() {
        List<com.q3lives.lsp.protocol.CompletionItem> completions = new ArrayList<>();

        // TODO: 从语义上下文中获取对象成员列表
        // 这里应该从语义上下文中提取对象字段和方法

        return completions;
    }

    /**
     * 获取补全列表（所有补全）
     */
    public List<com.q3lives.lsp.protocol.CompletionItem> getAllCompletions() {
        List<com.q3lives.lsp.protocol.CompletionItem> completions = new ArrayList<>();
        completions.addAll(TYPE_COMPLETIONS);
        completions.addAll(FUNCTION_COMPLETIONS);
        completions.addAll(ANNOTATION_COMPLETIONS);
        completions.addAll(KEYWORD_COMPLETIONS);
        return completions;
    }

    /**
     * 上下文类型枚举
     */
    protected enum Context {
        TYPE,           // 类型补全
        FUNCTION,       // 函数补全
        VARIABLE,       // 变量补全
        ANNOTATION,     // 注解补全
        KEYWORD,        // 关键字补全
        MODIFIER,       // 修饰符补全
        DOT,            // 点操作符补全
        UNKNOWN         // 未知上下文
    }

    /**
     * 转换自定义 CompletionItem 到 LSP4J CompletionItem
     */
    private List<org.eclipse.lsp4j.CompletionItem> convertToLsp4j(List<com.q3lives.lsp.protocol.CompletionItem> items) {
        List<org.eclipse.lsp4j.CompletionItem> result = new ArrayList<>();
        for (com.q3lives.lsp.protocol.CompletionItem item : items) {
            // Directly add CompletionItem since it extends LSP4J CompletionItem
            result.add(item);
        }
        return result;
    }
}
