package io.github.tt432.ccaforge.net;

import dev.onyxstudios.cca.internal.entity.CcaEntityClientNw;
import io.github.tt432.ccaforge.Ccaforge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * @author DustW
 */
public class CcaNetHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Ccaforge.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id;

    public static void init() {
        INSTANCE.registerMessage(id++, ServerPacket.class,
                (msg, buf) -> buf.writeBytes(msg.buf),
                ServerPacket::new,
                (msg, contentSup) -> {
                    NetworkEvent.Context context = contentSup.get();

                    CcaEntityClientNw.handlerPacket(msg.buf);

                    context.setPacketHandled(true);
                });
    }

    public static class ServerPacket {
        public FriendlyByteBuf buf;

        public ServerPacket(FriendlyByteBuf buf) {
            this.buf = buf;
        }
    }

    public static <M> void sendToClient(ServerPlayer player, M packet) {
        INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}
