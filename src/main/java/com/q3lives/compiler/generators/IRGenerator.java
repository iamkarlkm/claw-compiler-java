package com.q3lives.compiler.generators;


import com.q3lives.compiler.annotation.AnnotationResult;
import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.compiler.context.StructureContext;
import com.q3lives.compiler.hierarchy.BlockType;
import com.q3lives.compiler.hierarchy.CodeBlock;
import com.q3lives.ir.ClawIR;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Claw语言编译器 - 中间表示生成器 (IRGenerator)
 * 
 * 职责：从当前最底层的代码块开始，自下至上生成中间表示（IR）。
 * 
 * 设计依据：
 * - 思想2：扫描→配对→分层→分解→生成（阶段5）
 * - 思想3：第4层验证生成处理器
 * - 思想1：三层操作流模型（normal / exception / flow）
 * - 思想5：精确的注解系统（4个程序注解 + 5个系统注解）
 * 
 * @version 3.0 (最终版)
 * @date 2026-03-20
 */
public class IRGenerator {

    // ==================== IR指令操作码 ====================
    
    /**
     * IR操作码枚举 - 覆盖Claw语言所有操作
     */
    public enum OpCode {
        // 基础操作
        NOP,                    // 空操作
        LOAD_CONST,             // 加载常量
        LOAD_VAR,               // 加载变量
        STORE_VAR,              // 存储变量
        
        // 算术运算
        ADD,                    // 加法 +
        SUB,                    // 减法 -
        MUL,                    // 乘法 *
        DIV,                    // 除法 /
        MOD,                    // 取模 %
        
        // 比较运算
        CMP_EQ,                 // 等于 ==
        CMP_NE,                 // 不等于 !=
        CMP_LT,                 // 小于 <
        CMP_GT,                 // 大于 >
        CMP_LE,                 // 小于等于 <=
        CMP_GE,                 // 大于等于 >=
        
        // 逻辑运算
        AND,                    // 逻辑与 &&
        OR,                     // 逻辑或 ||
        NOT,                    // 逻辑非 !
        
        // 控制流
        JUMP,                   // 无条件跳转
        JUMP_IF_TRUE,           // 条件为真跳转
        JUMP_IF_FALSE,          // 条件为假跳转
        LABEL,                  // 标签定义
        
        // 函数相关
        FUNC_DEF,               // 函数定义
        FUNC_CALL,              // 函数调用
        PARAM,                  // 参数传递
        RETURN,                 // 函数返回
        
        // 类型相关
        TYPE_DEF,               // 类型定义
        TYPE_CHECK,             // 类型检查
        TYPE_CAST,              // 类型转换
        
        // 内存相关
        ALLOC,                  // 内存分配
        FREE,                   // 内存释放
        
        // 三层操作流 (思想1)
        NORMAL_FLOW_BEGIN,      // 正常流开始
        NORMAL_FLOW_END,        // 正常流结束
        EXCEPTION_CATCH,        // 异常捕获（去掉try和{}，保留catch）
        EXCEPTION_THROWS,       // 异常声明（保留throws）
        FLOW_TO,                // 业务逻辑流转（flow to target，不记录堆栈）
        
        // 注解相关 (思想5)
        BEFORE_NAME_HOOK,       // @BeforeName 构造函数钩子
        AFTER_NAME_HOOK,        // @AfterName 析构函数钩子
        BEFORE_PROPS_HOOK,      // @BeforeProps 属性变更前监听钩子
        AFTER_PROPS_HOOK,       // @AfterProps 属性变更后监听钩子
        METADATA,               // 系统注解元数据（@@description等）
        
        // 属性操作
        PROP_GET,               // 属性读取
        PROP_SET,               // 属性设置（触发监听）
        
        // 模块相关
        IMPORT,                 // 导入模块
        EXPORT,                 // 导出符号
        
        // 数组相关
        ARRAY_NEW,              // 创建数组
        ARRAY_GET,              // 数组元素读取
        ARRAY_SET,              // 数组元素设置
        
        // 作用域
        SCOPE_ENTER,            // 进入作用域
        SCOPE_EXIT              // 退出作用域
, FUNC_END, DEALLOC, CALL, TYPE_FIELD, TYPE_END,WHILE_LOOP,BREAK_LOOP,CONTINUE_LOOP,TRY_BLOCK,
FINALLY,MULTI_EXCEPTION_CATCH,BEFORE_ADVICE,AFTER_ADVICE,AFTER_RETURNING_ADVICE,AFTER_THROWING_ADVICE,
AROUND_ADVICE,ASPECT_DEF,JOIN_POINT_CREATE,METHOD_INVOCATION,ADVICE_PROCEED
    }

    // ==================== IR指令数据结构 ====================
    
    /**
     * 单条IR指令
     */
    public static class IRInstruction {
        private final OpCode opCode;
        private final List<Object> operands;
        private final int sourceLineNumber;     // 源代码行号（用于调试）
        private final String sourceFile;         // 源文件名
        private String label;                    // 可选标签
        private String comment;                  // 可选注释
        
        public IRInstruction(OpCode opCode, int sourceLineNumber, String sourceFile, Object... operands) {
            this.opCode = opCode;
            this.operands = new ArrayList<>(Arrays.asList(operands));
            this.sourceLineNumber = sourceLineNumber;
            this.sourceFile = sourceFile;
        }
        
        public OpCode getOpCode() { return opCode; }
        public List<Object> getOperands() { return Collections.unmodifiableList(operands); }
        public int getSourceLineNumber() { return sourceLineNumber; }
        public String getSourceFile() { return sourceFile; }
        
        public void setLabel(String label) { this.label = label; }
        public String getLabel() { return label; }
        
        public void setComment(String comment) { this.comment = comment; }
        public String getComment() { return comment; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (label != null) {
                sb.append(label).append(":");
            }
            sb.append("  ").append(opCode.name());
            if (!operands.isEmpty()) {
                sb.append(" ");
                sb.append(operands.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", ")));
            }
            if (comment != null) {
                sb.append("  ; ").append(comment);
            }
            sb.append("  [line:").append(sourceLineNumber).append("]");
            return sb.toString();
        }
    }
    
       /**
     * IR程序 - 完整的中间表示
     */
