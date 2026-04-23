package com.q3lives.lsp.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

/**
 * LSP 诊断生成器
 *
 * 将编译错误转换为 LSP Diagnostic 对象
 */
public class DiagnosticGenerator {

    /**
     * 创建诊断对象
     */
    public static Diagnostic createDiagnostic(
        String message,
        DiagnosticSeverity severity,
        Range range,
        String code
    ) {
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setMessage(message);
        diagnostic.setSeverity(severity);
        diagnostic.setRange(range);
        diagnostic.setCode(code);

        return diagnostic;
    }

    /**
     * 创建语法错误诊断
     */
    public static Diagnostic createSyntaxError(String message, Range range) {
        return createDiagnostic(
            "Syntax Error: " + message,
            DiagnosticSeverity.Error,
            range,
            "claw.syntax"
        );
    }

    /**
     * 创建类型错误诊断
     */
    public static Diagnostic createTypeError(String message, Range range) {
        return createDiagnostic(
            "Type Error: " + message,
            DiagnosticSeverity.Error,
            range,
            "claw.type"
        );
    }

    /**
     * 创建警告诊断
     */
    public static Diagnostic createWarning(String message, Range range) {
        return createDiagnostic(
            "Warning: " + message,
            DiagnosticSeverity.Warning,
            range,
            "claw.warning"
        );
    }

    /**
     * 创建信息诊断
     */
    public static Diagnostic createInfo(String message, Range range) {
        return createDiagnostic(
            message,
            DiagnosticSeverity.Information,
            range,
            "claw.info"
        );
    }

    /**
     * 创建提示诊断
     */
    public static Diagnostic createHint(String message, Range range) {
        return createDiagnostic(
            "Hint: " + message,
            DiagnosticSeverity.Hint,
            range,
            "claw.hint"
        );
    }

    /**
     * 创建从行和列开始的 Range
     */
    public static Range createRange(int line, int character) {
        Position start = new Position(line, character);
        Position end = new Position(line, character);
        return new Range(start, end);
    }

    /**
     * 创建从行范围开始的 Range
     */
    public static Range createRange(int startLine, int startChar, int endLine, int endChar) {
        Position start = new Position(startLine, startChar);
        Position end = new Position(endLine, endChar);
        return new Range(start, end);
    }

    /**
     * 创建错误列表
     */
    public static List<Diagnostic> createErrorList(Diagnostic... diagnostics) {
        List<Diagnostic> result = new ArrayList<>();
        if (diagnostics != null) {
            for (Diagnostic diag : diagnostics) {
                if (diag != null) {
                    result.add(diag);
                }
            }
        }
        return result;
    }
}
