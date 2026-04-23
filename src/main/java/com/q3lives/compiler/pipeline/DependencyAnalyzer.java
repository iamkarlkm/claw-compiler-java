package com.q3lives.compiler.pipeline;

import com.q3lives.compiler.frontend.ASTNode;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 依赖分析器
 *
 * 负责分析源代码文件之间的依赖关系，构建依赖图
 */
@Slf4j
public class DependencyAnalyzer {

    /** 依赖图：文件路径 -> 依赖的文件集合 */
    private final Map<String, Set<String>> dependencyGraph = new HashMap<>();
    /** 反向依赖图：文件路径 -> 依赖该文件的文件集合 */
    private final Map<String, Set<String>> reverseDependencyGraph = new HashMap<>();
    /** 文件到AST的映射 */
    private final Map<String, ASTNode> fileASTs = new HashMap<>();

    /**
     * 分析文件依赖
     */
    public DependencyResult analyzeDependencies(Map<String, String> sourceFiles) {
        log.info("开始分析 {} 个文件的依赖关系", sourceFiles.size());

        // 清空之前的分析结果
        dependencyGraph.clear();
        reverseDependencyGraph.clear();
        fileASTs.clear();

        // 1. 解析所有文件，构建初始依赖
        parseAllFiles(sourceFiles);

        // 2. 分析深层依赖
        analyzeDeepDependencies();

        // 3. 构建依赖图
        buildDependencyGraph();

        return new DependencyResult(
            Collections.unmodifiableMap(dependencyGraph),
            Collections.unmodifiableMap(reverseDependencyGraph)
        );
    }

    /**
     * 解析所有文件
     */
    private void parseAllFiles(Map<String, String> sourceFiles) {
        log.debug("解析所有文件");

        for (Map.Entry<String, String> entry : sourceFiles.entrySet()) {
            String fileName = entry.getKey();
            String source = entry.getValue();

            // 这里应该使用Parser解析source为AST
            // ASTNode ast = parseSource(source);
            // fileASTs.put(fileName, ast);

            // 临时模拟AST
            ASTNode mockAST = createMockAST(fileName);
            fileASTs.put(fileName, mockAST);

            // 分析直接依赖
            Set<String> directDependencies = analyzeDirectDependencies(mockAST);
            dependencyGraph.put(fileName, directDependencies);
        }
    }

    /**
     * 分析直接依赖
     */
    private Set<String> analyzeDirectDependencies(ASTNode ast) {
        Set<String> dependencies = new HashSet<>();

        // 分析import语句
        analyzeImportDependencies(ast, dependencies);

        // 分析类型依赖
        analyzeTypeDependencies(ast, dependencies);

        // 分析函数调用依赖
        analyzeFunctionDependencies(ast, dependencies);

        return dependencies;
    }

    /**
     * 分析import依赖
     */
    private void analyzeImportDependencies(ASTNode ast, Set<String> dependencies) {
        for (ASTNode node : ast.getChildren()) {
            if (node.getType() == ASTNode.NodeType.IMPORT_DECLARATION) {
                String path = node.getAttribute("path");
                if (path != null) {
                    // 将相对路径转换为绝对路径
                    String absolutePath = resolveImportPath(path);
                    dependencies.add(absolutePath);
                }
            }
            // 递归处理子节点
            analyzeImportDependencies(node, dependencies);
        }
    }

    /**
     * 分析类型依赖
     */
    private void analyzeTypeDependencies(ASTNode ast, Set<String> dependencies) {
        for (ASTNode node : ast.getChildren()) {
            if (node.getType() == ASTNode.NodeType.TYPE_DEFINITION) {
                String typeName = node.getAttribute("name");
                if (typeName != null) {
                    // 查找类型定义所在的文件
                    String definingFile = findTypeDefiningFile(typeName);
                    if (definingFile != null) {
                        dependencies.add(definingFile);
                    }
                }
            }
            // 递归处理子节点
            analyzeTypeDependencies(node, dependencies);
        }
    }

