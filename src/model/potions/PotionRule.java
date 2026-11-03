package model.potions;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record PotionRule(long id, String name, String type, String description,
                          int priority, boolean enabled, List<RuleCondition> conditions) {
    public static final Comparator<PotionRule> ORDER = Comparator.comparingInt(PotionRule::priority)
            .reversed().thenComparingLong(PotionRule::id);
    public PotionRule {
        if (id < 0) throw new IllegalArgumentException("Неверный ID правила.");
        name = TextValidation.required(name, 160, "Название зелья");
        type = TextValidation.required(type, 80, "Тип результата");
        description = Objects.requireNonNullElse(description, "").trim();
        if (description.length() > 800) throw new IllegalArgumentException("Описание: не больше 800 символов.");
        if (priority < 0 || priority > 10000) throw new IllegalArgumentException("Приоритет: от 0 до 10000.");
        conditions = List.copyOf(conditions);
    }
    public boolean matches(AlchemyContext context) {
        if (!enabled) return false;
        if (conditions.isEmpty()) return true;
        Map<Integer, Boolean> groups = new LinkedHashMap<>();
        for (RuleCondition condition : conditions) {
            groups.merge(condition.group(), condition.matches(context), (a, b) -> a && b);
        }
        return groups.values().stream().anyMatch(Boolean::booleanValue);
    }
}
