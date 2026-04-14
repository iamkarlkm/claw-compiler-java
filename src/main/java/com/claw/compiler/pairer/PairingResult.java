// ==================== PairingResult.java ====================
package com.claw.compiler.pairer;

import static com.claw.compiler.pairer.Pair.PairType.BRACE;
import static com.claw.compiler.pairer.Pair.PairType.BRACKET;
import static com.claw.compiler.pairer.Pair.PairType.PAREN;
import lombok.Getter;
import java.util.*;

/**
 * 配对结果 - 存储所有配对关系
 */
@Getter
public class PairingResult {
    /** 所有配对 */
    private final List<Pair> pairs;
    /** 花括号配对（用于代码块构建） */
    private final List<Pair> bracePairs;
    /** 圆括号配对 */
    private final List<Pair> parenPairs;
    /** 方括号配对 */
    private final List<Pair> bracketPairs;
    /** 是否配对成功 */
    private final boolean valid;
    /** 错误信息（如果配对失败） */
    private final List<String> errors;

    public PairingResult(List<Pair> pairs, boolean valid, List<String> errors) {
        this.pairs = Collections.unmodifiableList(pairs);
        this.valid = valid;
        this.errors = errors != null ? Collections.unmodifiableList(errors) : Collections.emptyList();

        List<Pair> braces = new ArrayList<>();
        List<Pair> parens = new ArrayList<>();
        List<Pair> brackets = new ArrayList<>();

        for (Pair pair : pairs) {
            switch (pair.getType()) {
                case BRACE -> braces.add(pair);
                case PAREN -> parens.add(pair);
                case BRACKET -> brackets.add(pair);
                default -> { /* 字符串引号不单独分类 */ }
            }
        }
        this.bracePairs = Collections.unmodifiableList(braces);
        this.parenPairs = Collections.unmodifiableList(parens);
        this.bracketPairs = Collections.unmodifiableList(brackets);
    }

    /** 查找包含指定位置的最内层花括号配对 */
    public Optional<Pair> findInnermostBrace(int line, int col) {
        return bracePairs.stream()
                .filter(p -> isInsidePair(p, line, col))
                .max(Comparator.comparingInt(Pair::getDepth));
    }

    private boolean isInsidePair(Pair pair, int line, int col) {
        if (line < pair.getOpenLine() || line > pair.getCloseLine()) return false;
        if (line == pair.getOpenLine() && col <= pair.getOpenCol()) return false;
        if (line == pair.getCloseLine() && col >= pair.getCloseCol()) return false;
        return true;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }

    public String formatErrors() {
        return String.join("\n", errors);
    }

    public boolean isValid() {
        return valid;
    }
}

