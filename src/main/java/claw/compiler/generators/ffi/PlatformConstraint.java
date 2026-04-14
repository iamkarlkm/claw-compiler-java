
package claw.compiler.generators.ffi;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

/**
 * 平台约束接口
 */
public class PlatformConstraint {

    public Iterable<String> getPlatforms() {
        return Collections.emptyList();
    }

    public Iterable<String> getArchitectures() {
        return Collections.emptyList();
    }
}
