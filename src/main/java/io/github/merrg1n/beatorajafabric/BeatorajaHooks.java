package io.github.merrg1n.beatorajafabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

public final class BeatorajaHooks {
    public static final String INTERNAL_NAME = BeatorajaHooks.class.getName().replace('.', '/');

    public static void init() {
        FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
        loader.prepareModInit(loader.getGameDir(), loader.getGameInstance());
        loader.invokeEntrypoints("main", ModInitializer.class, ModInitializer::onInitialize);
        loader.invokeEntrypoints("client", ClientModInitializer.class, ClientModInitializer::onInitializeClient);
        // NOTICE: beatoraja doesn't have server environment, this initialization is just to ensure compatibility.
        loader.invokeEntrypoints("server", DedicatedServerModInitializer.class, DedicatedServerModInitializer::onInitializeServer);
    }

    public static String insertBranding(final String brand) {
        String fabricBrand = "(Modded / fabric " + FabricLoaderImpl.VERSION + ")";
        if (brand == null || brand.isEmpty()) {
            Log.warn(LogCategory.GAME_PROVIDER, "Null or empty branding found!", new IllegalStateException());
            return fabricBrand;
        }

        return brand + " " + fabricBrand;
    }
}
