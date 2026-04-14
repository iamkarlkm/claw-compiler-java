// package claw.compiler.generators;

// import claw.compiler.context.StructureContext;
// import claw.compiler.context.StructureContext.BlockNode;
// import claw.compiler.context.SemanticContext;
// import claw.compiler.context.SemanticContext.*;
// import claw.compiler.annotation.AnnotationResult;

// import java.util.*;
// import java.util.stream.Collectors;

// /**
//  * IRGenerator - 更新后的生成入口
//  * 
//  * generate 方法现在接收：
//  *   1. moduleName     - 模块名
//  *   2. structureCtx   - 结构上下文（第3层输出：代码块、配对、作用域）
//  *   3. semanticCtx    - 语义上下文（第2层输出：类型、函数、变量、控制流）
//  *   4. annotationResult - 注解处理结果（思想5：4个程序注解 + 5个系统注解）
//  */
// public class IRGenerator {

//     // ... (前面已定义的 OpCode, IRInstruction, IRBasicBlock, IRProgram 等内部类保持不变) ...

//     private final String sourceFileName;
//     private IRProgram currentProgram;
//     private SemanticContext semanticCtx;
//     private StructureContext structureCtx;
//     private AnnotationResult annotationResult;
//     private int tempVarCounter = 0;

//     public IRGenerator(String sourceFileName) {
//         this.sourceFileName = sourceFileName;
//     }

//     /**
//      * 核心入口 — 与调用处签名完全匹配
//      *
//      * @param moduleName       模块名称
//      * @param structureCtx     结构上下文（层级化代码块 + 配对关系 + 作用域树）
//      * @param semanticCtx      语义上下文（类型 + 函数签名 + 变量 + 控制流 + 字面量 + 运算符）
//      * @param annotationResult 注解处理结果（4个程序注解 + 5个系统注解）
//      * @return 完整的IR程序 (ClawIR)
//      */
//     public ClawIR generate(String moduleName,
//                            StructureContext structureCtx,
//                            SemanticContext semanticCtx,
//                            AnnotationResult annotationResult) {

//         this.structureCtx = structureCtx;
//         this.semanticCtx = semanticCtx;
//         this.annotationResult = annotationResult;
//         this.tempVarCounter = 0;

//         // 创建IR程序
//         currentProgram = new IRProgram(moduleName);

//         // 1. 处理系统注解元数据（@@description, @@param, @@return, @@example, @@deprecated）
//         processSystemAnnotations();

//         // 2. 处理程序注解钩子（@BeforeName, @AfterName, @BeforeProps, @AfterProps）
//         processProgramAnnotations();

//         // 3. 从结构上下文获取所有顶层代码块，自下至上生成IR
//         List<BlockNode> rootBlocks = structureCtx.getRootBlocks();
//         for (BlockNode rootBlock : rootBlocks) {
//             IRBasicBlock irBlock = generateFromBlockNode(rootBlock, 0);
//             if (irBlock != null) {
//                 currentProgram.addTopLevelBlock(irBlock);
//             }
//         }

//         // 4. 封装为 ClawIR
//         return new ClawIR(currentProgram, structureCtx, semanticCtx, annotationResult);
//     }

//     /**
//      * 递归生成：先处理子块（最底层），再处理当前块
//      * 使用 StructureContext.BlockNode 替代原来的 CodeBlock
//      */
//     private IRBasicBlock generateFromBlockNode(BlockNode blockNode, int level) {
//         String blockId = currentProgram.generateLabel("block");
//         String blockType = blockNode.getBlockType();

//         IRBasicBlock irBlock = new IRBasicBlock(blockId, blockType, level);

//         // 第一步：递归处理所有子块（自下至上）
//         for (BlockNode child : blockNode.getChildren()) {
//             IRBasicBlock childIR = generateFromBlockNode(child, level + 1);
//             if (childIR != null) {
//                 irBlock.addChild(childIR);
//             }
//         }

//         // 第二步：根据块类型 + 语义上下文生成IR指令
//         generateBlockInstructions(irBlock, blockNode);

//         return irBlock;
//     }

//     /**
//      * 根据块类型分派到对应的生成方法
//      * 同时利用 semanticCtx 获取类型、函数签名等语义信息
//      */
//     private void generateBlockInstructions(IRBasicBlock irBlock, BlockNode blockNode) {
//         String blockType = blockNode.getBlockType();
//         int line = blockNode.getStartLine();

