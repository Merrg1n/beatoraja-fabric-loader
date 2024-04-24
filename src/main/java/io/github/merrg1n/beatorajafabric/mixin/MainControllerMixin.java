package io.github.merrg1n.beatorajafabric.mixin;

import bms.player.beatoraja.BMSPlayerMode;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.PlayerConfig;
import io.github.merrg1n.beatorajafabric.api.BeatorajaGame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@Mixin(MainController.class)
public class MainControllerMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onCreate(Path par1, Config par2, PlayerConfig par3, BMSPlayerMode par4, boolean par5, CallbackInfo ci) {
        BeatorajaGame.getInstance().setController((MainController) (Object) this);
    }
}
