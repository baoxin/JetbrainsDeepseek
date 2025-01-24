package com.example.myplugin.completion;

import com.example.myplugin.api.DeepSeekClient;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.lang.Language;

public class DeepSeekCompletionProvider extends CompletionProvider<CompletionParameters> {
    private final DeepSeekClient client;

    public DeepSeekCompletionProvider() {
        this.client = new DeepSeekClient("your-api-key");
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                @NotNull ProcessingContext context,
                                @NotNull CompletionResultSet result) {
        Document document = parameters.getEditor().getDocument();
        PsiElement element = parameters.getPosition();
        Language language = element.getLanguage();
        int offset = parameters.getOffset();

        String contextCode = getContext(document, element, offset);
        
        try {
            if (isDocumentationContext(element, language)) {
                addDocumentationCompletions(element, result, contextCode, language);
            } else if (isCommentContext(element, language)) {
                addCommentCompletions(element, result, contextCode, language);
            } else {
                addCodeCompletions(element, result, contextCode, language);
            }
        } catch (Exception e) {
            // 错误处理
        }
    }

    private void addDocumentationCompletions(PsiElement element, CompletionResultSet result, 
                                           String context, Language language) throws IOException {
        String docStyle = getDocStyle(language);
        String prompt = String.format(
            "请为以下%s代码生成%s格式的文档注释。只返回文档注释内容：\n%s",
            language.getDisplayName(),
            docStyle,
            context
        );

        String response = client.sendMessage(prompt);
        addCompletionWithDocumentation(result, response, docStyle, true, language);
    }

    private String getDocStyle(Language language) {
        String id = language.getID().toLowerCase();
        switch (id) {
            case "java":
                return "JavaDoc";
            case "python":
                return "DocString";
            case "javascript":
            case "typescript":
                return "JSDoc";
            default:
                return "Documentation";
        }
    }

    private void addCompletionWithDocumentation(CompletionResultSet result, String content,
                                              String type, boolean isDoc, Language language) {
        String formattedContent = formatDocumentation(content, isDoc, language);
        result.addElement(LookupElementBuilder.create(formattedContent)
                .withTypeText(type)
                .withIcon(com.intellij.icons.AllIcons.Actions.Lightning)
                .withPresentableText(type + " 补全"));
    }

    private String formatDocumentation(String content, boolean isDoc, Language language) {
        String id = language.getID().toLowerCase();
        switch (id) {
            case "java":
            case "javascript":
            case "typescript":
                return isDoc ? 
                    String.format("/**\n * %s\n */", content.replace("\n", "\n * ")) :
                    String.format("// %s", content.replace("\n", "\n// "));
            case "python":
                return isDoc ?
                    String.format("\"\"\"\n%s\n\"\"\"", content) :
                    String.format("# %s", content.replace("\n", "\n# "));
            case "html":
            case "xml":
                return isDoc ?
                    String.format("<!-- %s -->", content) :
                    String.format("<!-- %s -->", content);
            default:
                return isDoc ?
                    String.format("/* %s */", content) :
                    String.format("// %s", content.replace("\n", "\n// "));
        }
    }

    private void addCodeCompletions(PsiElement element, CompletionResultSet result,
                                  String context, Language language) throws IOException {
        String prompt = String.format(
            "请根据以下%s代码上下文提供代码补全建议。格式要求：\n" +
            "1. 每行一个建议\n" +
            "2. 建议应该包含完整的代码片段\n" +
            "3. 可以包含简短的说明（用||分隔）\n" +
            "上下文：\n%s",
            language.getDisplayName(),
            context
        );

        String response = client.sendMessage(prompt);
        for (String suggestion : response.split("\n")) {
            String[] parts = suggestion.split("\\|\\|");
            String code = parts[0].trim();
            String description = parts.length > 1 ? parts[1].trim() : "";
            
            if (!code.isEmpty()) {
                result.addElement(createLookupElement(code, description, language));
            }
        }
    }

    private LookupElement createLookupElement(String code, String description, Language language) {
        return LookupElementBuilder.create(code)
                .withTypeText(language.getDisplayName())
                .withIcon(com.intellij.icons.AllIcons.Actions.Lightning)
                .withTailText(" " + description, true)
                .withInsertHandler((context, item) -> {
                    Editor editor = context.getEditor();
                    int startOffset = context.getStartOffset();
                    int tailOffset = context.getTailOffset();
                    editor.getDocument().replaceString(startOffset, tailOffset, code);
                });
    }

    private boolean isDocumentationContext(PsiElement element, Language language) {
        String id = language.getID().toLowerCase();
        switch (id) {
            case "java":
                return element.getParent() instanceof PsiDocComment;
            case "python":
                return element.getText().startsWith("\"\"\"") || element.getText().startsWith("'''");
            case "javascript":
            case "typescript":
                return element.getText().startsWith("/**");
            case "html":
            case "xml":
                return element.getText().startsWith("<!--");
            default:
                return false;
        }
    }

    private boolean isCommentContext(PsiElement element, Language language) {
        String id = language.getID().toLowerCase();
        switch (id) {
            case "java":
            case "javascript":
            case "typescript":
                return element.getText().startsWith("//") || element.getText().startsWith("/*");
            case "python":
                return element.getText().startsWith("#");
            case "html":
            case "xml":
                return element.getText().startsWith("<!--");
            default:
                return element.getText().startsWith("//") || 
                       element.getText().startsWith("/*") ||
                       element.getText().startsWith("#");
        }
    }

    private String getContext(Document document, PsiElement element, int offset) {
        // 获取更多的上下文信息
        PsiElement context = element.getContext();
        if (context instanceof PsiMethod) {
            return getMethodContext((PsiMethod) context);
        } else if (context instanceof PsiClass) {
            return getClassContext((PsiClass) context);
        }

        // 默认获取周围的代码
        int lineNumber = document.getLineNumber(offset);
        int startLine = Math.max(0, lineNumber - 5);
        int endLine = Math.min(document.getLineCount(), lineNumber + 5);
        
        StringBuilder contextBuilder = new StringBuilder();
        for (int i = startLine; i < endLine; i++) {
            int lineStart = document.getLineStartOffset(i);
            int lineEnd = document.getLineEndOffset(i);
            contextBuilder.append(document.getText(new TextRange(lineStart, lineEnd))).append("\n");
        }
        return contextBuilder.toString();
    }

    private String getMethodContext(PsiMethod method) {
        StringBuilder context = new StringBuilder();
        context.append(method.getText()).append("\n");
        // 获取方法的参数、返回类型等信息
        return context.toString();
    }

    private String getClassContext(PsiClass clazz) {
        StringBuilder context = new StringBuilder();
        context.append(clazz.getText()).append("\n");
        // 获取类的字段、方法等信息
        return context.toString();
    }
} 