package model.potions;

import java.util.Objects;

/** Внутри группы — И; между группами — ИЛИ. Справа — число или показатель * множитель + число. */
public record RuleCondition(int group, RuleMetric metric, RuleComparison comparison,
                            RuleMetric otherMetric, double multiplier, double threshold) {
    public RuleCondition {
        if (group < 1 || group > 99) throw new IllegalArgumentException("Номер группы: от 1 до 99.");
        Objects.requireNonNull(metric, "Выберите показатель.");
        Objects.requireNonNull(comparison, "Выберите сравнение.");
        if (!Double.isFinite(multiplier) || !Double.isFinite(threshold)
                || Math.abs(multiplier) > 1e9 || Math.abs(threshold) > 1e9) {
            throw new IllegalArgumentException("Порог и множитель должны быть конечными числами от -1 млрд до 1 млрд.");
        }
    }
    public static RuleCondition number(int group, RuleMetric metric, RuleComparison comparison, double value) {
        return new RuleCondition(group, metric, comparison, null, 1, value);
    }
    public boolean matches(AlchemyContext context) {
        double right = otherMetric == null ? threshold : otherMetric.value(context) * multiplier + threshold;
        return comparison.test(metric.value(context), right);
    }
}
