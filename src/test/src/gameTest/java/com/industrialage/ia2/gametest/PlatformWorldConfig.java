package com.industrialage.ia2.gametest;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.industrialage.ia2.config.ActiveConfig;
import com.industrialage.ia2.platform.ConfigFacade;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

final class PlatformWorldConfig {
    private static final String RATIO = "energyRetainedInStorageBlockDrops";
    private static final String HEAT = "hU_per_run";
    private static final ConfigFacade.Key<Double> KEEP =
            ActiveConfig.scalarKey("balance/" + RATIO, Double.class);
    private static final ConfigFacade.Key<Integer> HU =
            ActiveConfig.scalarKey("balance/fermenter/" + HEAT, Integer.class);

    private PlatformWorldConfig() {}

    static void verify(String phase, MinecraftServer server, ConfigFacade config)
            throws IOException {
        var file = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/ic2-server.toml");
        var trace = new ArrayList<String>();
        var initial = config.current().orElseThrow();
        if ("write".equals(phase)) {
            reading(trace, "defaults", initial, 0.8, 4000);
            var original = Files.readString(file);
            var parsed = new TomlParser().parse(original);
            var names = new HashSet<String>();
            collectLeaves(parsed, "", names);
            for (var entry : ActiveConfig.entries()) {
                var expected = entry.defaultValue();
                if (!names.remove(entry.key().name())
                        || !expected.equals(initial.get(entry.key()))
                        || !expected.equals(entry.resolve(parsed.get(entry.path())))) {
                    throw new AssertionError(
                            "Incorrect generated default for " + entry.key().name());
                }
            }
            if (!names.isEmpty() || ActiveConfig.entries().size() != 58) {
                throw new AssertionError("Unexpected generated keys " + names);
            }
            trace.add("58 generated defaults and snapshot values");
            var candidate = set(set(original, RATIO, "0.25"), HEAT, "5000");
            Files.writeString(file, candidate);
            expectCommand(server, 0, false);
            reading(trace, "denied-permission-0", config.current().orElseThrow(), 0.8, 4000);
            expectCommand(server, 2, true);
            var valid = config.current().orElseThrow();
            reading(trace, "reloaded", valid, 0.25, 5000);
            reading(trace, "original-snapshot", initial, 0.8, 4000);
            Files.writeString(file, set(set(candidate, RATIO, "1.5"), HEAT, "6000"));
            expectCommand(server, 2, false);
            reading(trace, "rejected-candidate", config.current().orElseThrow(), 0.25, 5000);
            reading(trace, "retained-snapshot", valid, 0.25, 5000);
            record Invalid(String name, String toml) {}
            for (var invalid :
                    List.of(
                            new Invalid(
                                    "missing",
                                    candidate.replaceFirst(
                                            "(?m)^\\h*\"?hU_per_run\"?\\h*=.*$", "")),
                            new Invalid("unknown", candidate + "\nunknown_qa_key = true\n"),
                            new Invalid(
                                    "duplicate", set(candidate, HEAT, "5000\nhU_per_run = 5000")),
                            new Invalid("malformed", "[broken"),
                            new Invalid("bool-string", set(candidate, "wrenchLogging", "\"true\"")),
                            new Invalid("integer-overflow", set(candidate, HEAT, "2147483648")),
                            new Invalid("nonfinite", set(candidate, RATIO, "nan")),
                            new Invalid(
                                    "list-element", set(candidate, "recyclerWhitelist", "[15]")),
                            new Invalid(
                                    "list-prefix",
                                    set(candidate, "recyclerWhitelist", "[\"Other:ic2:test\"]")))) {
                Files.writeString(file, invalid.toml());
                expectCommand(server, 2, false);
                reading(
                        trace,
                        "rejected-" + invalid.name(),
                        config.current().orElseThrow(),
                        0.25,
                        5000);
            }
            Files.writeString(file, set(set(candidate, RATIO, "0.5"), HEAT, "6000"));
            expectCommand(server, 2, true);
            reading(trace, "recovered", config.current().orElseThrow(), 0.5, 6000);
            reading(trace, "previous-snapshot", valid, 0.25, 5000);
        } else {
            reading(trace, "reopened", initial, 0.5, 6000);
        }
        Files.write(Path.of("config-" + phase + ".txt"), trace);
    }

    private static void collectLeaves(UnmodifiableConfig config, String prefix, Set<String> names) {
        for (var entry : config.entrySet()) {
            String name = prefix + entry.getKey();
            if (entry.getValue() instanceof UnmodifiableConfig section) {
                collectLeaves(section, name + "/", names);
            } else {
                names.add(name);
            }
        }
    }

    private static void reading(
            List<String> trace,
            String stage,
            ConfigFacade.Snapshot snapshot,
            double expectedRatio,
            int expectedHeat) {
        double ratio = snapshot.get(KEEP);
        int heat = snapshot.get(HU);
        if (ratio != expectedRatio || heat != expectedHeat) {
            throw new AssertionError(stage + " read " + ratio + "/" + heat);
        }
        trace.add(stage + " " + ratio + "/" + heat);
    }

    private static String set(String toml, String leaf, String value) {
        var key =
                Pattern.compile("(?m)^(\\h*\"?" + Pattern.quote(leaf) + "\"?\\h*=\\h*)[^#\\r\\n]+");
        var match = key.matcher(toml);
        if (!match.find()) {
            throw new AssertionError("Missing TOML fixture key " + leaf);
        }
        String assignment = match.group(1) + value;
        if (match.find()) {
            throw new AssertionError("Ambiguous TOML fixture key " + leaf);
        }
        return match.replaceFirst(Matcher.quoteReplacement(assignment));
    }

    private static void expectCommand(MinecraftServer server, int permission, boolean expected) {
        var source = server.createCommandSourceStack().withPermission(permission);
        try {
            int result = server.getCommands().getDispatcher().execute("ic2 config reload", source);
            if ((result > 0) != expected) {
                throw new AssertionError(
                        "Unexpected reload result at permission " + permission + ": " + result);
            }
        } catch (CommandSyntaxException e) {
            if (expected) {
                throw new AssertionError("Authorized reload failed", e);
            }
        }
    }
}
