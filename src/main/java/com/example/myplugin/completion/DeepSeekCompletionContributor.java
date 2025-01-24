package com.example.myplugin.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.lang.Language;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiComment;
import org.jetbrains.annotations.NotNull;

public class DeepSeekCompletionContributor extends CompletionContributor {
    public DeepSeekCompletionContributor() {
        // 为所有语言添加补全支持
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(),
                new DeepSeekCompletionProvider());
    }

    @Override
    public boolean invokeAutoPopup(@NotNull PsiElement position, char typeChar) {
        Language language = position.getLanguage();
        
        // 根据不同语言定义触发字符
        if (isCommentStart(typeChar, language) ||
            isDocStart(typeChar, language) ||
            isMethodTrigger(typeChar, language)) {
            return true;
        }
        
        return super.invokeAutoPopup(position, typeChar);
    }
    
    private boolean isCommentStart(char c, Language language) {
        String id = language.getID().toLowerCase();
        switch (id) {
            case "java":
            case "kotlin":
            case "javascript":
            case "typescript":
            case "python":
                return c == '/' || c == '#';
            case "html":
            case "xml":
                return c == '<' || c == '!';
            default:
                return c == '/' || c == '#' || c == '-';
        }
    }
    
    private boolean isDocStart(char c, Language language) {
        String id = language.getID().toLowerCase();
        switch (id) {
            case "java":
            case "kotlin":
            case "javascript":
            case "typescript":
                return c == '*' || c == '/';
            case "python":
                return c == '"' || c == '\'';
            case "html":
            case "xml":
                return c == '<' || c == '!';
            default:
                return c == '*' || c == '"';
        }
    }
    
    private boolean isMethodTrigger(char c, Language language) {
        String id = language.getID().toLowerCase();
        switch (id) {
            case "python":
                return c == '.';
            case "html":
            case "xml":
                return c == '<' || c == '/';
            default:
                return c == '.' || c == ':';
        }
    }
} 