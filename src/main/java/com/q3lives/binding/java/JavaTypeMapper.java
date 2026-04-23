package com.q3lives.binding.java;

import java.util.List;
import java.util.Map;

import com.q3lives.binding.TypeMapper;

/**
 * Claw类型 → Java类型映射
 */
public class JavaTypeMapper implements TypeMapper {
    
    private static final Map<String, String> PRIMITIVE_MAP = Map.ofEntries(
        Map.entry("Int", "int"),
        Map.entry("Float", "double"),
        Map.entry("String", "String"),
        Map.entry("Bool", "boolean"),
        Map.entry("Void", "void"),
        Map.entry("Any", "Object"),
        Map.entry("Byte", "byte"),
        Map.entry("Short", "short"),
        Map.entry("Long", "long"),
        Map.entry("Char", "char"),
        Map.entry("Float32", "float")
    );

    private static final Map<String, String> BOXED_MAP = Map.ofEntries(
        Map.entry("Int", "Integer"),
        Map.entry("Float", "Double"),
        Map.entry("String", "String"),
        Map.entry("Bool", "Boolean"),
        Map.entry("Void", "Void"),
        Map.entry("Any", "Object"),
        Map.entry("Byte", "Byte"),
        Map.entry("Short", "Short"),
        Map.entry("Long", "Long"),
        Map.entry("Char", "Character"),
        Map.entry("Float32", "Float")
    );
    
    @Override
    public String mapType(String clawType) {
        if (clawType == null) return "void";

        // 基本类型
        if (PRIMITIVE_MAP.containsKey(clawType)) {
            return PRIMITIVE_MAP.get(clawType);
        }

        // 数组类型：Array<T> → T[]
        if (clawType.startsWith("Array<")) {
            String inner = extractGenericParam(clawType);
            return mapBoxedType(inner) + "[]";
        }

        // Map类型：Map<K,V> → Map<K,V>
        if (clawType.startsWith("Map<")) {
            String params = extractGenericParam(clawType);
            String[] parts = params.split(",", 2);
            return "Map<" + mapBoxedType(parts[0].trim())
                   + ", " + mapBoxedType(parts[1].trim()) + ">";
        }

        // 集合类型：Set<T> → Set<T>
        if (clawType.startsWith("Set<")) {
            String inner = extractGenericParam(clawType);
            return "Set<" + mapBoxedType(inner) + ">";
        }

        // 可选类型：Optional<T> → Optional<T>
        if (clawType.startsWith("Optional<")) {
            String inner = extractGenericParam(clawType);
            return "Optional<" + mapBoxedType(inner) + ">";
        }

        // 函数类型：(Args) -> ReturnType → ReturnType
        if (clawType.contains("->")) {
            String[] parts = clawType.split("->");
            return mapBoxedType(parts[1].trim().trim());
        }

        // 元组类型：Tuple<T1,T2> → Object (简化处理)
        if (clawType.startsWith("Tuple<")) {
            return "Object";
        }

        // 自定义类型，保持原样
        return clawType;
    }
    
    @Override
    public String mapPrimitiveType(String clawType) {
        return PRIMITIVE_MAP.getOrDefault(clawType, clawType);
    }
    
    @Override
    public String mapCollectionType(String clawType, List<String> typeParams) {
        if ("Array".equals(clawType)) {
            return mapBoxedType(typeParams.get(0)) + "[]";
        }
        if ("Map".equals(clawType)) {
            return "Map<" + mapBoxedType(typeParams.get(0))
                   + ", " + mapBoxedType(typeParams.get(1)) + ">";
        }
        if ("Set".equals(clawType)) {
            return "Set<" + mapBoxedType(typeParams.get(0)) + ">";
        }
        return clawType;
    }
    
    @Override
    public boolean isNullable(String clawType) {
        return !PRIMITIVE_MAP.containsKey(clawType);
    }
    
    private String mapBoxedType(String clawType) {
        return BOXED_MAP.getOrDefault(clawType, clawType);
    }
    
    private String extractGenericParam(String type) {
        int start = type.indexOf('<');
        int end = type.lastIndexOf('>');
        return type.substring(start + 1, end);
    }
}
