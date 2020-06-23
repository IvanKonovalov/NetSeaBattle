import java.awt.*;

public class Shot {
    private int x, y;
    private boolean shot;
    private boolean color=false;

    Shot(int x, int y, boolean shot) {
        this.x = x;
        this.y = y;
        this.shot = shot;
    }

    public void setColor(boolean color) {
        this.color = color;
    }

    int getX() { return x; }
    int getY() { return y; }
    boolean isShot() { return shot; }

    void paint(Graphics g, int cellSize) {
        if (color) {
            g.setColor(Color.red);
            if (shot)
                g.fillRect(x * cellSize, y * cellSize, 40, 40);
            else g.drawRect(x * cellSize, y * cellSize, 40, 40);
        } else {
            g.setColor(Color.gray);
            if (shot)
                g.fillRect(x * cellSize + cellSize / 2 - 3, y * cellSize + cellSize / 2 - 3, 8, 8);
            else g.drawRect(x * cellSize + cellSize / 2 - 3, y * cellSize + cellSize / 2 - 3, 8, 8);
        }
    }
}