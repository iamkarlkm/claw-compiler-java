import java.util.ArrayList;
import java.util.List;

import claw.compiler.generators.ffi.FFIBindingTable;
import claw.compiler.generators.ffi.platform.PlatformLibraryMapper;
import claw.compiler.generators.ffi.platform.TargetTriple;

/**
 * FFI 编译管道 — 将跨平台支持整合到完整编译流程中
 */
public class FFICompilationPipeline {

    private final TargetTriple target;

    public FFICompilationPipeline(TargetTriple target) {
        this.target = target;
    }

    /**
     * 完整的 FFI 处理流程
     *
     * 1. ExternProcessor 解析所有 extern 块 → FFIBindingTable (完整)
     * 2. 平台过滤 → FFIBindingTable (目标子集)
     * 3. 版本校验 → 确认库版本满足要求
     * 4. 类型映射解析 → 解析自定义 @c / @python / @java 映射
     * 5. 代码生成 → 调用对应的 Generator
     */
    public FFIGenerationResult process(FFIBindingTable fullTable) {
        FFIGenerationResult result = new FFIGenerationResult();

        // Step 1: 平台过滤
        FFIBindingTable filteredTable = fullTable.filterForPlatform(target);

        if (!filteredTable.hasExternDeclarations()) {
            result.hasFFI = false;
            return result;
        }

        result.hasFFI = true;
        result.filteredTable = filteredTable;

        // Step 2: 版本校验
        List<String> versionWarnings = checkVersionConstraints(filteredTable);
        result.warnings.addAll(versionWarnings);

        // Step 3: 按目标语言生成代码
        // 这里根据编译器配置决定生成哪种目标代码
        // 通常一次编译只会选一个目标

        result.linkFlags = generateLinkFlags(filteredTable);
        result.linkLibraries = filteredTable.getLibraryNames();

        return result;
    }

    /**
     * 生成链接参数
     */
    private String generateLinkFlags(FFIBindingTable table) {
        StringBuilder sb = new StringBuilder();

        for (FFIBindingTable.LinkDirective link : table.getAllLinks()) {
            // 获取平台映射后的库名
            String mappedName = PlatformLibraryMapper.mapLibraryName(
                link.libraryName, target);

            if (mappedName == null) {
                // 该平台不需要此库
                continue;
            }

            if (target.isWindows()) {
                sb.append(mappedName).append(".lib ");
            } else {
                sb.append("-l").append(mappedName).append(" ");
            }

            // 添加隐含库
            for (String implied : PlatformLibraryMapper.getImpliedLibraries(
                    link.libraryName, target)) {
                if (target.isWindows()) {
                    sb.append(implied).append(".lib ");
                } else {
                    sb.append("-l").append(implied).append(" ");
                }
            }
        }

        return sb.toString().trim();
    }

    /**
     * 检查版本约束
     */
    private List<String> checkVersionConstraints(FFIBindingTable table) {
        List<String> warnings = new ArrayList<>();
        // 这里可以通过 pkg-config 或其他机制检查已安装库的版本
        // 简化实现只输出警告
        return warnings;
    }

    /**
     * FFI 生成结果
     */
    public static class FFIGenerationResult {
        public boolean hasFFI = false;
        public FFIBindingTable filteredTable;
        public String linkFlags = "";
        public List<String> linkLibraries = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        public List<String> errors = new ArrayList<>();
    }
}
