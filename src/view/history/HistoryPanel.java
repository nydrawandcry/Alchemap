package view.history;

import data.AlchemyRepository;
import model.HistoryEntry;
import model.potions.RecipeDetails;
import view.common.NameDialogs;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;

/**
 * Журнал опытов: таблица, переименование и просмотр рецептов
 */
public final class HistoryPanel extends JPanel {
    private final AlchemyRepository repository;

    private final HistoryTableModel historyModel =
            new HistoryTableModel();

    private final JTable historyTable =
            new JTable(historyModel);

    private final JButton renameExperimentButton =
            new JButton("Переименовать опыт");

    public HistoryPanel(
            AlchemyRepository repository,
            Color accent
    ) {
        super(new BorderLayout(8, 8));
        this.repository = repository;

        setOpaque(false);
        configureTable(accent);
        bindEvents();

        add(new JScrollPane(historyTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout(8, 0));

        JButton refresh = new JButton("Обновить журнал");
        refresh.addActionListener(e -> refreshHistory());

        actions.add(refresh, BorderLayout.CENTER);
        actions.add(renameExperimentButton, BorderLayout.EAST);

        add(actions, BorderLayout.SOUTH);
    }

    /**
     * Настраивает отображение таблицы и столбца рецептов
     */
    private void configureTable(Color accent) {
        historyTable.setRowHeight(28);
        historyTable.setFillsViewportHeight(true);
        historyTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        historyTable.setAutoCreateRowSorter(true);

        historyTable.getColumnModel()
                .getColumn(4)
                .setCellRenderer(new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(
                            JTable table,
                            Object value,
                            boolean selected,
                            boolean focus,
                            int row,
                            int column
                    ) {
                        super.getTableCellRendererComponent(
                                table, value, selected,
                                focus, row, column
                        );

                        boolean recipe = !"—".equals(value);

                        setToolTipText(
                                recipe
                                        ? "Нажмите, чтобы открыть шаги рецепта."
                                        : "Этот опыт не отмечен как рецепт."
                        );

                        if (!selected) {
                            setForeground(
                                    recipe
                                            ? accent
                                            : table.getForeground()
                            );
                        }

                        return this;
                    }
                });

        renameExperimentButton.setEnabled(false);
    }

    /**
     * Подключает обработчики мыши, клавиатуры и выделения строк
     */
    private void bindEvents() {
        historyTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;

                int row = historyTable.rowAtPoint(e.getPoint());
                int column = historyTable.columnAtPoint(e.getPoint());

                if (row < 0 || column < 0) return;

                historyTable.setRowSelectionInterval(row, row);

                int modelColumn =
                        historyTable.convertColumnIndexToModel(column);

                if (modelColumn == 4 && e.getClickCount() == 1) {
                    showRecipe(
                            row,
                            e.getX(),
                            e.getY() + historyTable.getRowHeight()
                    );
                } else if (modelColumn == 1
                        && e.getClickCount() == 2) {
                    renameSelectedExperiment();
                }
            }
        });

        historyTable.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                "openRecipe"
        );

        historyTable.getActionMap().put(
                "openRecipe",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int row = historyTable.getSelectedRow();
                        if (row < 0) return;

                        int column =
                                historyTable.convertColumnIndexToView(4);

                        Rectangle cell = historyTable.getCellRect(
                                row, column, true
                        );

                        showRecipe(
                                row,
                                cell.x,
                                cell.y + cell.height
                        );
                    }
                }
        );

        historyTable.getSelectionModel()
                .addListSelectionListener(e ->
                        renameExperimentButton.setEnabled(
                                historyTable.getSelectedRow() >= 0
                        )
                );

        renameExperimentButton.addActionListener(
                e -> renameSelectedExperiment()
        );
    }

    /**
     * Обновляет журнал, сохраняя выделение по ID опыта
     */
    public void refreshHistory() {
        try {
            long selectedId = -1;
            int row = historyTable.getSelectedRow();

            if (row >= 0) {
                int modelRow =
                        historyTable.convertRowIndexToModel(row);
                selectedId = historyModel.entryAt(modelRow).id();
            }

            historyModel.setRows(repository.loadHistory());

            for (int i = 0; i < historyModel.getRowCount(); i++) {
                if (historyModel.entryAt(i).id() == selectedId) {
                    int viewRow =
                            historyTable.convertRowIndexToView(i);

                    if (viewRow >= 0) {
                        historyTable.setRowSelectionInterval(
                                viewRow, viewRow
                        );
                    }

                    break;
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Не удалось загрузить журнал: " + ex.getMessage(),
                    "Журнал опытов",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Открывает карточку рецепта выбранного опыта
     */
    private void showRecipe(int viewRow, int x, int y) {
        int modelRow =
                historyTable.convertRowIndexToModel(viewRow);

        HistoryEntry entry = historyModel.entryAt(modelRow);

        if (!entry.savedRecipe()) return;

        try {
            RecipeDetails recipe = repository.loadRecipe(entry.id());
            new RecipePopup(recipe).show(historyTable, x, y);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Не удалось открыть рецепт: " + ex.getMessage(),
                    "Рецепт",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Переименовывает выбранный опыт и обновляет таблицу
     */
    private void renameSelectedExperiment() {
        int row = historyTable.getSelectedRow();
        if (row < 0) return;

        int modelRow =
                historyTable.convertRowIndexToModel(row);

        HistoryEntry entry = historyModel.entryAt(modelRow);

        String newName = NameDialogs.askName(
                this,
                "Название опыта",
                entry.name()
        );

        if (newName == null || newName.equals(entry.name())) return;

        try {
            repository.renameExperiment(entry.id(), newName);
            refreshHistory();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Не удалось переименовать опыт: "
                            + ex.getMessage(),
                    "Название опыта",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}