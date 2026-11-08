package view.mixture;

import java.util.EventListener;

/**
 * Получает запрос на сохранение результата как рецепта
 */
@FunctionalInterface
public interface RecipeSaveListener extends EventListener {
    void recipeSaveRequested();
}