package com.example.myplugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;

public class HelloAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Messages.showMessageDialog(
            e.getProject(),
            "你好，这是我的第一个 JetBrains 插件！",
            "问候",
            Messages.getInformationIcon()
        );
    }
} 