public static class IRProgram {
        private final String sourceFileName;
        private final List<IRBasicBlock> topLevelBlocks;
        private final Map<String, Object> metadata;       // 系统注解元数据
        private final List<IRGenerator.FlowAnnotation> flowAnnotations; // 操作流注解
        final List<IRGenerator.HookAnnotation> hookAnnotations; // 钩子注解
        private int labelCounter = 0;
        
        public IRProgram(String sourceFileName) {
            this.sourceFileName = sourceFileName;
            this.topLevelBlocks = new ArrayList<>();
            this.metadata = new LinkedHashMap<>();
            this.flowAnnotations = new ArrayList<>();
            this.hookAnnotations = new ArrayList<>();
        }
        
        public void addTopLevelBlock(IRBasicBlock block) {
            topLevelBlocks.add(block);
        }
        
        public void addMetadata(String key, Object value) {
            metadata.put(key, value);
        }
        
        public void addFlowAnnotation(IRGenerator.FlowAnnotation annotation) {
            flowAnnotations.add(annotation);
        }
        
        public void addHookAnnotation(IRGenerator.HookAnnotation annotation) {
            hookAnnotations.add(annotation);
        }
        
        public String generateLabel(String prefix) {
            return prefix + "_" + (labelCounter++);
        }
        
        public String getSourceFileName() { return sourceFileName; }
        public List<IRBasicBlock> getTopLevelBlocks() { return Collections.unmodifiableList(topLevelBlocks); }
        public Map<String, Object> getMetadata() { return Collections.unmodifiableMap(metadata); }
        
        /**
         * 获取所有指令（自下至上展平）
         */
        public List<IRGenerator.IRInstruction> getAllInstructions() {
            List<IRGenerator.IRInstruction> all = new ArrayList<>();
            for (IRBasicBlock block : topLevelBlocks) {
                all.addAll(block.flattenBottomUp());
            }
            return all;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== IR Program: ").append(sourceFileName).append(" ===");
            
            // 元数据
            if (!metadata.isEmpty()) {
                sb.append("; --- Metadata (System Annotations) ---");
                for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                    sb.append(";   ").append(entry.getKey()).append(" = ").append(entry.getValue()).append(" ");
                }
                sb.append(" ");
            }
            
            // 钩子注解
            if (!hookAnnotations.isEmpty()) {
                sb.append("; --- Hook Annotations (Program Annotations) ---");
                for (IRGenerator.HookAnnotation hook : hookAnnotations) {
                    sb.append(";   ").append(hook).append("");
                }
                sb.append(" ");
            }
            
            // 操作流注解
            if (!flowAnnotations.isEmpty()) {
                sb.append("; --- Flow Annotations ---");
                for (IRGenerator.FlowAnnotation flow : flowAnnotations) {
                    sb.append(";   ").append(flow).append(" ");
                }
                sb.append(" ");
            }
            
            // 代码块
            for (IRBasicBlock block : topLevelBlocks) {
                sb.append(block.toString()).append(" ");
            }
            
