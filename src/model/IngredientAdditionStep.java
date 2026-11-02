package model;

/**
 * Добавление ингредиента
 * @param ingredient добавляемый ингредиент
 * @param quantity количество добавляемого ингредиента
 */
public record IngredientAdditionStep(Ingredient ingredient, double quantity) implements ExperimentStep{
    @Override
    public String displayText() {
        String q = quantity == Math.rint(quantity) ? String.format("%.0f", quantity) : String.format("%.1f", quantity);
        return "+ " + q + "× " + ingredient.name();
    }
}
