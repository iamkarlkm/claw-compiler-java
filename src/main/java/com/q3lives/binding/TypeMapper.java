package com.q3lives.binding;

import java.util.List;

/**
 * 类型映射器接口
 * Claw类型 → 目标语言类型
 */
public interface TypeMapper {
    
    String mapType(String clawType);
    
    String mapPrimitiveType(String clawType);
    
    String mapCollectionType(String clawType, List<String> typeParams);
    
    boolean isNullable(String clawType);
}