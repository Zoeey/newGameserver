package protocol.model;

/**
 * @author apomosov
 */
public  class Cell {
  private final int cellId;
  private final int playerId;
  private final float size;
  private double x;
  private double y;

  public Cell(int cellId, int playerId, float size, double x, double y) {
    this.cellId = cellId;
    this.playerId = playerId;
    this.size = size;
    this.x = x;
    this.y = y;
  }

  public int getPlayerId() {
    return playerId;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public int getCellId() {
    return cellId;
  }

  public float getSize() {
    return size;
  }
}
