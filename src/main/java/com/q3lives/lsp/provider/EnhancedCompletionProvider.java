package com.q3lives.lsp.provider;

import com.q3lives.ir.ClawIR;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import com.q3lives.lsp.protocol.CompletionItem;
import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.compiler.hierarchy.CodeBlock;
import com.q3lives.lsp.utils.CacheManager;
import com.q3lives.lsp.utils.PerformanceMonitor;

/**
 * 增强版 LSP 代码补全提供器
 *
 * 提供智能的代码补全功能：
 * - 上下文感知的补全
 * - 智能类型推导
 * - 代码片段支持
 * - 性能优化
 */
public class EnhancedCompletionProvider extends CompletionProvider {

    private final SemanticContext semanticContext;
    private final ClawIR ir;
    private final CacheManager<String, List<CompletionItem>> completionCache;
    private final PerformanceMonitor performanceMonitor;
    private static final String OPERATION_NAME = "completion";

    // 补全触发字符
    private static final List<String> TRIGGER_CHARS = List.of(".", "(", " ", "@", "[", "<");

    // 代码片段
    private static final Map<String, CompletionItem> SNIPPETS = Map.of(
        "function", CompletionItem.createSnippetCompletion(
            "function", "function ${1:name}(${2:params}) -> ${3:return_type}", "函数定义"
        ),
        "if", CompletionItem.createSnippetCompletion(
            "if", "if ${1:condition} {\n    ${2:// body}\n}", "if语句"
        ),
        "while", CompletionItem.createSnippetCompletion(
            "while", "while ${1:condition} {\n    ${2:// body}\n}", "while循环"
        ),
        "for", CompletionItem.createSnippetCompletion(
            "for", "for (${1:var} in ${2:collection}) {\n    ${3:// body}\n}", "for循环"
        ),
        "try", CompletionItem.createSnippetCompletion(
            "try", "try {\n    ${1:// body}\n} catch (${2:exception}) {\n    ${3:// handler}\n}", "try-catch"
        )
    );

    public EnhancedCompletionProvider(SemanticContext semanticContext, ClawIR ir) {
        super(semanticContext, ir);
        this.semanticContext = semanticContext;
        this.ir = ir;
        this.completionCache = CacheManager.completionCache();
        this.performanceMonitor = PerformanceMonitor.getInstance();
    }

    /**
     * 提供增强的代码补全
     */
    @Override
    public org.eclipse.lsp4j.CompletionList provideCompletion(String document, Position position) {
        long startTime = performanceMonitor.start(OPERATION_NAME);

        try {
            // 生成缓存键
            String contextStr = getContextString(document, position);
            String cacheKey = generateCacheKey(document, position, contextStr);

            // 尝试从缓存获取
            List<CompletionItem> cached = completionCache.get(cacheKey);
            if (cached != null) {
                performanceMonitor.end(OPERATION_NAME, startTime);
                return new org.eclipse.lsp4j.CompletionList(false, convertToLsp4j(cached));
            }

            // 识别当前上下文
            Context context = analyzeContextEnhanced(document, position);

            // 根据上下文提供补全
            List<CompletionItem> completions = new ArrayList<>();

            switch (context) {
                case TYPE:
                    completions.addAll(getTypeCompletions());
                    break;
                case FUNCTION:
                    completions.addAll(FUNCTION_COMPLETIONS);
                    completions.addAll(getSnippetCompletions("function"));
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
                case SNIPPET:
                    completions.addAll(getAllSnippets());
                    break;
                case UNKNOWN:
                    // 返回所有补全
                    completions.addAll(getAllCompletions());
                    break;
            }

            // 添加智能建议
            completions.addAll(getSmartCompletions(context));

            // 按标签排序
            completions.sort((a, b) -> a.getLabel().compareToIgnoreCase(b.getLabel()));

            // 缓存结果
            completionCache.put(cacheKey, completions);

            performanceMonitor.end(OPERATION_NAME, startTime);
            return new org.eclipse.lsp4j.CompletionList(false, convertToLsp4j(completions));

        } catch (Exception e) {
            performanceMonitor.end(OPERATION_NAME, startTime);
            System.err.println("Error providing completion: " + e.getMessage());
            return new org.eclipse.lsp4j.CompletionList(false, Collections.emptyList());
        }
    }

