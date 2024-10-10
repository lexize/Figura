package org.figuramc.figura.mixin.server;

import net.minecraft.server.MinecraftServer;
import org.figuramc.figura.FiguraCommonServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Unique
    private FiguraCommonServer figura$fsb;
    @Inject(method = "runServer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;buildServerStatus()Lnet/minecraft/network/protocol/status/ServerStatus;", ordinal = 0))
    private void onServerStart(CallbackInfo ci) {
        figura$fsb = new FiguraCommonServer((MinecraftServer) (Object)this);
        FiguraCommonServer.LOGGER.info("Initializing FSB");
        figura$fsb.init();
        FiguraCommonServer.LOGGER.info("FSB initialization complete.");
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void onServerStop(CallbackInfo ci) {
        FiguraCommonServer.LOGGER.info("Closing FSB");
        figura$fsb.close();
        FiguraCommonServer.LOGGER.info("FSB successfully closed");
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void onTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        figura$fsb.tick();
    }
}
