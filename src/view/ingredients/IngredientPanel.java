package view.ingredients;

import data.AlchemyRepository;
import model.Ingredient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static view.common.UiTheme.*;

/**
 * Каталог ингредиентов, их создание и выбор количества
 */
public final class IngredientPanel extends JPanel {
    private final AlchemyRepository repository;
    private final IngredientAddListener listener;

    private final List<Ingredient> ingredients;
    private final JList<Ingredient> ingredientList;

    private final JSpinner quantitySpinner = new JSpinner(
            new SpinnerNumberModel(1.0, 0.5, 20.0, 0.5)
    );

    private final JTextArea ingredientDescription =
            new JTextArea();

    private final JButton addButton =
            primaryButton("Добавить");

    public IngredientPanel(
            AlchemyRepository repository,
            List<Ingredient> ingredients,
            IngredientAddListener listener
    ) {
        super(new BorderLayout(8, 8));

        this.repository = repository;
        this.listener = Objects.requireNonNull(listener);
        this.ingredients = new ArrayList<>(ingredients);

        ingredientList = new JList<>(
                this.ingredients.toArray(Ingredient[]::new)
        );

        setOpaque(false);

        JButton createButton =
                primaryButton("Новый ингредиент");
        createButton.addActionListener(e -> createIngredient());
        add(createButton, BorderLayout.NORTH);

        ingredientList.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        ingredientList.setCellRenderer(new IngredientRenderer());

        ingredientList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateIngredientDescription();
            }
        });

        add(
                new JScrollPane(ingredientList),
                BorderLayout.CENTER
        );

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(
                new BoxLayout(bottom, BoxLayout.Y_AXIS)
        );

        ingredientDescription.setEditable(false);
        ingredientDescription.setLineWrap(true);
        ingredientDescription.setWrapStyleWord(true);
        ingredientDescription.setRows(4);
        ingredientDescription.setForeground(MUTED);
        ingredientDescription.setBorder(
                new EmptyBorder(8, 8, 8, 8)
        );

        bottom.add(ingredientDescription);
        bottom.add(Box.createVerticalStrut(8));

        JPanel addRow = new JPanel(new BorderLayout(6, 0));
        addRow.setOpaque(false);

        JLabel quantityLabel = new JLabel("Количество:");
        quantityLabel.setForeground(TEXT);

        addRow.add(quantityLabel, BorderLayout.WEST);
        addRow.add(quantitySpinner, BorderLayout.CENTER);

        addButton.addActionListener(
                e -> requestIngredientAddition()
        );

        addRow.add(addButton, BorderLayout.EAST);
        bottom.add(addRow);

        add(bottom, BorderLayout.SOUTH);

        if (!this.ingredients.isEmpty()) {
            ingredientList.setSelectedIndex(0);
        }

        updateIngredientDescription();
    }

    /**
     * Обновляет описание выбранного ингредиента
     */
    private void updateIngredientDescription() {
        Ingredient selected = ingredientList.getSelectedValue();
        addButton.setEnabled(selected != null);

        if (selected == null) {
            ingredientDescription.setText("");
            return;
        }

        ingredientDescription.setText(
                selected.category() + "\n"
                        + selected.description()
                        + "\nЛечение " + selected.healing()
                        + " · Токс. " + selected.toxicity()
                        + " · Магия " + selected.magic()
        );
    }

    /**
     * Передаёт ингредиент и подтверждённое количество слушателю
     */
    private void requestIngredientAddition() {
        Ingredient selected = ingredientList.getSelectedValue();
        if (selected == null) return;

        try {
            quantitySpinner.commitEdit();
        } catch (ParseException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Введите количество от 0,5 до 20.",
                    "Количество ингредиента",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        double quantity =
                ((Number) quantitySpinner.getValue()).doubleValue();

        listener.ingredientAddRequested(selected, quantity);
    }

    /**
     * Открывает форму и добавляет сохранённый ингредиент в каталог
     */
    private void createIngredient() {
        Window window = SwingUtilities.getWindowAncestor(this);
        Frame owner = window instanceof Frame frame ? frame : null;

        Ingredient created =
                new IngredientDialog(owner, repository).showDialog();

        if (created == null) return;

        ingredients.add(created);

        ingredients.sort(
                Comparator.comparing(
                        Ingredient::name,
                        String.CASE_INSENSITIVE_ORDER
                )
        );

        ingredientList.setListData(
                ingredients.toArray(Ingredient[]::new)
        );
        ingredientList.setSelectedValue(created, true);

        updateIngredientDescription();
    }

    private static final class IngredientRenderer
            extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean selected,
                boolean focus
        ) {
            JLabel label =
                    (JLabel) super.getListCellRendererComponent(
                            list, value, index, selected, focus
                    );

            if (value instanceof Ingredient ingredient) {
                label.setText(
                        "<html><b>"
                                + escape(ingredient.name())
                                + "</b><br><span style='color:#bfb4cc'>"
                                + escape(ingredient.category())
                                + "</span></html>"
                );

                label.setBorder(
                        new EmptyBorder(7, 7, 7, 7)
                );
            }

            return label;
        }
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}