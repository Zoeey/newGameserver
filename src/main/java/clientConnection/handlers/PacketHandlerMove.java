package clientConnection.handlers;

import clientConnection.JSONHelper.JSONDeserializationException;
import clientConnection.JSONHelper.JSONHelper;
import org.eclipse.jetty.websocket.api.Session;
import org.jetbrains.annotations.NotNull;
import protocol.CommandMove;


public class PacketHandlerMove {
    public PacketHandlerMove(@NotNull Session session, @NotNull String json) {
        CommandMove commandMove;
        try {
            commandMove = JSONHelper.fromJSON(json, CommandMove.class);
        } catch (JSONDeserializationException e) {
            e.printStackTrace();
            return;
        }
        //TODO
    }
}
