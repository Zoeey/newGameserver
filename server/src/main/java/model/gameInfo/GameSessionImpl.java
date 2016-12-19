package model.gameInfo;

import accountServer.authentification.Authentification;
import main.ApplicationContext;
import matchmaker.MatchMaker;
import model.authInfo.Leader;
import model.gameInfo.utils.*;
import model.gameObjects.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static model.gameInfo.GameConstants.MIN_MASS_FOR_ABILITIES;
import static model.gameInfo.GameConstants.EJECTED_DISTANCE;
import static model.gameInfo.GameConstants.EJECT_LOSE_MASS;
import static model.gameInfo.GameConstants.EJECTED_SPEED;
import static model.gameInfo.GameConstants.MAX_FOOD_ON_FIELD;

/**
 * @author apomosov
 */
public class GameSessionImpl implements GameSession {
    private final static Logger log = LogManager.getLogger(GameSessionImpl.class);
    private static final SequentialIDGenerator idGenerator = new SequentialIDGenerator();
    private final long id = idGenerator.next();
    @NotNull
    private final GameField field;
    @NotNull
    private final List<Player> players = new ArrayList<>();
    @NotNull
    private final FoodGenerator foodGenerator;
    @NotNull
    private final PlayerPlacer playerPlacer;
    @NotNull
    private final VirusGenerator virusGenerator;
    @NotNull
    private final List<EjectedMass> ejectedMasses = new ArrayList<>();

    public GameSessionImpl(@NotNull FoodGenerator foodGenerator, @NotNull PlayerPlacer playerPlacer, @NotNull VirusGenerator virusGenerator,
                           @NotNull GameField gameField) {
        this.foodGenerator = foodGenerator;
        this.playerPlacer = playerPlacer;
        this.virusGenerator = virusGenerator;
        field = gameField;
        //virusGenerator.generate();
        Thread foodGenerationTask = new Thread(foodGenerator);
        foodGenerationTask.start();
    }

    @Override
    public void offerNewLocation(Player player, double dx, double dy){
        if(player.cellsCount() < 2) {
            for (Cell cell : player.getCells()) {
                double newX = cell.getLocation().getX() +
                        Math.signum(dx - cell.getLocation().getX()) * Math.pow(dx - cell.getLocation().getX(), 2 / 3) *
                                cell.getSpeed() / GameConstants.SERVER_FPS;
                double newY = cell.getLocation().getY() +
                        Math.signum(dy - cell.getLocation().getY()) * Math.pow(dy - cell.getLocation().getY(), 2 / 3) *
                                cell.getSpeed() / GameConstants.SERVER_FPS;
                if (newX - cell.getRadius() < 0 || newX + cell.getRadius() > field.getWidth() ||
                        newY - cell.getRadius() < 0 || newY + cell.getRadius() > field.getHeight()) {
                    return;
                } else {
                    cell.setNewLocation(new Location(newX, newY));
                }
            }
        } else {
            Location[] newLocations = new Location[player.cellsCount()];
            int i=0;
            for (Cell cell : player.getCells()) {
                double newX = cell.getLocation().getX() +
                        Math.signum(dx - cell.getLocation().getX()) * Math.pow(dx - cell.getLocation().getX(), 2 / 3) *
                                cell.getSpeed() / GameConstants.SERVER_FPS;
                double newY = cell.getLocation().getY() +
                        Math.signum(dy - cell.getLocation().getY()) * Math.pow(dy - cell.getLocation().getY(), 2 / 3) *
                                cell.getSpeed() / GameConstants.SERVER_FPS;
                if (newX - cell.getRadius() < 0 || newX + cell.getRadius() > field.getWidth() ||
                        newY - cell.getRadius() < 0 || newY + cell.getRadius() > field.getHeight()) {
                    return;
                } else {
                    newLocations[i] = new Location(newX, newY);
                    i++;
                }
            }
            for(i=0; i < player.cellsCount()-1; i++) {
                for (int j = i + 1; j < player.cellsCount(); j++) {
                    double size1 = player.getCells().get(i).getRadius();
                    double size2 = player.getCells().get(j).getRadius();
                    if (newLocations[i].range(newLocations[j]) < (size1 + size2) ) {
                        int signX = (int) Math.signum(newLocations[j].getX() - newLocations[i].getX());
                        int signY = (int) Math.signum(newLocations[j].getY() - newLocations[i].getY());
                        double avgX = newLocations[i].getX() + (newLocations[j].getX() - newLocations[i].getX()) * size1 / (size1 + size2);
                        double avgY = newLocations[i].getY() + (newLocations[j].getY() - newLocations[i].getY()) * size1 / (size1 + size2);
                        newLocations[i].setLocation(avgX - signX * size1 , avgY - signY * size1 );
                        newLocations[j].setLocation(avgX + signX * size2 , avgY + signY * size2 );
                    }
                }
            }
            for(i = 0; i < newLocations.length; i++){
                player.getCells().get(i).setNewLocation(newLocations[i]);
            }
        }
    }