            return sb.toString();
        }

        public String getModuleName() {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }
    }

    /**
     * IR基本块 - 对应代码块结构
     */
    public static class IRBasicBlock {
        private final String blockId;
        private final String blockType;          // 对应18种代码块类型之一
        private final int level;                 // 层级深度
        private final List<IRInstruction> instructions;
        private final List<IRBasicBlock> children;
        private IRBasicBlock parent;
        
        public IRBasicBlock(String blockId, String blockType, int level) {
            this.blockId = blockId;
            this.blockType = blockType;
            this.level = level;
            this.instructions = new ArrayList<>();
            this.children = new ArrayList<>();
        }
        
        public void addInstruction(IRInstruction instruction) {
            instructions.add(instruction);
        }
        
        public void addChild(IRBasicBlock child) {
            child.parent = this;
            children.add(child);
        }
        
        public String getBlockId() { return blockId; }
        public String getBlockType() { return blockType; }
        public int getLevel() { return level; }
        public List<IRInstruction> getInstructions() { return Collections.unmodifiableList(instructions); }
        public List<IRBasicBlock> getChildren() { return Collections.unmodifiableList(children); }
        public IRBasicBlock getParent() { return parent; }
        
        /**
         * 获取所有指令（包括子块），自下至上展平
         */
        public List<IRInstruction> flattenBottomUp() {
            List<IRInstruction> result = new ArrayList<>();
            // 先处理所有子块（最底层优先）
            for (IRBasicBlock child : children) {
                result.addAll(child.flattenBottomUp());
            }
            // 再处理当前块的指令
            result.addAll(instructions);
            return result;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            String indent = "  ".repeat(level);
            sb.append(indent).append("Block[").append(blockId)
              .append(", type=").append(blockType)
              .append(", level=").append(level).append("] {");
            for (IRInstruction inst : instructions) {
                sb.append(indent).append(inst.toString()).append(" ");
            }
            for (IRBasicBlock child : children) {
                sb.append(child.toString());
            }
            sb.append(indent).append("}");
            return sb.toString();
        }
    }


    // ==================== 注解数据结构 ====================
    
    /**
     * 操作流注解信息
     */
    public static class FlowAnnotation {
        public enum FlowType { NORMAL, EXCEPTION, BUSINESS }
        
        private final FlowType flowType;
        private final String targetFunction;
        private final String flowTarget;         // 仅 BUSINESS 流使用
        
        public FlowAnnotation(FlowType flowType, String targetFunction, String flowTarget) {
            this.flowType = flowType;
            this.targetFunction = targetFunction;
            this.flowTarget = flowTarget;
        }
        
        public FlowType getFlowType() { return flowType; }
        public String getTargetFunction() { return targetFunction; }
        public String getFlowTarget() { return flowTarget; }
        
        @Override
        public String toString() {
            return flowType.name() + " flow -> " + targetFunction + 
                   (flowTarget != null ? " (target: " + flowTarget + ")" : "");
        }
    }

    /**
     * 钩子注解信息（程序注解）
     */
    public static class HookAnnotation {
        public enum HookType {
            BEFORE_NAME,    // @BeforeName - 构造函数
            AFTER_NAME,     // @AfterName - 析构函数
            BEFORE_PROPS,   // @BeforeProps - 属性变更前监听
            AFTER_PROPS     // @AfterProps - 属性变更后监听
        }
        
        private final HookType hookType;
        private final String methodName;
        private final String target;             // BeforeName/AfterName 的 target
        private final List<String> properties;   // BeforeProps/AfterProps 的属性列表
        
        /**
         * 构造/析构函数注解
         */
        public HookAnnotation(HookType hookType, String methodName, String target) {
            this.hookType = hookType;
            this.methodName = methodName;
            this.target = target;
            this.properties = Collections.emptyList();
        }
        
        /**
         * 属性变更监听注解
         */
        public HookAnnotation(HookType hookType, List<String> properties) {
            this.hookType = hookType;
            this.methodName = null;
            this.target = null;
            this.properties = new ArrayList<>(properties);
        }
        
        public HookType getHookType() { return hookType; }
        public String getMethodName() { return methodName; }
        public String getTarget() { return target; }
        public List<String> getProperties() { return Collections.unmodifiableList(properties); }
        
        @Override
        public String toString() {
            switch (hookType) {
                case BEFORE_NAME:
                    return "@BeforeName(\"" + methodName + "\", \"" + target + "\")";
                case AFTER_NAME:
                    return "@AfterName(\"" + methodName + "\", \"" + target + "\")";
                case BEFORE_PROPS:
                    return "@BeforeProps(\"" + String.join(",", properties) + "\")";
                case AFTER_PROPS:
                    return "@AfterProps(\"" + String.join(",", properties) + "\")";
                default:
                    return hookType.name();
            }
        }
    }

    // ==================== 生成器核心逻辑 ====================
    
    private final String sourceFileName;
    private IRProgram currentProgram;
    private int tempVarCounter = 0;
    private StructureContext structureCtx;
    private SemanticContext semanticCtx;
    private AnnotationResult annotationResult;
    
    /**
     * 构造函数
     * @param sourceFileName 源文件名
     */
    public IRGenerator(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }
    
    /**
     * 核心入口：从层级化代码块生成IR程序
     * 
     * 实现思想2第5步：从当前最底层的代码块开始生成伪代码/中间表示
     * 处理顺序：自下至上（bottom-up）
     * 
     * @param rootBlocks 顶层代码块列表（来自 HierarchyBuilder）
     * @return 完整的IR程序
     */
    public IRProgram generate(List<CodeBlock> rootBlocks) {
        currentProgram = new IRProgram(sourceFileName);
        tempVarCounter = 0;
        
        // 自下至上处理所有代码块
        for (CodeBlock rootBlock : rootBlocks) {
            IRBasicBlock irBlock = generateFromBlock(rootBlock, 0);
            if (irBlock != null) {
                currentProgram.addTopLevelBlock(irBlock);
            }
        }
        
        return currentProgram;
    }

    /**
     * 参数	类型	来源
moduleName	String	编译管道早期确定
structureCtx	StructureContext	第3层结构处理器（9个块处理器）的聚合输出
semanticCtx	SemanticContext	第2层语义处理器（6个语义处理器）的聚合输出
annotationResult	AnnotationResult	注解管理器的处理结果
返回值	ClawIR	封装了 IRProgram + 上下文，传给第4层的 TypeChecker 和 CodeGenerator
     * @param moduleName
     * @param structureCtx
     * @param semanticCtx
     * @param annotationResult
     * @return
     */
    /**
     * 核心入口 — 与调用处签名完全匹配
     *
     * @param moduleName       模块名称
     * @param structureCtx     结构上下文（层级化代码块 + 配对关系 + 作用域树）
     * @param semanticCtx      语义上下文（类型 + 函数签名 + 变量 + 控制流 + 字面量 + 运算符）
     * @param annotationResult 注解处理结果（4个程序注解 + 5个系统注解）
     * @return 完整的IR程序 (ClawIR)
     */
    public ClawIR generate(String moduleName,
                           StructureContext structureCtx,
                           SemanticContext semanticCtx,
                           AnnotationResult annotationResult) {

        this.structureCtx = structureCtx;
        this.semanticCtx = semanticCtx;
        this.annotationResult = annotationResult;
        this.tempVarCounter = 0;

        // 创建IR程序
        currentProgram = new IRProgram(moduleName);

        // 1. 处理系统注解元数据（@@description, @@param, @@return, @@example, @@deprecated）
        // processSystemAnnotations();

        // // 2. 处理程序注解钩子（@BeforeName, @AfterName, @BeforeProps, @AfterProps）
        // processProgramAnnotations();

        // // 3. 从结构上下文获取所有顶层代码块，自下至上生成IR
        // List<BlockNode> rootBlocks = structureCtx.getRootBlocks();
        // for (BlockNode rootBlock : rootBlocks) {
        //     IRBasicBlock irBlock = generateFromBlockNode(rootBlock, 0);
        //     if (irBlock != null) {
        //         currentProgram.addTopLevelBlock(irBlock);
        //     }
        // }

        // // 4. 封装为 ClawIR
        return new ClawIR(currentProgram, structureCtx, semanticCtx, annotationResult);
       
    }
    
    /**
     * 递归生成：先处理子块（最底层），再处理当前块
     * 
     * 这是"从当前最底层的代码块开始生成"的核心实现
     */
    private IRBasicBlock generateFromBlock(CodeBlock codeBlock, int level) {
        String blockId = currentProgram.generateLabel("block");
        BlockType blockType = codeBlock.getBlockType(); // 18种代码块类型之一
        
        IRBasicBlock irBlock = new IRBasicBlock(blockId, blockType.name(), level);
        
        // 第一步：递归处理所有子块（自下至上）
        for (CodeBlock child : codeBlock.getChildren()) {
            IRBasicBlock childIR = generateFromBlock(child, level + 1);
            if (childIR != null) {
                irBlock.addChild(childIR);
            }
        }
        
        // 第二步：根据块类型生成当前块的IR指令
        switch (blockType.name()) {//TODO
            case "function_block":
                generateFunctionBlock(irBlock, codeBlock);
                break;
            case "parameter_block":
                generateParameterBlock(irBlock, codeBlock);
                break;
            case "return_block":
                generateReturnBlock(irBlock, codeBlock);
                break;
            case "control_flow_block":
                generateControlFlowBlock(irBlock, codeBlock);
                break;
            case "condition_block":
                generateConditionBlock(irBlock, codeBlock);
                break;
            case "loop_body_block":
                generateLoopBodyBlock(irBlock, codeBlock);
                break;
            case "expression_block":
                generateExpressionBlock(irBlock, codeBlock);
                break;
            case "function_call_block":
                generateFunctionCallBlock(irBlock, codeBlock);
                break;
            case "array_block":
                generateArrayBlock(irBlock, codeBlock);
                break;
            case "variable_declaration_block":
                generateVariableDeclarationBlock(irBlock, codeBlock);
                break;
            case "import_declaration_block":
                generateImportDeclarationBlock(irBlock, codeBlock);
                break;
            case "scope_block":
                generateScopeBlock(irBlock, codeBlock);
                break;
            case "type_inner_block":
                generateTypeInnerBlock(irBlock, codeBlock);
                break;
            case "assignment_block":
                generateAssignmentBlock(irBlock, codeBlock);
                break;
            case "type_definition_block":
                generateTypeDefinitionBlock(irBlock, codeBlock);
                break;
            case "module_block":
                generateModuleBlock(irBlock, codeBlock);
                break;
            case "annotation_block":
                generateAnnotationBlock(irBlock, codeBlock);
                break;
            default:
                // 未知块类型，生成NOP
                irBlock.addInstruction(new IRInstruction(
                    OpCode.NOP, codeBlock.getStartLine(), sourceFileName,
                    "unknown_block_type: " + blockType
                ));
                break;
        }
        
        return irBlock;
    }
    
    // ==================== 18种代码块类型的IR生成器 ====================
    
    /**
     * 函数块处理器 - function_block
     * 处理三层操作流标记（思想1）
     */
    private void generateFunctionBlock(IRBasicBlock irBlock, CodeBlock codeBlock) {
        String functionName = codeBlock.getAttribute("name");
        String flowType = codeBlock.getAttribute("flow_type"); // normal / exception / flow
        int line = codeBlock.getStartLine();
        
        // 生成函数定义指令
        IRInstruction funcDef = new IRInstruction(
            OpCode.FUNC_DEF, line, sourceFileName, functionName
        );
        funcDef.setLabel(functionName);
        funcDef.setComment("Function definition");
        irBlock.addInstruction(funcDef);
        
        // 根据操作流类型生成相应的流标记
        if (flowType != null) {
            switch (flowType) {
                case "normal":
                    irBlock.addInstruction(new IRInstruction(
                        OpCode.NORMAL_FLOW_BEGIN, line, sourceFileName, functionName
                    ));
                    currentProgram.addFlowAnnotation(new FlowAnnotation(
                        FlowAnnotation.FlowType.NORMAL, functionName, null
                    ));
                    break;
                case "exception":
                    // 异常流：去掉try和{}，保留catch和throws
                    currentProgram.addFlowAnnotation(new FlowAnnotation(
                        FlowAnnotation.FlowType.EXCEPTION, functionName, null
                    ));
                    break;
                case "flow":
                    String target = codeBlock.getAttribute("flow_target");
                    irBlock.addInstruction(new IRInstruction(
                        OpCode.FLOW_TO, line, sourceFileName, target
                    ));
                    currentProgram.addFlowAnnotation(new FlowAnnotation(
                        FlowAnnotation.FlowType.BUSINESS, functionName, target
                    ));
                    break;
            }
        }
        
        // 进入函数作用域
        irBlock.addInstruction(new IRInstruction(
            OpCode.SCOPE_ENTER, line, sourceFileName, functionName
        ));
    }
    
    /**
     * 参数块处理器 - parameter_block
     */
    private void generateParameterBlock(IRBasicBlock irBlock, CodeBlock codeBlock) {
        List<Map<String, String>> params = codeBlock.getParameterList();
        int line = codeBlock.getStartLine();
        
        if (params != null) {
            for (Map<String, String> param : params) {
                String paramName = param.get("name");
                String paramType = param.get("type");
                
                IRInstruction paramInst = new IRInstruction(
                    OpCode.PARAM, line, sourceFileName, paramName, paramType
                );
                paramInst.setComment("Parameter: " + paramName + " : " + paramType);
                irBlock.addInstruction(paramInst);
            }
        }
    }
    
    /**
     * 返回块处理器 - return_block
     */
    private void generateReturnBlock(IRBasicBlock irBlock, CodeBlock codeBlock) {
        int line = codeBlock.getStartLine();
        String returnExpr = codeBlock.getAttribute("return_expression");
        
        if (returnExpr != null && !returnExpr.isEmpty()) {
            // 有返回值表达式
            String tempVar = generateTempVar();
            irBlock.addInstruction(new IRInstruction(
                OpCode.LOAD_VAR, line, sourceFileName, returnExpr
            ));
            irBlock.addInstruction(new IRInstruction(
                OpCode.RETURN, line, sourceFileName, tempVar
            ));
        } else {
            // 无返回值（Void）
            irBlock.addInstruction(new IRInstruction(
                OpCode.RETURN, line, sourceFileName
            ));
        }
    }
    
    /**
     * 控制流块处理器 - control_flow_block
     */
    private void generateControlFlowBlock(IRBasicBlock irBlock, CodeBlock codeBlock) {
        String controlType = codeBlock.getAttribute("control_type"); // if, for, while
        int line = codeBlock.getStartLine();
        
        if (controlType == null) return;
        
        switch (controlType) {
            case "if":
                generateIfBlock(irBlock, codeBlock, line);
                break;
            case "for":
                generateForBlock(irBlock, codeBlock, line);
                break;
            case "while":
                generateWhileBlock(irBlock, codeBlock, line);
                break;
        }
    }
    
    private void generateIfBlock(IRBasicBlock irBlock, CodeBlock codeBlock, int line) {
        String elseLabel = currentProgram.generateLabel("else");
        String endIfLabel = currentProgram.generateLabel("endif");
        
        // 条件判断（由子块condition_block生成）
        irBlock.addInstruction(new IRInstruction(
            OpCode.JUMP_IF_FALSE, line, sourceFileName, elseLabel
        ));
        
        // then分支（由子块生成）
        
        // else标签
        IRInstruction elseLabelInst = new IRInstruction(
            OpCode.LABEL, line, sourceFileName, elseLabel
        );
        elseLabelInst.setLabel(elseLabel);
        irBlock.addInstruction(elseLabelInst);
        
        // endif标签
        IRInstruction endIfLabelInst = new IRInstruction(
            OpCode.LABEL, line, sourceFileName, endIfLabel
        );
        endIfLabelInst.setLabel(endIfLabel);
        irBlock.addInstruction(endIfLabelInst);
    }
    
    private void generateForBlock(IRBasicBlock irBlock, CodeBlock codeBlock, int line) {
        String loopStartLabel = currentProgram.generateLabel("for_start");
        String loopEndLabel = currentProgram.generateLabel("for_end");
        
        // 循环开始标签
        IRInstruction startLabel = new IRInstruction(
            OpCode.LABEL, line, sourceFileName, loopStartLabel
        );
        startLabel.setLabel(loopStartLabel);
        irBlock.addInstruction(startLabel);
        
        // 条件检查（由子块生成）
        irBlock.addInstruction(new IRInstruction(
            OpCode.JUMP_IF_FALSE, line, sourceFileName, loopEndLabel
        ));
        
        // 循环体（由子块 loop_body_block 生成）
        
        // 跳回循环开始
        irBlock.addInstruction(new IRInstruction(
            OpCode.JUMP, line, sourceFileName, loopStartLabel
        ));
        
        // 循环结束标签
        IRInstruction endLabel = new IRInstruction(
            OpCode.LABEL, line, sourceFileName, loopEndLabel
        );
        endLabel.setLabel(loopEndLabel);
        irBlock.addInstruction(endLabel);
    }
    
    private void generateWhileBlock(IRBasicBlock irBlock, CodeBlock codeBlock, int line) {
        String loopStartLabel = currentProgram.generateLabel("while_start");
        String loopEndLabel = currentProgram.generateLabel("while_end");
        
        IRInstruction startLabel = new IRInstruction(
            OpCode.LABEL, line, sourceFileName, loopStartLabel
        );
        startLabel.setLabel(loopStartLabel);
        irBlock.addInstruction(startLabel);
        
        irBlock.addInstruction(new IRInstruction(
            OpCode.JUMP_IF_FALSE, line, sourceFileName, loopEndLabel
        ));
        
        irBlock.addInstruction(new IRInstruction(
            OpCode.JUMP, line, sourceFileName, loopStartLabel
        ));
        
        IRInstruction endLabel = new IRInstruction(
            OpCode.LABEL, line, sourceFileName, loopEndLabel
        );
        endLabel.setLabel(loopEndLabel);
        irBlock.addInstruction(endLabel);
    }
    
    /**
     * 条件块处理器 - condition_block
     */
    private void generateConditionBlock(IRBasicBlock irBlock, CodeBlock codeBlock) {
        int line = codeBlock.getStartLine();
        String condition = codeBlock.getAttribute("condition_expression");
        
        if (condition != null) {
            irBlock.addInstruction(new IRInstruction(
                OpCode.LOAD_VAR, line, sourceFileName, condition
            ));
        }
    }
    
    /**
     * 循环体块处理器 - loop_body_block
     */
    private void generateLoopBodyBlock(IRBasicBlock irBlock, CodeBlock codeBlock) {
        int line = codeBlock.getStartLine();
        irBlock.addInstruction(new IRInstruction(
            OpCode.SCOPE_ENTER, line, sourceFileName, "loop_body"
        ));
        // 循环体内容由子块递归生成
    }
    
    /**
     * 表达式块处理器 - expression_block
     */
    private void generateExpressionBlock(IRBasicBlock irBlock, CodeBlock codeBlock) {
        int line = codeBlock.getStartLine();
        String exprType = codeBlock.getAttribute("expression_type");
        
        if ("binary".equals(exprType)) {
            String operator = codeBlock.getAttribute("operator");
            String left = codeBlock.getAttribute("left_operand");
            String right = codeBlock.getAttribute("right_operand");
            
            // 加载左操作数
            irBlock.addInstruction(new IRInstruction(
                OpCode.LOAD_VAR, line, sourceFileName, left
            ));
            
            // 加载右操作数
            irBlock.addInstruction(new IRInstruction(
                OpCode.LOAD_VAR, line, sourceFileName, right
            ));
            
            // 执行运算
            OpCode opCode = mapOperatorToOpCode(operator);
            String tempVar = generateTempVar();
            IRInstruction binOp = new IRInstruction(
                opCode, line, sourceFileName, tempVar, left, right
            );
            binOp.setComment(left + " " + operator + " " + right);
            irBlock.addInstruction(binOp);
        } else if ("unary".equals(exprType)) {
            String operator = codeBlock.getAttribute("operator");
            String operand = codeBlock.getAttribute("operand");
            
            irBlock.addInstruction(new IRInstruction(
                OpCode.LOAD_VAR, line, sourceFileName, operand
            ));
            
            OpCode opCode = mapOperatorToOpCode(operator);
            String tempVar = generateTempVar();
            irBlock.addInstruction(new IRInstruction(
                opCode, line, sourceFileName, tempVar, operand
            ));
        }
    }
    
    /**
     * 函数调用块处理器 - function_call_block
     */
    private void generateFunctionCallBlock(IRBasicBlock irBlock, CodeBlock codeBlock) {
        int line = codeBlock.getStartLine();
        String functionName = codeBlock.getAttribute("callee_name");
        List<String> arguments = codeBlock.getArgumentList();
        
        // 加载所有参数
        if (arguments != null) {
            for (int i = 0; i < arguments.size(); i++) {
                irBlock.addInstruction(new IRInstruction(
                    OpCode.PARAM, line, sourceFileName, arguments.get(i), i
                ));
            }
        }
        
        // 函数调用
        String resultVar = generateTempVar();
        IRInstruction callInst = new IRInstruction(
            OpCode.FUNC_CALL, line, sourceFileName, functionName, resultVar
        );
        callInst.setComment("Call " + functionName + " with " + 
            (arguments != null ? arguments.size() : 0) + " args");
        irBlock.addInstruction(callInst);
    }
    
    /**
     * 数组块处理器 - array_block
     */
    private void generateArrayBlock(IRBasicBlock irBlock, CodeBlock codeBlock) {
        int line = codeBlock.getStartLine();
        String arrayName = codeBlock.getAttribute("array_name");
        String elementType = codeBlock.getAttribute("element_type");
        String size = codeBlock.getAttribute("size");
        
        irBlock.addInstruction(new IRInstruction(
            OpCode.ARRAY_NEW, line, sourceFileName, arrayName, elementType, size
        ));
    }
    
    /**
     * 变量声明块处理器 - variable_declaration_block
     */
    private void generateVariableDeclarationBlock(IRBasicBlock irBlock, CodeBlock codeBlock) {
        int line = codeBlock.getStartLine();
        String varName = codeBlock.getAttribute("var_name");
        String varType = codeBlock.getAttribute("var_type");
        String initValue = codeBlock.getAttribute("init_value");
        boolean isConst = "true".equals(codeBlock.getAttribute("is_const"));
        
        // 分配内存
        irBlock.addInstruction(new IRInstruction(
            OpCode.ALLOC, line, sourceFileName, varName, varType
        ));
        
        // 如果有初始值，进行赋值
        if (initValue != null) {
            irBlock.addInstruction(new IRInstruction(
                OpCode.LOAD_CONST, line, sourceFileName, initValue
            ));
            IRInstruction storeInst = new IRInstruction(
                OpCode.STORE_VAR, line, sourceFileName, varName
            );
            storeInst.setComment((isConst ? "const " : "var ") + varName + " : " + varType + " = " + initValue);
            irBlock.addInstruction(storeInst);
        }
    }
    
    /**
     * 导入声明块处理器 - import_declaration_block
     */
    private void generateImportDeclarationBlock(IRBasicBlock irBlock, CodeBlock codeBlock) {
        int line = codeBlock.getStartLine();
        String moduleName = codeBlock.getAttribute("module_name");
        List<String> symbols = codeBlock.getImportSymbols();
        
        if (symbols != null && !symbols.isEmpty()) {
            for (String symbol : symbols) {
                irBlock.addInstruction(new IRInstruction(
                    OpCode.IMPORT, line, sourceFileName, moduleName, symbol
                ));
            }
        } else {
            irBlock.addInstruction(new IRInstruction(
                OpCode.IMPORT, line, sourceFileName, moduleName
            ));
        }
    }
    
    /**
     * 作用域块处理器 - scope_block
     */
    private void generateScopeBlock(IRBasicBlock irBlock, CodeBlock codeBlock) {
        int line = codeBlock.getStartLine();
        String scopeName = codeBlock.getAttribute("scope_name");
        
        irBlock.addInstruction(new IRInstruction(
            OpCode.SCOPE_ENTER, line, sourceFileName, scopeName != null ? scopeName : "anonymous"
        ));
        
        // 子块内容递归生成
        
        irBlock.addInstruction(new IRInstruction(
            OpCode.SCOPE_EXIT, line, sourceFileName, scopeName != null ? scopeName : "anonymous"
        ));
    }
    
    /**
     * 类型内部块处理器 - type_inner_block
     */
    private void generateTypeInnerBlock(IRBasicBlock irBlock, CodeBlock codeBlock) {
        int line = codeBlock.getStartLine();
        // 类型内部成员由子块生成
        irBlock.addInstruction(new IRInstruction(
            OpCode.SCOPE_ENTER, line, sourceFileName, "type_inner"
        ));
    }
    
    /**
     * 赋值块处理器 - assignment_block
     * 
     * 重要：赋值操作需要检查是否有 @BeforeProps/@AfterProps 监听
     */
    private void generateAssignmentBlock(IRBasicBlock irBlock, CodeBlock codeBlock) {
        int line = codeBlock.getStartLine();
        String target = codeBlock.getAttribute("assignment_target");
        String value = codeBlock.getAttribute("assignment_value");
        
        // 检查是否有属性变更监听
        boolean hasBeforePropsHook = hasPropertyHook(target, HookAnnotation.HookType.BEFORE_PROPS);
        boolean hasAfterPropsHook = hasPropertyHook(target, HookAnnotation.HookType.AFTER_PROPS);
        
        // @BeforeProps 钩子（属性变更前触发）
        if (hasBeforePropsHook) {
            IRInstruction beforeHook = new IRInstruction(
                OpCode.BEFORE_PROPS_HOOK, line, sourceFileName, target
            );
            beforeHook.setComment("@BeforeProps trigger for: " + target);
            irBlock.addInstruction(beforeHook);
        }
        
        // 加载值
        irBlock.addInstruction(new IRInstruction(
            OpCode.LOAD_VAR, line, sourceFileName, value
        ));
        
        // 属性设置（区别于普通变量存储）
        if (hasBeforePropsHook || hasAfterPropsHook) {
            irBlock.addInstruction(new IRInstruction(
                OpCode.PROP_SET, line, sourceFileName, target
            ));
        } else {
            irBlock.addInstruction(new IRInstruction(
                OpCode.STORE_VAR, line, sourceFileName, target
            ));
        }
        
        // @AfterProps 钩子（属性变更后触发）
        if (hasAfterPropsHook) {
            IRInstruction afterHook = new IRInstruction(
                OpCode.AFTER_PROPS_HOOK, line, sourceFileName, target
            );
            afterHook.setComment("@AfterProps trigger for: " + target);
            irBlock.addInstruction(afterHook);
        }
    }
    
    /**
     * 类型定义块处理器 - type_definition_block
     */
    private void generateTypeDefinitionBlock(IRBasicBlock irBlock, CodeBlock codeBlock) {
        int line = codeBlock.getStartLine();
        String typeName = codeBlock.getAttribute("type_name");
        
        irBlock.addInstruction(new IRInstruction(
            OpCode.TYPE_DEF, line, sourceFileName, typeName
        ));
    }
    
    /**
     * 模块块处理器 - module_block
     */
    private void generateModuleBlock(IRBasicBlock irBlock, CodeBlock codeBlock) {
        int line = codeBlock.getStartLine();
        String moduleName = codeBlock.getAttribute("module_name");
        
        irBlock.addInstruction(new IRInstruction(
            OpCode.SCOPE_ENTER, line, sourceFileName, "module:" + moduleName
        ));
        
        // 处理 export
        List<String> exports = codeBlock.getExportSymbols();
        if (exports != null) {
            for (String symbol : exports) {
                irBlock.addInstruction(new IRInstruction(
                    OpCode.EXPORT, line, sourceFileName, symbol
                ));
            }
        }
    }
    
    /**
     * 注解块处理器 - annotation_block（新增，思想5）
     * 
     * 处理4个程序注解和5个系统注解
     */
    private void generateAnnotationBlock(IRBasicBlock irBlock, CodeBlock codeBlock) {
        int line = codeBlock.getStartLine();
        String annotationType = codeBlock.getAttribute("annotation_type");
        
        if (annotationType == null) return;
        
        switch (annotationType) {
            // === 4个程序注解 ===
            case "BeforeName": {
                String methodName = codeBlock.getAttribute("method_name");
                String target = codeBlock.getAttribute("target");
                
                IRInstruction hook = new IRInstruction(
                    OpCode.BEFORE_NAME_HOOK, line, sourceFileName, methodName, target
                );
                hook.setComment("Constructor hook: @BeforeName(\"" + methodName + "\", \"" + target + "\")");
                irBlock.addInstruction(hook);
                
                currentProgram.addHookAnnotation(new HookAnnotation(
                    HookAnnotation.HookType.BEFORE_NAME, methodName, target
                ));
                break;
            }
            
            case "AfterName": {
                String methodName = codeBlock.getAttribute("method_name");
                String target = codeBlock.getAttribute("target");
                
                IRInstruction hook = new IRInstruction(
                    OpCode.AFTER_NAME_HOOK, line, sourceFileName, methodName, target
                );
                hook.setComment("Destructor hook: @AfterName(\"" + methodName + "\", \"" + target + "\")");
                irBlock.addInstruction(hook);
                
                currentProgram.addHookAnnotation(new HookAnnotation(
                    HookAnnotation.HookType.AFTER_NAME, methodName, target
                ));
                break;
            }
            
            case "BeforeProps": {
                String propsStr = codeBlock.getAttribute("property_list");
                List<String> properties = parsePropertyList(propsStr);
                
                for (String prop : properties) {
                    irBlock.addInstruction(new IRInstruction(
                        OpCode.BEFORE_PROPS_HOOK, line, sourceFileName, prop
                    ));
                }
                
                currentProgram.addHookAnnotation(new HookAnnotation(
                    HookAnnotation.HookType.BEFORE_PROPS, properties
                ));
                break;
            }
            
            case "AfterProps": {
                String propsStr = codeBlock.getAttribute("property_list");
                List<String> properties = parsePropertyList(propsStr);
                
                for (String prop : properties) {
                    irBlock.addInstruction(new IRInstruction(
                        OpCode.AFTER_PROPS_HOOK, line, sourceFileName, prop
                    ));
                }
                
                currentProgram.addHookAnnotation(new HookAnnotation(
                    HookAnnotation.HookType.AFTER_PROPS, properties
                ));
                break;
            }
            
            // === 5个系统注解 ===
            case "description": {
                String humanDesc = codeBlock.getAttribute("human_desc");
                String ioSpec = codeBlock.getAttribute("io_spec");
                
                currentProgram.addMetadata("@@description.human", humanDesc);
                currentProgram.addMetadata("@@description.io", ioSpec);
                
                IRInstruction meta = new IRInstruction(
                    OpCode.METADATA, line, sourceFileName, "description", humanDesc, ioSpec
                );
                meta.setComment("@@description(\"" + humanDesc + "\", \"" + ioSpec + "\")");
                irBlock.addInstruction(meta);
                break;
            }
            
            case "param": {
                String paramName = codeBlock.getAttribute("param_name");
                String description = codeBlock.getAttribute("description");
                
                currentProgram.addMetadata("@@param." + paramName, description);
                
                irBlock.addInstruction(new IRInstruction(
                    OpCode.METADATA, line, sourceFileName, "param", paramName, description
                ));
                break;
            }
            
            case "return": {
                String description = codeBlock.getAttribute("description");
                currentProgram.addMetadata("@@return", description);
                
                irBlock.addInstruction(new IRInstruction(
                    OpCode.METADATA, line, sourceFileName, "return", description
                ));
                break;
            }
            
            case "example": {
                String usage = codeBlock.getAttribute("usage_example");
                currentProgram.addMetadata("@@example", usage);
                
                irBlock.addInstruction(new IRInstruction(
                    OpCode.METADATA, line, sourceFileName, "example", usage
                ));
                break;
            }
            
            case "deprecated": {
                String reason = codeBlock.getAttribute("reason");
                String alternative = codeBlock.getAttribute("alternative");
                
                currentProgram.addMetadata("@@deprecated.reason", reason);
                currentProgram.addMetadata("@@deprecated.alternative", alternative);
                
                irBlock.addInstruction(new IRInstruction(
                    OpCode.METADATA, line, sourceFileName, "deprecated", reason, alternative
                ));
                break;
            }
        }
    }
    
    // ==================== 异常流IR生成（思想1） ====================
    
    /**
     * 生成异常流IR
     * 
     * Claw特性：去掉try和{}，保留catch和throws
     * catch与当前代码块边界一致，不生成堆栈信息
     */
    public void generateExceptionFlow(IRBasicBlock irBlock, CodeBlock codeBlock) {
        int line = codeBlock.getStartLine();
        String exceptionType = codeBlock.getAttribute("exception_type");
        String exceptionVar = codeBlock.getAttribute("exception_var");
        
        // catch指令（无需try包围，与代码块边界一致）
        IRInstruction catchInst = new IRInstruction(
            OpCode.EXCEPTION_CATCH, line, sourceFileName, exceptionType, exceptionVar
        );
        catchInst.setComment("Simplified catch (no try/{}): " + exceptionType + " " + exceptionVar);
        irBlock.addInstruction(catchInst);
    }
    
    /**
     * 生成业务逻辑流转IR
     * 
     * Claw特性：flow to target，直接向后向上跳转，不记录堆栈
     */
    public void generateBusinessFlow(IRBasicBlock irBlock, String target, int line) {
        IRInstruction flowInst = new IRInstruction(
            OpCode.FLOW_TO, line, sourceFileName, target
        );
        flowInst.setComment("Business flow: direct jump to " + target + " (no stack trace)");
        irBlock.addInstruction(flowInst);
    }
    
    // ==================== 内存管理IR生成（构造/析构） ====================
    
    /**
     * 生成构造函数调用IR（@BeforeName触发）
     * 系统在分配内存时自动调用
     */
    public void generateConstructorCall(IRBasicBlock irBlock, String methodName, 
                                         String target, int line) {
        // 内存分配
        irBlock.addInstruction(new IRInstruction(
            OpCode.ALLOC, line, sourceFileName, target
        ));
        
        // 构造函数钩子调用
        IRInstruction hookCall = new IRInstruction(
            OpCode.BEFORE_NAME_HOOK, line, sourceFileName, methodName, target
        );
        hookCall.setComment("Auto-called on memory allocation: @BeforeName");
        irBlock.addInstruction(hookCall);
        
        // 调用构造函数
        irBlock.addInstruction(new IRInstruction(
            OpCode.FUNC_CALL, line, sourceFileName, methodName
        ));
    }
    
    /**
     * 生成析构函数调用IR（@AfterName触发）
     * 系统在回收内存时自动调用
     */
    public void generateDestructorCall(IRBasicBlock irBlock, String methodName, 
                                        String target, int line) {
        // 析构函数钩子调用
        IRInstruction hookCall = new IRInstruction(
            OpCode.AFTER_NAME_HOOK, line, sourceFileName, methodName, target
        );
        hookCall.setComment("Auto-called on memory deallocation: @AfterName");
        irBlock.addInstruction(hookCall);
        
        // 调用析构函数
        irBlock.addInstruction(new IRInstruction(
            OpCode.FUNC_CALL, line, sourceFileName, methodName
        ));
        
        // 内存释放
        irBlock.addInstruction(new IRInstruction(
            OpCode.FREE, line, sourceFileName, target
        ));
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 生成临时变量名
     */
    private String generateTempVar() {
        return "_t" + (tempVarCounter++);
    }
    
    /**
     * 将运算符字符串映射到IR操作码
     */
    private OpCode mapOperatorToOpCode(String operator) {
        if (operator == null) return OpCode.NOP;
        
        switch (operator) {
            case "+":  return OpCode.ADD;
            case "-":  return OpCode.SUB;
            case "*":  return OpCode.MUL;
            case "/":  return OpCode.DIV;
            case "%":  return OpCode.MOD;
            case "==": return OpCode.CMP_EQ;
            case "!=": return OpCode.CMP_NE;
            case "<":  return OpCode.CMP_LT;
            case ">":  return OpCode.CMP_GT;
            case "<=": return OpCode.CMP_LE;
            case ">=": return OpCode.CMP_GE;
            case "&&": return OpCode.AND;
            case "||": return OpCode.OR;
            case "!":  return OpCode.NOT;
            default:   return OpCode.NOP;
        }
    }
    
    /**
     * 解析属性列表字符串（逗号分隔）
     * 例如："user.age,user.name" -> ["user.age", "user.name"]
     */
    private List<String> parsePropertyList(String propsStr) {
        if (propsStr == null || propsStr.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(propsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
    
    /**
     * 检查指定属性是否有对应的钩子注解
     */
    private boolean hasPropertyHook(String propertyName, HookAnnotation.HookType hookType) {
        if (currentProgram == null || propertyName == null) return false;
        
        // 遍历当前程序中注册的钩子注解
        for (HookAnnotation hook : currentProgram.hookAnnotations) {
            if (hook.getHookType() == hookType) {
                for (String prop : hook.getProperties()) {
                    if (prop.equals(propertyName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    // ==================== 调试与输出 ====================
    
    /**
     * 将IR程序输出为可读文本格式
     */
    public String dumpIR(IRProgram program) {
        return program.toString();
    }
    
    /**
     * 将IR程序输出为紧凑的字节码格式描述
     */
    public String dumpCompactIR(IRProgram program) {
        StringBuilder sb = new StringBuilder();
        List<IRInstruction> allInstructions = program.getAllInstructions();
        
        sb.append("; Compact IR dump for: ").append(program.getSourceFileName()).append(" ");
        sb.append("; Total instructions: ").append(allInstructions.size()).append(" ");
        
        int index = 0;
        for (IRInstruction inst : allInstructions) {
            sb.append(String.format("%04d: %-20s", index, inst.getOpCode().name()));
            if (!inst.getOperands().isEmpty()) {
                sb.append(inst.getOperands().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", ")));
            }
            sb.append(" ");
            index++;
        }
        
        return sb.toString();
    }
}
