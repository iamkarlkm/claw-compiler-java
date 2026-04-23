package com.q3lives.lsp.provider;

import com.q3lives.compiler.context.SemanticContext;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import java.util.*;
import java.util.regex.*;

/**
 * 增强版定义提供器
 */
public class EnhancedDefinitionProvider {

    private final SemanticContext semanticContext;

    public EnhancedDefinitionProvider(SemanticContext semanticContext) {
        this.semanticContext = semanticContext;
    }

    public List<Location> provideDefinition(String document, Position position) {
        List<Location> locations = new ArrayList<>();

        // 解析文档，查找定义
        String[] lines = document.split("\n");
        if (position.getLine() < lines.length) {
            String line = lines[position.getLine()];
            String word = extractWordAtPosition(line, position.getCharacter());

            if (word != null) {
                // 查找定义
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].contains("function " + word) ||
                        lines[i].contains("var " + word) ||
                        lines[i].contains("type " + word)) {
                        Location location = new Location();
                        location.setUri("file://" + i);
                        location.setRange(new org.eclipse.lsp4j.Range(
                            new org.eclipse.lsp4j.Position(i, 0),
                            new org.eclipse.lsp4j.Position(i, lines[i].length())
                        ));
                        locations.add(location);
                        break;
                    }
                }
            }
        }

        return locations;
    }

    private String extractWordAtPosition(String line, int position) {
        if (position < 0 || position >= line.length()) {
            return null;
        }

        int start = position;
        while (start > 0 && Character.isJavaIdentifierPart(line.charAt(start - 1))) {
            start--;
        }

        int end = position;
        while (end < line.length() && Character.isJavaIdentifierPart(line.charAt(end))) {
            end++;
        }

        return line.substring(start, end);
    }
}