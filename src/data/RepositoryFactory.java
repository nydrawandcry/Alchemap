package data;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public final class RepositoryFactory {
    private RepositoryFactory() {}

    public record Result(AlchemyRepository repository, String warning) {}

    public static Result create() throws Exception {
        Properties config = new Properties();
        Path path = Path.of("config.properties");
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                config.load(reader);
            }
        }

        String type = config.getProperty("storage.type", "h2")
                .trim().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "h2" -> new Result(new H2Repository(), null);
            case "mysql" -> new Result(new MySqlRepository(config), null);
            case "demo" -> new Result(new DemoRepository(),
                    "Демонстрационный режим: данные сохраняются только до закрытия приложения.");
            default -> throw new IllegalArgumentException(
                    "Неизвестный storage.type: " + type + ". Допустимы h2, mysql, demo."
            );
        };
    }
}
