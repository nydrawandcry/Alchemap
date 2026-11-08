package view;

import data.AlchemyRepository;
import model.Ingredient;
import model.NewIngredient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.ExecutionException;

/**
 * Форма создания ингредиента
 * */
public final class IngredientDialog extends JDialog {
    private final AlchemyRepository repository;

    private final JTextField name = new JTextField(28);
    private final JTextField category = new JTextField(28);
    private final JTextArea description = new JTextArea(3, 28);

    private final String[] statNames = {
            "Лечение", "Токсичность", "Огонь", "Холод",
            "Летучесть", "Магия", "Стабильность"
    };

    private final JTextField[] stats = new JTextField[7];

    private final JButton save =
            new JButton("Создать ингредиент");
    private final JButton cancel =
            new JButton("Отмена");
    private final JLabel status = new JLabel(" ");

    private boolean saving;
    private Ingredient created;

    public IngredientDialog(
            Frame owner,
            AlchemyRepository repository
    ) {
        super(owner, "Новый ингредиент", true);
        this.repository = repository;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel hint = new JLabel(
                "<html>* Обязательные поля.<br>"
                        + "Характеристики: 0–99999,99, "
                        + "до двух знаков после запятой.<br>"
                        + "Можно вводить точку или запятую.</html>"
        );
        root.add(hint, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());

        addRow(form, 0, "Название *", name);
        addRow(form, 1, "Категория *", category);

        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        addRow(
                form, 2, "Описание",
                new JScrollPane(description)
        );

        for (int i = 0; i < stats.length; i++) {
            stats[i] = new JTextField("0", 12);
            addRow(form, i + 3, statNames[i], stats[i]);
        }

        root.add(form, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.add(status, BorderLayout.NORTH);

        JPanel buttons = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );
        buttons.add(cancel);
        buttons.add(save);

        bottom.add(buttons, BorderLayout.SOUTH);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);

        save.addActionListener(e -> saveIngredient());
        cancel.addActionListener(e -> dispose());

        getRootPane().setDefaultButton(save);

        getRootPane().registerKeyboardAction(
                e -> {
                    if (!saving) dispose();
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /**
     * Возвращает созданный ингредиент либо null при отмене
     */
    public Ingredient showDialog() {
        setVisible(true);
        return created;
    }

    private void saveIngredient() {
        if (saving) return;

        final NewIngredient draft;

        try {
            double[] values = new double[stats.length];

            for (int i = 0; i < stats.length; i++) {
                try {
                    values[i] = Double.parseDouble(
                            stats[i].getText()
                                    .trim()
                                    .replace(',', '.')
                    );
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(
                            statNames[i]
                                    + ": введите число, например 2,5."
                    );
                }
            }

            draft = new NewIngredient(
                    name.getText(),
                    category.getText(),
                    description.getText(),
                    values[0],
                    values[1],
                    values[2],
                    values[3],
                    values[4],
                    values[5],
                    values[6]
            );
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }

        setSaving(true);

        new SwingWorker<Ingredient, Void>() {
            @Override
            protected Ingredient doInBackground() throws Exception {
                return repository.createIngredient(draft);
            }

            @Override
            protected void done() {
                setSaving(false);

                try {
                    created = get();
                    dispose();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();

                    showError(
                            "Сохранение прервано. Проверьте каталог "
                                    + "перед повторной попыткой."
                    );
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();

                    if (cause instanceof IllegalArgumentException) {
                        showError(cause.getMessage());
                    } else {
                        showError(
                                "Не удалось сохранить ингредиент. "
                                        + "Проверьте подключение к БД.\n"
                                        + "Если связь оборвалась, проверьте "
                                        + "каталог перед повторной попыткой.\n"
                                        + cause.getMessage()
                        );
                    }
                }
            }
        }.execute();
    }

    private void setSaving(boolean value) {
        saving = value;

        save.setEnabled(!value);
        cancel.setEnabled(!value);
        name.setEnabled(!value);
        category.setEnabled(!value);
        description.setEnabled(!value);

        for (JTextField field : stats) {
            field.setEnabled(!value);
        }

        setDefaultCloseOperation(
                value ? DO_NOTHING_ON_CLOSE : DISPOSE_ON_CLOSE
        );

        status.setText(value ? "Сохранение…" : " ");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Новый ингредиент",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private static void addRow(
            JPanel form,
            int row,
            String label,
            JComponent input
    ) {
        GridBagConstraints c = new GridBagConstraints();

        c.gridy = row;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 4, 4, 10);

        form.add(new JLabel(label), c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        form.add(input, c);
    }
}