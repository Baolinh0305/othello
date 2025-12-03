import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class Othello extends JFrame {

    public static final int SIZE = 8;
    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = 2;

    private CardLayout layout;
    private JPanel mainPanel;
    private Board boardPanel;
    private Menu menuPanel;

    public Othello() {
        setTitle("Othello Modern AI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        layout = new CardLayout();
        mainPanel = new JPanel(layout);

        menuPanel = new Menu(this);
        boardPanel = new Board(this);

        mainPanel.add(menuPanel, "MENU");
        mainPanel.add(boardPanel, "GAME");

        add(mainPanel);
        mainPanel.setPreferredSize(new Dimension(600, 700));
        pack();
        setLocationRelativeTo(null);
    }

    public void startGame() {
        boardPanel.resetGame();
        layout.show(mainPanel, "GAME");
    }

    public void showMenu() {
        layout.show(mainPanel, "MENU");
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            new Othello().setVisible(true);
        });
    }

    // Modern Button
    static class Btn extends JButton {
        private Color normal = new Color(50, 50, 50);
        private Color hover = new Color(80, 80, 80);
        private Color press = new Color(30, 30, 30);

        public Btn(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 16));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { setBackground(hover); }
                @Override public void mouseExited(MouseEvent e) { setBackground(normal); }
                @Override public void mousePressed(MouseEvent e) { setBackground(press); }
                @Override public void mouseReleased(MouseEvent e) { setBackground(hover); }
            });
            setBackground(normal);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.setColor(getForeground());
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() + fm.getAscent()) / 2 - 2;
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

    // Menu Panel
    class Menu extends JPanel {
        public Menu(Othello frame) {
            setLayout(new GridBagLayout());
            JLabel title = new JLabel("OTHELLO");
            title.setFont(new Font("Segoe UI", Font.BOLD, 60));
            title.setForeground(Color.WHITE);

            JLabel subtitle = new JLabel("Minimax Alpha-Beta AI");
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            subtitle.setForeground(new Color(200, 200, 200));

            Btn btnStart = new Btn("CHƠI VỚI AI");
            btnStart.setPreferredSize(new Dimension(200, 50));
            btnStart.addActionListener(e -> frame.startGame());

            Btn btnExit = new Btn("THOÁT");
            btnExit.setPreferredSize(new Dimension(200, 50));
            btnExit.addActionListener(e -> System.exit(0));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 0, 10, 0);
            gbc.gridx = 0;
            gbc.gridy = 0; add(title, gbc);
            gbc.gridy = 1; add(subtitle, gbc);
            gbc.insets = new Insets(40, 0, 10, 0);
            gbc.gridy = 2; add(btnStart, gbc);
            gbc.insets = new Insets(10, 0, 10, 0);
            gbc.gridy = 3; add(btnExit, gbc);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            GradientPaint gp = new GradientPaint(0, 0, new Color(20, 20, 20), getWidth(), getHeight(), new Color(40, 44, 52));
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // Board Panel
    class Board extends JPanel implements MouseListener {
        private Othello frame;
        private int[][] board;
        private int currentPlayer;
        private boolean isGameOver;
        private AI ai;
        private boolean isAIThinking = false;

        private JPanel header;
        private JLabel scoreBlack, scoreWhite;
        private Btn btnBack;

        private List<Anim> animDiscs = new ArrayList<>();
        private Timer animTimer;

        private final int[][] WEIGHTS = {
            {100, -20, 10,  5,  5, 10, -20, 100},
            {-20, -50, -2, -2, -2, -2, -50, -20},
            { 10,  -2, -1, -1, -1, -1,  -2,  10},
            {  5,  -2, -1, -1, -1, -1,  -2,   5},
            {  5,  -2, -1, -1, -1, -1,  -2,   5},
            { 10,  -2, -1, -1, -1, -1,  -2,  10},
            {-20, -50, -2, -2, -2, -2, -50, -20},
            {100, -20, 10,  5,  5, 10, -20, 100}
        };

        public Board(Othello frame) {
            this.frame = frame;
            this.ai = new AI(this);
            setLayout(new BorderLayout());

            header = new JPanel(new BorderLayout());
            header.setBackground(new Color(30, 30, 30));
            header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

            JPanel scorePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));
            scorePanel.setOpaque(false);

            scoreBlack = new JLabel("YOU: 2");
            scoreBlack.setFont(new Font("Segoe UI", Font.BOLD, 18));
            scoreBlack.setForeground(Color.WHITE);

            scoreWhite = new JLabel("AI: 2");
            scoreWhite.setFont(new Font("Segoe UI", Font.BOLD, 18));
            scoreWhite.setForeground(Color.GRAY);

            scorePanel.add(scoreBlack);
            scorePanel.add(scoreWhite);

            btnBack = new Btn("MENU");
            btnBack.setPreferredSize(new Dimension(80, 30));
            btnBack.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnBack.addActionListener(e -> frame.showMenu());

            header.add(btnBack, BorderLayout.WEST);
            header.add(scorePanel, BorderLayout.CENTER);
            JLabel dummy = new JLabel("");
            dummy.setPreferredSize(new Dimension(80, 30));
            header.add(dummy, BorderLayout.EAST);

            add(header, BorderLayout.NORTH);

            JPanel boardWrapper = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.setColor(new Color(40, 40, 40));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    drawBoard(g);
                }
            };
            boardWrapper.addMouseListener(this);
            add(boardWrapper, BorderLayout.CENTER);

            animTimer = new Timer(16, e -> {
                boolean finished = true;
                Iterator<Anim> it = animDiscs.iterator();
                while(it.hasNext()){
                    Anim d = it.next();
                    d.update();
                    if(!d.isDone) finished = false;
                }
                repaint();
                if(finished && !animDiscs.isEmpty()) {
                    animDiscs.clear();
                }
            });
        }

        public void resetGame() {
            board = new int[SIZE][SIZE];
            board[3][3] = WHITE;
            board[3][4] = BLACK;
            board[4][3] = BLACK;
            board[4][4] = WHITE;

            currentPlayer = BLACK;
            isGameOver = false;
            isAIThinking = false;
            animDiscs.clear();
            updateScoreUI();
            repaint();
        }

        private void updateScoreUI() {
            int b = countScore(board, BLACK);
            int w = countScore(board, WHITE);
            scoreBlack.setText("YOU: " + b);
            scoreWhite.setText("AI: " + w);

            if (currentPlayer == BLACK) {
                scoreBlack.setForeground(new Color(0, 255, 127));
                scoreWhite.setForeground(Color.GRAY);
            } else {
                scoreBlack.setForeground(Color.GRAY);
                scoreWhite.setForeground(new Color(255, 100, 100));
            }
        }

        private void drawBoard(Graphics g) {
            int w = getComponent(1).getWidth();
            int h = getComponent(1).getHeight();
            int boardSize = Math.min(w, h) - 40;
            int cellSize = boardSize / SIZE;
            int xOffset = (w - boardSize) / 2;
            int yOffset = (h - boardSize) / 2;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(30, 100, 60));
            g2.fillRoundRect(xOffset, yOffset, boardSize, boardSize, 10, 10);

            g2.setColor(new Color(20, 70, 40));
            g2.setStroke(new BasicStroke(2));
            for (int i = 0; i <= SIZE; i++) {
                g2.drawLine(xOffset + i * cellSize, yOffset, xOffset + i * cellSize, yOffset + boardSize);
                g2.drawLine(xOffset, yOffset + i * cellSize, xOffset + boardSize, yOffset + i * cellSize);
            }

            if (currentPlayer == BLACK && !isGameOver && !isAIThinking) {
                List<Point> moves = getValidMoves(board, BLACK);
                g2.setColor(new Color(0, 0, 0, 40));
                for (Point p : moves) {
                    int cx = xOffset + p.y * cellSize + cellSize / 2;
                    int cy = yOffset + p.x * cellSize + cellSize / 2;
                    g2.fillOval(cx - 5, cy - 5, 10, 10);
                }
            }

            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    boolean isAnimating = false;
                    for(Anim ad : animDiscs) {
                        if(ad.r == r && ad.c == c) {
                            isAnimating = true;
                            drawDisc(g2, xOffset + c * cellSize, yOffset + r * cellSize, cellSize, ad.currentColor, ad.scaleX);
                            break;
                        }
                    }
                    if (!isAnimating && board[r][c] != EMPTY) {
                        drawDisc(g2, xOffset + c * cellSize, yOffset + r * cellSize, cellSize, board[r][c], 1.0);
                    }
                }
            }
        }

        private void drawDisc(Graphics2D g2, int x, int y, int size, int player, double scaleX) {
            int discSize = size - 10;
            int centerX = x + size / 2;
            int centerY = y + size / 2;
            int currentWidth = (int)(discSize * scaleX);
            int currentX = centerX - currentWidth / 2;
            int currentY = centerY - discSize / 2;

            if (player == BLACK) {
                g2.setColor(new Color(20, 20, 20));
                g2.fillOval(currentX, currentY, currentWidth, discSize);
                g2.setColor(new Color(60, 60, 60));
                g2.drawOval(currentX + 2, currentY + 2, currentWidth - 4, discSize - 4);
            } else {
                g2.setColor(new Color(240, 240, 240));
                g2.fillOval(currentX, currentY, currentWidth, discSize);
                g2.setColor(new Color(200, 200, 200));
                g2.drawOval(currentX + 2, currentY + 2, currentWidth - 4, discSize - 4);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (isGameOver || currentPlayer != BLACK || isAIThinking) return;
            int w = getComponent(1).getWidth();
            int h = getComponent(1).getHeight();
            int boardSize = Math.min(w, h) - 40;
            int cellSize = boardSize / SIZE;
            int xOffset = (w - boardSize) / 2;
            int yOffset = (h - boardSize) / 2;

            int col = (e.getX() - xOffset) / cellSize;
            int row = (e.getY() - yOffset) / cellSize;

            if (row >= 0 && row < SIZE && col >= 0 && col < SIZE) {
                if (isValidMove(board, BLACK, row, col)) {
                    executeMove(BLACK, row, col);
                    currentPlayer = WHITE;
                    isAIThinking = true;
                    updateScoreUI();
                    repaint();

                    new Thread(() -> {
                        try { Thread.sleep(700); } catch (Exception ex) {}
                        if (hasValidMove(board, WHITE)) {
                            Point aiMove = ai.getBestMove(board);
                            if (aiMove != null) {
                                executeMove(WHITE, aiMove.x, aiMove.y);
                            }
                        }
                        if (hasValidMove(board, BLACK)) {
                            currentPlayer = BLACK;
                        } else if (hasValidMove(board, WHITE)) {
                            currentPlayer = WHITE;
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(this, "Bạn hết nước đi! AI đi tiếp.");
                                mousePressed(e);
                            });
                        } else {
                            checkGameOver();
                        }
                        isAIThinking = false;
                        SwingUtilities.invokeLater(() -> {
                            updateScoreUI();
                            repaint();
                            checkGameOver();
                        });
                    }).start();
                }
            }
        }

        private void executeMove(int player, int r, int c) {
            int opponent = (player == BLACK) ? WHITE : BLACK;
            board[r][c] = player;
            int[] dr = {-1, -1, -1, 0, 0, 1, 1, 1};
            int[] dc = {-1, 0, 1, -1, 1, -1, 0, 1};
            List<Point> flippedPoints = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                int nr = r + dr[i];
                int nc = c + dc[i];
                List<Point> path = new ArrayList<>();
                while (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE && board[nr][nc] == opponent) {
                    path.add(new Point(nr, nc));
                    nr += dr[i];
                    nc += dc[i];
                }
                if (!path.isEmpty() && nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE && board[nr][nc] == player) {
                    flippedPoints.addAll(path);
                }
            }
            for (Point p : flippedPoints) {
                board[p.x][p.y] = player;
                animDiscs.add(new Anim(p.x, p.y, opponent, player));
            }
            animTimer.start();
            SwingUtilities.invokeLater(this::repaint);
        }

        private void checkGameOver() {
            if (!hasValidMove(board, BLACK) && !hasValidMove(board, WHITE)) {
                isGameOver = true;
                int b = countScore(board, BLACK);
                int w = countScore(board, WHITE);
                String msg = (b > w) ? "BẠN THẮNG!" : (w > b) ? "AI THẮNG!" : "HÒA!";
                JOptionPane.showMessageDialog(this, msg + "\nBạn: " + b + " - AI: " + w);
            }
        }

        public boolean isValidMove(int[][] currentBoard, int player, int r, int c) {
            if (currentBoard[r][c] != EMPTY) return false;
            int opponent = (player == BLACK) ? WHITE : BLACK;
            int[] dr = {-1, -1, -1, 0, 0, 1, 1, 1};
            int[] dc = {-1, 0, 1, -1, 1, -1, 0, 1};
            for (int i = 0; i < 8; i++) {
                int nr = r + dr[i];
                int nc = c + dc[i];
                boolean foundOpp = false;
                while (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE && currentBoard[nr][nc] == opponent) {
                    nr += dr[i]; nc += dc[i]; foundOpp = true;
                }
                if (foundOpp && nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE && currentBoard[nr][nc] == player) return true;
            }
            return false;
        }

        public List<Point> getValidMoves(int[][] b, int player) {
            List<Point> moves = new ArrayList<>();
            for (int r = 0; r < SIZE; r++) for (int c = 0; c < SIZE; c++) if (isValidMove(b, player, r, c)) moves.add(new Point(r, c));
            return moves;
        }
        public boolean hasValidMove(int[][] b, int p) { return !getValidMoves(b, p).isEmpty(); }
        public int countScore(int[][] b, int p) {
            int s = 0; for(int[] r: b) for(int v: r) if(v==p) s++; return s;
        }

        // AI class
        class AI {
            Board g;
            int maxDepth = 6;
            public AI(Board g) { this.g = g; }
            public Point getBestMove(int[][] board) {
                List<Point> moves = g.getValidMoves(board, WHITE);
                Point best = null; int maxVal = Integer.MIN_VALUE, alpha = Integer.MIN_VALUE, beta = Integer.MAX_VALUE;
                for(Point m : moves) {
                    int[][] next = cloneBoard(board);
                    simulateMove(next, WHITE, m.x, m.y);
                    int val = minimax(next, maxDepth-1, alpha, beta, false);
                    if(val > maxVal) { maxVal = val; best = m; }
                    alpha = Math.max(alpha, maxVal);
                }
                return best;
            }
            private int minimax(int[][] bd, int depth, int alpha, int beta, boolean maxing) {
                if(depth == 0 || !g.hasValidMove(bd, BLACK) && !g.hasValidMove(bd, WHITE)) return eval(bd);
                if(maxing) {
                    List<Point> moves = g.getValidMoves(bd, WHITE);
                    if(moves.isEmpty()) return minimax(bd, depth-1, alpha, beta, false);
                    int maxEval = Integer.MIN_VALUE;
                    for(Point m : moves) {
                        int[][] next = cloneBoard(bd); simulateMove(next, WHITE, m.x, m.y);
                        int eval = minimax(next, depth-1, alpha, beta, false);
                        maxEval = Math.max(maxEval, eval); alpha = Math.max(alpha, eval);
                        if(beta <= alpha) break;
                    }
                    return maxEval;
                } else {
                    List<Point> moves = g.getValidMoves(bd, BLACK);
                    if(moves.isEmpty()) return minimax(bd, depth-1, alpha, beta, true);
                    int minEval = Integer.MAX_VALUE;
                    for(Point m : moves) {
                        int[][] next = cloneBoard(bd); simulateMove(next, BLACK, m.x, m.y);
                        int eval = minimax(next, depth-1, alpha, beta, true);
                        minEval = Math.min(minEval, eval); beta = Math.min(beta, eval);
                        if(beta <= alpha) break;
                    }
                    return minEval;
                }
            }
            private int eval(int[][] bd) {
                int s = 0;
                for(int r=0;r<SIZE;r++) for(int c=0;c<SIZE;c++) {
                    if(bd[r][c]==WHITE) s+= g.WEIGHTS[r][c];
                    else if(bd[r][c]==BLACK) s-= g.WEIGHTS[r][c];
                }
                return s;
            }
        }

        private int[][] cloneBoard(int[][] src) {
            int[][] d = new int[SIZE][SIZE]; for(int i=0;i<SIZE;i++) System.arraycopy(src[i], 0, d[i], 0, SIZE); return d;
        }
        private void simulateMove(int[][] bd, int p, int r, int c) {
            bd[r][c] = p; int opp = (p==BLACK)?WHITE:BLACK;
            int[] dr = {-1,-1,-1,0,0,1,1,1}, dc = {-1,0,1,-1,1,-1,0,1};
            for(int i=0;i<8;i++){
                int nr=r+dr[i], nc=c+dc[i]; List<Point> fl = new ArrayList<>();
                while(nr>=0 && nr<SIZE && nc>=0 && nc<SIZE && bd[nr][nc]==opp) { fl.add(new Point(nr,nc)); nr+=dr[i]; nc+=dc[i]; }
                if(!fl.isEmpty() && nr>=0 && nr<SIZE && nc>=0 && nc<SIZE && bd[nr][nc]==p) for(Point pt: fl) bd[pt.x][pt.y] = p;
            }
        }

        @Override public void mouseClicked(MouseEvent e) {}
        @Override public void mouseReleased(MouseEvent e) {}
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}
    }

    // Animation Disc
    class Anim {
        int r, c;
        int startColor, endColor, currentColor;
        double scaleX = 1.0;
        double step = 0.15;
        boolean shrinking = true;
        boolean isDone = false;

        public Anim(int r, int c, int startColor, int endColor) {
            this.r = r; this.c = c; this.startColor = startColor; this.endColor = endColor;
            this.currentColor = startColor;
        }
        public void update() {
            if(isDone) return;
            if(shrinking) {
                scaleX -= step;
                if(scaleX <= 0) {
                    scaleX = 0; shrinking = false; currentColor = endColor;
                }
            } else {
                scaleX += step;
                if(scaleX >= 1.0) { scaleX = 1.0; isDone = true; }
            }
        }
    }
}