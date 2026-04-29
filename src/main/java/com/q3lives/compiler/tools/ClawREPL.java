package com.q3lives.compiler.tools;

import com.q3lives.compiler.ClawCompiler;
import com.q3lives.compiler.pipeline.CompilationResult;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * REPL - 增强的交互式编程环境
 * 支持：变量查看、命令历史、脚本执行
 */
public class ClawREPL {

    private final ClawCompiler compiler;
    private final Scanner scanner;
    private boolean running;

    // 变量存储
    private final Map<String, String> variables = new HashMap<>();

    // 命令历史
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;

    public ClawREPL() {
        this.compiler = new ClawCompiler();
        this.scanner = new Scanner(System.in);
        this.running = true;
    }

    public void start() {
        printBanner();
        StringBuilder buffer = new StringBuilder();

        while (running) {
            // 显示变量上下文
            String prompt = getPrompt();

            System.out.print(prompt);
            String line = readLineWithHistory();

            if (line == null) break;
            line = line.trim();

            if (line.isEmpty()) continue;

            // 添加到历史
            addToHistory(line);

            // 命令处理
            switch (line) {
                case ":quit", ":q" -> {
                    System.out.println("再见!");
                    return;
                }
                case ":help", ":h" -> {
                    printHelp();
                    continue;
                }
                case ":clear", ":c" -> {
                    clearScreen();
                    continue;
                }
                case ":info", ":i" -> {
                    printInfo();
                    continue;
                }
                case ":vars", ":v" -> {
                    printVariables();
                    continue;
                }
                case ":history", ":hist" -> {
                    printHistory();
                    continue;
                }
            }

            // :load 加载脚本
            if (line.startsWith(":load ")) {
                String file = line.substring(6).trim();
                loadScript(file);
                continue;
            }

            // :run 运行脚本
            if (line.startsWith(":run ")) {
                String file = line.substring(5).trim();
                runScript(file);
                continue;
            }

            // :save 保存变量
            if (line.startsWith(":save ")) {
                String file = line.substring(6).trim();
                saveVariables(file);
                continue;
            }

            // :load-var 加载变量
            if (line.startsWith(":load-var ")) {
                String file = line.substring(10).trim();
                loadVariables(file);
                continue;
            }

            // 累积多行输入
            buffer.append(line).append("\n");

            // 检查是否完整语句（以 } 结尾）
            if (line.endsWith("}")) {
                String source = buffer.toString();
                buffer.setLength(0);

                try {
                    CompilationResult result = compiler.compile(source, "repl.claw");

                    if (result.isSuccess()) {
                        // 提取变量定义
                        extractVariables(source);

                        String code = result.getGeneratedCode().getTargetCode();
                        if (code != null && !code.isEmpty()) {
                            System.out.println("=> 编译成功 (" + result.getElapsedMillis() + "ms)");
                            printCodePreview(code);
                        }
                    } else {
                        System.out.println("=> 错误: " + result.getErrors());
                    }
                } catch (Exception e) {
                    System.out.println("=> 异常: " + e.getMessage());
                }
            }
        }

        scanner.close();
    }

    private String readLineWithHistory() {
        String input = scanner.nextLine();

        // 上箭头 - 历史上一条
        if (input.equals("\u001b[A") && !history.isEmpty()) {
            if (historyIndex < history.size() - 1) historyIndex++;
            return history.get(history.size() - 1 - historyIndex);
        }

        // 下箭头 - 历史下一条
        if (input.equals("\u001b[B") && historyIndex > 0) {
            historyIndex--;
            return history.get(history.size() - 1 - historyIndex);
        }

        // 重置索引
        if (!input.isEmpty()) {
            historyIndex = -1;
        }

        return input;
    }

    private void addToHistory(String line) {
        if (!line.isEmpty() && (history.isEmpty() || !history.get(history.size() - 1).equals(line))) {
            history.add(line);
            if (history.size() > 100) history.remove(0);
        }
    }

    private String getPrompt() {
        String varCount = variables.isEmpty() ? "" : "[" + variables.size() + " vars]";
        return "claw" + varCount + "> ";
    }

