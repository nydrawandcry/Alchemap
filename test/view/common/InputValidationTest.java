package view.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;
import static support.Fixtures.onEdt;

class InputValidationTest {

    @ParameterizedTest
    @ValueSource(ints = {60, 100, 160, 500})
    void textLimitAppliesToTypingPasteAndSelectionReplacement(
            int limit
    ) throws Exception {
        onEdt(() -> {
            JTextArea field = new JTextArea();
            InputFilters.limit(field, limit);

            field.setText("я".repeat(limit));
            field.getDocument().insertString(limit, "x", null);

            assertEquals(limit, field.getText().length());

            field.select(0, 2);
            field.replaceSelection("аб");

            assertEquals("аб", field.getText().substring(0, 2));

            String before = field.getText();

            field.selectAll();
            field.replaceSelection("x".repeat(limit + 1));

            assertEquals(before, field.getText());

            field.selectAll();
            field.replaceSelection("");

            assertEquals("", field.getText());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "-1", "100000", "1.234", "NaN",
            "Infinity", "1e2", "12а", "1,2.3"
    })
    void ingredientFilterRejectsInvalidPaste(
            String text
    ) throws Exception {
        onEdt(() -> {
            JTextField field = new JTextField("0");
            InputFilters.ingredientStat(field);

            field.selectAll();
            field.replaceSelection(text);

            assertEquals("0", field.getText());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0", "99999.99", "123,45", ".5"
    })
    void ingredientFilterAcceptsFinishedNumbers(
            String text
    ) throws Exception {
        onEdt(() -> {
            JTextField field = new JTextField();
            InputFilters.ingredientStat(field);
            field.setText(text);

            assertEquals(text, field.getText());
            assertTrue(
                    InputFilters.isIngredientStatComplete(text)
            );
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ".", ","})
    void ingredientDraftCanBeEditedButCannotBeSaved(
            String text
    ) throws Exception {
        onEdt(() -> {
            JTextField field = new JTextField();
            InputFilters.ingredientStat(field);
            field.setText(text);

            assertEquals(text, field.getText());
            assertFalse(
                    InputFilters.isIngredientStatComplete(text)
            );
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "12a", "1e-4", "NaN", "Infinity",
            "1000000000.01", "-1000000000.01"
    })
    void ruleFilterRejectsLettersExponentAndOutOfRangeValues(
            String text
    ) throws Exception {
        onEdt(() -> {
            JTextField field = new JTextField("1");
            InputFilters.ruleNumber(field);

            field.selectAll();
            field.replaceSelection(text);

            assertEquals("1", field.getText());
        });
    }

    @ParameterizedTest
    @CsvSource({
            "-2.5,-2.5",
            "'0,75',0.75",
            "1000000000,1000000000",
            "-1000000000,-1000000000"
    })
    void ruleFilterAcceptsSignsSeparatorsAndBoundaries(
            String text,
            double expected
    ) throws Exception {
        onEdt(() -> {
            JTextField field = new JTextField();
            InputFilters.ruleNumber(field);
            field.setText(text);

            assertEquals(text, field.getText());

            assertEquals(
                    expected,
                    InputFilters.parseRuleNumber(field.getText())
            );
        });
    }

    @Test
    void changeListenerUpdatesAvailabilityForBlankAndNonblankText()
            throws Exception {
        onEdt(() -> {
            JTextField field = new JTextField();
            JButton save = new JButton();

            InputFilters.onChange(
                    field,
                    () -> save.setEnabled(
                            !field.getText().trim().isEmpty()
                    )
            );

            assertFalse(save.isEnabled());

            field.setText("Имя");
            assertTrue(save.isEnabled());

            field.setText(" ");
            assertFalse(save.isEnabled());
        });
    }

    @ParameterizedTest
    @ValueSource(doubles = {
            1e-7, Double.MIN_VALUE, 1e9, -1e9
    })
    void decimalEditorLoadsScientificModelValuesWithoutLosingThem(
            double value
    ) throws Exception {
        onEdt(() -> {
            var editor = new DecimalCellEditor();

               JTextField field =
                (JTextField) editor.getTableCellEditorComponent(
                        new JTable(1, 1),
                        value,
                        true,
                        0,
                        0
                );

            assertFalse(
                    field.getText().toLowerCase().contains("e")
            );

            assertTrue(editor.stopCellEditing());

            assertEquals(
                    value,
                    (Double) editor.getCellEditorValue()
            );
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-", ".", "-."})
    void decimalEditorKeepsUnfinishedTextOutOfTableModel(
            String text
    ) throws Exception {
        onEdt(() -> {
            JTable table = new JTable(
                    new Object[][]{{1.0}},
                    new String[]{"Число"}
            );

            table.getColumnModel()
                    .getColumn(0)
                    .setCellEditor(new DecimalCellEditor());

            assertTrue(table.editCellAt(0, 0));

            JTextField field =
                    (JTextField) table.getEditorComponent();

            field.setText(text);

            assertFalse(table.getCellEditor().stopCellEditing());
            assertEquals(1.0, table.getValueAt(0, 0));

            assertEquals(
                    "error",
                    field.getClientProperty("JComponent.outline")
            );

            field.setText("-2,75");

            assertNull(
                    field.getClientProperty("JComponent.outline")
            );

            assertTrue(table.getCellEditor().stopCellEditing());
            assertEquals(-2.75, table.getValueAt(0, 0));
        });
    }
}