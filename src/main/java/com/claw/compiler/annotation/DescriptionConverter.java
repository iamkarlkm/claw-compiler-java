// ==================== DescriptionConverter.java ====================
package com.claw.compiler.annotation;

import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * @@description 转换器 - 将人类可读描述转换为机器可读格式
 */
@Slf4j
public class DescriptionConverter {

    /**
     * 转换为机器可读格式
     * 
     * @param humanDesc 人类可读描述
     * @param ioSpec 输入输出规范 (如 "UserData -> ProcessResult")
     * @return 机器可读格式的JSON字符串
     */
    public String convert(String humanDesc, String ioSpec) {
        Map<String, Object> machineFormat = new LinkedHashMap<>();
        machineFormat.put("description", humanDesc);

        // 解析IO规范
        if (ioSpec != null && ioSpec.contains("->")) {
            String[] parts = ioSpec.split("->");
            machineFormat.put("input", parts[0].trim());
            machineFormat.put("output", parts.length > 1 ? parts[1].trim() : "Void");
        }

        machineFormat.put("generatedAt", System.currentTimeMillis());
        machineFormat.put("version", "1.0");

        return toJson(machineFormat);
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (i > 0) sb.append(",");
            sb.append("  \"").append(entry.getKey()).append("\": ");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
            i++;
        }
        sb.append("}");
        return sb.toString();
    }
}
