package io.github.merrg1n.beatorajafabric.mixin;

import io.github.merrg1n.beatorajafabric.api.BeatorajaGame;
import org.luaj.vm2.lib.jse.LuajavaLib;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LuajavaLib.class)
public abstract class LuajavaLibMixin {
    @Redirect(
            method = "classForName",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/ClassLoader;getSystemClassLoader()Ljava/lang/ClassLoader;"
            )
    )
    private ClassLoader getKnotClassLoader() {
        return BeatorajaGame.class.getClassLoader();
    }
}
