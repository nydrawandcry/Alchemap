package model.potions;

import model.MixtureSnapshot;

/** Итог вычисления смеси и признаки выполненных действий. */
public record AlchemyContext(MixtureSnapshot snapshot, boolean liquid,
                             boolean distilled, boolean cooled, int burnCount) {}
