package model;

public enum AlchemyAction {
    GRIND("Измельчить", "Раскрывает активные свойства, но слегка снижает стабильность."),
    CUT("Нарезать", "Слабо усиливает свойства без серьёзного вмешательства."),
    BOIL("Сварить", "Переводит смесь в жидкое состояние и усиливает растворимые свойства."),
    BURN("Сжечь", "Разрушает лечебные свойства, усиливает огненные и токсичные."),
    COOL("Охладить", "Повышает стабильность и усиливает холодные свойства."),
    FILTER("Фильтровать", "Снижает токсичность и удаляет часть тяжёлых примесей."),
    DISTILL("Перегнать", "Концентрирует летучие и магические свойства."),
    INFUSE("Настоять", "Мягко усиливает лечебные и магические свойства."),
    DRY("Высушить", "Снижает воду, повышает концентрацию и летучесть сырья.");

    private final String label;
    private final String description;

    AlchemyAction(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() { return label; }
    public String description() { return description; }

    @Override
    public String toString() { return label; }
}
