package view.common;

import model.potions.TextValidation;

import javax.swing.*;
import java.awt.Component;
import java.awt.event.KeyEvent;

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
        JTextField input = new JTextField(28);
        InputFilters.limit(input, 160);
        input.setText(initial == null ? "" : initial);

        JButton ok = new JButton("ОК");
        JButton cancel = new JButton("Отмена");

        InputFilters.onChange(
                input,
                () -> ok.setEnabled(
                        !input.getText().trim().isEmpty()
                )
        );

        JOptionPane pane = new JOptionPane(
                new Object[]{
                        "Введите название (до 160 символов):",
                        input
                },
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                new Object[]{ok, cancel},
                ok
        );

        JDialog dialog = pane.createDialog(parent, title);

        ok.addActionListener(
                e -> pane.setValue(JOptionPane.OK_OPTION)
        );

        cancel.addActionListener(
                e -> pane.setValue(JOptionPane.CANCEL_OPTION)
        );

        dialog.getRootPane().setDefaultButton(ok);

        dialog.getRootPane().registerKeyboardAction(
                e -> pane.setValue(JOptionPane.CANCEL_OPTION),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        SwingUtilities.invokeLater(() -> {
            input.requestFocusInWindow();
            input.selectAll();
        });

        try {
            dialog.setVisible(true);

            if (!Integer.valueOf(JOptionPane.OK_OPTION)
                    .equals(pane.getValue())) {
                return null;
            }

            return TextValidation.required(
                    input.getText(), 160, title
            );
        } finally {
            dialog.dispose();
        }
    }
}