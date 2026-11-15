package view.mixture;

import model.AlchemyResult;
import model.MixtureSnapshot;
import view.common.SectionCard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Objects;

import static view.common.UiTheme.*;

/**
 * Отображает текущую смесь и результат завершённого опыта
 */
public final class MixturePanel extends JPanel {
    private final JLabel stateValue = valueLabel();
    private final JLabel temperatureValue = valueLabel();
    private final JLabel stabilityValue = valueLabel();
    private final JLabel colorValue = valueLabel();
    private final JLabel healingValue = valueLabel();
    private final JLabel toxicityValue = valueLabel();
    private final JLabel fireValue = valueLabel();
    private final JLabel frostValue = valueLabel();
    private final JLabel magicValue = valueLabel();
    private final JLabel volatilityValue = valueLabel();

    private final JTextArea resultTitle = new JTextArea();
    private final JLabel resultType = new JLabel();
    private final JTextArea resultDescription = new JTextArea();

    private final JButton saveRecipeButton =
            new JButton("Сохранить как рецепт");

    public MixturePanel(RecipeSaveListener listener) {
        Objects.requireNonNull(listener);

        setOpaque(false);
        setLayout(new BorderLayout(0, scale(12)));

        add(buildMixtureCard(), BorderLayout.NORTH);
        add(buildResultCard(), BorderLayout.CENTER);

        saveRecipeButton.addActionListener(
                e -> listener.recipeSaveRequested()
        );

        showPending(true);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        // Длина названия и описания результата не меняет ширину колонок.
        size.width = scale(340);
        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(scale(300), scale(540));
    }

    private JPanel buildMixtureCard() {
        SectionCard card = card("Текущая смесь");
        JPanel state = card.body();
        state.setLayout(new GridLayout(10, 2, scale(6), scale(6)));

        addMetric(state, "Состояние", stateValue);
        addMetric(state, "Температура", temperatureValue);
        addMetric(state, "Цвет", colorValue);
        addMetric(state, "Стабильность", stabilityValue);
        addMetric(state, "Лечение", healingValue);
        addMetric(state, "Токсичность", toxicityValue);
        addMetric(state, "Огонь", fireValue);
        addMetric(state, "Холод", frostValue);
        addMetric(state, "Магия", magicValue);
        addMetric(state, "Летучесть", volatilityValue);

        return card;
    }

    private JPanel buildResultCard() {
        SectionCard card = card("Результат");
        JPanel result = card.body();
        result.setLayout(new BorderLayout(scale(6), scale(10)));

        resultTitle.setEditable(false);
        resultTitle.setLineWrap(true);
        resultTitle.setWrapStyleWord(true);
        resultTitle.setColumns(20);
        resultTitle.setOpaque(false);
        resultTitle.setBorder(null);
        resultTitle.setFont(resultTitle.getFont().deriveFont(Font.BOLD, (float) scale(18)));
        resultTitle.setForeground(new Color(220, 196, 255));

        JPanel heading = new JPanel(new BorderLayout(0, scale(6)));
        heading.setOpaque(false);
        heading.add(resultTitle, BorderLayout.CENTER);
        resultType.setForeground(MUTED);
        heading.add(resultType, BorderLayout.SOUTH);
        result.add(heading, BorderLayout.NORTH);

        resultDescription.setEditable(false);
        resultDescription.setLineWrap(true);
        resultDescription.setWrapStyleWord(true);
        resultDescription.setRows(7);
        resultDescription.setColumns(24);
        resultDescription.setForeground(MUTED);
        resultDescription.setBorder(
                new EmptyBorder(8, 8, 8, 8)
        );

        JScrollPane descriptionScroll = new JScrollPane(resultDescription);
        descriptionScroll.setBorder(null);
        result.add(descriptionScroll, BorderLayout.CENTER);
        result.add(saveRecipeButton, BorderLayout.SOUTH);

        return card;
    }

    /**
     * Показывает характеристики, вычисленные движком
     */
    public void showMixture(MixtureSnapshot snapshot) {
        stateValue.setText(snapshot.state());
        temperatureValue.setText(snapshot.temperature());
        colorValue.setText(snapshot.color());

        stabilityValue.setText(format(snapshot.stability()));
        healingValue.setText(format(snapshot.healing()));
        toxicityValue.setText(format(snapshot.toxicity()));
        fireValue.setText(format(snapshot.fire()));
        frostValue.setText(format(snapshot.frost()));
        magicValue.setText(format(snapshot.magic()));
        volatilityValue.setText(format(snapshot.volatility()));
    }

    /**
     * Сбрасывает прежний итог при изменении или очистке цепочки
     */
    public void showPending(boolean empty) {
        saveRecipeButton.setEnabled(false);
        resultType.setVisible(false);

        resultTitle.setText(
                empty
                        ? "Эксперимент ещё не завершён"
                        : "Эксперимент продолжается"
        );

        resultDescription.setText(
                empty
                        ? "Соберите цепочку ингредиентов и действий, "
                          + "затем нажмите «Завершить эксперимент»."
                        : "Цепочка изменилась. "
                          + "Завершите эксперимент повторно."
        );
    }

    /**
     * Показывает сохранённый результат и разрешает сохранение рецепта
     */
    public void showResult(AlchemyResult result) {
        MixtureSnapshot snapshot = result.snapshot();

        showMixture(snapshot);

        resultTitle.setText(result.name());
        resultType.setText(result.type());
        resultType.setToolTipText(result.type());
        resultType.setVisible(true);

        resultDescription.setText(
                result.description()
                        + "\n\nИтог: стабильность "
                        + format(snapshot.stability())
                        + ", токсичность "
                        + format(snapshot.toxicity())
                        + ", магия "
                        + format(snapshot.magic())
                        + "."
        );

        saveRecipeButton.setEnabled(true);
    }

    /**
     * Отключает повторное сохранение уже отмеченного рецепта
     */
    public void markRecipeSaved() {
        saveRecipeButton.setEnabled(false);
    }

    private static JLabel valueLabel() {
        JLabel label = new JLabel("—");
        label.setForeground(new Color(222, 208, 240));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }

    private static void addMetric(
            JPanel panel,
            String key,
            JLabel value
    ) {
        JLabel label = new JLabel(key);
        label.setForeground(MUTED);

        panel.add(label);
        panel.add(value);
    }

    private static String format(double value) {
        return String.format("%.1f", value);
    }
}