    @Override
    public void split(Player player, double dx,double dy){
        List<Integer> indexes= new ArrayList<>();
        int last = player.getCells().size();
        int i = 0;
        for(Cell cell : player.getCells()){
            if( cell.getMass() > MIN_MASS_FOR_ABILITIES){
                indexes.add(i);
                i++;
            }
        }
        i = 0;
        for(Integer index: indexes) {
            //player.getCells().get(index).setMass(player.getCells().get(index)/2);
            double newX = player.getCells().get(0).getLocation().getX() +
                    2*Math.signum(dx - player.getCells().get(0).getLocation().getX()) * Math.pow(dx - player.getCells().get(0).getLocation().getX(), 2 / 3);// *
            //player.getCells().get(0).getSpeed() / GameConstants.SERVER_FPS;
            double newY = player.getCells().get(0).getLocation().getY() +
                    2*Math.signum(dy - player.getCells().get(0).getLocation().getY()) * Math.pow(dy - player.getCells().get(0).getLocation().getY(), 2/3);//*
            //player.getCells().get(0).getSpeed() / GameConstants.SERVER_FPS;
            if(newX - player.getCells().get(0).getRadius() < 0 || newX + player.getCells().get(0).getRadius() > field.getWidth() ||
                    newY - player.getCells().get(0).getRadius() < 0 || newY + player.getCells().get(0).getRadius() > field.getHeight()) {
                return;
            } else {
                //player.addCell(new PlayerCell(1, new Location(newX, newY), player.getName()));
                player.addCell(new PlayerCell(last+i, new Location(dx, dy), player.getName()));
                i++;
            }
        }

    }

    @Override
    public void ejectMass(Player player,double dx,double dy){
        for(Cell cell: player.getCells()){
            if(cell.getMass() > MIN_MASS_FOR_ABILITIES){
                double distX = dx - cell.getLocation().getX();
                double distY = dy - cell.getLocation().getY();
                double mod = Math.sqrt(distX*distX + distY*distY);
                distX/=mod;
                distY/=mod;
                EjectedMass ejectedMass = new EjectedMass(cell.getLocation(),new Location(cell.getLocation().getX() + distX *EJECTED_DISTANCE,cell.getLocation().getY() + distY *EJECTED_DISTANCE ));
                ejectedMasses.add(ejectedMass);
                cell.setMass(cell.getMass() - EJECT_LOSE_MASS);
                break;
            }
        }
    }

    @Override
    public void tick() {
        for(Player player: players){
            move(player);
            checkFoodCollisions(player);
            updateEjectedMasses();
        }
    }

    @Override
    public List<EjectedMass> getEjectedMasses(){return ejectedMasses;}

    private void move(Player player){
        for(Cell cell: player.getCells()){
            cell.setLocation(cell.getNewLocation());
        }
    }

    private void checkFoodCollisions(Player player){
        for(Cell cell: player.getCells()) {
            double lowerX = cell.getLocation().getX() - cell.getRadius() * 10;
            double upperX = cell.getLocation().getX() + cell.getRadius() * 10;
            double lowerY = cell.getLocation().getY() - cell.getRadius() * 10;
            double upperY = cell.getLocation().getY() + cell.getRadius() * 10;
            for(Food food: field.getFoods()){
                double x = food.getLocation().getX();
                double y = food.getLocation().getY();
                if( x > lowerX && x < upperX && y > lowerY && y < upperY){
                    cell.setMass(cell.getMass() + GameConstants.FOOD_MASS);
                    field.getFoodsToRemove().add(food);
                    field.getFoods().remove(food);
                }
            }
        }
    }

    private void updateEjectedMasses(){
        //log.info("here we are");
        for(EjectedMass em: ejectedMasses){
            if(em.getLocation().range(em.getDistination()) > 0.1) {
                double newX = em.getLocation().getX() +
                        2 * Math.signum(em.getDistination().getX() - em.getLocation().getX()) * EJECTED_SPEED * GameConstants.SERVER_FPS;
                double newY = em.getLocation().getY() +
                        2 * Math.signum(em.getDistination().getY() - em.getLocation().getY()) * EJECTED_SPEED * GameConstants.SERVER_FPS;
                if (newX - em.getRadius() < 0 || newX + em.getRadius() > field.getWidth() ||
                        newY - em.getRadius() < 0 || newY + em.getRadius() > field.getHeight()) {
                    return;
                } else {
                    em.setNewLocation(new Location(newX, newY));
                }
            }
        }
    }

    @Override
    public void join(@NotNull Player player) {
        players.add(player);
        this.playerPlacer.place(player);
    }

    @Override
    public void leave(@NotNull Player player) {
        log.info("Player " + player.getName() + " leaved");
        ApplicationContext.get(MatchMaker.class).removePlayerSession(player.getId());
        Authentification.LB.updateScore(player.getId(),0);
        Authentification.tokenDAO.delete(Authentification.tokenDAO.getTokenByUserId(player.getId()));
        players.remove(player);
    }

    @Override
    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    @Override
    public GameField getField() {
        return field;
    }

    @Override
    public String toString() {
        return "GameSessionImpl{" +
                "id=" + id +
                '}';
    }
}
