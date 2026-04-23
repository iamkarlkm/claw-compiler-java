package com.claw.lsp.idea.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.DesignedAdapter;

/**
 * 显示性能报告操作
 */
public class ShowPerformanceReportAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            Messages.showErrorDialog("No project open", "Error");
            return;
        }

        // TODO: 获取性能报告
        String performanceReport = "Performance Report:\n\n" +
            "- Code Completion: ~10ms (100 calls)\n" +
            "- Syntax Check: ~100ms (50 calls)\n" +
            "- Definition: ~10ms (100 calls)\n" +
            "- References: ~100ms (50 calls)";

        Messages.showInfoMessage(performanceReport, "Performance Report");
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(true);
    }
}
