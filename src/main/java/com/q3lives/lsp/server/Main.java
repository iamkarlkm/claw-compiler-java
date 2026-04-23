package com.q3lives.lsp.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;

import com.google.gson.Gson;

/**
 * Claw 编译器 LSP 服务器入口
 *
 * 运行方式:
 * java -cp target/classes com.q3lives.lsp.server.Main
 */
public class Main {

    public static void main(String[] args) {
        try {
            // 创建标准输入输出流
            java.io.InputStream in = System.in;
            java.io.OutputStream out = System.out;

            // 创建 LSP 服务器
            ClawLanguageServer server = new ClawLanguageServer();

            // 创建 Launcher（启动器）
            org.eclipse.lsp4j.jsonrpc.Launcher<LanguageServer> launcher =
                new org.eclipse.lsp4j.jsonrpc.Launcher.Builder<LanguageServer>()
                    .setLocalService(server)
                    .setInput(in)
                    .setOutput(out)
                    .create();

            // 设置服务器
            server.setLifecycleListener(launcher.getRemoteProxy());

            // 启动服务器
            launcher.startListening();

            System.out.println("Claw Compiler Language Server started successfully!");

        } catch (Exception e) {
            System.err.println("Failed to start Claw Compiler Language Server:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
