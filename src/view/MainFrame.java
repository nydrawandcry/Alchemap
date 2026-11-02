package view;

import data.AlchemyRepository;
import engine.AlchemyEngine;
import model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class MainFrame extends JFrame {
    private static final Color BG = new Color(25, 22, 32);
    private static final Color PANEL = new Color(38, 32, 48);
    private static final Color PANEL_2 = new Color(48, 39, 60);
    private static final Color ACCENT = new Color(153, 109, 210);
    private static final Color TEXT = new Color(238, 233, 244);
    private static final Color MUTED = new Color(190, 180, 204);

    private final AlchemyRepository repository;
    private final AlchemyEngine engine = new AlchemyEngine();
    private final List<Ingredient> ingredients;
    private final List<ExperimentStep> steps = new ArrayList<>();

    private final DefaultListModel<ExperimentStep> stepModel = new DefaultListModel<>();
    private final JList<ExperimentStep> stepList = new JList<>(stepModel);
    private final JList<Ingredient> ingredientList;
    private final JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.5, 20.0, 0.5));
    private final JTextArea ingredientDescription = new JTextArea();
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
    private final JLabel resultTitle = new JLabel("Эксперимент ещё не завершён");
    private final JTextArea resultDescription = new JTextArea("Соберите цепочку ингредиентов и действий, затем нажмите «Завершить эксперимент».");
    private final JButton saveRecipeButton = new JButton("Сохранить как рецепт");
    private final JTable historyTable = new JTable();
    private final HistoryTableModel historyModel = new HistoryTableModel();
    private final JLabel modeLabel;

    private long lastExperimentId = -1;
    private AlchemyResult lastResult;

    public MainFrame(AlchemyRepository repository, List<Ingredient> ingredients, String startupWarning) {
        super("Alchemap — алхимическая лаборатория");
        this.repository = repository;
        this.ingredients = ingredients;
        this.ingredientList = new JList<>(ingredients.toArray(Ingredient[]::new));
        this.modeLabel = new JLabel("Хранилище: " + repository.modeName());

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 760));
        setSize(1360, 820);
        setLocationRelativeTo(null);

        configureLook();
        setContentPane(buildRoot());
        bindEvents();
        updateIngredientDescription();
        updatePreview();
        refreshHistory();

        if (startupWarning != null && !startupWarning.isBlank()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    this, startupWarning, "Режим запуска", JOptionPane.INFORMATION_MESSAGE
            ));
        }
    }

    private void configureLook() {
        getContentPane().setBackground(BG);
        UIManager.put("Panel.background", PANEL);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Button.background", PANEL_2);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("List.background", PANEL_2);
        UIManager.put("List.foreground", TEXT);
        UIManager.put("Table.background", PANEL_2);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.gridColor", new Color(75, 65, 88));
        UIManager.put("TableHeader.background", PANEL);
        UIManager.put("TableHeader.foreground", TEXT);
        UIManager.put("TextArea.background", PANEL_2);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("Spinner.background", PANEL_2);
    }

    private JComponent buildRoot() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        root.add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Лаборатория", buildLaboratory());
        tabs.addTab("Журнал опытов", buildHistory());
        root.add(tabs, BorderLayout.CENTER);
        return root;
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        JLabel title = new JLabel("ALCHEMAP");
        title.setForeground(TEXT);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        JLabel sub = new JLabel("Интерактивная алхимическая лаборатория для D&D");
        sub.setForeground(MUTED);
        sub.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBackground(BG);
        text.add(title);
        text.add(sub);
        header.add(text, BorderLayout.WEST);
        modeLabel.setForeground(MUTED);
        header.add(modeLabel, BorderLayout.EAST);
        return header;
    }

    private JComponent buildLaboratory() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(BG);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 10);

        c.gridx = 0;
        c.weightx = 0.26;
        body.add(buildIngredientPanel(), c);

        c.gridx = 1;
        c.weightx = 0.44;
        body.add(buildExperimentPanel(), c);

        c.gridx = 2;
        c.weightx = 0.30;
        c.insets = new Insets(0, 0, 0, 0);
        body.add(buildStatePanel(), c);
        return body;
    }

    private JPanel buildIngredientPanel() {
        JPanel panel = card("Ингредиенты");
        panel.setLayout(new BorderLayout(8, 8));

        ingredientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ingredientList.setSelectedIndex(0);
        ingredientList.setCellRenderer(new IngredientRenderer());
        panel.add(new JScrollPane(ingredientList), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        ingredientDescription.setEditable(false);
        ingredientDescription.setLineWrap(true);
        ingredientDescription.setWrapStyleWord(true);
        ingredientDescription.setRows(4);
        ingredientDescription.setForeground(MUTED);
        ingredientDescription.setBorder(new EmptyBorder(8, 8, 8, 8));
        bottom.add(ingredientDescription);
        bottom.add(Box.createVerticalStrut(8));

        JPanel addRow = new JPanel(new BorderLayout(6, 0));
        addRow.setOpaque(false);
        JLabel qty = new JLabel("Количество:");
        qty.setForeground(TEXT);
        addRow.add(qty, BorderLayout.WEST);
        addRow.add(quantitySpinner, BorderLayout.CENTER);
        JButton addButton = primaryButton("Добавить");
        addButton.addActionListener(e -> addIngredient());
        addRow.add(addButton, BorderLayout.EAST);
        bottom.add(addRow);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildExperimentPanel() {
        JPanel panel = card("Рабочая область · цепочка эксперимента");
        panel.setLayout(new BorderLayout(8, 8));

        stepList.setCellRenderer(new StepRenderer());
        panel.add(new JScrollPane(stepList), BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));

        JLabel actionLabel = new JLabel("Действия с текущей смесью");
        actionLabel.setForeground(MUTED);
        south.add(actionLabel);
        south.add(Box.createVerticalStrut(6));

        JPanel actions = new JPanel(new GridLayout(3, 3, 6, 6));
        actions.setOpaque(false);
        for (AlchemyAction action : AlchemyAction.values()) {
            JButton b = new JButton(action.label());
            b.setToolTipText(action.description());
            b.addActionListener(e -> addAction(action));
            actions.add(b);
        }
        south.add(actions);
        south.add(Box.createVerticalStrut(8));

        JPanel controls = new JPanel(new GridLayout(1, 3, 6, 0));
        controls.setOpaque(false);
        JButton undo = new JButton("Удалить последний шаг");
        undo.addActionListener(e -> undoStep());
        JButton clear = new JButton("Новый эксперимент");
        clear.addActionListener(e -> clearExperiment());
        JButton finish = primaryButton("Завершить эксперимент");
        finish.addActionListener(e -> finishExperiment());
        controls.add(undo);
        controls.add(clear);
        controls.add(finish);
        south.add(controls);

        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildStatePanel() {
        JPanel outer = new JPanel();
        outer.setOpaque(false);
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));

        JPanel state = card("Текущая смесь");
        state.setLayout(new GridLayout(10, 2, 6, 7));
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
        state.setMaximumSize(new Dimension(Integer.MAX_VALUE, 310));
        outer.add(state);
        outer.add(Box.createVerticalStrut(10));

        JPanel result = card("Результат");
        result.setLayout(new BorderLayout(6, 8));
        resultTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        resultTitle.setForeground(new Color(220, 196, 255));
        result.add(resultTitle, BorderLayout.NORTH);
        resultDescription.setEditable(false);
        resultDescription.setLineWrap(true);
        resultDescription.setWrapStyleWord(true);
        resultDescription.setRows(7);
        resultDescription.setForeground(MUTED);
        resultDescription.setBorder(new EmptyBorder(8, 8, 8, 8));
        result.add(resultDescription, BorderLayout.CENTER);
        saveRecipeButton.setEnabled(false);
        saveRecipeButton.addActionListener(e -> saveRecipe());
        result.add(saveRecipeButton, BorderLayout.SOUTH);
        outer.add(result);
        return outer;
    }

    private JComponent buildHistory() {
        JPanel panel = card("Журнал опытов и сохранённые рецепты");
        panel.setLayout(new BorderLayout(8, 8));
        historyTable.setModel(historyModel);
        historyTable.setRowHeight(28);
        historyTable.setFillsViewportHeight(true);
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        JButton refresh = new JButton("Обновить журнал");
        refresh.addActionListener(e -> refreshHistory());
        panel.add(refresh, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel card(String titleText) {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(72, 60, 88)),
                new EmptyBorder(10, 10, 10, 10)
        ));
        panel.setName(titleText);
        panel.setBorder(BorderFactory.createTitledBorder(
                panel.getBorder(), titleText, 0, 0,
                new Font(Font.SANS_SERIF, Font.BOLD, 14), TEXT
        ));
        return panel;
    }

    private static JLabel valueLabel() {
        JLabel l = new JLabel("—");
        l.setForeground(new Color(222, 208, 240));
        l.setHorizontalAlignment(SwingConstants.RIGHT);
        return l;
    }

    private void addMetric(JPanel panel, String key, JLabel value) {
        JLabel label = new JLabel(key);
        label.setForeground(MUTED);
        panel.add(label);
        panel.add(value);
    }

    private JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }

    private void bindEvents() {
        ingredientList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateIngredientDescription();
        });
    }

    private void updateIngredientDescription() {
        Ingredient i = ingredientList.getSelectedValue();
        if (i == null) {
            ingredientDescription.setText("");
            return;
        }
        ingredientDescription.setText(i.category() + "\n" + i.description() +
                "\nЛечение " + i.healing() + " · Токс. " + i.toxicity() + " · Магия " + i.magic());
    }

    private void addIngredient() {
        Ingredient i = ingredientList.getSelectedValue();
        if (i == null) return;
        double q = ((Number) quantitySpinner.getValue()).doubleValue();
        addStep(new IngredientAdditionStep(i, q));
    }

    private void addAction(AlchemyAction action) {
        if (steps.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Сначала добавьте хотя бы один ингредиент.");
            return;
        }
        addStep(new ActionStep(action));
    }

    private void addStep(ExperimentStep step) {
        steps.add(step);
        stepModel.addElement(step);
        lastExperimentId = -1;
        lastResult = null;
        saveRecipeButton.setEnabled(false);
        resultTitle.setText("Эксперимент продолжается");
        resultDescription.setText("Изменение цепочки сбрасывает предыдущий итог. Завершите эксперимент повторно.");
        updatePreview();
        int last = stepModel.size() - 1;
        if (last >= 0) stepList.ensureIndexIsVisible(last);
    }

    private void undoStep() {
        if (steps.isEmpty()) return;
        steps.remove(steps.size() - 1);
        stepModel.remove(stepModel.size() - 1);
        lastExperimentId = -1;
        lastResult = null;
        saveRecipeButton.setEnabled(false);
        updatePreview();
    }

    private void clearExperiment() {
        steps.clear();
        stepModel.clear();
        lastExperimentId = -1;
        lastResult = null;
        saveRecipeButton.setEnabled(false);
        resultTitle.setText("Эксперимент ещё не завершён");
        resultDescription.setText("Соберите новую цепочку ингредиентов и действий.");
        updatePreview();
    }

    private void updatePreview() {
        MixtureSnapshot s = engine.preview(steps);
        stateValue.setText(s.state());
        temperatureValue.setText(s.temperature());
        colorValue.setText(s.color());
        stabilityValue.setText(format(s.stability()));
        healingValue.setText(format(s.healing()));
        toxicityValue.setText(format(s.toxicity()));
        fireValue.setText(format(s.fire()));
        frostValue.setText(format(s.frost()));
        magicValue.setText(format(s.magic()));
        volatilityValue.setText(format(s.volatility()));
    }

    private void finishExperiment() {
        if (steps.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Эксперимент пуст. Добавьте ингредиенты.");
            return;
        }
        AlchemyResult result = engine.finish(steps);
        String defaultName = "Опыт — " + result.name();
        try {
            long id = repository.saveExperiment(defaultName, List.copyOf(steps), result);
            lastExperimentId = id;
            lastResult = result;
            resultTitle.setText(result.name() + " · " + result.type());
            MixtureSnapshot s = result.snapshot();
            resultDescription.setText(result.description() + "\n\nИтог: " +
                    "стабильность " + format(s.stability()) + ", токсичность " + format(s.toxicity()) +
                    ", магия " + format(s.magic()) + ".");
            saveRecipeButton.setEnabled(true);
            refreshHistory();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось сохранить эксперимент: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveRecipe() {
        if (lastExperimentId < 0 || lastResult == null) return;
        String name = JOptionPane.showInputDialog(this, "Название рецепта:", lastResult.name());
        if (name == null || name.isBlank()) return;
        try {
            repository.markAsRecipe(lastExperimentId, name.trim());
            saveRecipeButton.setEnabled(false);
            refreshHistory();
            JOptionPane.showMessageDialog(this, "Рецепт «" + name.trim() + "» сохранён.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось сохранить рецепт: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshHistory() {
        try {
            historyModel.setRows(repository.loadHistory());
        } catch (Exception ex) {
            historyModel.setRows(List.of());
        }
    }

    private String format(double v) {
        return String.format("%.1f", v);
    }

    @Override
    public void dispose() {
        try { repository.close(); } catch (Exception ignored) {}
        super.dispose();
    }

    private static final class IngredientRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
            if (value instanceof Ingredient i) {
                l.setText("<html><b>" + escape(i.name()) + "</b><br><span style='color:#bfb4cc'>" + escape(i.category()) + "</span></html>");
                l.setBorder(new EmptyBorder(7, 7, 7, 7));
            }
            return l;
        }
    }

    private static final class StepRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
            if (value instanceof ExperimentStep step) {
                l.setText(String.format("%02d   %s", index + 1, step.displayText()));
                l.setBorder(new EmptyBorder(7, 8, 7, 8));
                if (!selected && value instanceof ActionStep) l.setForeground(new Color(211, 180, 247));
            }
            return l;
        }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static final class HistoryTableModel extends AbstractTableModel {
        private final String[] columns = {"Дата", "Название", "Результат", "Тип", "Рецепт"};
        private List<HistoryEntry> rows = List.of();
        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        void setRows(List<HistoryEntry> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            HistoryEntry h = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> h.createdAt().format(fmt);
                case 1 -> h.name();
                case 2 -> h.resultName();
                case 3 -> h.resultType();
                case 4 -> h.savedRecipe() ? "Да" : "—";
                default -> "";
            };
        }
    }
}