    /**
     * 增强的上下文分析
     */
    private Context analyzeContextEnhanced(String document, Position position) {
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
        String linePrefix = currentLine.substring(0, charIndex);

        // 检查代码片段触发
        if (linePrefix.trim().startsWith("fun")) {
            return Context.SNIPPET;
        }
        if (linePrefix.trim().startsWith("if ")) {
            return Context.SNIPPET;
        }
        if (linePrefix.trim().startsWith("wh")) {
            return Context.SNIPPET;
        }
        if (linePrefix.trim().startsWith("for ")) {
            return Context.SNIPPET;
        }
        if (linePrefix.trim().startsWith("try ")) {
            return Context.SNIPPET;
        }

        // 检查是否是 @ 符号
        if (linePrefix.endsWith("@")) {
            return Context.ANNOTATION;
        }

        // 检查是否是 . 符号
        if (linePrefix.endsWith(".")) {
            return Context.DOT;
        }

        // 检查是否是函数定义
        if (linePrefix.trim().startsWith("function ")) {
            return Context.FUNCTION;
        }

        // 检查是否是 (
        if (linePrefix.endsWith("(")) {
            return Context.FUNCTION;
        }

        // 检查是否是类型定义
        if (linePrefix.trim().startsWith("type ")) {
            return Context.TYPE;
        }

        // 检查是否是修饰符
        if (!linePrefix.isEmpty() && isModifierStart(linePrefix.charAt(linePrefix.length() - 1))) {
            return Context.MODIFIER;
        }

        // 检查是否是变量
        if (!linePrefix.isEmpty() && isVariableStart(linePrefix.charAt(linePrefix.length() - 1))) {
            return Context.VARIABLE;
        }

        return Context.UNKNOWN;
    }

    /**
     * 获取智能补全建议
     */
    private List<CompletionItem> getSmartCompletions(Context context) {
        List<CompletionItem> completions = new ArrayList<>();

        // 基于上下文添加智能建议
        switch (context) {
            case FUNCTION:
                // 添加常用函数
                completions.add(CompletionItem.createFunctionCompletion(
                    "log", "String", "Void", "日志输出"
                ));
                completions.add(CompletionItem.createFunctionCompletion(
                    "debug", "String", "Void", "调试输出"
                ));
                break;
            case DOT:
                // 添加常用对象方法
                completions.add(CompletionItem.createMethodCompletion(
                    "length", "Int", "获取长度"
                ));
                completions.add(CompletionItem.createMethodCompletion(
                    "toString", "String", "转换为字符串"
                ));
                break;
        }

        return completions;
    }

    /**
     * 获取所有代码片段
     */
    private List<CompletionItem> getAllSnippets() {
        return new ArrayList<>(SNIPPETS.values());
    }

    /**
     * 获取特定类型的代码片段
     */
    private List<CompletionItem> getSnippetCompletions(String type) {
        return SNIPPETS.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(type))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    /**
     * 获取类型补全（增强版）
     */
    private List<CompletionItem> getTypeCompletions() {
        List<CompletionItem> completions = new ArrayList<>();

        // 基础类型
        completions.addAll(super.getAllCompletions());

        // 自定义类型（从 IR 中获取）
        if (ir != null) {
            // 这里应该从 IR 中提取自定义类型
            // completions.addAll(extractCustomTypes());
        }

        // 泛型类型
        completions.add(CompletionItem.createTypeCompletion(
            "List<T>", "泛型列表"
        ));
        completions.add(CompletionItem.createTypeCompletion(
            "Map<K,V>", "泛型映射"
        ));
        completions.add(CompletionItem.createTypeCompletion(
            "Set<T>", "泛型集合"
        ));

        return completions;
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(String document, Position position, String context) {
        return String.format("%d:%d:%s:%s",
            position.getLine(),
            position.getCharacter(),
            document.hashCode(),
            context
        );
    }

    /**
     * 获取上下文字符串
     */
    private String getContextString(String document, Position position) {
        int line = position.getLine();
        int charIndex = position.getCharacter();

        if (line < 0 || line >= document.split("\n").length) {
            return "unknown";
        }

        String lineText = document.split("\n")[line];
        if (charIndex <= 0) {
            return "unknown";
        }

        return lineText.substring(0, charIndex);
    }

    /**
     * 更新配置
     */
    public void updateConfiguration(Object configuration) {
        // 根据配置更新补全行为
        // 例如：是否启用代码片段、是否显示文档等
    }
}