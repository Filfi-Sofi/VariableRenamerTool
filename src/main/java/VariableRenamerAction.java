import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

public class VariableRenamerAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Editor editor = event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);
        Project project = event.getProject();

        if (editor == null || project == null) {
            return;
        }

        Document document = editor.getDocument();
        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();

        // Проверка на пустое выделение или недопустимые символы
        if (selectedText != null && isValidVariableName(selectedText)) {
            String renamedText = toSnakeCase(selectedText);

            // Запускаем замену во всём документе
            replaceVariableInDocument(document, selectedText, renamedText, project);
        }
    }

    /**
     * Проверка, что выделенный текст является допустимым именем переменной.
     */
    private boolean isValidVariableName(String text) {
        return text.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }

    /**
     * Преобразование строки в snake_case.
     * Пример:
     * - someVariableName -> some_variable_name
     * - SomeVariable -> some_variable
     */
    private String toSnakeCase(String text) {
        String result = text.replaceAll("([a-z])([A-Z])", "$1_$2") // Разделяем маленькую букву перед заглавной
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2") // Учитываем аббревиатуры
                .toLowerCase(); // Переводим все символы в нижний регистр
        return result;
    }

    /**
     * Замена всех вхождений переменной в документе.
     */
    private void replaceVariableInDocument(Document document, String oldName, String newName, Project project) {
        final String[] documentText = {document.getText()};
        final int[] startOffset = {0};

        WriteCommandAction.runWriteCommandAction(project, () -> {
            while ((startOffset[0] = documentText[0].indexOf(oldName, startOffset[0])) != -1) {
                int endOffset = startOffset[0] + oldName.length();

                // Убедимся, что найденное имя - это отдельное слово (не часть другого слова)
                if (isWordBoundary(documentText[0], startOffset[0], endOffset)) {
                    document.replaceString(startOffset[0], endOffset, newName);
                    documentText[0] = document.getText(); // Обновляем текст документа
                }
                startOffset[0] += newName.length(); // Продолжаем поиск после замены
            }
        });
    }

    /**
     * Проверка границ слова для корректной замены переменной.
     */
    private boolean isWordBoundary(String text, int start, int end) {
        boolean isStartBoundary = (start == 0) || !Character.isJavaIdentifierPart(text.charAt(start - 1));
        boolean isEndBoundary = (end >= text.length()) || !Character.isJavaIdentifierPart(text.charAt(end));
        return isStartBoundary && isEndBoundary;
    }
}