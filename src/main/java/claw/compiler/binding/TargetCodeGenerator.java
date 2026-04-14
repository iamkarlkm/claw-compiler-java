package claw.compiler.binding;

import com.claw.binding.GenerationConfig;
import com.claw.binding.GenerationResult;

import claw.compiler.generators.ClawIR;

/**
 * 目标语言代码生成器接口
 * 
 * 解耦设计：编译管道只依赖此接口。
 * 切换目标语言只需提供不同的实现：
 *   - JavaCodeGenerator (Java)
 *   - PythonCodeGenerator (Python) — 未来实现
 *   - CCodeGenerator (C) — 未来实现
 */
public interface TargetCodeGenerator {

    /**
     * 获取关联的运行时
     */
    TargetRuntime getRuntime();

    /**
     * 从ClawIR生成目标语言代码
     */
    String generate(ClawIR ir);

    /**
     * 获取目标语言名称
     */
    String getLanguageName();

    /**
     * 获取目标文件扩展名
     */
    String getFileExtension();

    GenerationResult generate(com.claw.ir.ClawIR ir, GenerationConfig defaultConfig);
}


// package com.claw.binding;

// import com.claw.ir.ClawIR;

// /**
//  * 目标语言代码生成器接口
//  * 每种目标语言实现此接口
//  */
// public interface TargetCodeGenerator {
    
//     /**
//      * 将ClawIR转换为目标语言代码
//      */
//     GenerationResult generate(ClawIR ir, GenerationConfig config);
    
//     /**
//      * 获取目标语言名称
//      */
//     String getTargetLanguage();
    
//     /**
//      * 获取支持的文件扩展名
//      */
//     String getFileExtension();
// }


