package io.github.merrg1n.beatorajafabric.mixin;

import bms.player.beatoraja.*;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.merrg1n.beatorajafabric.api.BeatorajaGame;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MainLoader.class)
public abstract class MainLoaderMixin {
    @Inject(
            method = "main",
            at = @At("HEAD")
    )
    private static void main_modInit(String[] args, CallbackInfo ci) {
        FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
        loader.prepareModInit(FabricLoader.getInstance().getGameDir(), BeatorajaGame.getInstance());
        loader.invokeEntrypoints("main", ModInitializer.class, ModInitializer::onInitialize);
    }

    @Redirect(
            method = "play",
            at = @At(value = "INVOKE", target = "Lbms/player/beatoraja/MainController;getVersion()Ljava/lang/String;")
    )
    private static String main_changeTitle() {
        return BeatorajaGame.getInstance().getTitle();
    }
}
