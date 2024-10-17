package org.figuramc.figura.mixin.network;

import com.google.common.collect.Lists;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.server.CustomPayloadCodec;
import org.figuramc.figura.server.PayloadWrapper;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.server.utils.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(ClientboundCustomPayloadPacket.class)
public class ClientboundCustomPayloadMixin {

    //@Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;"))
    //private static ArrayList<CustomPacketPayload.TypeAndCodec<?,?>> collectCustomPayloads(Object[] elements) {
    //    CustomPacketPayload.TypeAndCodec<?,?>[] elems = (CustomPacketPayload.TypeAndCodec<?,?>[]) elements;
    //    CustomPacketPayload.TypeAndCodec<?,?>[] newElements = new CustomPacketPayload.TypeAndCodec[elems.length + Packet.PACKETS.size()];
    //    System.arraycopy(elems, 0, newElements, 0, elems.length);
    //    int i = elems.length;
    //    for (var pair: Packet.PACKETS) {
    //        var id = pair.left();
    //        var type = new CustomPacketPayload.Type<PayloadWrapper>(new ResourceLocation(id.namespace(), id.path()));
    //        var codec = new CustomPayloadCodec(pair.right());
    //        newElements[i] = new CustomPacketPayload.TypeAndCodec<>(type, codec);
    //        i++;
    //    }
    //    return Lists.newArrayList(newElements);
    //}

    //@Inject(method = "method_56461", at = @At("HEAD"), cancellable = true)
    //private static void onReadPayload(ResourceLocation payload, CallbackInfoReturnable<StreamCodec<RegistryFriendlyByteBuf, CustomPacketPayload>> cir) {
    //    var ident = new Identifier(payload.getNamespace(), payload.getPath());
    //    for (Pair<Identifier, Packet.Deserializer> pair: Packet.PACKETS) {
    //        if (pair.left().equals(ident)) {
    //            var deserializer = pair.right();
    //            cir.setReturnValue( new ClientboundStreamCodec(deserializer));
    //            return;
    //        }
    //    }
    //    cir.setReturnValue((StreamCodec<RegistryFriendlyByteBuf, CustomPacketPayload>) (Object) DiscardedPayload.codec(payload, 1048576));
    //}
}
