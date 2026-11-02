package engine;

import model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Логика взаимодействия ингредиентов между собой
 * Главный движок приложения
 */
public class AlchemyEngine {

    // -------------- поля --------------

    private static final double EPS = 0.0001;

    // ------------- вложенные классы -------------

    private static final class Portion {
        double qty;
        double healing, toxicity, fire, frost, volatility, magic, stability;

        Portion(Ingredient i, double qty) {
            this.qty = qty;
            this.healing = i.healing() * qty;
            this.toxicity = i.toxicity() * qty;
            this.fire = i.fire() * qty;
            this.frost = i.frost() * qty;
            this.volatility = i.volatility() * qty;
            this.magic = i.magic() * qty;
            this.stability = i.stability() * qty;
        }
    }

    private record Processed(MixtureSnapshot snapshot, boolean liquid, boolean distilled, boolean cooled, int burnCount) {}

    // ------------- вложенные интерфейсы -------------

    private interface Getter { double get(Portion p); }

    // ------------- методы -------------

    public MixtureSnapshot preview(List<ExperimentStep> steps) {
        return process(steps).snapshot;
    }

    public AlchemyResult finish(List<ExperimentStep> steps) {
        Processed p = process(steps);
        MixtureSnapshot s = p.snapshot;

        if (s.ingredientCount() == 0) {
            return new AlchemyResult(
                    "Пустой котёл",
                    "Бесполезный осадок",
                    "Эксперимент завершён без ингредиентов. Получать здесь пока нечего.",
                    s
            );
        }

        // Несколько узнаваемых результатов для демонстрации воспроизводимых рецептов.
        if (s.healing() >= 20 && s.toxicity() <= 10 && s.stability() >= 12 && p.liquid) {
            return new AlchemyResult(
                    "Зелье малого лечения",
                    "Зелье",
                    "Стабильный лечебный состав. В игровой сессии может восстанавливать небольшое количество хитов.",
                    s
            );
        }
        if (s.fire() >= 18 && s.magic() >= 8 && p.liquid && !"холодная".equals(s.temperature())) {
            return new AlchemyResult(
                    "Эликсир огненного дыхания",
                    "Зелье",
                    "Горячая магическая смесь с выраженным огненным профилем. Требует осторожного хранения.",
                    s
            );
        }
        if (s.frost() >= 16 && p.distilled && p.cooled) {
            return new AlchemyResult(
                    "Морозная эссенция",
                    "Реагент",
                    "Концентрированный холодный реагент, пригодный для зелий, ловушек и зачарования.",
                    s
            );
        }
        if (s.toxicity() >= 20 && (s.volatility() >= 7 || p.distilled)) {
            return new AlchemyResult(
                    "Яд туманного укуса",
                    "Яд",
                    "Токсичный концентрат. Летучесть делает его особенно опасным при вскрытии сосуда.",
                    s
            );
        }

        double conflict = Math.min(s.fire(), s.frost());
        double useful = Math.max(Math.max(s.healing(), s.toxicity()), Math.max(s.magic(), Math.max(s.fire(), s.frost())));

        if (s.stability() < 5 || conflict >= 10 || (s.volatility() >= 18 && s.fire() >= 10)) {
            return new AlchemyResult(
                    "Нестабильная алхимическая смесь",
                    "Нестабильная смесь",
                    "Состав конфликтует сам с собой. Его эффект непредсказуем, а хранение потенциально опасно.",
                    s
            );
        }

        if (useful < 8 || p.burnCount >= 2 && s.magic() < 5 && s.toxicity() < 10) {
            return new AlchemyResult(
                    "Бесполезный осадок",
                    "Бесполезный осадок",
                    "Активные свойства почти разрушены. В колбе осталась алхимически малоценная масса.",
                    s
            );
        }

        if (s.toxicity() > s.healing() * 1.15 && s.toxicity() > 12) {
            return new AlchemyResult(
                    "Сырой алхимический яд",
                    "Яд",
                    "Токсичность доминирует над полезными свойствами. Эффект есть, но состав ещё грубый.",
                    s
            );
        }

        if (s.healing() >= 12 && s.healing() >= s.toxicity()) {
            return new AlchemyResult(
                    "Травяной восстановительный настой",
                    "Зелье",
                    "Лечебные свойства выражены сильнее побочных. Получился простой пригодный настой.",
                    s
            );
        }

        if (s.magic() >= 10 || s.fire() >= 12 || s.frost() >= 12) {
            return new AlchemyResult(
                    "Алхимический реагент",
                    "Реагент",
                    "Смесь имеет заметные активные свойства, но не сформировалась в законченное зелье.",
                    s
            );
        }

        return new AlchemyResult(
                "Мутная алхимическая смесь",
                "Бесполезный осадок",
                "Компоненты взаимодействуют слишком слабо. Результат можно использовать только как сырьё для нового опыта.",
                s
        );
    }

