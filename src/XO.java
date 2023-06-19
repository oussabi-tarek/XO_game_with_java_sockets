import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

// classe qui permet de jouer au jeu XO avec un autre joueur en reseau  , qui a lance le jeu est le serveur et l'autre est le client
// qui a lance le defi va avoir le premier tour et le "X" le client c'est le "O"
public class XO implements Runnable{
    private String ip = "localhost";
    private String player1;
    private String player2;
    private int port ;
    private Scanner scanner = new Scanner(System.in);
    private JFrame frame;
    private final int WIDTH = 506;
    private final int HEIGHT = 527;
    private Thread thread;
    private String role;

    private Painter painter;
    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;

    private ServerSocket serverSocket;

    private BufferedImage board;
    private BufferedImage redX;
    private BufferedImage blueX;
    private BufferedImage redCircle;
    private BufferedImage blueCircle;

    private String[] spaces = new String[9];

    private boolean yourTurn = false;
    private boolean circle = true;
    private boolean accepted = false;
    private boolean unableToCommunicateWithOpponent = false;
    private boolean won = false;
    private boolean enemyWon = false;
    private boolean tie = false;

    private int lengthOfSpace = 160;
    private int errors = 0;
    private int firstSpot = -1;
    private int secondSpot = -1;

    private Font font = new Font("Verdana", Font.BOLD, 32);
    private Font smallerFont = new Font("Verdana", Font.BOLD, 20);
    private Font largerFont = new Font("Verdana", Font.BOLD, 50);

    private String waitingString = "En attente d'un autre joueur";
    private String unableToCommunicateWithOpponentString = "Impossible de communiquer avec l'adversaire.";
    private String wonString = "Tu as gagné!";
    private String enemyWonString = "L'adversaire a gagné !";
    private String tieString = "Le match s'est terminé par une égalité.";

    // specify the wins combinations
    private int[][] wins = new int[][] { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, { 0, 4, 8 }, { 2, 4, 6 } };


    // constructor for server
    public XO(int port,String player) {
        this.port=port;

        loadImages();

        painter = new Painter();
        painter.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        // if the player is not connected to the server , he will be the server
        if (!connect()) {
            role= "server";
            player1= player;
            player2="Waiting for another player";
            initializeServer();
        }



        frame = new JFrame();
        frame.setTitle(player1+" VS "+player2);
        frame.setContentPane(painter);
        frame.setSize(WIDTH, HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);

        thread = new Thread(this, "XO");
        thread.start();
    }

    // constructor for client
    public XO(int port,String player1,String player2) throws IOException {
        this.port=port;

        loadImages();

        painter = new Painter();
        painter.setPreferredSize(new Dimension(WIDTH, HEIGHT));


        if (!connect())
            initializeServer();
        // if the player is connected to the server , he will be the client
        else {
            role= "client";
            player2=player2;
            player1=player1;
            dos.writeUTF("player2:"+player1);
            dos.flush();
        }

        frame = new JFrame();
        frame.setTitle(player1+" VS "+player2);
        frame.setContentPane(painter);
        frame.setSize(WIDTH, HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);

        thread = new Thread(this, "XO");
        thread.start();
    }

