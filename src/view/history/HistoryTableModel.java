package view.history;

import model.HistoryEntry;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Данные таблицы журнала опытов.
 */
public final class HistoryTableModel extends AbstractTableModel {
    private final String[] columns = {
            "Дата", "Название", "Результат", "Тип", "Рецепт"
    };

    private List<HistoryEntry> rows = List.of();

    private final DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public void setRows(List<HistoryEntry> rows) {
        this.rows = rows;
        fireTableDataChanged();
    }

    public HistoryEntry entryAt(int row) {
        return rows.get(row);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        HistoryEntry entry = rows.get(rowIndex);

        return switch (columnIndex) {
            case 0 -> entry.createdAt().format(fmt);
            case 1 -> entry.name();
            case 2 -> entry.resultName();
            case 3 -> entry.resultType();
            case 4 -> entry.savedRecipe() ? "Показать ▾" : "—";
            default -> "";
        };
    }
}