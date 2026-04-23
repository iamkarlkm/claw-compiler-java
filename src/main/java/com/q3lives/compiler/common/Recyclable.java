// ==================== Recyclable.java ====================
package com.q3lives.compiler.common;

/**
 * 可回收对象接口 - 用于对象池
 */
public interface Recyclable {
    
    /**
     * 清理对象状态，使其可以被复用
     */
    void clear();
}