    /**
     * 分析函数调用依赖
     */
    private void analyzeFunctionDependencies(ASTNode ast, Set<String> dependencies) {
        for (ASTNode node : ast.getChildren()) {
            if (node.getType() == ASTNode.NodeType.FUNCTION_CALL) {
                String functionName = node.getAttribute("name");
                if (functionName != null) {
                    // 查找函数定义所在的文件
                    String definingFile = findFunctionDefiningFile(functionName);
                    if (definingFile != null) {
                        dependencies.add(definingFile);
                    }
                }
            }
            // 递归处理子节点
            analyzeFunctionDependencies(node, dependencies);
        }
    }

    /**
     * 分析深层依赖
     */
    private void analyzeDeepDependencies() {
        log.debug("分析深层依赖");

        // 使用传递闭包算法分析深层依赖
        for (String file : dependencyGraph.keySet()) {
            Set<String> transitiveDependencies = computeTransitiveDependencies(file);
            dependencyGraph.put(file, transitiveDependencies);
        }
    }

    /**
     * 计算传递闭包（所有间接依赖）
     */
    private Set<String> computeTransitiveDependencies(String file) {
        Set<String> result = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        queue.add(file);
        visited.add(file);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            // 获取当前文件的所有直接依赖
            Set<String> directDeps = dependencyGraph.getOrDefault(current, Collections.emptySet());

            for (String dep : directDeps) {
                if (!visited.contains(dep)) {
                    visited.add(dep);
                    result.add(dep);
                    queue.add(dep);
                }
            }
        }

        // 移除自身依赖
        result.remove(file);
        return result;
    }

    /**
     * 构建依赖图
     */
    private void buildDependencyGraph() {
        log.debug("构建依赖图");

        // 构建反向依赖图
        for (Map.Entry<String, Set<String>> entry : dependencyGraph.entrySet()) {
            String file = entry.getKey();
            Set<String> dependencies = entry.getValue();

            for (String dep : dependencies) {
                reverseDependencyGraph
                    .computeIfAbsent(dep, k -> new HashSet<>())
                    .add(file);
            }
        }
    }

    /**
     * 解析import路径
     */
    private String resolveImportPath(String importPath) {
        // 将相对路径转换为绝对路径
        // 这里简化处理，实际应该根据文件系统结构解析
        return importPath;
    }

    /**
     * 查找类型定义所在的文件
     */
    private String findTypeDefiningFile(String typeName) {
        // 遍历所有AST，查找类型定义
        for (Map.Entry<String, ASTNode> entry : fileASTs.entrySet()) {
            ASTNode ast = entry.getValue();
            if (findTypeDefInAST(ast, typeName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 在AST中查找类型定义
     */
    private boolean findTypeDefInAST(ASTNode ast, String typeName) {
        if (ast.getType() == ASTNode.NodeType.TYPE_DEFINITION) {
            String name = ast.getAttribute("name");
            if (typeName.equals(name)) {
                return true;
            }
        }

        for (ASTNode child : ast.getChildren()) {
            if (findTypeDefInAST(child, typeName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 查找函数定义所在的文件
     */
    private String findFunctionDefiningFile(String functionName) {
        // 遍历所有AST，查找函数定义
        for (Map.Entry<String, ASTNode> entry : fileASTs.entrySet()) {
            ASTNode ast = entry.getValue();
            if (findFunctionDefInAST(ast, functionName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 在AST中查找函数定义
     */
    private boolean findFunctionDefInAST(ASTNode ast, String functionName) {
        if (ast.getType() == ASTNode.NodeType.FUNCTION_DECLARATION) {
            String name = ast.getAttribute("name");
            if (functionName.equals(name)) {
                return true;
            }
        }

        for (ASTNode child : ast.getChildren()) {
            if (findFunctionDefInAST(child, functionName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 创建模拟AST（用于演示）
     */
    private ASTNode createMockAST(String fileName) {
        ASTNode root = new ASTNode(ASTNode.NodeType.PROGRAM);

        // 根据文件名创建不同的AST结构
        if (fileName.contains("util")) {
            // 工具文件
            ASTNode utilFunc = new ASTNode(ASTNode.NodeType.FUNCTION_DECLARATION);
            utilFunc.setAttribute("name", "utilHelper");
            root.addChild(utilFunc);
        }

        return root;
    }

    /**
     * 依赖分析结果
     */
    public record DependencyResult(
        Map<String, Set<String>> dependencyGraph,
        Map<String, Set<String>> reverseDependencyGraph
    ) {}
}