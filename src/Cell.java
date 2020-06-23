import java.awt.*;

public class Cell {
    private final Color RED = Color.red;
    private int x, y;
    private Color color;

    Cell(int x, int y) {
        this.x = x;
        this.y = y;
        color = Color.black; // default color;
    }

    int getX() { return x; }
    int getY() { return y; }

    boolean checkHit(int x, int y) {
        if (this.x == x && this.y == y) {
            color = RED; // change color if hit
            return true;
        }
        return false;
    }
    void setColor(int x, int y) {
        if (this.x == x && this.y == y)
            color = RED;
    }

    boolean isAlive() {
        return color != RED; // judged by color
    }

    void paint(Graphics g, int cellSize, boolean hide) {
        if (!hide || (hide && color == RED)) {
            g.setColor(color);
            g.fill3DRect(x*cellSize + 1, y*cellSize + 1, cellSize - 2, cellSize - 2, true);
        }
    }
}