//         switch (blockType) {
//             case "function_block":
//                 generateFunctionBlockFromNode(irBlock, blockNode);
//                 break;
//             case "parameter_block":
//                 generateParameterBlockFromNode(irBlock, blockNode);
//                 break;
//             case "return_block":
//                 generateReturnBlockFromNode(irBlock, blockNode);
//                 break;
//             case "control_flow_block":
//                 generateControlFlowBlockFromNode(irBlock, blockNode);
//                 break;
//             case "condition_block":
//                 generateConditionBlockFromNode(irBlock, blockNode);
//                 break;
//             case "loop_body_block":
//                 generateLoopBodyBlockFromNode(irBlock, blockNode);
//                 break;
//             case "expression_block":
//                 generateExpressionBlockFromNode(irBlock, blockNode);
//                 break;
//             case "function_call_block":
//                 generateFunctionCallBlockFromNode(irBlock, blockNode);
//                 break;
//             case "array_block":
//                 generateArrayBlockFromNode(irBlock, blockNode);
//                 break;
//             case "variable_declaration_block":
//                 generateVariableDeclarationBlockFromNode(irBlock, blockNode);
//                 break;
//             case "import_declaration_block":
//                 generateImportDeclarationBlockFromNode(irBlock, blockNode);
//                 break;
//             case "scope_block":
//                 generateScopeBlockFromNode(irBlock, blockNode);
//                 break;
//             case "type_inner_block":
//                 generateTypeInnerBlockFromNode(irBlock, blockNode);
//                 break;
//             case "assignment_block":
//                 generateAssignmentBlockFromNode(irBlock, blockNode);
//                 break;
//             case "type_definition_block":
//                 generateTypeDefinitionBlockFromNode(irBlock, blockNode);
//                 break;
//             case "module_block":
//                 generateModuleBlockFromNode(irBlock, blockNode);
//                 break;
//             case "annotation_block":
//                 generateAnnotationBlockFromNode(irBlock, blockNode);
//                 break;
//             default:
//                 irBlock.addInstruction(new IRInstruction(
//                     OpCode.NOP, line, sourceFileName,
//                     "unknown_block_type: " + blockType
//                 ));
//                 break;
//         }
//     }

//     // ==================== 利用语义上下文生成的示例 ====================

//     private void generateFunctionBlockFromNode(IRBasicBlock irBlock, BlockNode blockNode) {
//         String funcName = blockNode.getAttribute("name");
//         int line = blockNode.getStartLine();

//         // 从语义上下文获取函数签名
//         FunctionSignature sig = semanticCtx.resolveFunction(funcName);

//         IRInstruction funcDef = new IRInstruction(
//             OpCode.FUNC_DEF, line, sourceFileName, funcName
//         );
//         funcDef.setLabel(funcName);

//         if (sig != null) {
//             funcDef.setComment(sig.toString());

//             // 根据操作流类型生成流标记
//             switch (sig.getFlowType()) {
//                 case NORMAL:
//                     irBlock.addInstruction(new IRInstruction(
//                         OpCode.NORMAL_FLOW_BEGIN, line, sourceFileName, funcName
//                     ));
//                     break;
//                 case EXCEPTION:
//                     // 异常流：throws声明
//                     for (String throwsType : sig.getThrowsTypes()) {
//                         irBlock.addInstruction(new IRInstruction(
//                             OpCode.EXCEPTION_THROWS, line, sourceFileName, throwsType
//                         ));
//                     }
//                     break;
//                 case FLOW:
//                     irBlock.addInstruction(new IRInstruction(
//                         OpCode.FLOW_TO, line, sourceFileName, sig.getFlowTarget()
//                     ));
//                     break;
//             }
//         }

//         irBlock.addInstruction(funcDef);
//         irBlock.addInstruction(new IRInstruction(
//             OpCode.SCOPE_ENTER, line, sourceFileName, funcName
//         ));
//     }

//     private void generateVariableDeclarationBlockFromNode(IRBasicBlock irBlock, BlockNode blockNode) {
//         String varName = blockNode.getAttribute("var_name");
//         String varTypeName = blockNode.getAttribute("var_type");
//         String initValue = blockNode.getAttribute("init_value");
//         boolean isConst = "true".equals(blockNode.getAttribute("is_const"));
//         int line = blockNode.getStartLine();

//         // 从语义上下文获取精确类型信息
//         TypeInfo typeInfo = semanticCtx.resolveType(varTypeName);
//         String resolvedType = (typeInfo != null) ? typeInfo.getTypeName() : varTypeName;

//         irBlock.addInstruction(new IRInstruction(
//             OpCode.ALLOC, line, sourceFileName, varName, resolvedType
//         ));

//         if (initValue != null) {
//             irBlock.addInstruction(new IRInstruction(
//                 OpCode.LOAD_CONST, line, sourceFileName, initValue
//             ));
//             IRInstruction storeInst = new IRInstruction(
//                 OpCode.STORE_VAR, line, sourceFileName, varName
//             );
//             storeInst.setComment((isConst ? "const " : "var ") + varName + " : " + resolvedType);
//             irBlock.addInstruction(storeInst);
//         }
//     }

//     private void generateAssignmentBlockFromNode(IRBasicBlock irBlock, BlockNode blockNode) {
//         String target = blockNode.getAttribute("assignment_target");
//         String value = blockNode.getAttribute("assignment_value");
//         int line = blockNode.getStartLine();

