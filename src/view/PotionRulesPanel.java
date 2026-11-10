package view;

import data.AlchemyRepository;
import model.*;
import model.potions.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Локальный редактор правил кампании. Изменения действуют на следующие завершённые опыты. */
public final class PotionRulesPanel extends JPanel {
    private static final String NUMBER = "Числом";
    private final AlchemyRepository repository;
    private final DefaultListModel<PotionRule> rules = new DefaultListModel<>();
    private final JList<PotionRule> ruleList = new JList<>(rules);
    private final JTextField name = new JTextField();
    private final JComboBox<String> type = new JComboBox<>(new String[]{"Зелье", "Яд", "Реагент", "Нестабильная смесь", "Бесполезный осадок"});
    private final JTextArea description = new JTextArea(3, 30);
    private final JSpinner priority = new JSpinner(new SpinnerNumberModel(100, 0, 10000, 10));
    private final JCheckBox enabled = new JCheckBox("Правило включено", true);
    private final ConditionModel conditions = new ConditionModel();
    private final JTable table = new JTable(conditions);
    private final JLabel status = new JLabel(" ");
    private long currentId;
    private int previousIndex = -1;
    private boolean populating;
    private boolean dirty;

    public PotionRulesPanel(AlchemyRepository repository) {
        super(new BorderLayout(10, 10));
        this.repository = repository;
        setBorder(new EmptyBorder(10, 10, 10, 10));
        type.setEditable(true);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        ruleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ruleList.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof PotionRule r) {
                    setText((r.enabled() ? "" : "[выключено] ") + r.name() + " · " + r.priority());
                }
                return this;
            }
        });
        JPanel left = new JPanel(new BorderLayout(5, 5));
        left.add(new JLabel("Результаты (по приоритету)"), BorderLayout.NORTH);
        left.add(new JScrollPane(ruleList), BorderLayout.CENTER);
        JPanel listButtons = new JPanel(new GridLayout(1, 3, 5, 5));
        listButtons.add(button("Новый", () -> {
            if (!discardChanges()) return;
            selectNew(new PotionRule(0, "Новое зелье", "Зелье", "", 100, true, List.of()));
        }));
        listButtons.add(button("Копия", () -> {
            try {
                PotionRule r = editedRule();
                String copyName = r.name().substring(0, Math.min(r.name().length(), 151)) + " (копия)";
                selectNew(new PotionRule(0, copyName, r.type(), r.description(), r.priority(), r.enabled(), r.conditions()));
            } catch (Exception ex) { error(ex); }
        }));
        listButtons.add(button("Обновить", () -> { if (discardChanges()) reload(currentId); }));
        left.add(listButtons, BorderLayout.SOUTH);

        JPanel editor = new JPanel(new BorderLayout(8, 8));
        editor.add(buildForm(), BorderLayout.NORTH);
        editor.add(buildConditions(), BorderLayout.CENTER);
        JButton save = button("Сохранить результат", this::save);
        save.setBackground(new Color(153, 109, 210));
        save.setForeground(Color.WHITE);
        editor.add(save, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, editor);
        split.setResizeWeight(0.28);
        split.setDividerLocation(340);
        left.setMinimumSize(new Dimension(240, 200));
        editor.setMinimumSize(new Dimension(650, 300));
        add(split, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        configureConditionEditors();
        installDirtyTracking();
        ruleList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || populating) return;
            int selected = ruleList.getSelectedIndex();
            if (selected == previousIndex) return;
            if (!discardChanges()) {
                populating = true;
                ruleList.setSelectedIndex(previousIndex);
                populating = false;
                return;
            }
            previousIndex = selected;
            if (selected >= 0) setRule(rules.get(selected));
        });
        reload(-1);
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0; c.gridx = 0; panel.add(new JLabel("Название"), c);
        c.gridx = 1; c.weightx = 1; c.gridwidth = 3; panel.add(name, c);
        c.gridy++; c.gridx = 0; c.gridwidth = 1; c.weightx = 0; panel.add(new JLabel("Тип"), c);
        c.gridx = 1; c.weightx = 1; panel.add(type, c);
        c.gridx = 2; c.weightx = 0; panel.add(new JLabel("Приоритет"), c);
        c.gridx = 3; panel.add(priority, c);
        c.gridy++; c.gridx = 1; c.gridwidth = 3; panel.add(enabled, c);
        c.gridy++; c.gridx = 0; c.gridwidth = 1; panel.add(new JLabel("Эффект / описание"), c);
        c.gridx = 1; c.gridwidth = 3; c.weightx = 1; panel.add(new JScrollPane(description), c);
        return panel;
    }

    private JPanel buildConditions() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JLabel help = new JLabel("<html>Все условия одной группы должны выполняться вместе. Достаточно любой группы.<br>"
                + "Без условий правило подходит всегда: оставляйте ему низкий приоритет.<br>"
                + "Справа: «Числом» → только число; показатель → показатель × множитель + число.<br>"
                + "Для признаков «Жидкая смесь», «Было охлаждение» и т. п.: 1 — да, 0 — нет.</html>");
        help.setBorder(new EmptyBorder(4, 4, 6, 4));
        panel.add(help, BorderLayout.NORTH);
        table.setRowHeight(29);
        table.getTableHeader().setReorderingAllowed(false);
        table.putClientProperty("terminateEditOnFocusLost", true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        panel.add(scroll, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(button("Добавить условие", () -> {
            if (!stopEditing()) return;
            conditions.add();
        }));
        actions.add(button("Удалить условие", () -> {
            int row = table.getSelectedRow();
            if (row >= 0 && stopEditing()) conditions.remove(row);
        }));
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private void configureConditionEditors() {
        table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JComboBox<>(RuleMetric.values())));
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JComboBox<>(RuleComparison.values())));
        JComboBox<Object> other = new JComboBox<>();
        other.addItem(NUMBER);
        for (RuleMetric metric : RuleMetric.values()) other.addItem(metric);
        table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(other));
        table.getColumnModel().getColumn(0).setPreferredWidth(65);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(45);
        table.getColumnModel().getColumn(3).setPreferredWidth(160);
    }

    private void installDirtyTracking() {
        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
        };
        name.getDocument().addDocumentListener(listener);
        description.getDocument().addDocumentListener(listener);
        ((JTextField) type.getEditor().getEditorComponent()).getDocument().addDocumentListener(listener);
        enabled.addActionListener(e -> changed());
        priority.addChangeListener(e -> changed());
        conditions.addTableModelListener(e -> changed());
    }

    private void changed() {
        if (!populating) {
            dirty = true;
            status.setText("Есть несохранённые изменения. Они начнут действовать после сохранения.");
        }
    }

    private boolean stopEditing() {
        return !table.isEditing() || table.getCellEditor().stopCellEditing();
    }

    public boolean confirmDiscard() { return discardChanges(); }

    private boolean discardChanges() {
        if (table.isEditing()) stopEditing();
        return !dirty || JOptionPane.showConfirmDialog(this, "Отменить несохранённые изменения результата?",
                "Редактор мастера", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void selectNew(PotionRule rule) {
        populating = true;
        ruleList.clearSelection();
        previousIndex = -1;
        populating = false;
        setRule(rule);
        dirty = true;
        status.setText("Новый результат. Задайте условия и нажмите «Сохранить результат».");
        name.requestFocusInWindow();
        name.selectAll();
    }

    void setRule(PotionRule rule) {
        populating = true;
        if (table.isEditing()) table.getCellEditor().cancelCellEditing();
        currentId = rule.id();
        name.setText(rule.name()); type.setSelectedItem(rule.type());
        description.setText(rule.description()); priority.setValue(rule.priority());
        enabled.setSelected(rule.enabled()); conditions.set(rule.conditions());
        dirty = false;
        populating = false;
        status.setText("Правки применяются к будущим опытам; сохранённые результаты остаются прежними."
                + (repository.isPersistent() ? "" : " В демо изменения действуют до закрытия приложения."));
    }

    PotionRule editedRule() {
        if (!stopEditing()) throw new IllegalArgumentException("Завершите ввод условия.");
        try { priority.commitEdit(); }
        catch (java.text.ParseException ex) { throw new IllegalArgumentException("Введите целый приоритет от 0 до 10000."); }
        return new PotionRule(currentId, name.getText(), String.valueOf(type.getEditor().getItem()),
                description.getText(), ((Number) priority.getValue()).intValue(), enabled.isSelected(), conditions.read());
    }

    private void save() {
        try {
            PotionRule rule = editedRule();
            if (rule.enabled() && rule.conditions().isEmpty()
                    && JOptionPane.showConfirmDialog(this,
                    "У результата нет условий: он подойдёт любой смеси. Сохранить?",
                    "Правило без условий", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            long id = repository.savePotionRule(rule);
            dirty = false;
            reload(id);
            status.setText("Результат сохранён. Он участвует в следующих опытах."
                    + (repository.isPersistent() ? "" : " Демо: до закрытия приложения."));
        } catch (Exception ex) { error(ex); }
    }

    private void reload(long selectId) {
        try {
            List<PotionRule> loaded = repository.loadPotionRules();
            populating = true;
            rules.clear();
            for (PotionRule rule : loaded) rules.addElement(rule);
            int index = -1;
            for (int i = 0; i < rules.size(); i++) if (rules.get(i).id() == selectId) index = i;
            if (index < 0 && !rules.isEmpty()) index = 0;
            ruleList.setSelectedIndex(index);
            previousIndex = index;
            populating = false;
            if (index >= 0) setRule(rules.get(index));
            else selectNew(new PotionRule(0, "Новое зелье", "Зелье", "", 100, true, List.of()));
        } catch (Exception ex) {
            populating = false;
            status.setText("Правила не загружены. Проверьте хранилище и нажмите «Обновить».");
            status.setToolTipText(ex.getMessage());
        }
    }

    private JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        return button;
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Редактор мастера", JOptionPane.ERROR_MESSAGE);
    }

    static final class ConditionModel extends AbstractTableModel {
        private final String[] columns = {"Группа ИЛИ", "Показатель", "Знак", "Сравнить с", "Множитель", "Число"};
        private final List<Object[]> rows = new ArrayList<>();
        public int getRowCount() { return rows.size(); }
        public int getColumnCount() { return columns.length; }
        public String getColumnName(int column) { return columns[column]; }
        public Object getValueAt(int row, int column) { return rows.get(row)[column]; }
        public boolean isCellEditable(int row, int column) { return true; }
        public void setValueAt(Object value, int row, int column) {
            rows.get(row)[column] = value;
            fireTableCellUpdated(row, column);
        }
        void add() {
            rows.add(new Object[]{1, RuleMetric.HEALING, RuleComparison.GE, NUMBER, 1.0, 0.0});
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        }
        void remove(int row) { rows.remove(row); fireTableRowsDeleted(row, row); }
        void set(List<RuleCondition> conditions) {
            rows.clear();
            for (RuleCondition c : conditions) rows.add(new Object[]{c.group(), c.metric(), c.comparison(),
                    c.otherMetric() == null ? NUMBER : c.otherMetric(), c.multiplier(), c.threshold()});
            fireTableDataChanged();
        }
        List<RuleCondition> read() {
            List<RuleCondition> result = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                Object[] row = rows.get(i);
                try {
                    result.add(new RuleCondition(Integer.parseInt(String.valueOf(row[0]).trim()),
                            (RuleMetric) row[1], (RuleComparison) row[2],
                            row[3] instanceof RuleMetric m ? m : null,
                            number(row[4]), number(row[5])));
                } catch (RuntimeException ex) {
                    throw new IllegalArgumentException("Условие " + (i + 1) + ": проверьте группу и числа. "
                            + (ex.getMessage() == null ? "" : ex.getMessage()));
                }
            }
            return result;
        }
        private double number(Object value) {
            return Double.parseDouble(String.valueOf(value).trim().replace(',', '.'));
        }
    }
}
