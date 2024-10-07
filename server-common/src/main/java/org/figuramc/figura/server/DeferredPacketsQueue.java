package org.figuramc.figura.server;

import org.figuramc.figura.server.events.Events;
import org.figuramc.figura.server.events.packets.DeferredPacketErrorEvent;
import org.figuramc.figura.server.packets.Packet;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DeferredPacketsQueue {
    private final FiguraServer parent;
    private final HashMap<UUID, LinkedList<CompletableFuture<? extends Packet>>> queues = new HashMap<>();

    public DeferredPacketsQueue(FiguraServer parent) {
        this.parent = parent;
    }

    public void sendPacket(UUID receiver, CompletableFuture<? extends Packet> futurePacket) {
        queues.computeIfAbsent(receiver, u -> new LinkedList<>()).add(futurePacket);
    }

    void tick() {

    }

    private boolean completedAndSent(UUID receiver, CompletableFuture<Packet> packet) {
        if (packet.isDone()) {
            try {
                Packet p = packet.join();
                parent.sendPacket(receiver, p);
            } catch (Exception e) {
                Events.call(new DeferredPacketErrorEvent(e));
            }
            return true;
        }
        return false;
    }
}
