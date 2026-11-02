package engine;

import model.Ingredient;
import model.MixtureSnapshot;

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


}
