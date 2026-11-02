package ru.alchemap.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class RepositoryFactory {
    private RepositoryFactory() {}

    public record Result(AlchemyRepository repository, String warning) {}

    public static Result create() {
        Properties config = new Properties();
        Path path = Path.of("config.properties");
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                config.load(in);
            } catch (IOException ignored) {
            }
        }

        boolean enabled = Boolean.parseBoolean(config.getProperty("db.enabled", "false"));
        if (!enabled) {
            return new Result(new DemoRepository(), "MySQL отключён в config.properties. Используется демонстрационный режим.");
        }

        try {
            return new Result(new MySqlRepository(config), null);
        } catch (Exception ex) {
            String msg = "Не удалось подключиться к MySQL: " + ex.getMessage() + ". Запущен демонстрационный режим.";
            return new Result(new DemoRepository(), msg);
        }
    }
}
