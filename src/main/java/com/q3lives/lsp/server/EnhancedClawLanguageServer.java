package com.q3lives.lsp.server;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.q3lives.compiler.ClawCompiler;
import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.compiler.generators.IRGenerator;
import com.q3lives.compiler.pipeline.CompilationResult;
import com.q3lives.ir.ClawIR;
import com.q3lives.lsp.provider.*;
import com.q3lives.lsp.utils.FileWatcher;
import com.q3lives.lsp.utils.CacheManager;
import com.q3lives.lsp.utils.PerformanceMonitor;

/**
 * 增强的 Claw 编译器 LSP 服务器
 *
 * 实现了完整的 LSP 协议功能：
 * 1. 代码补全（CompletionProvider）
 * 2. 语法检查（DiagnosticProvider）
 * 3. 查找引用（ReferenceProvider）
 * 4. 重命名功能（RenameProvider）
 * 5. 文档格式化（FormattingProvider）
 * 6. 文件监视（FileWatcher）
 * 7. 编译结果集成
 * 8. 性能监控
 * 9. 代码操作（CodeAction）
 */
public class EnhancedClawLanguageServer implements LanguageServer, TextDocumentService, WorkspaceService {

    // IR 生成器和编译结果
    private IRGenerator irGenerator;
    private CompilationResult lastCompilationResult;
    private SemanticContext semanticContext;
    private ClawIR currentIR;

    // LSP Providers（使用增强类型）
    private final EnhancedCompletionProvider completionProvider;
    private final EnhancedDiagnosticProvider diagnosticProvider;
    private final EnhancedDefinitionProvider definitionProvider;
    private final EnhancedReferenceProvider referenceProvider;
    private final EnhancedHoverProvider hoverProvider;
    private final EnhancedRenameProvider renameProvider;
    private final EnhancedDocumentSymbolProvider documentSymbolProvider;
    private final EnhancedFormattingProvider formattingProvider;
    private final EnhancedCodeActionProvider codeActionProvider;

    // 文件监视器
    private final FileWatcher fileWatcher;

    // 缓存管理器
    private final CacheManager<String, List<com.q3lives.lsp.protocol.CompletionItem>> completionCache;
    private final CacheManager<String, Object> syntaxCache;

    // 性能监控器
    private final PerformanceMonitor performanceMonitor;

    // 服务器能力
    private ServerCapabilities capabilities;

    // 文档管理
    private final Map<String, TextDocumentItem> documents = new ConcurrentHashMap<>();

    // 工作空间文件夹
    private List<WorkspaceFolder> workspaceFolders = new ArrayList<>();

    public EnhancedClawLanguageServer() {
        // 初始化 IR 生成器
        try {
            this.irGenerator = new IRGenerator("");
        } catch (UnsupportedOperationException e) {
            System.err.println("IRGenerator not fully implemented, using mock: " + e.getMessage());
            this.irGenerator = new IRGenerator("__lsp_mock__");
        }

        // 初始化编译结果缓存
        this.lastCompilationResult = CompilationResult.mock("__lsp__");

        // 初始化语义上下文
        this.semanticContext = new SemanticContext();

        // 初始化 LSP Providers
        this.completionProvider = new EnhancedCompletionProvider(semanticContext, null);
        this.diagnosticProvider = new EnhancedDiagnosticProvider(semanticContext, completionProvider);
        this.definitionProvider = new EnhancedDefinitionProvider(semanticContext);
        this.referenceProvider = new EnhancedReferenceProvider(semanticContext);
        this.hoverProvider = new EnhancedHoverProvider(semanticContext, completionProvider);
        this.renameProvider = new EnhancedRenameProvider(semanticContext, completionProvider);
        this.documentSymbolProvider = new EnhancedDocumentSymbolProvider(semanticContext);
        this.formattingProvider = new EnhancedFormattingProvider(semanticContext);
        this.codeActionProvider = new EnhancedCodeActionProvider(semanticContext);

        // 初始化文件监视器
        this.fileWatcher = new FileWatcher();

        // 初始化缓存管理器
        this.completionCache = CacheManager.completionCache();
        this.syntaxCache = CacheManager.syntaxCache();

        // 初始化性能监控器
        this.performanceMonitor = PerformanceMonitor.getInstance();

        // 初始化服务器能力
        initializeCapabilities();
    }

