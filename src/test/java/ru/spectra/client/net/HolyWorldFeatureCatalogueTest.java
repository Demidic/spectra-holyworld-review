package ru.spectra.client.net;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Builds moderator material from compiled registrations, without booting Minecraft. */
final class HolyWorldFeatureCatalogueTest {
    private static ClassNode read(String name) throws Exception {
        try (InputStream stream = HolyWorldFeatureCatalogueTest.class.getClassLoader()
                .getResourceAsStream(name + ".class")) {
            assertNotNull(stream, name);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static String moduleName(String type) throws Exception {
        for (MethodNode constructor : read(type).methods) {
            if (!constructor.name.equals("<init>")) {
                continue;
            }
            String name = null;
            for (AbstractInsnNode instruction : constructor.instructions) {
                if (instruction instanceof LdcInsnNode constant && constant.cst instanceof String text) {
                    name = text;
                }
                if (instruction instanceof MethodInsnNode call && call.name.equals("<init>")
                        && (call.owner.equals("ru/spectra/client/module/Module")
                        || call.owner.equals("ru/spectra/client/module/HudModules$WidgetModule"))) {
                    assertNotNull(name, type);
                    return name;
                }
            }
        }
        fail("Module has no statically auditable English name: " + type);
        return null;
    }

    @Test
    void everyRegisteredModuleAndCommandHasAUniqueStableIdAndAnExportEntry() throws Exception {
        Set<String> types = new TreeSet<>();
        for (MethodNode method : read("ru/spectra/client/util/ModuleProvider").methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                    for (Object argument : dynamic.bsmArgs) {
                        if (argument instanceof Handle handle && handle.getName().equals("<init>")
                                && handle.getOwner().startsWith("ru/spectra/client/module/")) {
                            types.add(handle.getOwner());
                        }
                    }
                }
            }
        }
        assertTrue(types.size() > 40, "Unexpectedly incomplete catalogue");
        // Do not mistake the menu registrations for a complete source audit.
        // Every concrete module compiled into the client must be in this catalogue.
        Path compiled = Path.of("build/classes/java/main");
        try (var paths = Files.walk(compiled.resolve("ru/spectra/client"))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                ClassNode node = new ClassNode();
                new ClassReader(Files.readAllBytes(path)).accept(node,
                        ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                if ((node.access & org.objectweb.asm.Opcodes.ACC_ABSTRACT) == 0
                        && !node.name.equals("ru/spectra/client/module/Module")) {
                    String parent = node.superName;
                    while (parent != null && parent.startsWith("ru/spectra/client/")) {
                        if (parent.equals("ru/spectra/client/module/Module")) {
                            assertTrue(types.contains(node.name), "Unregistered compiled module: " + node.name);
                            break;
                        }
                        parent = read(parent).superName;
                    }
                }
                for (MethodNode method : node.methods) {
                    for (AbstractInsnNode instruction : method.instructions) {
                        if (instruction instanceof TypeInsnNode allocation
                                && allocation.getOpcode() == org.objectweb.asm.Opcodes.NEW) {
                            assertNotEquals("ru/spectra/client/module/Module", allocation.desc,
                                    "Unregistered base Module instance in " + node.name);
                        }
                    }
                }
            }
        }
        SortedMap<String, JsonObject> entries = new TreeMap<>();
        for (String type : types) {
            String name = moduleName(type);
            String id = HolyWorldFeaturePolicy.featureId(name);
            JsonObject entry = new JsonObject();
            entry.addProperty("id", id);
            entry.addProperty("name", name);
            entry.addProperty("kind", "module");
            entry.addProperty("source", "src/main/java/" + type.replace('$', '/') + ".java");
            if (type.contains("$")) {
                entry.addProperty("source", "src/main/java/" + type.substring(0, type.indexOf('$')) + ".java");
            }
            assertNull(entries.put(id, entry), "Duplicate module ID: " + id);
        }
        Set<String> commands = new TreeSet<>();
        for (MethodNode method : read("ru/spectra/client/Spectra").methods) {
            if (!method.name.equals("registerCommands")) {
                continue;
            }
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof TypeInsnNode type
                        && type.getOpcode() == org.objectweb.asm.Opcodes.NEW
                        && type.desc.startsWith("ru/spectra/client/command/")) {
                    commands.add(type.desc);
                }
            }
        }
        assertEquals(6, commands.size());
        Map<String, String> commandIds = Map.of("macro", "macros", "waypoint", "waypoints",
                "friend", "friends", "rct", "reconnect", "bind", "binds", "prefix", "commandprefix");
        for (String type : commands) {
            String name = null;
            for (MethodNode method : read(type).methods) {
                if (method.name.equals("getName")) {
                    for (AbstractInsnNode instruction : method.instructions) {
                        if (instruction instanceof LdcInsnNode literal && literal.cst instanceof String text) {
                            name = text;
                            break;
                        }
                    }
                }
            }
            assertNotNull(name, type);
            String id = commandIds.get(name);
            assertNotNull(id, "Update catalogue and ClientCommand.getFeatureId: " + name);
            JsonObject entry = new JsonObject();
            entry.addProperty("id", id);
            entry.addProperty("name", "." + name);
            entry.addProperty("kind", "command");
            entry.addProperty("source", "src/main/java/" + type + ".java");
            assertNull(entries.put(id, entry), id);
        }
        JsonObject dropAll = new JsonObject();
        dropAll.addProperty("id", "dropall");
        dropAll.addProperty("name", "Drop matching items (Ctrl+Shift+Q)");
        dropAll.addProperty("kind", "inventory hotkey");
        dropAll.addProperty("source", "src/main/java/ru/spectra/client/util/DropAllHandler.java");
        assertNull(entries.put("dropall", dropAll));
        assertFalse(entries.containsKey("seeinvisibles"));
        JsonObject catalogue = new JsonObject();
        catalogue.addProperty("client", "spectra");
        catalogue.addProperty("channel", "liteapi:feature-control");
        catalogue.addProperty("method", "checkFeatures");
        JsonArray features = new JsonArray();
        entries.values().forEach(features::add);
        catalogue.add("features", features);
        Path output = Path.of("build/reports/holyworld/features.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(catalogue));
    }
}
