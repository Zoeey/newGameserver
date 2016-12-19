package clientConnection.packets;

import clientConnection.JSONHelper.JSONHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.jetbrains.annotations.NotNull;
import protocol.CommandReplicate;
import protocol.model.Cell;
import protocol.model.pEjectedMass;
import protocol.model.pFood;
import protocol.model.pVirus;

import java.io.IOException;

public class PacketReplicate {
    @NotNull
    private static final Logger log = LogManager.getLogger(PacketReplicate.class);
    @NotNull
    private final Cell[] cells;
    @NotNull
    private final pFood[] foodToAdd;
    @NotNull
    private final pFood[] foodToRemove;
    @NotNull
    private final pVirus[] virus;
    @NotNull
    private final pEjectedMass[] ejectedMasses;
    public PacketReplicate(@NotNull Cell[] cells, @NotNull pFood[] foodToAdd, @NotNull pFood[] foodToRemove,
                           @NotNull pVirus[] virus, @NotNull pEjectedMass[] em){
        this.cells = cells;
        this.foodToAdd = foodToAdd;
        this.foodToRemove = foodToRemove;
        this.virus = virus;
        this.ejectedMasses = em;
    }

    public void write(@NotNull Session session) throws IOException {
        String msg = JSONHelper.toJSON(new CommandReplicate(foodToAdd, foodToRemove, cells,virus,ejectedMasses));
        //log.info("Sending [" + msg + "]");
        session.getRemote().sendString(msg);
    }
}
