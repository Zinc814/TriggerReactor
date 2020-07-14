package io.github.wysohn.triggerreactor.bukkit.tools;

import io.github.wysohn.triggerreactor.core.config.IConfigSource;
import io.github.wysohn.triggerreactor.core.config.IMigrationHelper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.Map;
import java.util.function.BiConsumer;

public class BukkitMigrationHelper implements IMigrationHelper {
    protected final FileConfiguration oldConfig;
    protected final File oldFile;

    public BukkitMigrationHelper(FileConfiguration oldConfig, File oldFile) {
        this.oldConfig = oldConfig;
        this.oldFile = oldFile;
    }

    protected void traversal(String parentNode, Map<String, Object> map, BiConsumer<String, Object> consumer) {
        map.forEach(((s, o) -> {
            if (o instanceof ConfigurationSection) {
                Map<String, Object> section = ((ConfigurationSection) o).getValues(false);
                if (parentNode == null) {
                    traversal(s, section, consumer);
                } else {
                    traversal(parentNode + "." + s, section, consumer);
                }
            } else {
                if (parentNode == null) {
                    consumer.accept(s, o);
                } else {
                    consumer.accept(parentNode + "." + s, o);
                }
            }
        }));
    }

    @Override
    public void migrate(IConfigSource current) {
        traversal(null, oldConfig.getValues(false), current::put);

        if (oldFile.exists())
            oldFile.renameTo(new File(oldFile.getParentFile(), oldFile.getName() + ".bak"));

        current.saveAll();
    }
}
