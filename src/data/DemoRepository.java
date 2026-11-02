package data;

import model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class DemoRepository implements AlchemyRepository {
    private final List<Ingredient> ingredients = defaultIngredients();
    private final List<HistoryEntry> history = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong(1);

    @Override
    public List<Ingredient> loadIngredients() {
        return ingredients;
    }

    @Override
    public List<HistoryEntry> loadHistory() {
        List<HistoryEntry> copy = new ArrayList<>(history);
        Collections.reverse(copy);
        return copy;
    }

    @Override
    public long saveExperiment(String name, List<ExperimentStep> steps, AlchemyResult result) {
        long id = ids.getAndIncrement();
        history.add(new HistoryEntry(id, name, LocalDateTime.now(), result.name(), result.type(), false));
        return id;
    }

    @Override
    public void markAsRecipe(long experimentId, String recipeName) {
        for (int i = 0; i < history.size(); i++) {
            HistoryEntry h = history.get(i);
            if (h.id() == experimentId) {
                history.set(i, new HistoryEntry(h.id(), recipeName, h.createdAt(), h.resultName(), h.resultType(), true));
                return;
            }
        }
    }

    @Override
    public String modeName() {
        return "демо-режим (без MySQL)";
    }

    public static List<Ingredient> defaultIngredients() {
        return List.of(
                new Ingredient(1, "Лунная трава", "Трава", "Светлая трава, усиливающая восстановительные настои.", 5, 0.5, 0, 1, 1, 2, 3),
                new Ingredient(2, "Пепел саламандры", "Минерал", "Горячий алхимический пепел с выраженной огненной природой.", 0, 2, 6, 0, 2, 2, 1),
                new Ingredient(3, "Слизь тролля", "Органика", "Вязкая регенерирующая субстанция. Полезна, но плохо очищена.", 4, 2.5, 0, 0, 0.5, 1, 4),
                new Ingredient(4, "Серебряная пыль", "Минерал", "Магически проводящий порошок для стабилизации сложных составов.", 0.5, 0, 0, 0.5, 1, 5, 5),
                new Ingredient(5, "Ночная спорынья", "Гриб", "Тёмные споры, содержащие сильные токсичные алкалоиды.", 0, 7, 0, 0, 3, 1, 1),
                new Ingredient(6, "Морозник", "Трава", "Редкая холодная трава, реагирующая на охлаждение.", 1, 1, 0, 6, 1, 2, 3),
                new Ingredient(7, "Кровь василиска", "Органика", "Опасный магический компонент с высокой токсичностью.", 0, 6, 1, 0, 2, 5, 2),
                new Ingredient(8, "Роса фей", "Жидкость", "Лёгкая магическая жидкость, усиливающая настои и эссенции.", 3, 0, 0, 1, 3, 6, 4),
                new Ingredient(9, "Корень мандрагоры", "Корень", "Сильный универсальный реагент с лечебными и токсичными фракциями.", 3, 3, 0, 0, 1, 4, 3),
                new Ingredient(10, "Уголь драконьего дуба", "Минерал", "Пористый уголь, который хорошо переносит нагрев и фильтрацию.", 0, 1, 4, 0, 1, 1, 4),
                new Ingredient(11, "Лепесток снежной розы", "Цветок", "Хрупкий лепесток с выраженным морозным эффектом.", 2, 0, 0, 5, 2, 3, 2),
                new Ingredient(12, "Жёлчь виверны", "Органика", "Агрессивный токсичный реагент, особенно опасный после перегонки.", 0, 8, 1, 0, 4, 2, 1)
        );
    }
}
