package com.q3lives.lsp.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;

/**
 * Claw LSP 客户端（用于测试）
 *
 * 这是一个简单的命令行客户端，用于测试 LSP 服务器
 */
@JsonSegment
public class ClawLanguageClient {

    private final Socket socket;
    private final BufferedReader reader;
    private final OutputStream writer;

    public ClawLanguageClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = socket.getOutputStream();
    }

    /**
     * 发送测试消息
     */
    public void sendMessage(String message) throws IOException {
        writer.write(message.getBytes());
        writer.flush();
    }

    /**
     * 接收响应
     */
    public String receiveMessage() throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("Content-Length: ")) {
                // 读取内容长度
                int contentLength = Integer.parseInt(line.substring("Content-Length: ".length()));
                // 读取内容
                char[] buffer = new char[contentLength];
                reader.read(buffer, 0, contentLength);
                return new String(buffer);
            }
        }

        return null;
    }

    /**
     * 关闭连接
     */
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
        if (writer != null) {
            writer.close();
        }
        if (socket != null) {
            socket.close();
        }
    }

    /**
     * 测试补全功能
     */
    public String testCompletion() throws IOException {
        // TODO: 实现测试代码补全
        return null;
    }

    /**
     * 测试语法检查
     */
    public String testDiagnostics() throws IOException {
        // TODO: 实现测试语法检查
        return null;
    }

    /**
     * 测试跳转定义
     */
    public String testDefinition() throws IOException {
        // TODO: 实现测试跳转定义
        return null;
    }

    public static void main(String[] args) {
        try {
            // 连接到本地 LSP 服务器
            ClawLanguageClient client = new ClawLanguageClient("localhost", 5007);

            System.out.println("Connected to Claw LSP Server!");

            // 发送初始化请求
            String initRequest = "{\n" +
                "  \"jsonrpc\": \"2.0\",\n" +
                "  \"id\": 1,\n" +
                "  \"method\": \"initialize\",\n" +
                "  \"params\": {\n" +
                "    \"processId\": " + ProcessHandle.current().pid() + ",\n" +
                "    \"rootUri\": \"file:///tmp/claw\",\n" +
                "    \"capabilities\": {}\n" +
                "  }\n" +
                "}\n";

            client.sendMessage(initRequest);
            System.out.println("Initialization request sent.");

            // 接收响应
            String response = client.receiveMessage();
            System.out.println("Server response:");
            System.out.println(response);

            // 关闭连接
            client.close();

        } catch (IOException e) {
            System.err.println("Failed to connect to LSP server:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
