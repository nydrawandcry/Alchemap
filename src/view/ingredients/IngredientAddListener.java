package view.ingredients;

import model.Ingredient;

import java.util.EventListener;

/**
 * Получает запрос панели на добавление ингредиента в опыт
 * (контрактт между панелью и ее слушателем)
 */
@FunctionalInterface
public interface IngredientAddListener extends EventListener {
    void ingredientAddRequested(
            Ingredient ingredient,
            double quantity
    );
}