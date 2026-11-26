package com.industrialage.ia2.platform;

import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.WritingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.industrialage.ia2.config.ActiveConfig;
import com.mojang.brigadier.context.CommandContext;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Owns the strictly validated server configuration of the current saved world. */
public final class NeoForgeConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeoForgeConfig.class);
    private final String fileName;
    private final ModConfigSpec schema;
    private final AtomicReference<Session> active = new AtomicReference<>();
    private final ConfigFacade facade =
            () ->
                    Optional.ofNullable(active.get())
                            .flatMap(session -> session.publisher().current());

    /** Hooks the shared configuration schema and commands into the server lifecycle. */
    public NeoForgeConfig(ModContainer container, IEventBus gameBus) {
        fileName = container.getModId() + "-server.toml";
        var builder = new ModConfigSpec.Builder();
        for (var entry : ActiveConfig.entries()) {
            builder.comment(entry.comment())
                    .define(entry.path(), entry.defaultValue(), entry::accepts);
        }
        schema = builder.build();
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
        var keys = ActiveConfig.entries().stream().map(ActiveConfig.Entry::key).toList();
        var session = new Session(server, file, new ConfigFacade.Publisher(keys, snapshot -> {}));
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
                                                .requires(NeoForgeVersion::canReload)
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
        source.sendSuccess(() -> Component.literal("Reloaded IC2 configuration."), true);
        return 1;
    }

    private Optional<String> load(Session session, boolean createIfMissing) {
        try {
            if (createIfMissing && Files.notExists(session.file())) {
                createDefaults(session.file());
            }
            var source = Files.readString(session.file());
            var candidate = new TomlParser().parse(new StringReader(source));
            // This listener observes value corrections, not comment changes. A correction aborts
            // validation of this detached parse result, which is never written back to the file.
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
                        if (!createIfMissing) {
                            throw new IllegalArgumentException(detail);
                        }
                        LOGGER.warn(
                                "IC2 configuration at {}: {} (using default for this entry)",
                                session.file(),
                                detail);
                    });
            var values = new HashMap<String, Object>();
            for (var entry : ActiveConfig.entries()) {
                values.put(entry.key().name(), entry.resolve(candidate.get(entry.path())));
            }
            session.publisher().publish(values);
            LOGGER.info(
                    "Loaded {} IC2 configuration values from {}", values.size(), session.file());
            return Optional.empty();
        } catch (IOException
                | ParsingException
                | WritingException
                | IllegalArgumentException failure) {
            var reason =
                    Optional.ofNullable(failure.getMessage())
                            .orElse(failure.getClass().getSimpleName());
            LOGGER.warn("Rejected IC2 configuration at {} because {}", session.file(), reason);
            return Optional.of(reason);
        }
    }

    private void createDefaults(Path file) throws IOException {
        var defaults = TomlFormat.newConfig(LinkedHashMap::new);
        schema.correct(defaults);
        Files.createDirectories(file.getParent());
        var temporary = Files.createTempFile(file.getParent(), "ic2-config-", ".toml");
        try {
            try (var writer = Files.newBufferedWriter(temporary)) {
                new TomlWriter().write(defaults, writer);
            }
            Files.move(temporary, file);
        } catch (FileAlreadyExistsException concurrentCreation) {
            // Validate the file written by the other writer instead of replacing it.
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private record Session(MinecraftServer server, Path file, ConfigFacade.Publisher publisher) {}
}
