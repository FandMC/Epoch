package cn.fandmc.epoch;

import cn.fandmc.epoch.config.ClientConfig;
import cn.fandmc.epoch.core.EpochClient;
import java.nio.file.Path;

/** The only process entry point. Services and runtime classes never declare main. */
public final class Epoch {
    private Epoch() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("Epoch starts the desktop client without command modes");
        ClientConfig config = ClientConfig.load(Path.of("run/config.json"));
        new EpochClient(config).run();
    }
}
