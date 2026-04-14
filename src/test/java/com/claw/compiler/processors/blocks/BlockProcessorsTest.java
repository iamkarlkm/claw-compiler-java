// ==================== BlockProcessorsTest.java ====================
package com.claw.compiler.processors.blocks;

import com.claw.compiler.core.Token;
import com.claw.compiler.core.TokenType;
import com.claw.compiler.hierarchy.BlockType;
import com.claw.compiler.hierarchy.CodeBlock;
import com.claw.compiler.frontend.ASTNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 块处理器单元测试
 *
 * <p>测试所有块处理器的功能正确性。</p>
 *
 * @author Claw Compiler Team
 * @since 3.1.0
 */
@DisplayName("块处理器测试")
class BlockProcessorsTest {

    // ==================== FunctionBlockProcessor 测试 ====================

    @Nested
    @DisplayName("FunctionBlockProcessor 测试")
    class FunctionBlockProcessorTest {

        private FunctionBlockProcessor processor;

        @BeforeEach
        void setUp() {
            processor = new FunctionBlockProcessor();
        }

        @Test
        @DisplayName("应该支持 FUNCTION_BLOCK 类型")
        void shouldSupportFunctionBlock() {
            CodeBlock block = createMockBlock(BlockType.FUNCTION_BLOCK, 1, 5);
            assertTrue(processor.canProcess(block));
        }

        @Test
        @DisplayName("应该支持 PARAMETER_BLOCK 类型")
        void shouldSupportParameterBlock() {
            CodeBlock block = createMockBlock(BlockType.PARAMETER_BLOCK, 1, 1);
            assertTrue(processor.canProcess(block));
        }

        @Test
        @DisplayName("应该支持 RETURN_BLOCK 类型")
        void shouldSupportReturnBlock() {
            CodeBlock block = createMockBlock(BlockType.RETURN_BLOCK, 1, 1);
            assertTrue(processor.canProcess(block));
        }

        @Test
        @DisplayName("不应该支持其他块类型")
        void shouldNotSupportOtherBlockTypes() {
            CodeBlock block = createMockBlock(BlockType.VARIABLE_DECLARATION_BLOCK, 1, 1);
            assertFalse(processor.canProcess(block));
        }

        @Test
        @DisplayName("应该返回正确的支持块类型集合")
        void shouldReturnCorrectSupportedTypes() {
            Set<BlockType> supported = processor.getSupportedBlockTypes();
            assertEquals(3, supported.size());
            assertTrue(supported.contains(BlockType.FUNCTION_BLOCK));
            assertTrue(supported.contains(BlockType.PARAMETER_BLOCK));
            assertTrue(supported.contains(BlockType.RETURN_BLOCK));
        }

        @Test
        @DisplayName("应该正确处理简单函数块")
        void shouldProcessSimpleFunctionBlock() {
            CodeBlock block = createMockBlock(BlockType.FUNCTION_BLOCK, 1, 5);
            List<Token> tokens = createFunctionTokens("hello");

            ASTNode result = processor.process(block, tokens);

            assertNotNull(result);
            assertEquals(ASTNode.NodeType.FUNCTION_DECLARATION, result.getType());
            assertEquals("hello", result.getAttribute("name"));
        }

        @Test
        @DisplayName("应该正确提取 public 可见性")
        void shouldExtractPublicVisibility() {
            CodeBlock block = createMockBlock(BlockType.FUNCTION_BLOCK, 1, 5);
            List<Token> tokens = new ArrayList<>();
            tokens.add(new Token(TokenType.KW_PUBLIC, "public", 1, 0));
            tokens.add(new Token(TokenType.KW_FUNCTION, "function", 1, 7));
            tokens.add(new Token(TokenType.IDENTIFIER, "myFunc", 1, 16));

            ASTNode result = processor.process(block, tokens);

            assertEquals("public", result.getAttribute("visibility"));
        }

        @Test
        @DisplayName("应该正确提取 normal 操作流类型")
        void shouldExtractNormalFlowType() {
            CodeBlock block = createMockBlock(BlockType.FUNCTION_BLOCK, 1, 5);
            List<Token> tokens = new ArrayList<>();
            tokens.add(new Token(TokenType.KW_NORMAL, "normal", 1, 0));
            tokens.add(new Token(TokenType.KW_FUNCTION, "function", 1, 7));
            tokens.add(new Token(TokenType.IDENTIFIER, "process", 1, 16));

            ASTNode result = processor.process(block, tokens);

            assertEquals("normal", result.getAttribute("flowType"));
        }

        @Test
        @DisplayName("null 块应该返回 false")
        void nullBlockShouldReturnFalse() {
            assertFalse(processor.canProcess(null));
        }

        @Test
        @DisplayName("null tokens 应该抛出异常")
        void nullTokensShouldThrowException() {
            CodeBlock block = createMockBlock(BlockType.FUNCTION_BLOCK, 1, 5);
            assertThrows(IllegalArgumentException.class, () -> {
                processor.process(block, null);
            });
        }
    }

    // ==================== DeclarationBlockProcessor 测试 ====================

    @Nested
    @DisplayName("DeclarationBlockProcessor 测试")
    class DeclarationBlockProcessorTest {

        private DeclarationBlockProcessor processor;

        @BeforeEach
        void setUp() {
            processor = new DeclarationBlockProcessor();
        }

