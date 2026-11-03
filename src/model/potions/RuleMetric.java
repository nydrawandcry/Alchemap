package model.potions;

import model.MixtureSnapshot;

public enum RuleMetric {
    HEALING("Лечение"), TOXICITY("Токсичность"), FIRE("Огонь"), FROST("Холод"),
    VOLATILITY("Летучесть"), MAGIC("Магия"), STABILITY("Стабильность"),
    CONFLICT("Конфликт огня и холода"), USEFUL("Максимум полезных свойств"),
    INGREDIENT_COUNT("Число добавлений ингредиентов"), ACTION_COUNT("Число действий"),
    BURN_COUNT("Число сжиганий"), LIQUID("Жидкая смесь (1 — да, 0 — нет)"),
    DISTILLED("Была перегонка (1 — да, 0 — нет)"),
    COOLED("Было охлаждение (1 — да, 0 — нет)"),
    COLD("Сейчас холодная (1 — да, 0 — нет)");

    private final String label;
    RuleMetric(String label) { this.label = label; }
    @Override public String toString() { return label; }

    public double value(AlchemyContext context) {
        MixtureSnapshot s = context.snapshot();
        return switch (this) {
            case HEALING -> s.healing(); case TOXICITY -> s.toxicity();
            case FIRE -> s.fire(); case FROST -> s.frost();
            case VOLATILITY -> s.volatility(); case MAGIC -> s.magic();
            case STABILITY -> s.stability();
            case CONFLICT -> Math.min(s.fire(), s.frost());
            case USEFUL -> Math.max(Math.max(s.healing(), s.toxicity()),
                    Math.max(s.magic(), Math.max(s.fire(), s.frost())));
            case INGREDIENT_COUNT -> s.ingredientCount();
            case ACTION_COUNT -> s.actionCount(); case BURN_COUNT -> context.burnCount();
            case LIQUID -> context.liquid() ? 1 : 0;
            case DISTILLED -> context.distilled() ? 1 : 0;
            case COOLED -> context.cooled() ? 1 : 0;
            case COLD -> "холодная".equals(s.temperature()) ? 1 : 0;
        };
    }
}
