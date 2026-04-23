package com.claw.lsp.idea.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerAdapter;
import com.intellij.ui.content.ContentManagerEvent;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;

import com.q3lives.lsp.server.EnhancedClawLanguageServer;
import com.q3lives.lsp.utils.CacheManager;
import com.q3lives.lsp.utils.PerformanceMonitor;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

/**
 * Claw LSP 服务管理器
 *
 * 负责管理LSP服务器的生命周期：
 * - 启动/停止服务器
 * - 管理缓存
 * - 监控性能
 * - 提供统一的访问接口
 */
@Service
public final class ClawLspServiceManager {

    private EnhancedClawLanguageServer languageServer;
    private final CacheManager cacheManager;
    private final PerformanceMonitor performanceMonitor;
    private final Project project;
    private boolean isRunning = false;

    public ClawLspServiceManager(@NotNull Project project) {
        this.project = project;
        this.cacheManager = CacheManager.getInstance();
        this.performanceMonitor = PerformanceMonitor.getInstance();
    }

    /**
     * 启动LSP服务器
     */
    public synchronized void startServer() {
        if (isRunning) {
            return;
        }

        try {
            // 创建语言服务器实例
            languageServer = new EnhancedClawLanguageServer();

            // 注册服务器
            registerServer();

            isRunning = true;

            // 启动性能监控
            performanceMonitor.startMonitoring();

        } catch (Exception e) {
            showError("Failed to start LSP server: " + e.getMessage());
        }
    }

    /**
     * 停止LSP服务器
     */
    public synchronized void stopServer() {
        if (!isRunning || languageServer == null) {
            return;
        }

        try {
            // 停止性能监控
            performanceMonitor.stopMonitoring();

            // 清理资源
            if (languageServer != null) {
                // 注销服务器
                unregisterServer();

                languageServer = null;
            }

            isRunning = false;

        } catch (Exception e) {
            showError("Failed to stop LSP server: " + e.getMessage());
        }
    }

    /**
     * 重启LSP服务器
     */
    public synchronized void restartServer() {
        stopServer();

        // 短暂延迟确保完全停止
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        startServer();
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        try {
            // 清除LSP服务器缓存
            if (languageServer != null) {
                languageServer.clearAllCache();
            }

            // 清除文件监视缓存
            cacheManager.clearAllCache();

            // 重置性能监控
            performanceMonitor.reset();

            showInfo("All caches cleared successfully");

        } catch (Exception e) {
            showError("Failed to clear cache: " + e.getMessage());
        }
    }

    /**
     * 显示性能报告
     */
    public void showPerformanceReport() {
        if (languageServer == null) {
            showError("LSP server is not running");
            return;
        }

        try {
            // 获取性能报告
            String performanceReport = languageServer.getPerformanceReport();
            String cacheStatistics = languageServer.getCacheStatistics();

            // 创建性能报告窗口
            createPerformanceReportWindow(performanceReport, cacheStatistics);

        } catch (Exception e) {
            showError("Failed to get performance report: " + e.getMessage());
        }
    }

    /**
     * 获取语言服务器
     */
    public EnhancedClawLanguageServer getLanguageServer() {
        return languageServer;
    }

    /**
     * 检查服务器是否正在运行
     */
    public boolean isServerRunning() {
        return isRunning && languageServer != null;
    }

    /**
     * 注册服务器
     */
    private void registerServer() {
        // 这里应该将服务器注册到IDE的LSP客户端
        // 实际实现需要根据IDE的API调整
        System.out.println("LSP server registered for project: " + project.getName());
    }

    /**
     * 注销服务器
     */
    private void unregisterServer() {
        // 这里应该从IDE的LSP客户端注销服务器
        System.out.println("LSP server unregistered for project: " + project.getName());
    }

    /**
     * 创建性能报告窗口
     */
    private void createPerformanceReportWindow(String performanceReport, String cacheStatistics) {
        JFrame frame = new JFrame("Claw LSP Performance Report");
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);

        JTextArea textArea = new JTextArea();
        textArea.setText(performanceReport + "\n\n" + cacheStatistics);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        frame.add(scrollPane);

        frame.setVisible(true);
    }

    /**
     * 显示错误消息
     */
    private void showError(String message) {
        Messages.showErrorDialog(project, message, "Claw LSP Error");
    }

    /**
     * 显示信息消息
     */
    private void showInfo(String message) {
        Messages.showInfoMessage(project, message, "Claw LSP");
    }

    /**
     * 获取服务器状态
     */
    public String getServerStatus() {
        if (!isRunning) {
            return "Stopped";
        }

        if (languageServer == null) {
            return "Error";
        }

        return "Running";
    }

    /**
     * 获取缓存统计
     */
    public String getCacheStats() {
        return cacheManager.getStatistics();
    }

    /**
     * 清除文档缓存
     */
    public void clearDocumentCache(String documentUri) {
        cacheManager.clearDocumentCache(documentUri);
    }
}