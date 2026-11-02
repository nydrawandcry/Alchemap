package model;

/**
 * Ингредиент смеси
 */
public record Ingredient(
        int id,             // уникальный идентификатор
        String name,        // название ингредиента
        String category,    // категория ингредиента
        String description, // описание ингредиента
        double healing,     // лечение
        double toxicity,    // токсичность
        double fire,        // огонь
        double frost,       // заморозка
        double volatility,  // летучесть
        double magic,       // магия
        double stability    // стабильность
) {
    @Override
    public String toString() {
        return name;
    }
}