    /**
     * 初始化服务器能力
     */
    private void initializeCapabilities() {
        capabilities = new ServerCapabilities();

        // 1. 代码补全
        CompletionOptions completionOptions = new CompletionOptions();
        completionOptions.setTriggerCharacters(List.of(".", "(", " ", "@", "["));
        capabilities.setCompletionProvider(completionOptions);

        // 2. 语法诊断
        capabilities.setDiagnosticProvider(new DiagnosticRegistrationOptions(true, false));

        // 3. 跳转定义
        capabilities.setDefinitionProvider(true);
        capabilities.setImplementationProvider(true);
        capabilities.setTypeDefinitionProvider(true);

        // 4. 查找引用
        capabilities.setReferencesProvider(true);

        // 5. 重命名
        capabilities.setRenameProvider(new RenameOptions(true));

        // 6. Hover 信息
        capabilities.setHoverProvider(true);

        // 7. 文档符号
        capabilities.setDocumentSymbolProvider(true);

        // 8. 文档格式化
        capabilities.setDocumentFormattingProvider(true);
        capabilities.setDocumentRangeFormattingProvider(true);

        // 9. 代码操作
        capabilities.setCodeActionProvider(true);

        // 10. 工作空间符号
        capabilities.setWorkspaceSymbolProvider(true);
    }

    // ==================== LanguageServer 接口 ====================

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        InitializeResult result = new InitializeResult(capabilities);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    // ==================== TextDocumentService 接口 ====================

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
        long startTime = performanceMonitor.start("completion");

        try {
            TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
            if (doc == null) {
                return CompletableFuture.completedFuture(Either.forRight(new CompletionList(false, Collections.emptyList())));
            }

            CompletionList result = completionProvider.provideCompletion(doc.getText(), params.getPosition());
            return CompletableFuture.completedFuture(Either.forRight(result));
        } catch (Exception e) {
            System.err.println("Error in completion: " + e.getMessage());
            return CompletableFuture.completedFuture(Either.forRight(new CompletionList(false, Collections.emptyList())));
        } finally {
            performanceMonitor.end("completion", startTime);
        }
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
        return CompletableFuture.completedFuture(unresolved);
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        long startTime = performanceMonitor.start("hover");

        try {
            TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
            if (doc == null) {
                return CompletableFuture.completedFuture(new Hover(new MarkupContent(MarkupKind.PLAINTEXT, "")));
            }

            Hover result = hoverProvider.provideHover(doc.getText(), params.getPosition());
            return CompletableFuture.completedFuture(result != null ? result : new Hover(new MarkupContent(MarkupKind.PLAINTEXT, "")));
        } catch (Exception e) {
            System.err.println("Error in hover: " + e.getMessage());
            return CompletableFuture.completedFuture(new Hover(new MarkupContent(MarkupKind.PLAINTEXT, "")));
        } finally {
            performanceMonitor.end("hover", startTime);
        }
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
        long startTime = performanceMonitor.start("definition");

        try {
            TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
            if (doc == null) {
                return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
            }

            List<Location> result = definitionProvider.provideDefinition(doc.getText(), params.getPosition());
            return CompletableFuture.completedFuture(Either.forLeft(result));
        } catch (Exception e) {
            System.err.println("Error in definition: " + e.getMessage());
            return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
        } finally {
            performanceMonitor.end("definition", startTime);
        }
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        long startTime = performanceMonitor.start("references");