//         // 检查注解结果中是否有属性监听
//         boolean hasBeforeHook = annotationResult != null &&
//             annotationResult.hasBeforePropsFor(target);
//         boolean hasAfterHook = annotationResult != null &&
//             annotationResult.hasAfterPropsFor(target);

//         if (hasBeforeHook) {
//             IRInstruction hook = new IRInstruction(
//                 OpCode.BEFORE_PROPS_HOOK, line, sourceFileName, target
//             );
//             hook.setComment("@BeforeProps trigger");
//             irBlock.addInstruction(hook);
//         }

//         irBlock.addInstruction(new IRInstruction(
//             OpCode.LOAD_VAR, line, sourceFileName, value
//         ));

//         if (hasBeforeHook || hasAfterHook) {
//             irBlock.addInstruction(new IRInstruction(
//                 OpCode.PROP_SET, line, sourceFileName, target
//             ));
//         } else {
//             irBlock.addInstruction(new IRInstruction(
//                 OpCode.STORE_VAR, line, sourceFileName, target
//             ));
//         }

//         if (hasAfterHook) {
//             IRInstruction hook = new IRInstruction(
//                 OpCode.AFTER_PROPS_HOOK, line, sourceFileName, target
//             );
//             hook.setComment("@AfterProps trigger");
//             irBlock.addInstruction(hook);
//         }
//     }

//     // ... 其余15种块类型的 *FromNode 方法结构类似，此处省略以控制篇幅 ...
//     // （generateParameterBlockFromNode, generateReturnBlockFromNode,
//     //  generateControlFlowBlockFromNode, generateConditionBlockFromNode, 
//     //  generateLoopBodyBlockFromNode, generateExpressionBlockFromNode,
//     //  generateFunctionCallBlockFromNode, generateArrayBlockFromNode,
//     //  generateImportDeclarationBlockFromNode, generateScopeBlockFromNode,
//     //  generateTypeInnerBlockFromNode, generateTypeDefinitionBlockFromNode,
//     //  generateModuleBlockFromNode, generateAnnotationBlockFromNode）

//     // ==================== 注解处理 ====================

//     private void processSystemAnnotations() {
//         if (annotationResult == null) return;

//         for (Map.Entry<String, String> entry : annotationResult.getSystemAnnotations().entrySet()) {
//             currentProgram.addMetadata(entry.getKey(), entry.getValue());
//         }
//     }

//     private void processProgramAnnotations() {
//         if (annotationResult == null) return;

//         for (AnnotationResult.ProgramAnnotation pa : annotationResult.getProgramAnnotations()) {
//             switch (pa.getType()) {
//                 case "BeforeName":
//                     currentProgram.addHookAnnotation(new HookAnnotation(
//                         HookAnnotation.HookType.BEFORE_NAME,
//                         pa.getMethodName(), pa.getTarget()
//                     ));
//                     break;
//                 case "AfterName":
//                     currentProgram.addHookAnnotation(new HookAnnotation(
//                         HookAnnotation.HookType.AFTER_NAME,
//                         pa.getMethodName(), pa.getTarget()
//                     ));
//                     break;
//                 case "BeforeProps":
//                     currentProgram.addHookAnnotation(new HookAnnotation(
//                         HookAnnotation.HookType.BEFORE_PROPS,
//                         pa.getProperties()
//                     ));
//                     break;
//                 case "AfterProps":
//                     currentProgram.addHookAnnotation(new HookAnnotation(
//                         HookAnnotation.HookType.AFTER_PROPS,
//                         pa.getProperties()
//                     ));
//                     break;
//             }
//         }
//     }

//     // ==================== 辅助方法 ====================

//     private String generateTempVar() {
//         return "_t" + (tempVarCounter++);
//     }

//     private OpCode mapOperatorToOpCode(String operator) {
//         switch (operator) {
//             case "+": return OpCode.ADD;
//             case "-": return OpCode.SUB;
//             case "*": return OpCode.MUL;
//             case "/": return OpCode.DIV;
//             case "%": return OpCode.MOD;
//             case "==": return OpCode.CMP_EQ;
//             case "!=": return OpCode.CMP_NE;
//             case "<": return OpCode.CMP_LT;
//             case ">": return OpCode.CMP_GT;
//             case "<=": return OpCode.CMP_LE;
//             case ">=": return OpCode.CMP_GE;
//             case "&&": return OpCode.AND;
//             case "||": return OpCode.OR;
//             case "!": return OpCode.NOT;
//             default: return OpCode.NOP;
//         }
//     }

//     private List<String> parsePropertyList(String propsStr) {
//         if (propsStr == null || propsStr.isEmpty()) return Collections.emptyList();
//         return Arrays.stream(propsStr.split(","))
//             .map(String::trim)
//             .collect(Collectors.toList());
//     }
// }
