package model;

/**
 * Ингредиент смеси
 * @param id уникальный и
 * @param name название инг
 * @param category категория ин
 * @param description описание инг
 * @param healing лечение
 * @param toxicity токсичность
 * @param fire огонь
 * @param frost заморозка
 * @param volatility летучесть
 * @param magic магия
 * @param stability стабильность
 */
public record Ingredient(
        int id,
        String name,
        String category,
        String description,
        double healing,
        double toxicity,
        double fire,
        double frost,
        double volatility,
        double magic,
        double stability
) {
    @Override
    public String toString() {
        return name;
    }
}
