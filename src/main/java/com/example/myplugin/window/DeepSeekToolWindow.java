package com.example.myplugin.window;

import com.example.myplugin.api.DeepSeekClient;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DeepSeekToolWindow implements ToolWindowFactory {
    private JTextPane chatArea;
    private JTextArea inputArea;
    private DeepSeekClient client;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        client = new DeepSeekClient("your-api-key"); // 请替换为实际的 API key
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 聊天显示区域
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setContentType("text/html");
        JBScrollPane chatScroll = new JBScrollPane(chatArea);
        
        // 输入区域
        inputArea = new JTextArea(3, 40);
        inputArea.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isShiftDown()) {
                    e.consume();
                    sendMessage();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {}
        });
        JBScrollPane inputScroll = new JBScrollPane(inputArea);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton sendButton = new JButton("发送");
        sendButton.addActionListener(e -> sendMessage());
        
        JButton clearButton = new JButton("清除历史");
        clearButton.addActionListener(e -> {
            client.clearHistory();
            chatArea.setText("");
            inputArea.setText("");
        });
        
        buttonPanel.add(sendButton);
        buttonPanel.add(clearButton);
        
        // 底部面板
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputScroll, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // 设置分隔面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chatScroll, bottomPanel);
        splitPane.setResizeWeight(0.8);
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        Content content = ContentFactory.getInstance().createContent(mainPanel, "", false);
        toolWindow.getContentManager().addContent(content);
        
        // 添加欢迎消息
        appendToChat("DeepSeek", "你好！我是 DeepSeek AI 助手，有什么我可以帮你的吗？");
    }
    
    private void sendMessage() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            return;
        }
        
        appendToChat("用户", input);
        inputArea.setText("");
        
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return client.sendChatMessage(input);
            }
            
            @Override
            protected void done() {
                try {
                    String response = get();
                    appendToChat("DeepSeek", response);
                } catch (Exception e) {
                    appendToChat("系统", "错误: " + e.getMessage());
                }
            }
        }.execute();
    }
    
    private void appendToChat(String sender, String message) {
        String time = timeFormat.format(new Date());
        String color = sender.equals("用户") ? "#4A90E2" : "#50C878";
        String html = String.format(
            "<div style='margin: 5px;'>" +
            "<span style='color: gray;'>[%s]</span> " +
            "<span style='color: %s;'><b>%s:</b></span><br/>" +
            "<div style='margin-left: 15px; margin-top: 5px; margin-bottom: 10px;'>%s</div>" +
            "</div>",
            time, color, sender, formatMessage(message)
        );
        
        chatArea.setText(chatArea.getText() + html);
        // 滚动到底部
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    private String formatMessage(String message) {
        // 转义 HTML 特殊字符
        message = message.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\n", "<br/>");
        
        // 处理代码块
        if (message.contains("```")) {
            message = message.replaceAll(
                "```([^`]+)```",
                "<pre style='background-color: #f5f5f5; padding: 10px; border-radius: 5px;'>$1</pre>"
            );
        }
        
        return message;
    }
} 