    public void run() {
        while (true) {
            // if the player is the server , he will get the name of the client his adversary
            if(dis!=null && role.equals("server")){
                if(player2.equals("Waiting for another player")){
                    try {
                        getEnemyPlayer();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            try {
                tick();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            painter.repaint();

            if (!circle && !accepted) {
                listenForServerRequest();
            }
        }
    }
    // method that modify graphics of the game and test if the player has won or not or if the game is finished by a tie
    private void render(Graphics g) {
        g.drawImage(board, 0, 0, null);
        if (unableToCommunicateWithOpponent) {
            g.setColor(Color.RED);
            g.setFont(smallerFont);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stringWidth = g2.getFontMetrics().stringWidth(unableToCommunicateWithOpponentString);
            g.drawString(unableToCommunicateWithOpponentString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
            return;
        }

        if (accepted) {
            for (int i = 0; i < spaces.length; i++) {
                if (spaces[i] != null) {
                    if (spaces[i].equals("X")) {
                        if (circle) {
                            g.drawImage(redX, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        } else {
                            g.drawImage(blueX, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        }
                    } else if (spaces[i].equals("O")) {
                        if (circle) {
                            g.drawImage(blueCircle, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        } else {
                            g.drawImage(redCircle, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        }
                    }
                }
            }
            if (won || enemyWon) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(10));
                g.setColor(Color.BLACK);
                g.drawLine(firstSpot % 3 * lengthOfSpace + 10 * firstSpot % 3 + lengthOfSpace / 2, (int) (firstSpot / 3) * lengthOfSpace + 10 * (int) (firstSpot / 3) + lengthOfSpace / 2, secondSpot % 3 * lengthOfSpace + 10 * secondSpot % 3 + lengthOfSpace / 2, (int) (secondSpot / 3) * lengthOfSpace + 10 * (int) (secondSpot / 3) + lengthOfSpace / 2);

                g.setColor(Color.RED);
                g.setFont(largerFont);
               // si le joueur a gagné on va afficher un message de victoire,suppimer la ligne du fichier texte et ajouter un bouton pour retourner à la liste des défis
                if (won) {
                    int stringWidth = g2.getFontMetrics().stringWidth(wonString);
                    g.drawString(wonString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
                    // add a button to return to the list
                    JButton btn = new JButton("Retour à la liste des défis");
                    btn.setBounds(103, 13, 300, 100);

                    // supprimer la ligne du fichier texte
                    if(role.equals("server")) {
                        Main.deleteLineFromFile(player2);
                        btn.setVisible(true);
                        frame.add(btn);
                        btn.addActionListener((ActionEvent e) -> {
                                    frame.dispose();
                                    new ListeDefis(player1);
                                }
                        );
                        // wait for 1 second so the user can see the board
//                        try {
//                            Thread.sleep(3000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        frame.dispose();
//                        new ListeDefis(player1);
                    }
                    else if(role.equals("client")) {
                        btn.setVisible(true);
                        frame.add(btn);
                        btn.addActionListener((ActionEvent e) -> {
                                    frame.dispose();
                                    new ListeDefis(player2);
                                }
                        );
//                        try {
//                            Thread.sleep(3000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        frame.dispose();
//                        new ListeDefis(player2);
                    }
                }
                // si l'adversaire a gagne on va faire la meme chose que le cas du joueur gagnant
                else if (enemyWon) {
                    int stringWidth = g2.getFontMetrics().stringWidth(enemyWonString);
                    g.drawString(enemyWonString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
                    JButton btn = new JButton("Retour à la liste des défis");
                    btn.setBounds(103, 13, 300, 100);
                    if(role.equals("server")) {
                        Main.deleteLineFromFile(player2);
                        btn.setVisible(true);
                        frame.add(btn);
                        btn.addActionListener((ActionEvent e) -> {
                                    frame.dispose();
                                    new ListeDefis(player1);
                                }
                        );
//                        try {
//                            Thread.sleep(3000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        frame.dispose();
//                        new ListeDefis(player1);
                    }
                    else if(role.equals("client")) {
                        btn.setVisible(true);
                        frame.add(btn);
                        btn.addActionListener((ActionEvent e) -> {
                                    frame.dispose();
                                    new ListeDefis(player2);
                                }
                        );
//                        try {
//                            Thread.sleep(3000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        frame.dispose();
//                        new ListeDefis(player2);
                    }
                }
            }
            // si le jeu a fini avec un tie on va afficher un message de tie et ajouter un bouton pour retourner à la liste des défis
            else
            if (tie) {
                Graphics2D g2 = (Graphics2D) g;
                g.setColor(Color.BLACK);
                g.setFont(largerFont);
                int stringWidth = g2.getFontMetrics().stringWidth(tieString);
                g.drawString(tieString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
                JButton btn = new JButton("Retour à la liste des défis");
                btn.setBounds(103, 13, 300, 100);

                if(role.equals("server")) {
                    Main.deleteLineFromFile(player2);
                    btn.setVisible(true);
                    frame.add(btn);
                    btn.addActionListener((ActionEvent e) -> {
                                frame.dispose();
                                new ListeDefis(player1);
                            }
                    );
//                    try {
//                        Thread.sleep(3000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    frame.dispose();
//                    new ListeDefis(player1);
                }
                else if(role.equals("client")) {
                    btn.setVisible(true);
                    frame.add(btn);
                    btn.addActionListener((ActionEvent e) -> {
                                frame.dispose();
                                new ListeDefis(player2);
                            }
                    );
//                    try {
//                        Thread.sleep(3000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    frame.dispose();
//                    new ListeDefis(player2);
                }
            }
        } else {
            g.setColor(Color.RED);
            g.setFont(font);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stringWidth = g2.getFontMetrics().stringWidth(waitingString);
            g.drawString(waitingString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
        }

    }

    // cet methode permet au serveur d'avoir le nom de son adversaire qui a joigné le jeu
    private void getEnemyPlayer() throws IOException {
        System.out.println("getEnemyPlayer");
        String player2=dis.readUTF();
        System.out.println("player2: "+player2);
        if(player2.contains("player"))
        {
            String[] parts = player2.split(":");
            String first = parts[0];
            String player = parts[1];
            // get the player name from the string
            this.player2=player;
            frame.setTitle(player1+" VS "+this.player2);
            // supprimer le defi de la liste des defis
            Main.modifyStatus(player1);
        }
    }

    // methode permet de recevoir le mouvement de l'adversaire
    private void tick() throws IOException {
        if (errors >= 10) unableToCommunicateWithOpponent = true;

        if (!yourTurn && !unableToCommunicateWithOpponent) {
            try {
                int space = dis.readInt();
                if (circle) spaces[space] = "X";
                else spaces[space] = "O";
                checkForEnemyWin();
                checkForTie();
                yourTurn = true;
            } catch (IOException e) {
                e.printStackTrace();
                errors++;
            }
        }
    }

    // check for  win
    private void checkForWin() {
        for (int i = 0; i < wins.length; i++) {
            if (circle) {
                if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    won = true;
                }
            } else {
                if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    won = true;
                }
            }
        }
    }

    // check for enemy win
    private void checkForEnemyWin() {
        for (int i = 0; i < wins.length; i++) {
            if (circle) {
                if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    enemyWon = true;
                }
            } else {
                if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    enemyWon = true;
                }
            }
        }
    }

    // check for tie
    private void checkForTie() {
        for (int i = 0; i < spaces.length; i++) {
            if (spaces[i] == null) {
                return;
            }
        }
        tie = true;
    }

    // methode permet d'accepter la demande de connexion au serveur(lanceur du defi)
    private void listenForServerRequest() {
        Socket socket = null;
        try {
            socket = serverSocket.accept();
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            accepted = true;
            System.out.println("CLIENT HAS REQUESTED TO JOIN, AND WE HAVE ACCEPTED");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //  connecter au serveur
    private boolean connect() {
        try {
            socket = new Socket(ip, port);
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            accepted = true;
        } catch (IOException e) {
            System.out.println("Unable to connect to the address: " + ip + ":" + port + " | Starting a server");
            return false;
        }
        System.out.println("Successfully connected to the server.");
        return true;
    }

    // initialiser le serveur
    private void initializeServer() {
        try {
            serverSocket = new ServerSocket(port, 8, InetAddress.getByName(ip));
        } catch (Exception e) {
            e.printStackTrace();
        }
        yourTurn = true;
        circle = false;
    }

    // telecharger les images du src folder pour les utiliser dans notre interface
    private void loadImages() {
        try {
            board = ImageIO.read(getClass().getResourceAsStream("/board.png"));
            redX = ImageIO.read(getClass().getResourceAsStream("/redX.png"));
            redCircle = ImageIO.read(getClass().getResourceAsStream("/redCircle.png"));
            blueX = ImageIO.read(getClass().getResourceAsStream("/blueX.png"));
            blueCircle = ImageIO.read(getClass().getResourceAsStream("/blueCircle.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    // classe responsable sur la painture de l interface
    private class Painter extends JPanel implements MouseListener {
        private static final long serialVersionUID = 1L;

        public Painter() {
            setFocusable(true);
            requestFocus();
            setBackground(Color.WHITE);
            addMouseListener(this);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            render(g);
        }

        // gerer les evenements du click sur l interface
        @Override
        public void mouseClicked(MouseEvent e) {
            if (accepted) {
                if (yourTurn && !unableToCommunicateWithOpponent && !won && !enemyWon) {
                    int x = e.getX() / lengthOfSpace;
                    int y = e.getY() / lengthOfSpace;
                    y *= 3;
                    int position = x + y;

                    if (spaces[position] == null) {
                        if (!circle) spaces[position] = "X";
                        else spaces[position] = "O";
                        yourTurn = false;
                        repaint();
                        Toolkit.getDefaultToolkit().sync();

                        try {
                            dos.writeInt(position);
                            dos.flush();
                        } catch (IOException e1) {
                            errors++;
                            e1.printStackTrace();
                        }

                        System.out.println("DATA WAS SENT");
                        checkForWin();
                        checkForTie();

                    }
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

    }

}
