package model;

/**
 * Быстрый снимок смеси
 * Предназначен для показа состояния смеси в момент приготовления
 * @param healing лечение
 * @param toxicity токсичность
 * @param fire огонь
 * @param frost заморозка
 * @param volatility летучесть
 * @param magic магия
 * @param stability стабильность
 * @param state состояние
 * @param temperature температура
 * @param color цвет
 * @param ingredientCount количество добавленных ингредиентов на момент снимка
 * @param actionCount количество совершенных действий над зельем на момент снимка
 */
public record MixtureSnapshot(
        double healing,
        double toxicity,
        double fire,
        double frost,
        double volatility,
        double magic,
        double stability,
        String state,
        String temperature,
        String color,
        int ingredientCount,
        int actionCount
) {}