    private Processed process(List<ExperimentStep> steps) {
        List<Portion> portions = new ArrayList<>();
        boolean liquid = false;
        boolean distilled = false;
        boolean cooled = false;
        int burnCount = 0;
        int actionCount = 0;
        String temperature = "комнатная";

        for (ExperimentStep step : steps) {
            if (step instanceof IngredientAdditionStep add) {
                if (add.quantity() > EPS) {
                    portions.add(new Portion(add.ingredient(), add.quantity()));
                }
                continue;
            }
            if (!(step instanceof ActionStep actionStep)) continue;
            actionCount++;
            AlchemyAction action = actionStep.action();
            switch (action) {
                case GRIND -> {
                    for (Portion p : portions) {
                        p.healing *= 1.15;
                        p.toxicity *= 1.12;
                        p.magic *= 1.10;
                        p.stability *= 0.96;
                    }
                }
                case CUT -> {
                    for (Portion p : portions) {
                        p.healing *= 1.05;
                        p.toxicity *= 1.04;
                        p.magic *= 1.03;
                    }
                }
                case BOIL -> {
                    liquid = true;
                    temperature = "горячая";
                    for (Portion p : portions) {
                        p.healing *= 1.12;
                        p.toxicity *= 1.07;
                        p.magic *= 1.06;
                        p.volatility *= 0.82;
                        p.stability *= 0.98;
                    }
                }
                case BURN -> {
                    burnCount++;
                    temperature = "горячая";
                    for (Portion p : portions) {
                        p.healing *= 0.22;
                        p.toxicity *= 1.18;
                        p.fire = p.fire * 1.35 + 2.2 * p.qty;
                        p.frost *= 0.25;
                        p.volatility *= 0.45;
                        p.magic *= 0.82;
                        p.stability *= 0.72;
                    }
                }
                case COOL -> {
                    cooled = true;
                    temperature = "холодная";
                    for (Portion p : portions) {
                        p.fire *= 0.84;
                        p.frost *= 1.18;
                        p.stability = p.stability * 1.12 + 0.4 * p.qty;
                    }
                }
                case FILTER -> {
                    liquid = true;
                    for (Portion p : portions) {
                        p.toxicity *= 0.74;
                        p.healing *= 0.96;
                        p.fire *= 0.93;
                        p.frost *= 0.96;
                        p.stability *= 1.08;
                    }
                }
                case DISTILL -> {
                    liquid = true;
                    distilled = true;
                    temperature = "горячая";
                    for (Portion p : portions) {
                        p.volatility = p.volatility * 1.35 + 0.5 * p.qty;
                        p.magic *= 1.14;
                        p.toxicity *= 1.06;
                        p.healing *= 0.94;
                        p.stability *= 0.93;
                    }
                }
                case INFUSE -> {
                    liquid = true;
                    for (Portion p : portions) {
                        p.healing *= 1.08;
                        p.magic *= 1.15;
                        p.volatility *= 0.95;
                        p.stability *= 1.04;
                    }
                }
                case DRY -> {
                    for (Portion p : portions) {
                        p.healing *= 0.97;
                        p.toxicity *= 1.05;
                        p.volatility *= 1.15;
                        p.magic *= 1.05;
                    }
                }
            }
        }

        double healing = sum(portions, p -> p.healing);
        double toxicity = sum(portions, p -> p.toxicity);
        double fire = sum(portions, p -> p.fire);
        double frost = sum(portions, p -> p.frost);
        double volatility = sum(portions, p -> p.volatility);
        double magic = sum(portions, p -> p.magic);
        double stability = sum(portions, p -> p.stability);

        String state;
        if (portions.isEmpty()) state = "пусто";
        else if (liquid) state = distilled ? "дистиллят" : "жидкость";
        else if (burnCount > 0) state = "порошок / зола";
        else state = "смесь компонентов";

        String color = chooseColor(healing, toxicity, fire, frost, magic);

        MixtureSnapshot snapshot = new MixtureSnapshot(
                round(healing), round(toxicity), round(fire), round(frost), round(volatility),
                round(magic), round(stability), state, temperature, color, portions.size(), actionCount
        );
        return new Processed(snapshot, liquid, distilled, cooled, burnCount);
    }

    private double sum(List<Portion> portions, Getter getter) {
        double v = 0;
        for (Portion p : portions) v += getter.get(p);
        return v;
    }

    private String chooseColor(double healing, double toxicity, double fire, double frost, double magic) {
        double max = Math.max(Math.max(healing, toxicity), Math.max(fire, Math.max(frost, magic)));
        if (max < 1) return "прозрачный";
        if (max == fire) return "янтарно-красный";
        if (max == frost) return "ледяно-голубой";
        if (max == toxicity) return "болотно-зелёный";
        if (max == healing) return "изумрудный";
        return "фиолетовый";
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

}