        try {
            TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
            if (doc == null) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            List<? extends Location> result = referenceProvider.provideReferences(doc.getText(), params.getPosition());
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            System.err.println("Error in references: " + e.getMessage());
            return CompletableFuture.completedFuture(Collections.emptyList());
        } finally {
            performanceMonitor.end("references", startTime);
        }
    }

    @Override
    public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams params) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
        long startTime = performanceMonitor.start("documentSymbol");

        try {
            TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
            if (doc == null) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            List<? extends DocumentSymbol> symbols = documentSymbolProvider.provideDocumentSymbols(doc.getText());
            List<Either<SymbolInformation, DocumentSymbol>> result = new ArrayList<>();
            for (DocumentSymbol symbol : symbols) {
                result.add(Either.forRight(symbol));
            }
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            System.err.println("Error in documentSymbol: " + e.getMessage());
            return CompletableFuture.completedFuture(Collections.emptyList());
        } finally {
            performanceMonitor.end("documentSymbol", startTime);
        }
    }

    @Override
    public CompletableFuture<List<DocumentLink>> documentLink(DocumentLinkParams params) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
        long startTime = performanceMonitor.start("rename");

        try {
            TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
            if (doc == null) {
                return CompletableFuture.completedFuture(new WorkspaceEdit(Collections.emptyMap()));
            }

            WorkspaceEdit result = renameProvider.provideRename(doc.getText(), params.getPosition(), params.getNewName());
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            System.err.println("Error in rename: " + e.getMessage());
            return CompletableFuture.completedFuture(new WorkspaceEdit(Collections.emptyMap()));
        } finally {
            performanceMonitor.end("rename", startTime);
        }
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        long startTime = performanceMonitor.start("codeAction");

        try {
            List<CodeAction> actions = codeActionProvider.provideCodeActions(params);
            List<Either<Command, CodeAction>> result = new ArrayList<>();
            for (CodeAction action : actions) {
                result.add(Either.forRight(action));
            }
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            System.err.println("Error in codeAction: " + e.getMessage());
            return CompletableFuture.completedFuture(Collections.emptyList());
        } finally {
            performanceMonitor.end("codeAction", startTime);
        }
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
        long startTime = performanceMonitor.start("formatting");

        try {
            TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
            if (doc == null) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            List<TextEdit> result = formattingProvider.provideFormatting(doc.getText());
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            System.err.println("Error in formatting: " + e.getMessage());
            return CompletableFuture.completedFuture(Collections.emptyList());
        } finally {
            performanceMonitor.end("formatting", startTime);
        }
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        documents.put(params.getTextDocument().getUri(), params.getTextDocument());
        compileDocument(params.getTextDocument());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
        if (doc != null) {
            StringBuilder content = new StringBuilder(doc.getText());
            for (TextDocumentContentChangeEvent change : params.getContentChanges()) {
                if (change.getRange() == null) {
                    // 全量替换
                    content = new StringBuilder(change.getText());
                } else {
                    // 增量替换：将 LSP 的 Range（行+列）转换为字符串偏移量
                    int startOffset = positionToOffset(content.toString(), change.getRange().getStart());
                    int endOffset = positionToOffset(content.toString(), change.getRange().getEnd());
                    content.replace(startOffset, endOffset, change.getText());
                }
            }
            doc.setText(content.toString());
            compileDocument(doc);
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        documents.remove(uri);
        // 清除该文档的编译错误和缓存
        diagnosticProvider.clearCompilationErrors(uri);
        completionCache.remove(uri);
        syntaxCache.remove(uri);
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
        if (doc != null) {
            compileDocument(doc);
        }
    }

    // ==================== WorkspaceService 接口 ====================

    @Override
    public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
        if (params.getEvent() != null && params.getEvent().getAdded() != null) {
            for (WorkspaceFolder folder : params.getEvent().getAdded()) {
                workspaceFolders.add(folder);
            }
        }
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        // 处理配置更改
        Object configuration = params.getSettings();
        if (configuration != null) {
            updateConfiguration(configuration);
        }
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        // 处理文件系统事件
        for (FileEvent event : params.getChanges()) {
            System.out.println("File changed: " + event.getUri() + " type=" + event.getType());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 编译文档
     */
    private void compileDocument(TextDocumentItem document) {
        String uri = document.getUri();
        try {
            long startTime = performanceMonitor.start("compile");

            String content = document.getText();
            String fileName = uri.substring(uri.lastIndexOf('/') + 1);
            if (fileName.isEmpty()) {
                fileName = "unnamed.claw";
            }

            // 调用编译器进行真实编译
            ClawCompiler compiler = new ClawCompiler();
            CompilationResult result = compiler.compile(content, fileName);

            lastCompilationResult = result;
            if (result.isSuccess()) {
                currentIR = result.getIR(); // CompilationPipeline 当前不返回 ClawIR，但保留接口
            }

            // 将编译错误传递给 diagnosticProvider
            diagnosticProvider.setCompilationErrors(uri, result.getErrors());

            // 清除相关缓存（补全和语法缓存因源码变更而失效）
            completionCache.remove(uri);
            syntaxCache.remove(uri);

            performanceMonitor.end("compile", startTime);
        } catch (Exception e) {
            System.err.println("Error compiling document: " + e.getMessage());
            e.printStackTrace();
            // 记录异常为编译错误
            diagnosticProvider.setCompilationErrors(uri,
                Collections.singletonList("编译异常: " + e.getMessage()));
        }
    }

    /**
     * 更新配置
     */
    private void updateConfiguration(Object configuration) {
        completionProvider.updateConfiguration(configuration);
        formattingProvider.updateConfiguration(configuration);
    }

    /**
     * 将 LSP Position（行+列）转换为字符串中的字符偏移量
     */
    private int positionToOffset(String text, Position position) {
        int targetLine = position.getLine();
        int targetCharacter = position.getCharacter();
        int offset = 0;
        int currentLine = 0;

        // 定位到目标行
        while (offset < text.length() && currentLine < targetLine) {
            if (text.charAt(offset) == '\n') {
                currentLine++;
            }
            offset++;
        }

        // 定位到目标列（不超过行尾）
        int lineStart = offset;
        while (offset - lineStart < targetCharacter
               && offset < text.length()
               && text.charAt(offset) != '\n') {
            offset++;
        }

        return offset;
    }

    // ==================== Getter 方法 ====================

    @Override
    public TextDocumentService getTextDocumentService() {
        return this;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return this;
    }

    public ServerCapabilities getCapabilities() {
        return capabilities;
    }

    /**
     * 获取性能报告
     */
    public String getPerformanceReport() {
        return performanceMonitor.getPerformanceReport();
    }

    /**
     * 获取缓存统计
     */
    public String getCacheStatistics() {
        return "Completion cache: " + completionCache.getStats() + "\nSyntax cache: " + syntaxCache.getStats();
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        completionCache.clear();
        syntaxCache.clear();
    }
}
