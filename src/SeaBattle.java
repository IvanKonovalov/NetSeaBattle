import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SeaBattle extends JFrame {
    //const
    public static final int PORT = 911;
    final int FIELD_SIZE = 10;
    final int AI_PANEL_SIZE = 400;
    final int AI_CELL_SIZE = AI_PANEL_SIZE / FIELD_SIZE;
    final int HUMAN_PANEL_SIZE = AI_PANEL_SIZE / 2;
    final int HUMAN_CELL_SIZE = HUMAN_PANEL_SIZE / FIELD_SIZE;
    private boolean isConnected = false;
    private boolean isActive = true;

    //networking
    ServerSocket serverSocket = null; // server
    Socket socket = null; // for client
    public static final String HOST = "localhost";
    InputStream in = null;
    OutputStream out = null;

    Canvas oponentPanel, hostPanel; // for game fields
    Ships oponentShips, hostShips; // set of human's and AI ships
    Shots hostShots, oponentShots; // set of shots from human and AI
    Random random;
    boolean gameOver;
    boolean alreadyPressed = false;

    public static void main (String[] Args) {
        new SeaBattle();
    }

    //main
    SeaBattle() {
        //MainFrame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        //Error page
        JFrame Error = new JFrame();
        Error.setResizable(false);
        Error.setSize(new Dimension(150,100));
        Error.setLayout(new BorderLayout());
        //Label
        JLabel errorLabel = new JLabel("Lost connection. Game will be closed.");
        //Button
        JButton errorOkay = new JButton("Okay");

        errorOkay.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        Error.add(errorLabel);
        Error.add(errorOkay, BorderLayout.SOUTH);
        Error.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        //Oponent BattleShip
        oponentPanel = new Canvas(); // panel for AI ships
        //oponentPanel.setSize(new Dimension(AI_PANEL_SIZE,AI_PANEL_SIZE));
        oponentPanel.setPreferredSize(new Dimension(AI_PANEL_SIZE, AI_PANEL_SIZE));
        oponentPanel.setBackground(Color.white);
        oponentPanel.setBorder(BorderFactory.createLineBorder(Color.blue));

        //Yours BattleShip
        hostPanel = new Canvas(); // panel for human ships
        //hostPanel.setSize(new Dimension(HUMAN_PANEL_SIZE, HUMAN_PANEL_SIZE));
        hostPanel.setPreferredSize(new Dimension(HUMAN_PANEL_SIZE, HUMAN_PANEL_SIZE));
        hostPanel.setBackground(new Color(255, 247, 229));
        hostPanel.setBorder(BorderFactory.createLineBorder(new Color(191, 172, 124)));

        //Buttons
        JButton randomizeButton = new JButton("Randomize");
        JButton connectButton = new JButton("Connect");
        JButton hostButton = new JButton("Host");
        JTextField ipHostTextField = new JTextField(9);
        JButton exit = new JButton("EXIT"); // exit game button

        ipHostTextField.setHorizontalAlignment(JTextField.CENTER);
        //ipHostTextField.setFont(new Font() );

        //Panels for buttons
        JPanel MenuPanel = new JPanel();
        MenuPanel.setLayout(new GridLayout(5,1));
        MenuPanel.add(randomizeButton);
        MenuPanel.add(ipHostTextField);
        MenuPanel.add(connectButton);
        MenuPanel.add(hostButton);
        MenuPanel.add(exit);

        JPanel shipPanel = new JPanel();
        shipPanel.setLayout(new GridLayout(2,1));
        shipPanel.add(hostPanel);
        shipPanel.add(MenuPanel);


        //Listeners
        oponentPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (!alreadyPressed) {
                    if (isActive) {
                        if(isConnected)
                            alreadyPressed = true;
                        super.mouseReleased(e);
                        int x = e.getX() / AI_CELL_SIZE; // coordinates transformation
                        int y = e.getY() / AI_CELL_SIZE;
                        if (!gameOver)
                            if (!hostShots.hitSamePlace(x, y)) {
                                hostShots.add(x, y, true);
                                oponentPanel.repaint();
                                System.out.println("repaint");
                                if (isConnected) {//если сетевая игра:
                                    int answer = -1;
                                    sendShoot(x, y);
                                    hostPanel.repaint();
                                    System.out.println("repaint");
                                    oponentPanel.repaint();
                                    try {
                                        System.out.print("Waiting answer >>");
                                        answer = in.read();
                                        System.out.println(answer);
                                        if (answer == 8) {
                                            Error.setVisible(true);
                                            setVisible(false);
                                        }
                                        if (answer == -2) {
                                            hostShots.setColor(x,y);
                                            hostPanel.repaint();
                                            oponentPanel.repaint();
                                            System.out.println("repaint");
                                            System.out.println("Game Over");
                                            gameOver = false;
                                            isActive = false;
                                            in.close();
                                            out.close();
                                            winner(true);
                                        }
                                        if (answer == 1) {
                                            hostShots.setColor(x,y);
                                            hostPanel.repaint();
                                            System.out.println("repaint");
                                            oponentPanel.repaint();
                                            alreadyPressed = false;
                                        }
                                        if (answer == 0) {
                                            System.out.println("Turn end");
                                            oponentPanel.repaint();
                                            hostPanel.repaint();
                                            System.out.println("repaint");
                                            alreadyPressed = false;
                                            //isActive = false;
                                            connectedOponentShots();
                                        }
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    }

                                } else if (oponentShips.checkHit(x, y)) { // human hit the target
                                    if (!oponentShips.checkSurvivors()) {
                                        System.out.println("FINISH");
                                        gameOver = true;
                                        winner(true);
                                        alreadyPressed = false;
                                    }
                                    isActive = true;
                                } else {
                                    isActive = false;
                                    alreadyPressed = false;
                                    oponentShots(); // human missed - AI will shoot
                                }

                                oponentPanel.repaint();
                                hostPanel.repaint();
                                System.out.println("repaint");
                                hostButton.setEnabled(false);
                                connectButton.setEnabled(false);
                            }
                    }
                }
            }
        });
        randomizeButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                init();
                hostPanel.repaint();
                oponentPanel.repaint();
                hostButton.setEnabled(true);
                connectButton.setEnabled(true);
                isActive = true;
                alreadyPressed = false;
            }
        });
        exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isConnected) {
                    try {
                        out.write(8);
                        out.flush();
                    }catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                System.exit(0);
            }
        });
        hostButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    serverSocket = new ServerSocket(PORT);

                    System.out.println("Waiting for connection");
                    Socket socket = serverSocket.accept();
                    System.out.println("Done:" + socket.getInetAddress());

                    in = socket.getInputStream();
                    out = socket.getOutputStream();

                } catch (IOException e1) {
                    e1.printStackTrace();
                } //catch (ClassNotFoundException e1) {
