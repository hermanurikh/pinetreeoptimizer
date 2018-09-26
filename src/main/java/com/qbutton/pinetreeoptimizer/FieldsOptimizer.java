package com.qbutton.pinetreeoptimizer;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FieldsOptimizer extends AnAction {

    private static final String PINETREEFICATION_SUCCESS = "Fields pinetreefized";
    private static final String NO_PINETREEFICATION_NEEDED = "No pinetreefication needed";

    @Override
    public void actionPerformed(AnActionEvent e) {
        //Get all the required data from data keys
        final Project project = e.getProject();
        final Editor editor = e.getData(PlatformDataKeys.EDITOR);

        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);

        if (psiFile != null && editor != null) {
            PsiElement[] children = psiFile.getChildren();

            Optional<PsiClass> topLevelClassOptional = toPsiClassesStream(children)
                    .findAny();

            boolean pinetreefied[] = {false};
            topLevelClassOptional.ifPresent(topLevelClass -> {

                List<PsiClass> allClasses = getAllPageClasses(topLevelClass);

                allClasses.forEach(psiClass -> {
                    PsiField[] initialFields = psiClass.getFields();
                    PsiField[] copy = Arrays.copyOf(initialFields, initialFields.length);

                    Arrays.sort(initialFields, stringLengthComparator());

                    if (pineTreeficationNeeded(initialFields, copy)) {
                        WriteCommandAction.runWriteCommandAction(project, (Computable<PsiElement>) () -> {
                            Arrays.stream(initialFields)
                                    .forEach(psiField -> psiClass.add(psiField.copy()));
                            Arrays.stream(initialFields).forEach(PsiElement::delete);
                            return null;
                        });

                        pinetreefied[0] = true;
                    }
                });
            });

            showResults(project, editor, pinetreefied[0]);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        final Project project = e.getProject();
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        //Set visibility only in case of existing project, editor and top-level class
        if (project != null && editor != null) {
            PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
            if (psiFile != null && Arrays.stream(psiFile.getChildren()).anyMatch(c -> c instanceof PsiClass)) {
                e.getPresentation().setEnabled(true);
                return;
            }
        }
        e.getPresentation().setEnabled(false);
    }

    private boolean pineTreeficationNeeded(PsiField[] initialFields, PsiField[] copy) {

        return !Arrays.equals(initialFields, copy);
    }

    private Comparator<PsiField> stringLengthComparator() {
        return (a, b) ->
                a.getTextLength() == b.getTextLength()
                        ? a.getText().compareTo(b.getText())
                        : a.getTextLength() - b.getTextLength();
    }

    @NotNull
    private List<PsiClass> getAllPageClasses(PsiClass topLevelClass) {
        List<PsiClass> allClasses = new ArrayList<>();
        allClasses.add(topLevelClass);

        findChildrenClasses(topLevelClass, allClasses);
        return allClasses;
    }

    private void findChildrenClasses(PsiClass psiClass, List<PsiClass> existing) {
        List<PsiClass> children = toPsiClassesStream(psiClass.getChildren())
                .collect(Collectors.toList());

        if (!children.isEmpty()) {
            existing.addAll(children);
            children.forEach(c -> findChildrenClasses(c, existing));
        }
    }

    private void showResults(Project project, Editor editor, boolean b) {
        WriteCommandAction.runWriteCommandAction(project, (Computable<PsiElement>) () -> {
            HintManager.getInstance().showInformationHint(editor, b
                    ? PINETREEFICATION_SUCCESS
                    : NO_PINETREEFICATION_NEEDED);
            return null;
        });
    }

    @NotNull
    private Stream<PsiClass> toPsiClassesStream(PsiElement[] children) {
        return Arrays.stream(children)
                .filter(c -> c instanceof PsiClass)
                .map(c -> (PsiClass) c);
    }
}
