package view.history;

import model.HistoryEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static support.Fixtures.onEdt;

class HistoryTableModelTest {

    @Test
    void displaysDateResultAndRecipeAvailability() throws Exception {
        onEdt(() -> {
            var model = new HistoryTableModel();

            var entry = new HistoryEntry(
                    7,
                    "Опыт",
                    LocalDateTime.of(2026, 11, 23, 20, 8),
                    "Настой",
                    "Зелье",
                    true
            );

            model.setRows(List.of(entry));

            assertEquals(
                    "23.11.2026 20:08",
                    model.getValueAt(0, 0)
            );

            assertEquals("Опыт", model.getValueAt(0, 1));
            assertEquals("Настой", model.getValueAt(0, 2));
            assertEquals("Зелье", model.getValueAt(0, 3));
            assertEquals("Показать ▾", model.getValueAt(0, 4));
            assertEquals(7, model.entryAt(0).id());
        });
    }

    @Test
    void refreshReplacesRowsAndNotifiesTheTable() throws Exception {
        onEdt(() -> {
            var model = new HistoryTableModel();
            AtomicInteger changes = new AtomicInteger();

            model.addTableModelListener(
                    e -> changes.incrementAndGet()
            );

            model.setRows(List.of(new HistoryEntry(
                    1, "Опыт", LocalDateTime.now(),
                    "Итог", "Тип", false
            )));

            assertEquals("—", model.getValueAt(0, 4));

            model.setRows(List.of());

            assertEquals(0, model.getRowCount());
            assertEquals(2, changes.get());
        });
    }
}