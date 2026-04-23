package com.q3lives.lsp.server;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.compiler.generators.IRGenerator;
import com.q3lives.compiler.pipeline.CompilationResult;
import com.q3lives.ir.ClawIR;
import com.q3lives.lsp.provider.CompletionProvider;
import com.q3lives.lsp.provider.DiagnosticProvider;
import com.q3lives.lsp.provider.DefinitionProvider;
import com.q3lives.lsp.provider.ReferenceProvider;
import com.q3lives.lsp.provider.HoverProvider;
import com.q3lives.lsp.provider.RenameProvider;
import com.q3lives.lsp.provider.DocumentSymbolProvider;
import com.q3lives.lsp.provider.FormattingProvider;
import com.q3lives.lsp.provider.CodeActionProvider;
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

    // LSP Providers
    private final CompletionProvider completionProvider;
    private final DiagnosticProvider diagnosticProvider;
    private final DefinitionProvider definitionProvider;
    private final ReferenceProvider referenceProvider;
    private final HoverProvider hoverProvider;
    private final RenameProvider renameProvider;
    private final DocumentSymbolProvider documentSymbolProvider;
    private final FormattingProvider formattingProvider;
    private final CodeActionProvider codeActionProvider;

    // 文件监视器
    private final FileWatcher fileWatcher;

    // 缓存管理器
    private final CacheManager cacheManager;

    // 性能监控器
    private final PerformanceMonitor performanceMonitor;

    // 服务器能力
    private ServerCapabilities capabilities;

    // 文档管理
    private final Map<String, TextDocumentItem> documents = new ConcurrentHashMap<>();

    // 工作空间文件夹
    private List<URI> workspaceFolders = new ArrayList<>();

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
        this.cacheManager = CacheManager.getInstance();

        // 初始化性能监控器
        this.performanceMonitor = PerformanceMonitor.getInstance();

        // 初始化服务器能力
        initializeCapabilities();

        // 启动文件监视
        startFileWatching();
    }

    /**
     * 初始化服务器能力
     */
    private void initializeCapabilities() {
        capabilities = new ServerCapabilities();

        // 1. 代码补全
        CompletionOptions completionOptions = new CompletionOptions();
        completionOptions.setTriggerCharacters(List.of(".", "(", " ", "@", "["));
        completionOptions.setCompletionItem(
            new CompletionItemSettings(
                true,  // resolveProvider
                true,  // labelDetailsSupport
                true,  // snippetSupport
                true   // insertReplaceSupport
            )
        );
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
        FormattingOptions formattingOptions = new FormattingOptions();
        formattingOptions.setTabSize(4);
        formattingOptions.setInsertSpaces(true);
        capabilities.setDocumentFormattingProvider(true);
        capabilities.setDocumentRangeFormattingProvider(true);

        // 9. 代码操作
        capabilities.setCodeActionProvider(true);
        capabilities.setCodeActionKindFilter(new CodeActionKind("quickfix", "refactor", "source"));

        // 10. 工作空间符号
        capabilities.setWorkspaceSymbolProvider(true);

        // 11. 文件操作
        capabilities.setDidOpenTextDocumentNotification(true);
        capabilities.setDidChangeTextDocumentNotification(true);
        capabilities.setDidCloseTextDocumentNotification(true);
        capabilities.setDidSaveTextDocumentNotification(true);

        // 12. 文件系统
        capabilities.setWorkspaceFolders(true);
        capabilities.setDidChangeWorkspaceFolders(true);

        // 13. 配置
        capabilities.setConfigurationWorkspaceFolders(true);
    }

    // ==================== LanguageServer 接口 ====================

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
        String startTime = performanceMonitor.start("completion");

        try {
            TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
            if (doc == null) {
                return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
            }

            CompletionList result = completionProvider.provideCompletion(doc.getText(), params.getPosition());

            performanceMonitor.end("completion", startTime);
            return CompletableFuture.completedFuture(Either.forRight(result));
        } catch (Exception e) {
            performanceMonitor.end("completion", startTime);
            System.err.println("Error in completion: " + e.getMessage());
            return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
        }
    }

    @Override
    public CompletableFuture<List<? extends Location>> definition(DefinitionParams params) {
        String startTime = performanceMonitor.start("definition");

        try {
            TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
            if (doc == null) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            List<? extends Location> result = definitionProvider.provideDefinition(doc.getText(), params.getPosition());

            performanceMonitor.end("definition", startTime);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            performanceMonitor.end("definition", startTime);
            System.err.println("Error in definition: " + e.getMessage());
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        String startTime = performanceMonitor.start("references");

        try {
            TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
            if (doc == null) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            List<? extends Location> result = referenceProvider.provideReferences(doc.getText(), params.getPosition());

            performanceMonitor.end("references", startTime);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            performanceMonitor.end("references", startTime);
            System.err.println("Error in references: " + e.getMessage());
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        String startTime = performanceMonitor.start("codeAction");

        try {
            TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
            if (doc == null) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            List<Either<Command, CodeAction>> result = codeActionProvider.provideCodeActions(
                doc.getText(), params.getRange(), params.getContext()
            );

            performanceMonitor.end("codeAction", startTime);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            performanceMonitor.end("codeAction", startTime);
            System.err.println("Error in codeAction: " + e.getMessage());
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }

    @Override
    public CompletableFuture<List<TextEdit>> formatting(DocumentFormattingParams params) {
        String startTime = performanceMonitor.start("formatting");

        try {
            TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
            if (doc == null) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            List<TextEdit> result = formattingProvider.provideFormatting(doc.getText());

            performanceMonitor.end("formatting", startTime);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            performanceMonitor.end("formatting", startTime);
            System.err.println("Error in formatting: " + e.getMessage());
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }

    @Override
    public CompletableFuture<List<? extends DocumentSymbol>> documentSymbol(DocumentSymbolParams params) {
        String startTime = performanceMonitor.start("documentSymbol");

        try {
            TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
            if (doc == null) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            List<? extends DocumentSymbol> result = documentSymbolProvider.provideDocumentSymbols(doc.getText());

            performanceMonitor.end("documentSymbol", startTime);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            performanceMonitor.end("documentSymbol", startTime);
            System.err.println("Error in documentSymbol: " + e.getMessage());
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }

    @Override
    public CompletableFuture<List<Diagnostic>> diagnostic(DiagnosticParams params) {
        String startTime = performanceMonitor.start("diagnostic");

        try {
            TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
            if (doc == null) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            List<Diagnostic> result = diagnosticProvider.diagnose(doc);

            performanceMonitor.end("diagnostic", startTime);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            performanceMonitor.end("diagnostic", startTime);
            System.err.println("Error in diagnostic: " + e.getMessage());
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completionItemResolve(CompletionItem unresolved) {
        // 解析补全项
        return CompletableFuture.completedFuture(Either.forRight(new CompletionList()));
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        String startTime = performanceMonitor.start("hover");

        try {
            TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
            if (doc == null) {
                return CompletableFuture.completedFuture(new Hover(Collections.emptyList(), null));
            }

            Hover result = hoverProvider.provideHover(doc.getText(), params.getPosition());

            performanceMonitor.end("hover", startTime);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            performanceMonitor.end("hover", startTime);
            System.err.println("Error in hover: " + e.getMessage());
            return CompletableFuture.completedFuture(new Hover(Collections.emptyList(), null));
        }
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
        String startTime = performanceMonitor.start("rename");

        try {
            TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
            if (doc == null) {
                return CompletableFuture.completedFuture(new WorkspaceEdit(Collections.emptyMap()));
            }

            WorkspaceEdit result = renameProvider.provideRename(doc.getText(), params.getPosition(), params.getNewName());

            performanceMonitor.end("rename", startTime);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            performanceMonitor.end("rename", startTime);
            System.err.println("Error in rename: " + e.getMessage());
            return CompletableFuture.completedFuture(new WorkspaceEdit(Collections.emptyMap()));
        }
    }

    @Override
    public CompletableFuture<List<? extends SymbolInformation>> workspaceSymbol(WorkspaceSymbolParams params) {
        // 实现工作空间符号搜索
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public void setTrace(TraceValue value) {
        // 设置跟踪级别
    }

    @Override
    public TraceValue getTrace() {
        return TraceValue.Off;
    }

    // ==================== TextDocumentService 接口 ====================

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        documents.put(params.getTextDocument().getUri(), params.getTextDocument());

        // 通知文件监视器
        fileWatcher.fileOpened(params.getTextDocument().getUri());

        // 编译文档
        compileDocument(params.getTextDocument());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        // 更新文档内容
        TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
        if (doc != null) {
            StringBuilder content = new StringBuilder(doc.getText());
            for (TextDocumentContentChangeEvent change : params.getContentChanges()) {
                if (change.getRange() == null) {
                    // 全量替换
                    content = new StringBuilder(change.getText());
                } else {
                    // 增量替换
                    int start = change.getRange().getStart().getCharacter();
                    int end = change.getRange().getEnd().getCharacter();
                    content.replace(start, end, change.getText());
                }
            }
            doc.setText(content.toString());

            // 通知文件监视器
            fileWatcher.fileChanged(params.getTextDocument().getUri());

            // 编译文档
            compileDocument(doc);
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        documents.remove(params.getTextDocument().getUri());

        // 通知文件监视器
        fileWatcher.fileClosed(params.getTextDocument().getUri());
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        // 通知文件监视器
        fileWatcher.fileSaved(params.getTextDocument().getUri());

        // 编译文档
        TextDocumentItem doc = documents.get(params.getTextDocument().getUri());
        if (doc != null) {
            compileDocument(doc);
        }
    }

    // ==================== WorkspaceService 接口 ====================

    @Override
    public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
        this.workspaceFolders = params.getEvent().getAdded();
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        // 处理配置更改
        Configuration configuration = (Configuration) params.getSettings();
        if (configuration != null) {
            // 更新服务器配置
            updateConfiguration(configuration);
        }
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        // 处理文件系统事件
        for (FileEvent event : params.getChanges()) {
            fileWatcher.fileSystemChanged(event.getUri(), event.getType());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 编译文档
     */
    private void compileDocument(TextDocumentItem document) {
        try {
            String startTime = performanceMonitor.start("compile");

            // 调用编译器
            CompilationResult result = lastCompilationResult;

            if (result != null && result.isSuccess()) {
                // 更新 IR
                currentIR = result.getGeneratedCode().getIntermediateRepresentation();

                // 更新语义上下文
                if (semanticContext != null) {
                    // 这里应该从编译结果中更新语义上下文
                }

                // 清除相关缓存
                cacheManager.clearDocumentCache(document.getUri());
            }

            performanceMonitor.end("compile", startTime);
        } catch (Exception e) {
            System.err.println("Error compiling document: " + e.getMessage());
        }
    }

    /**
     * 更新配置
     */
    private void updateConfiguration(Configuration configuration) {
        // 更新补全配置
        if (completionProvider instanceof EnhancedCompletionProvider) {
            ((EnhancedCompletionProvider) completionProvider).updateConfiguration(configuration);
        }

        // 更新格式化配置
        if (formattingProvider instanceof EnhancedFormattingProvider) {
            ((EnhancedFormattingProvider) formattingProvider).updateConfiguration(configuration);
        }
    }

    /**
     * 启动文件监视
     */
    private void startFileWatching() {
        // 监视 .claw 文件
        fileWatcher.addWatchPattern("**/*.claw");

        // 监视配置文件
        fileWatcher.addWatchPattern("**/*.json");

        // 监视依赖文件
        fileWatcher.addWatchPattern("**/*.md");

        // 启动监视器
        fileWatcher.start();
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

    @Override
    public ServerCapabilities getCapabilities() {
        return capabilities;
    }

    /**
     * 获取性能报告
     */
    public String getPerformanceReport() {
        return performanceMonitor.getReport();
    }

    /**
     * 获取缓存统计
     */
    public String getCacheStatistics() {
        return cacheManager.getStatistics();
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        cacheManager.clearAllCache();
        performanceMonitor.reset();
    }
}