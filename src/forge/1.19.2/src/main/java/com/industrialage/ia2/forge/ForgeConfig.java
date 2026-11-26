package com.industrialage.ia2.forge;

import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.WritingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.industrialage.ia2.config.ActiveConfig;
import com.industrialage.ia2.config.WorldConfigFile;
import com.industrialage.ia2.platform.ConfigFacade;
import com.mojang.brigadier.context.CommandContext;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Owns the strictly validated server configuration of the current saved world. */
public final class ForgeConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeConfig.class);
    private final String fileName;
    private final Format format;
    private final AtomicReference<Session> active = new AtomicReference<>();
    private final ConfigFacade facade =
            () -> Optional.ofNullable(active.get()).flatMap(session -> session.file().current());

    /** Hooks the shared configuration schema and commands into the server lifecycle. */
    public ForgeConfig(ModContainer container, IEventBus gameBus) {
        fileName = container.getModId() + "-server.toml";
        var builder = new ForgeConfigSpec.Builder();
        for (var entry : ActiveConfig.entries()) {
            builder.comment(entry.comment())
                    .define(entry.path(), entry.defaultValue(), entry::accepts);
        }
        format = new Format(builder.build());
        gameBus.addListener(this::start);
        gameBus.addListener(this::stop);
        gameBus.addListener(this::registerCommands);
    }

    /** {@return a stable read-only facade for the active session's configuration} */
    public ConfigFacade facade() {
        return facade;
    }

    private void start(ServerAboutToStartEvent event) {
        var server = event.getServer();
        var file =
                server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve(fileName);
        var session = new Session(server, new WorldConfigFile(file, format));
        active.set(session);
        load(session, true);
    }

    private void stop(ServerStoppedEvent event) {
        active.updateAndGet(
                session ->
                        session != null && session.server() == event.getServer() ? null : session);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher()
                .register(
                        Commands.literal("ic2")
                                .then(
                                        Commands.literal("config")
                                                .requires(source -> source.hasPermission(2))
                                                .then(
                                                        Commands.literal("reload")
                                                                .executes(this::reload))));
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var session = active.get();
        if (session == null || session.server() != source.getServer()) {
            source.sendFailure(Component.literal("No IC2 configuration is active for this world."));
            return 0;
        }
        var error = load(session, false);
        if (error.isPresent()) {
            source.sendFailure(Component.literal("IC2 configuration rejected. " + error.get()));
            return 0;
        }
        source.sendSuccess(Component.literal("Reloaded IC2 configuration."), true);
        return 1;
    }

    private Optional<String> load(Session session, boolean createIfMissing) {
        try {
            format.allowFallback(createIfMissing);
            session.file().load(createIfMissing);
            LOGGER.info(
                    "Loaded {} IC2 configuration values from {}",
                    ActiveConfig.entries().size(),
                    session.file().path());
            return Optional.empty();
        } catch (IOException
                | ParsingException
                | WritingException
                | IllegalArgumentException failure) {
            var reason =
                    Optional.ofNullable(failure.getMessage())
                            .orElse(failure.getClass().getSimpleName());
            LOGGER.warn(
                    "Rejected IC2 configuration at {} because {}", session.file().path(), reason);
            return Optional.of(reason);
        } finally {
            format.allowFallback(false);
        }
    }

    private static final class Format implements WorldConfigFile.Format {
        private final ForgeConfigSpec schema;
        private boolean fallback;

        private Format(ForgeConfigSpec schema) {
            this.schema = schema;
        }

        private void allowFallback(boolean allow) {
            fallback = allow;
        }

        @Override
        public Function<List<String>, Object> read(Reader text) {
            var candidate = new TomlParser().parse(text);
            // This listener observes value corrections, not comment changes. A correction aborts
            // validation on explicit reload; on initial startup, bad entries fall back to defaults.
            schema.correct(
                    candidate,
                    (action, path, original, replacement) -> {
                        var message =
                                switch (action) {
                                    case ADD -> "Missing configuration key ";
                                    case REPLACE -> "Invalid configuration value at ";
                                    case REMOVE -> "Unknown configuration key ";
                                };
                        String detail = message + String.join("/", path);
                        if (!fallback) {
                            throw new IllegalArgumentException(detail);
                        }
                        LOGGER.warn("IC2 configuration: {} (using default for this entry)", detail);
                    });
            return candidate::get;
        }

        @Override
        public void writeDefaults(Writer writer) {
            var defaults = TomlFormat.newConfig(LinkedHashMap::new);
            schema.correct(defaults);
            new TomlWriter().write(defaults, writer);
        }
    }

    private record Session(MinecraftServer server, WorldConfigFile file) {}
}
