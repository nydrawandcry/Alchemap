package view;

import model.potions.RecipeDetails;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.stream.IntStream;

/** Выпадающая карточка с исторически сохранёнными шагами рецепта. */
public final class RecipePopup extends JPopupMenu {
    public RecipePopup(RecipeDetails recipe) {
        setLayout(new BorderLayout(6, 8));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setBackground(new Color(38, 32, 48));
        JTextArea title = text(recipe.recipeName() + "\nОпыт: " + recipe.experimentName()
                + "\nРезультат: " + recipe.resultName() + " · " + recipe.resultType());
        title.setColumns(48);
        title.setRows(4);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        add(title, BorderLayout.NORTH);
        String[] lines = IntStream.range(0, recipe.steps().size())
                .mapToObj(i -> (i + 1) + ". " + recipe.steps().get(i)).toArray(String[]::new);
        if (lines.length == 0) lines = new String[]{"У этого опыта нет сохранённых шагов."};
        JList<String> steps = new JList<>(lines);
        steps.setBackground(new Color(25, 22, 32));
        steps.setForeground(new Color(238, 233, 244));
        steps.setFixedCellHeight(30);
        steps.setBorder(new EmptyBorder(4, 8, 4, 8));
        JScrollPane scroll = new JScrollPane(steps);
        scroll.setPreferredSize(new Dimension(540, Math.min(330, Math.max(65, lines.length * 30 + 12))));
        add(scroll, BorderLayout.CENTER);
        JTextArea description = text(recipe.resultDescription());
        description.setColumns(48);
        JScrollPane details = new JScrollPane(description);
        details.setPreferredSize(new Dimension(540, 85));
        details.setBorder(null);
        add(details, BorderLayout.SOUTH);
    }

    private JTextArea text(String value) {
        JTextArea area = new JTextArea(value);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(getBackground());
        area.setForeground(new Color(238, 233, 244));
        return area;
    }
}
