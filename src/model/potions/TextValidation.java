package model.potions;

public final class TextValidation {
    private TextValidation() {}
    public static String required(String text, int max, String label) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty() || value.length() > max) {
            throw new IllegalArgumentException(label + ": от 1 до " + max + " символов.");
        }
        return value;
    }
}
