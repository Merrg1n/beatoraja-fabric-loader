package io.github.merrg1n.beatorajafabric;

import io.github.merrg1n.beatorajafabric.patch.*;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.ContactInformationImpl;
import net.fabricmc.loader.impl.util.Arguments;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BeatorajaGameProvider implements GameProvider {
    private static final GameTransformer TRANSFORMER = new GameTransformer(
            new MainLoaderPatch(),
            new LuaJClassLoaderPatch()
    );

    /**
     * The entry point of the target, in this case it's "com.example.base.Launcher".
     */
    private static final String ENTRY_CLASS = "bms.player.beatoraja.MainLoader";

    /**
     * The class path for this mod.
     */
    private List<Path> classPath;

    /**
     * Fabric uses a wrapped version of the program arguments so it can take CLI args and strip them before it reaches
     * the target.
     */
    private Arguments arguments;

    /**
     * The version of game.
     */
    private String version;


    @Override
    public String getGameId() {
        return "beatoraja";
    }

    @Override
    public String getGameName() {
        return "beatoraja";
    }

    @Override
    public String getRawGameVersion() {
        return version;
    }

    @Override
    public String getNormalizedGameVersion() {
        return version;
    }

    @Override
    public Collection<BuiltinMod> getBuiltinMods() {
        HashMap<String, String> authorContactInformation = new HashMap<>();
        authorContactInformation.put("homepage", "https://github.com/exch-bms2");

        HashMap<String, String> modContactInformation = new HashMap<>();
        modContactInformation.put("homepage", "https://github.com/exch-bms2/beatoraja");
        modContactInformation.put("issues", "https://github.com/exch-bms2/beatoraja/issues");

        BuiltinModMetadata.Builder exampleMetadata = new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
                .setName(getGameName())
                .addAuthor("exch-bms2", authorContactInformation)
                .setContact(new ContactInformationImpl(modContactInformation))
                .setDescription("Cross-platform rhythm game based on Java and libGDX.");


        return Collections.singletonList(new BuiltinMod(classPath, exampleMetadata.build()));
    }

    @Override
    public String getEntrypoint() {
        return ENTRY_CLASS;
    }

    @Override
    public Path getLaunchDirectory() {
        try {
            return Paths.get(".").toRealPath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve launch dir", e);
        }
    }

    @Override
    public boolean isObfuscated() {
        return false;
    }

    @Override
    public boolean requiresUrlClassLoader() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean locateGame(FabricLauncher launcher, String[] args) {
        this.arguments = new Arguments();
        arguments.parse(args);

        classPath = new ArrayList<>();
        // This is a little messy and depends on the layout of this project, for a real provider write this in a way
        // that it can survive existing in production.
        CodeSource codeSource = BeatorajaGameProvider.class.getProtectionDomain().getCodeSource();
        Path codePath;
        try {
            codePath = Paths.get(codeSource.getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to find source of BeatorajaGameProvider?", e);
        }
        classPath.add(codePath);

        Path gamePath;
        try {
            gamePath = getLaunchDirectory().resolve("beatoraja.jar").toRealPath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to find beatoraja.jar", e);
        }
        classPath.add(gamePath);

        try {
            Path irPath = getLaunchDirectory().resolve("ir").toRealPath();
            try (Stream<Path> stream = Files.list(irPath)) {
                stream.forEach(classPath::add);
            }
        } catch (IOException e) {
            Log.warn(LogCategory.GAME_PROVIDER, "Failed to load beatoraja ir jar.", e);
        }

        version = BeatorajaVersionLookup.getVersion(gamePath);

        return true;
    }

    @Override
    public void initialize(FabricLauncher launcher) {
        List<Path> parentClassPath = Stream.of(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(Paths::get)
                .map((path) -> {
                    try {
                        return path.toRealPath();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to get real path of " + path, e);
                    }
                })
                .filter((path) -> !classPath.contains(path))
                .collect(Collectors.toList());

        if (System.getProperty("java.version").startsWith("1.8")) // JAVA 8
        {
            String javaHome = System.getProperty("java.home");
            Path jfxrtPath = Paths.get(javaHome, "lib", "ext", "jfxrt.jar");
            parentClassPath.add(jfxrtPath);
        }

        launcher.setValidParentClassPath(parentClassPath);


        TRANSFORMER.locateEntrypoints(launcher, classPath);
    }

    @Override
    public GameTransformer getEntrypointTransformer() {
        return TRANSFORMER;
    }

    @Override
    public void unlockClassPath(FabricLauncher launcher) {
        classPath.forEach(launcher::addToClassPath);
    }

    @Override
    public void launch(ClassLoader loader) {
        String targetClass = getEntrypoint();

        MethodHandle invoker;
        try {
            Class<?> target = loader.loadClass(targetClass);
            invoker = MethodHandles.lookup().findStatic(target, "main", MethodType.methodType(void.class, String[].class));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Failed to find entry point", e);
        }

        try {
            // Idea doesn't understand that this is a polymorphic method.
            //noinspection ConfusingArgumentToVarargsMethod
            invoker.invokeExact(arguments.toArray());
        } catch (Throwable e) {
            throw new RuntimeException("The game has crashed!", e);
        }
    }

    @Override
    public Arguments getArguments() {
        return arguments;
    }

    @Override
    public String[] getLaunchArguments(boolean sanitize) {
        return arguments.toArray();
    }
}