        @Test
        @DisplayName("应该支持 VARIABLE_DECLARATION_BLOCK 类型")
        void shouldSupportVariableDeclarationBlock() {
            CodeBlock block = createMockBlock(BlockType.VARIABLE_DECLARATION_BLOCK, 1, 1);
            assertTrue(processor.canProcess(block));
        }

        @Test
        @DisplayName("应该支持 IMPORT_DECLARATION_BLOCK 类型")
        void shouldSupportImportDeclarationBlock() {
            CodeBlock block = createMockBlock(BlockType.IMPORT_DECLARATION_BLOCK, 1, 1);
            assertTrue(processor.canProcess(block));
        }

        @Test
        @DisplayName("应该正确处理变量声明")
        void shouldProcessVariableDeclaration() {
            CodeBlock block = createMockBlock(BlockType.VARIABLE_DECLARATION_BLOCK, 1, 1);
            List<Token> tokens = new ArrayList<>();
            tokens.add(new Token(TokenType.KW_VAR, "var", 1, 0));
            tokens.add(new Token(TokenType.IDENTIFIER, "name", 1, 4));
            tokens.add(new Token(TokenType.OP_COLON, ":", 1, 8));
            tokens.add(new Token(TokenType.IDENTIFIER, "String", 1, 10));
            tokens.add(new Token(TokenType.OP_ASSIGN, "=", 1, 17));
            tokens.add(new Token(TokenType.LIT_STRING, "Hello", 1, 19));

            ASTNode result = processor.process(block, tokens);

            assertNotNull(result);
            assertEquals(ASTNode.NodeType.VARIABLE_DECLARATION, result.getType());
            assertEquals("name", result.getAttribute("name"));
            assertEquals("String", result.getAttribute("type"));
            assertEquals("false", result.getAttribute("isConst"));
        }

        @Test
        @DisplayName("应该正确处理常量声明")
        void shouldProcessConstantDeclaration() {
            CodeBlock block = createMockBlock(BlockType.VARIABLE_DECLARATION_BLOCK, 1, 1);
            List<Token> tokens = new ArrayList<>();
            tokens.add(new Token(TokenType.KW_CONST, "const", 1, 0));
            tokens.add(new Token(TokenType.IDENTIFIER, "PI", 1, 6));

            ASTNode result = processor.process(block, tokens);

            assertEquals("true", result.getAttribute("isConst"));
        }

        @Test
        @DisplayName("应该正确处理导入声明")
        void shouldProcessImportDeclaration() {
            CodeBlock block = createMockBlock(BlockType.IMPORT_DECLARATION_BLOCK, 1, 1);
            List<Token> tokens = new ArrayList<>();
            tokens.add(new Token(TokenType.KW_IMPORT, "import", 1, 0));
            tokens.add(new Token(TokenType.IDENTIFIER, "utils", 1, 7));

            ASTNode result = processor.process(block, tokens);

            assertNotNull(result);
            assertEquals(ASTNode.NodeType.IMPORT_DECLARATION, result.getType());
        }

        @Test
        @DisplayName("应该返回正确的支持块类型集合")
        void shouldReturnCorrectSupportedTypes() {
            Set<BlockType> supported = processor.getSupportedBlockTypes();
            assertEquals(2, supported.size());
        }
    }

    // ==================== BlockProcessor 基类测试 ====================

    @Nested
    @DisplayName("BlockProcessor 基类测试")
    class BlockProcessorBaseTest {

        @Test
        @DisplayName("process 方法应该在 canProcess 返回 false 时返回 null")
        void processShouldReturnNullWhenCannotProcess() {
            BlockProcessor processor = new TestBlockProcessor();
            CodeBlock block = createMockBlock(BlockType.FUNCTION_BLOCK, 1, 1);

            // TestBlockProcessor 只支持 ROOT_BLOCK
            ASTNode result = processor.process(block, List.of());

            assertNull(result);
        }

        @Test
        @DisplayName("null 块应该抛出 IllegalArgumentException")
        void nullBlockShouldThrowIllegalArgumentException() {
            BlockProcessor processor = new TestBlockProcessor();
            assertThrows(IllegalArgumentException.class, () -> {
                processor.process(null, List.of());
            });
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建模拟代码块
     */
    private CodeBlock createMockBlock(BlockType type, int startLine, int endLine) {
        CodeBlock block = new CodeBlock(0, startLine);
        block.setBlockType(type);
        block.setEndLine(endLine);
        return block;
    }

    /**
     * 创建函数 Token 列表
     */
    private List<Token> createFunctionTokens(String funcName) {
        List<Token> tokens = new ArrayList<>();
        tokens.add(new Token(TokenType.KW_FUNCTION, "function", 1, 0));
        tokens.add(new Token(TokenType.IDENTIFIER, funcName, 1, 9));
        tokens.add(new Token(TokenType.OPEN_PAREN, "(", 1, 9 + funcName.length()));
        tokens.add(new Token(TokenType.CLOSE_PAREN, ")", 1, 10 + funcName.length()));
        return tokens;
    }

    // ==================== 测试用处理器 ====================

    /**
     * 测试用块处理器
     */
    private static class TestBlockProcessor extends BlockProcessor {

        @Override
        public boolean canProcess(CodeBlock block) {
            return block != null && block.getBlockType() == BlockType.ROOT_BLOCK;
        }

        @Override
        public Set<BlockType> getSupportedBlockTypes() {
            return Set.of(BlockType.ROOT_BLOCK);
        }

        @Override
        protected ASTNode doProcess(CodeBlock block, List<Token> tokens) {
            return new ASTNode(ASTNode.NodeType.PROGRAM);
        }
    }
}