    private void extractVariables(String source) {
        // 简单的变量提取：var name = value
        var regex = java.util.regex.Pattern.compile("var (\\w+)\\s*=");
        var matcher = regex.matcher(source);
        while (matcher.find()) {
            String varName = matcher.group(1);
            if (!variables.containsKey(varName)) {
                variables.put(varName, "<value>");
            }
        }
    }

    private void printVariables() {
        if (variables.isEmpty()) {
            System.out.println("没有定义的变量");
            return;
        }
        System.out.println("已定义的变量 (" + variables.size() + "):");
        for (var entry : variables.entrySet()) {
            System.out.println("  " + entry.getKey() + " = " + entry.getValue());
        }
    }

    private void printHistory() {
        if (history.isEmpty()) {
            System.out.println("没有命令历史");
            return;
        }
        System.out.println("命令历史 (" + history.size() + "):");
        for (int i = 0; i < Math.min(20, history.size()); i++) {
            System.out.println("  " + (i + 1) + ": " + history.get(i));
        }
    }

    private void loadScript(String file) {
        try {
            String content = Files.readString(Path.of(file));
            System.out.println("已加载: " + file + " (" + content.lines().count() + " 行)");
        } catch (IOException e) {
            System.out.println("加载失败: " + e.getMessage());
        }
    }

    private void runScript(String file) {
        try {
            String content = Files.readString(Path.of(file));
            CompilationResult result = compiler.compile(content, file);
            if (result.isSuccess()) {
                System.out.println("执行成功 (" + result.getElapsedMillis() + "ms)");
                extractVariables(content);
            } else {
                System.out.println("错误: " + result.getErrors());
            }
        } catch (IOException e) {
            System.out.println("执行失败: " + e.getMessage());
        }
    }

    private void saveVariables(String file) {
        try {
            var sb = new StringBuilder();
            for (var entry : variables.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            Files.writeString(Path.of(file), sb.toString());
            System.out.println("已保存: " + file);
        } catch (IOException e) {
            System.out.println("保存失败: " + e.getMessage());
        }
    }

    private void loadVariables(String file) {
        try {
            for (String line : Files.readAllLines(Path.of(file))) {
                var parts = line.split("=", 2);
                if (parts.length == 2) {
                    variables.put(parts[0].trim(), parts[1].trim());
                }
            }
            System.out.println("已加载 " + variables.size() + " 个变量");
        } catch (IOException e) {
            System.out.println("加载失败: " + e.getMessage());
        }
    }

    private void printCodePreview(String code) {
        String[] lines = code.split("\n");
        for (int i = 0; i < Math.min(8, lines.length); i++) {
            System.out.println("   " + lines[i]);
        }
        if (lines.length > 8) {
            System.out.println("   ... (" + (lines.length - 8) + " more)");
        }
    }

    private void clearScreen() {
        System.out.print("\033[2J\033[H");
        printBanner();
    }

    private void printBanner() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║     Claw REPL v3.1                  ║");
        System.out.println("║     :help 获取帮助                  ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println();
    }

    private void printHelp() {
        System.out.println("命令:");
        System.out.println("  :h, :help     显示帮助");
        System.out.println("  :q, :quit     退出");
        System.out.println("  :c, :clear   清除屏幕");
        System.out.println("  :i, :info    显示信息");
        System.out.println("  :v, :vars    查看变量");
        System.out.println("  :hist        命令历史");
        System.out.println("  :load <file> 加载脚本");
        System.out.println("  :run <file>  执行脚本");
        System.out.println("  :save <file> 保存变量");
        System.out.println("  :load-var    加载变量");
        System.out.println();
        System.out.println("多行输入: 以 } 结尾时执行");
        System.out.println("上下箭头: 命令历史");
    }

    private void printInfo() {
        System.out.println("Claw Compiler Java v3.0.0");
        System.out.println("三层操作流 | 18种代码块 | 4层处理器");
        System.out.println("变量: " + variables.size() + " | 历史: " + history.size());
    }

    public static void main(String[] args) {
        ClawREPL repl = new ClawREPL();
        repl.start();
    }
}