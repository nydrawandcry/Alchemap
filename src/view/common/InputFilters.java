package view.common;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.math.BigDecimal;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Ограничения во время редактирования полей
 */
public final class InputFilters {
    private static final Pattern STAT =
            Pattern.compile("[0-9]{0,5}([.,][0-9]{0,2})?");

    private static final Pattern RULE =
            Pattern.compile("-?[0-9]*(\\.[0-9]*)?");

    private static final BigDecimal RULE_MAX =
            new BigDecimal("1000000000");

    private InputFilters() {}

    public static void limit(JTextComponent field, int maxLength) {
        install(field, text -> text.length() <= maxLength);
    }

    public static void ingredientStat(JTextComponent field) {
        install(field, text -> STAT.matcher(text).matches());
    }

    public static void ruleNumber(JTextComponent field) {
        install(field, InputFilters::ruleDraft);
    }

    public static boolean isIngredientStatComplete(String text) {
        if (!STAT.matcher(text).matches()) return false;

        try {
            double value = Double.parseDouble(normalize(text));

            return Double.isFinite(value)
                    && value >= 0
                    && value <= 99_999.99;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static double parseRuleNumber(String text) {
        if (!ruleDraft(text)) {
            throw new IllegalArgumentException(
                    "Введите число от -1 млрд до 1 млрд."
            );
        }

        // Промежуточный ввод вроде "-" здесь вызовет исключение.
        return Double.parseDouble(normalize(text));
    }

    private static boolean ruleDraft(String text) {
        String value = normalize(text);

        if (!RULE.matcher(value).matches()) return false;

        // Такие состояния нужны, чтобы пользователь мог
        // очистить поле и начать ввод нового числа.
        if (value.isEmpty()
                || value.equals("-")
                || value.equals(".")
                || value.equals("-.")) {
            return true;
        }

        return new BigDecimal(value)
                .abs()
                .compareTo(RULE_MAX) <= 0;
    }

    private static String normalize(String text) {
        return text.replace(',', '.');
    }

    public static void onChange(
            JTextComponent field,
            Runnable action
    ) {
        field.getDocument().addDocumentListener(
                new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        action.run();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        action.run();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        action.run();
                    }
                }
        );

        action.run();
    }

    private static void install(
            JTextComponent field,
            Predicate<String> allowed
    ) {
        AbstractDocument document =
                (AbstractDocument) field.getDocument();

        document.setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(
                    FilterBypass fb,
                    int offset,
                    String text,
                    AttributeSet attrs
            ) throws BadLocationException {
                replace(fb, offset, 0, text, attrs);
            }

            @Override
            public void remove(
                    FilterBypass fb,
                    int offset,
                    int length
            ) throws BadLocationException {
                replace(fb, offset, length, "", null);
            }

            @Override
            public void replace(
                    FilterBypass fb,
                    int offset,
                    int length,
                    String text,
                    AttributeSet attrs
            ) throws BadLocationException {
                String inserted = text == null ? "" : text;

                String current = fb.getDocument().getText(
                        0, fb.getDocument().getLength()
                );

                String next = current.substring(0, offset)
                        + inserted
                        + current.substring(offset + length);

                if (allowed.test(next)) {
                    fb.replace(offset, length, inserted, attrs);
                }
            }
        });
    }
}