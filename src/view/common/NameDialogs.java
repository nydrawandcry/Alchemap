package view.common;

import model.potions.TextValidation;

import javax.swing.*;
import java.awt.Component;

/**
 * Общий ввод названий опытов и рецептов
 */
public final class NameDialogs {
    private NameDialogs() {}

    public static String askName(
            Component parent,
            String title,
            String initial
    ) {
        String value = initial;

        while (true) {
            value = (String) JOptionPane.showInputDialog(
                    parent,
                    "Введите название (до 160 символов):",
                    title,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    value
            );

            if (value == null) return null;

            try {
                return TextValidation.required(value, 160, title);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(
                        parent,
                        ex.getMessage(),
                        title,
                        JOptionPane.WARNING_MESSAGE
                );
            }
        }
    }
}