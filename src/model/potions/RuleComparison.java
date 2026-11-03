package model.potions;

public enum RuleComparison {
    GE(">="), LE("<="), GT(">"), LT("<"), EQ("="), NE("!=");
    private final String symbol;
    RuleComparison(String symbol) { this.symbol = symbol; }
    @Override public String toString() { return symbol; }
    public boolean test(double left, double right) {
        return switch (this) {
            case GE -> left >= right; case LE -> left <= right;
            case GT -> left > right; case LT -> left < right;
            case EQ -> left == right; case NE -> left != right;
        };
    }
}
