package model;

import model.potions.TextValidation;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Класс для возможности добавления новых ингредиентов
 * Проверенные данные ингредиента до добавления в базу данных (назначения ID)
 * @param name название
 * @param category категория
 * @param description описание
 * @param healing лечение
 * @param toxicity токсичность
 * @param fire огонь
 * @param frost холод
 * @param volatility летучесть
 * @param magic магия
 * @param stability стабильность
 */
public record NewIngredient(
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

    /**
     * Конструктор нового ингредиента
     * @param name название
     * @param category категория
     * @param description описание
     * @param healing лечение
     * @param toxicity токсичность
     * @param fire огонь
     * @param frost холод
     * @param volatility летучесть
     * @param magic магия
     * @param stability стабильность
     */
    public NewIngredient {
        name = TextValidation.required(name, 100, "Название");
        category = TextValidation.required(category, 60, "Категория");

        description = Objects.requireNonNullElse(description,"").trim();
        if(description.length() > 500) {
            throw new IllegalArgumentException("Описание не должно быть больше 500 символов.");
        }

        healing = checkStat(healing, "Лечение");
        toxicity = checkStat(toxicity, "Токсичность");
        fire = checkStat(fire, "Огонь");
        frost = checkStat(frost, "Холод");
        volatility = checkStat(volatility, "Летучесть");
        magic = checkStat(magic, "Магия");
        stability = checkStat(stability, "Стабильность");

    }

    /**
     * Проверка статов ингредиентов на корректность
     * @param value значение
     * @param label название стата
     * @return значение стата после проверки
     */
    private static double checkStat(double value, String label) {

        if (!Double.isFinite(value) || value < 0 || value > 99_999.99) {
            throw new IllegalArgumentException(
                    label + ": число от 0 до 99999,99."
            );
        }

        if (BigDecimal.valueOf(value).stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    label + ": не больше двух знаков после запятой."
            );
        }

        return value == 0 ? 0 : value;
    }

    /**
     * Создаёт модель после получения постоянного идентификатора
     * @param id уникальный идентификатор ингредиента
     * @return созданный новый ингредиент для БД
     */
    public Ingredient toIngredient(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Некорректный ID ингредиента."
            );
        }

        return new Ingredient(
                id, name, category, description,
                healing, toxicity, fire, frost,
                volatility, magic, stability
        );
    }
}
