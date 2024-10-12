package org.figuramc.figura.mixin.network.server;

import net.minecraft.server.MinecraftServer;
import org.figuramc.figura.server.FiguraModServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Unique
    private FiguraModServer figura$fsb;
    @Inject(method = "runServer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;buildServerStatus()Lnet/minecraft/network/protocol/status/ServerStatus;", ordinal = 0))
    private void onServerStart(CallbackInfo ci) {
        figura$fsb = new FiguraModServer((MinecraftServer) (Object)this);
        figura$fsb.init();
    }

    @Inject(method = "stopServer", at = @At("TAIL"))
    private void onServerStop(CallbackInfo ci) {
        figura$fsb.close();
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void onTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        figura$fsb.tick();
    }
}
