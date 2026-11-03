package data;

import model.*;
import model.potions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class DemoRepository implements AlchemyRepository {
    private final List<Ingredient> ingredients = defaultIngredients();
    private final List<HistoryEntry> history = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong(1);
    private final Map<Long, RecipeDetails> details = new HashMap<>();
    private final Map<Long, String> recipeNames = new HashMap<>();
    private final List<PotionRule> rules = new ArrayList<>(DefaultPotionRules.create());
    private final AtomicLong ruleIds = new AtomicLong(11);

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
        name = TextValidation.required(name, 160, "Название опыта");
        long id = ids.getAndIncrement();
        details.put(id, new RecipeDetails(id, name, null, result.name(), result.type(),
                result.description(), steps.stream().map(ExperimentStep::displayText).toList()));
        history.add(new HistoryEntry(id, name, LocalDateTime.now(), result.name(), result.type(), false));
        return id;
    }

    @Override
    public void markAsRecipe(long experimentId, String recipeName) {
        recipeName = TextValidation.required(recipeName, 160, "Название рецепта");
        for (int i = 0; i < history.size(); i++) {
            HistoryEntry h = history.get(i);
            if (h.id() == experimentId) {
                recipeNames.put(experimentId, recipeName);
                history.set(i, new HistoryEntry(h.id(), h.name(), h.createdAt(), h.resultName(), h.resultType(), true));
                return;
            }
        }
        throw new IllegalArgumentException("Опыт не найден.");
    }

    @Override
    public RecipeDetails loadRecipe(long experimentId) {
        RecipeDetails d = details.get(experimentId);
        String recipeName = recipeNames.get(experimentId);
        if (d == null || recipeName == null) throw new IllegalArgumentException("Рецепт не найден.");
        return new RecipeDetails(d.experimentId(), d.experimentName(), recipeName,
                d.resultName(), d.resultType(), d.resultDescription(), d.steps());
    }

    @Override
    public void renameExperiment(long experimentId, String name) {
        name = TextValidation.required(name, 160, "Название опыта");
        for (int i = 0; i < history.size(); i++) {
            HistoryEntry h = history.get(i);
            if (h.id() == experimentId) {
                history.set(i, new HistoryEntry(h.id(), name, h.createdAt(), h.resultName(), h.resultType(), h.savedRecipe()));
                RecipeDetails d = details.get(experimentId);
                details.put(experimentId, new RecipeDetails(experimentId, name, d.recipeName(),
                        d.resultName(), d.resultType(), d.resultDescription(), d.steps()));
                return;
            }
        }
        throw new IllegalArgumentException("Опыт не найден.");
    }

    @Override
    public List<PotionRule> loadPotionRules() {
        return rules.stream().sorted(PotionRule.ORDER).toList();
    }

    @Override
    public long savePotionRule(PotionRule rule) {
        long id = rule.id();
        if (id == 0) id = ruleIds.getAndIncrement();
        else {
            long existingId = id;
            if (rules.stream().noneMatch(r -> r.id() == existingId)) {
                throw new IllegalArgumentException("Правило не найдено.");
            }
        }
        long savedId = id;
        rules.removeIf(r -> r.id() == savedId);
        rules.add(new PotionRule(id, rule.name(), rule.type(), rule.description(),
                rule.priority(), rule.enabled(), rule.conditions()));
        return id;
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
