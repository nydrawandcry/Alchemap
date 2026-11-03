package engine;

import model.*;
import model.potions.*;

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


    // ------------- вложенные интерфейсы -------------

    private interface Getter { double get(Portion p); }

    // ------------- методы -------------

    public MixtureSnapshot preview(List<ExperimentStep> steps) {
        return process(steps).snapshot();
    }

    /** Выбирает первое подходящее правило из БД по приоритету. */
    public AlchemyResult finish(List<ExperimentStep> steps, List<PotionRule> rules) {
        AlchemyContext context = process(steps);
        MixtureSnapshot snapshot = context.snapshot();
        if (snapshot.ingredientCount() == 0) {
            return new AlchemyResult("Пустой котёл", "Бесполезный осадок",
                    "Эксперимент завершён без ингредиентов. Получать здесь пока нечего.", snapshot);
        }
        PotionRule rule = rules.stream().sorted(PotionRule.ORDER)
                .filter(r -> r.matches(context)).findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Для этой смеси нет подходящего результата. Добавьте правило в редакторе мастера. "
                        + "Правило без условий с приоритетом 0 можно использовать как запасной результат."));
        return new AlchemyResult(rule.name(), rule.type(), rule.description(), snapshot);
    }

    private AlchemyContext process(List<ExperimentStep> steps) {
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
        return new AlchemyContext(snapshot, liquid, distilled, cooled, burnCount);
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
