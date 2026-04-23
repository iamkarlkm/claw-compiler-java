package com.claw.lsp.idea;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.util.concurrent.CompletableFuture;

/**
 * Claw LSP 服务器端点
 *
 * IntelliJ IDEA LSP 插件端点实现
 */
public class ClawLspServerEndpoint implements TextDocumentService, WorkspaceService {

    private final Project project;
    private LanguageServer languageServer;

    public ClawLspServerEndpoint(Project project) {
        this.project = project;
    }

    /**
     * 设置语言服务器实例
     */
    public void setLanguageServer(LanguageServer server) {
        this.languageServer = server;
    }

    // ===== TextDocumentService =====

    @Override
    public CompletableFuture<CompletionList> completion(CompletionParams params) {
        if (languageServer == null) {
            return CompletableFuture.completedFuture(new CompletionList(Collections.emptyList(), false));
        }

        TextDocumentIdentifier textDocument = params.getTextDocument();
        Position position = params.getPosition();

        // 获取文件内容
        VirtualFile file = getFile(textDocument.getUri());
        if (file == null) {
            return CompletableFuture.completedFuture(new CompletionList(Collections.emptyList(), false));
        }

        String document = file.getText();

        return CompletableFuture.supplyAsync(() -> {
            return languageServer.getTextDocumentService().completion(params);
        });
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        if (languageServer == null) {
            return CompletableFuture.completedFuture(new Hover("", null));
        }

        return CompletableFuture.supplyAsync(() -> {
            return languageServer.getTextDocumentService().hover(params);
        });
    }

    @Override
    public CompletableFuture<List<? extends Location>> definition(DefinitionParams params) {
        if (languageServer == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> {
            return languageServer.getTextDocumentService().definition(params);
        });
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        if (languageServer == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> {
            return languageServer.getTextDocumentService().references(params);
        });
    }

    @Override
    public CompletableFuture<Diagnostic[]> documentHighlight(DocumentHighlightParams params) {
        if (languageServer == null) {
            return CompletableFuture.completedFuture(new Diagnostic[0]);
        }

        return CompletableFuture.supplyAsync(() -> {
            return languageServer.getTextDocumentService().documentHighlight(params);
        });
    }

    @Override
    public CompletableFuture<List<? extends SymbolInformation>> documentSymbol(DocumentSymbolParams params) {
        if (languageServer == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> {
            return languageServer.getTextDocumentService().documentSymbol(params);
        });
    }

    @Override
    public CompletableFuture<List<? extends DocumentLink>> documentLink(DocumentLinkParams params) {
        if (languageServer == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> {
            return languageServer.getTextDocumentService().documentLink(params);
        });
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
        if (languageServer == null) {
            return CompletableFuture.completedFuture(new WorkspaceEdit(Collections.emptyMap()));
        }

        return CompletableFuture.supplyAsync(() -> {
            return languageServer.getTextDocumentService().rename(params);
        });
    }

    // ===== WorkspaceService =====

    @Override
    public CompletableFuture<ApplyWorkspaceEditResponse> applyEdit(ApplyWorkspaceEditParams params) {
        if (languageServer == null) {
            return CompletableFuture.completedFuture(new ApplyWorkspaceEditResponse(true));
        }

        return CompletableFuture.supplyAsync(() -> {
            return languageServer.getWorkspaceService().applyEdit(params);
        });
    }

    // ===== Helper Methods =====

    /**
     * 根据 URI 获取虚拟文件
     */
    private VirtualFile getFile(String uri) {
        try {
            return com.intellij.openapi.util.io.FileUtil.toFile(new java.net.URI(uri));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前文件
     */
    public PsiFile getCurrentFile() {
        PsiFile[] files = project.getOpenedFiles();
        for (PsiFile file : files) {
            if (file.getLanguage().getID().equals("Claw")) {
                return file;
            }
        }
        return null;
    }

    /**
     * 获取语言服务器状态
     */
    public boolean isServerConnected() {
        return languageServer != null;
    }
}