//                    e1.printStackTrace();
//                }
                hostButton.setEnabled(false);
                randomizeButton.setEnabled(false);
                connectButton.setEnabled(false);
                isConnected = true;
                isActive = true;
            }
        });
        connectButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    socket = new Socket(ipHostTextField.getText(), PORT);
                    in = socket.getInputStream();
                    out = socket.getOutputStream();
                    System.out.println("Done");
                } catch (IOException e1) {
                    e1.printStackTrace();
                    return;
                }
                hostButton.setEnabled(false);
                randomizeButton.setEnabled(false);
                connectButton.setEnabled(false);
                isConnected = true;
                isActive = false;
                connectedOponentShots();
            }
        });

        setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));
        add(oponentPanel);
        add(shipPanel);
        pack();
        setVisible(true);
        init();
    }

    void init() { // init all game object
        oponentShips = new Ships(FIELD_SIZE, AI_CELL_SIZE, true);
        hostShips = new Ships(FIELD_SIZE, HUMAN_CELL_SIZE, false);
        oponentShots = new Shots(HUMAN_CELL_SIZE);
        hostShots = new Shots(AI_CELL_SIZE);
        gameOver = false;
        random = new Random();
    }

    void winner(boolean win) {
        JFrame winnerMes = new JFrame();
        winnerMes.setSize(200,100);
        winnerMes.setResizable(false);
        winnerMes.setLayout(new BorderLayout());
        winnerMes.setLocationRelativeTo(null);
        winnerMes.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //Label
        JLabel errorLabel = new JLabel();
        errorLabel.setHorizontalAlignment(JLabel.CENTER);
        errorLabel.setHorizontalTextPosition(0);
        errorLabel.setVerticalAlignment(0);
        //Button
        JButton errorOkay = new JButton("Okay");
        errorOkay.setSize(new Dimension(50,15));
        errorOkay.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        if(win) {
            winnerMes.setTitle("You Won");
            errorLabel.setText("Congratulations, you win!");
        }
        else {
            winnerMes.setTitle("You Loose");
            errorLabel.setText("Sorry, you are pathetic!");
        }

        winnerMes.add(errorLabel);
        winnerMes.add(errorOkay, BorderLayout.SOUTH);
        winnerMes.setVisible(true);
        return;
    }

    //Ai shots
    void oponentShots() { // AI shoots randomly
        int x, y;
        do {
            x = random.nextInt(FIELD_SIZE);
            y = random.nextInt(FIELD_SIZE);
        } while (oponentShots.hitSamePlace(x, y));
        oponentShots.add(x, y, true);
        if (!hostShips.checkHit(x, y)) { // AI missed
            isActive = true;
            alreadyPressed = false;
            return;
        } else { // AI hit the target - AI can shoot again
            if (!hostShips.checkSurvivors()) {
                gameOver = true;
                winner(false);
            } else
                oponentShots();
        }
    }

    //send coordinates
    void sendShoot (int x,int y) {
        System.out.println("Send " +x+y);
        try {
            System.out.println("1");
            int coordinates = x*10+y;
            System.out.println("Send "+coordinates);
            out.write(coordinates);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //Player shots
    void connectedOponentShots() {
        hostPanel.repaint();
        oponentPanel.repaint();
        int x=-1, y=-1;
        //waiting for oponent's shot
        try {
            System.out.println("Waiting shots");
            int coordinates;
            coordinates = in.read();
            System.out.print("Receive coordinates");
            x = coordinates/10;
            y = coordinates%10;
            System.out.print("x:"+x +"y:"+y);
            if(x==-1 || y ==-1)
                throw new IOException("Cant read coordinates");
        } catch (IOException e) {
            e.printStackTrace();
        }
        oponentShots.add(x, y, true);
        hostPanel.repaint();
        oponentPanel.repaint();
        if (!hostShips.checkHit(x, y)) { // oponent missed
            //send answer: -2 - win, 0 - miss, 1 - hit
            try {
                out.write(0);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            isActive = true;
            alreadyPressed = false;
            return;
        } else { // oponent hit the target - can shoot again
            System.out.println("Repaint\nHit or shot");
            if (!hostShips.checkSurvivors()) {
                try {
                    out.write(-2);
                    out.flush();
                    in.close();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                gameOver = true;
                winner(false);
            } else {
                try {
                    out.write(1);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                connectedOponentShots();
            }
        }
    }


    //drawing
    class Canvas extends JPanel { // for painting
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            int cellSize = (int) getSize().getWidth() / FIELD_SIZE;
            g.setColor(Color.lightGray);
            for (int i = 1; i < FIELD_SIZE; i++) {
                g.drawLine(0, i*cellSize, FIELD_SIZE*cellSize, i*cellSize);
                g.drawLine(i*cellSize, 0, i*cellSize, FIELD_SIZE*cellSize);
            }
            if (cellSize == AI_CELL_SIZE) {
                hostShots.paint(g);
                oponentShips.paint(g);
            } else {
                oponentShots.paint(g);
                hostShips.paint(g);
            }
        }
    }
}

