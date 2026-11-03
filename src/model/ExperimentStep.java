package model;

/**
 * Шаг эксперимента
 * Общий интерфейс, который имплементируют все шаги эксперимента (добавление ингредиентов(Addition) и манипуляции с ними (Action))
 */
public sealed interface ExperimentStep permits IngredientAdditionStep, ActionStep {
    String displayText();
}
