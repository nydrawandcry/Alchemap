package data;

import model.*;
import model.potions.*;
import java.util.List;
import static model.potions.RuleMetric.*;
import static model.potions.RuleComparison.*;
import static model.potions.RuleCondition.number;

/** Начальные правила демо. В MySQL те же данные добавляет migration-v2.sql; движок их не зашивает. */
public final class DefaultPotionRules {
    private DefaultPotionRules() {}
    public static List<PotionRule> create() {
        return List.of(
            new PotionRule(1, "Зелье малого лечения", "Зелье",
                "Стабильный лечебный состав. В игровой сессии может восстанавливать небольшое количество хитов.",
                1000, true, List.of(number(1, HEALING, GE, 20), number(1, TOXICITY, LE, 10),
                    number(1, STABILITY, GE, 12), number(1, LIQUID, EQ, 1))),
            new PotionRule(2, "Эликсир огненного дыхания", "Зелье",
                "Горячая магическая смесь с выраженным огненным профилем. Требует осторожного хранения.",
                900, true, List.of(number(1, FIRE, GE, 18), number(1, MAGIC, GE, 8),
                    number(1, LIQUID, EQ, 1), number(1, COLD, EQ, 0))),
            new PotionRule(3, "Морозная эссенция", "Реагент",
                "Концентрированный холодный реагент, пригодный для зелий, ловушек и зачарования.",
                800, true, List.of(number(1, FROST, GE, 16), number(1, DISTILLED, EQ, 1), number(1, COOLED, EQ, 1))),
            new PotionRule(4, "Яд туманного укуса", "Яд",
                "Токсичный концентрат. Летучесть делает его особенно опасным при вскрытии сосуда.",
                700, true, List.of(number(1, TOXICITY, GE, 20), number(1, VOLATILITY, GE, 7),
                    number(2, TOXICITY, GE, 20), number(2, DISTILLED, EQ, 1))),
            new PotionRule(5, "Нестабильная алхимическая смесь", "Нестабильная смесь",
                "Состав конфликтует сам с собой. Его эффект непредсказуем, а хранение потенциально опасно.",
                600, true, List.of(number(1, STABILITY, LT, 5), number(2, CONFLICT, GE, 10),
                    number(3, VOLATILITY, GE, 18), number(3, FIRE, GE, 10))),
            new PotionRule(6, "Бесполезный осадок", "Бесполезный осадок",
                "Активные свойства почти разрушены. В колбе осталась алхимически малоценная масса.",
                500, true, List.of(number(1, USEFUL, LT, 8), number(2, BURN_COUNT, GE, 2),
                    number(2, MAGIC, LT, 5), number(2, TOXICITY, LT, 10))),
            new PotionRule(7, "Сырой алхимический яд", "Яд",
                "Токсичность доминирует над полезными свойствами. Эффект есть, но состав ещё грубый.",
                400, true, List.of(new RuleCondition(1, TOXICITY, GT, HEALING, 1.15, 0), number(1, TOXICITY, GT, 12))),
            new PotionRule(8, "Травяной восстановительный настой", "Зелье",
                "Лечебные свойства выражены сильнее побочных. Получился простой пригодный настой.",
                300, true, List.of(number(1, HEALING, GE, 12), new RuleCondition(1, HEALING, GE, TOXICITY, 1, 0))),
            new PotionRule(9, "Алхимический реагент", "Реагент",
                "Смесь имеет заметные активные свойства, но не сформировалась в законченное зелье.",
                200, true, List.of(number(1, MAGIC, GE, 10), number(2, FIRE, GE, 12), number(3, FROST, GE, 12))),
            new PotionRule(10, "Мутная алхимическая смесь", "Бесполезный осадок",
                "Компоненты взаимодействуют слишком слабо. Результат можно использовать только как сырьё для нового опыта.",
                0, true, List.of())
        );
    }
}
