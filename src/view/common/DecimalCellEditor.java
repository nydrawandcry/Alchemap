package view.common;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.Component;
import java.math.BigDecimal;

/**
 * Редактор десятичного числа для JTable
 */
public final class DecimalCellEditor extends DefaultCellEditor {
    private final JTextField field;

    public DecimalCellEditor() {
        super(new JTextField());

        field = (JTextField) getComponent();
        InputFilters.ruleNumber(field);

        InputFilters.onChange(field, () -> {
            field.putClientProperty("JComponent.outline", null);
            field.setToolTipText(null);
        });
    }

    @Override
    public Component getTableCellEditorComponent(
            JTable table,
            Object value,
            boolean selected,
            int row,
            int column
    ) {
        // загруженный Double может иметь вид 1.0E-4.
        // в редактор передаём десятичную запись 0.0001.
        String text = value instanceof Number number
                ? BigDecimal.valueOf(number.doubleValue())
                  .stripTrailingZeros()
                  .toPlainString()
                : value == null ? "" : value.toString();

        return super.getTableCellEditorComponent(
                table, text, selected, row, column
        );
    }

    @Override
    public boolean stopCellEditing() {
        try {
            InputFilters.parseRuleNumber(field.getText());
        } catch (IllegalArgumentException ex) {
            field.putClientProperty(
                    "JComponent.outline", "error"
            );
            field.setToolTipText(
                    "Введите число от -1 млрд до 1 млрд."
            );

            return false;
        }

        return super.stopCellEditing();
    }

    @Override
    public Object getCellEditorValue() {
        return InputFilters.parseRuleNumber(field.getText());
    }
}