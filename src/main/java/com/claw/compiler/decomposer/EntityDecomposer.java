// ==================== EntityDecomposer.java ====================
package com.claw.compiler.decomposer;

import com.claw.compiler.hierarchy.CodeBlock;
import com.claw.compiler.hierarchy.HierarchicalBlocks;
import com.claw.compiler.decomposer.EntityRecognizer.RecognizedEntity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 实体分解器
 * 
 * 思想2第3-4步：
 * - 自前往后、自下至上分解各种用途的处理器和数据实体
 * - 确定归属关系（作用域）
 */
@Slf4j
public class EntityDecomposer {

    private final EntityRecognizer recognizer = new EntityRecognizer();

    /**
     * 分解层级代码块中的所有实体
     */
    public DecompositionResult decompose(HierarchicalBlocks hierarchicalBlocks) {
        log.debug("开始实体分解");
        
        List<RecognizedEntity> entities = new ArrayList<>();
        Map<String, List<RecognizedEntity>> scopeEntities = new LinkedHashMap<>();

        // 自下至上遍历所有块
        List<CodeBlock> bottomUpBlocks = hierarchicalBlocks.getBottomUpOrder();
        
        for (CodeBlock block : bottomUpBlocks) {
            RecognizedEntity entity = recognizer.recognize(block);
            if (entity != null) {
                entities.add(entity);

                // 确定归属作用域
                String scope = determineScopeName(block);
                scopeEntities.computeIfAbsent(scope, k -> new ArrayList<>()).add(entity);
                
                log.debug("识别实体: {} 归属作用域: {}", entity, scope);
            }
        }

        log.info("实体分解完成: {} 个实体, {} 个作用域", 
                entities.size(), scopeEntities.size());

        return new DecompositionResult(entities, scopeEntities);
    }

    /**
     * 确定代码块的归属作用域名称
     */
    private String determineScopeName(CodeBlock block) {
        CodeBlock parent = block.getParent();
        while (parent != null) {
            if (parent.getScopeName() != null) {
                return parent.getScopeName();
            }
            parent = parent.getParent();
        }
        return "__global__";
    }

    /** 分解结果 */
    @Getter
    public static class DecompositionResult {
        private final List<RecognizedEntity> entities;
        private final Map<String, List<RecognizedEntity>> scopeEntities;

        public DecompositionResult(List<RecognizedEntity> entities,
                                   Map<String, List<RecognizedEntity>> scopeEntities) {
            this.entities = Collections.unmodifiableList(entities);
            this.scopeEntities = Collections.unmodifiableMap(scopeEntities);
        }

        public List<RecognizedEntity> getEntitiesInScope(String scope) {
            return scopeEntities.getOrDefault(scope, Collections.emptyList());
        }

        public List<RecognizedEntity> getEntitiesByType(EntityType type) {
            return entities.stream()
                    .filter(e -> e.getType() == type)
                    .toList();
        }
    }
}

