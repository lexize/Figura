package org.figuramc.figura.mixin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.server.CustomPayloadCodec;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net/minecraft/network/protocol/common/custom/CustomPacketPayload$1")
public class CustomPayloadCodecMixin {
    @Inject(method = "findCodec", at = @At("HEAD"), cancellable = true)
    private void onFindCodec(ResourceLocation id, CallbackInfoReturnable<StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload>> cir) {
        Identifier ident = new Identifier(id.getNamespace(), id.getPath());
        System.out.println("Ident is %s".formatted(ident));
        Packet.Deserializer deserializer = Packet.PACKETS.get(ident);
        if (deserializer != null) {
            System.out.println("Found deserializer");
            cir.setReturnValue(new CustomPayloadCodec(deserializer));
        }
    }
}
