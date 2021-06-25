import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class BattleShipGUI extends JFrame implements ActionListener {
    private final int SIZEOFALPHABET = 25;
    private static final String LETTERCOLUMNS = "ABCDEFGHIJKLMNOPQRSTUVWXY"; //used to print Alpha Grid

    private int playerShips = 0;
    private int enemyShips = 0;

    private final int MISS = 1;

    public JTextField chatBox = new JTextField(10);
    public JTextField clientMessages = new JTextField(10);
    private JButton aircraftButton;
    private JButton battleShipButton;
    private JButton submarineButton;
    private JButton destroyerButton;
    private JButton patrolBoatButton;
    private int playerID;
    private int otherPlayer;
    private Boolean turnState;

    public final Color[] shots = {Color.RED, Color.BLUE}; //if it's red, a shot has landed, blue is if it missed

    private BattleshipClient client;

    private final Ships aircraftCarrier = new Ships("Aircraft Carrier", 5);
    private final Ships battleship = new Ships("Battleship" , 4);
    private final Ships submarine = new Ships("Submarine" , 3);
    private final Ships destroyer = new Ships("Destroyer" , 3);
    private final Ships patrolBoat= new Ships("Patrol Boat" , 2);

    public JButton sendMessage = new JButton("Send Message!");
    public JLabel sendMessageLabel = new JLabel("Send Message:");
    public JLabel receivedLabel = new JLabel("Message Received:");
    public JScrollBar messageHistory = new JScrollBar(JScrollBar.VERTICAL, 30, 40, 0, 300);

    private Boolean myTurn = true;
    private Boolean readyCheck = false;

    private BattleshipBoard[][] board = new BattleshipBoard[SIZEOFALPHABET][SIZEOFALPHABET]; //same process as making the grid for the GameOfLife
    private final BattleshipBoard[][] clientBoard = new BattleshipBoard[SIZEOFALPHABET][SIZEOFALPHABET]; //same process as making the grid for the GameOfLife

    public BattleShipGUI() {

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        connectToServer();
        initiatePlayerGUI();
        setPlayerGrid();

    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new BattleShipGUI();

            }
        });
    }

    public JPanel xGrid() {
        JPanel xGrid = new JPanel(new GridLayout(1, 25, 1, 1));
        xGrid.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        for (int columns = 0; columns < SIZEOFALPHABET; columns++) {
            xGrid.add(new JLabel(LETTERCOLUMNS.substring(columns, columns + 1), SwingConstants.CENTER));
        }

        return xGrid;

    }

    public JPanel yGrid() {
        JPanel yGrid = new JPanel(new GridLayout(25, 1, 1, 1));
        yGrid.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        for (int rows = 1; rows < SIZEOFALPHABET + 1; rows++) { // creates a grid of Y for numbers, needs to start from one and add one to size of alphabets to get 1-25
            yGrid.add(new JLabel(" " + rows, SwingConstants.CENTER));
        }
        return yGrid;
    }

    public JPanel clientGameGrid() {
        JPanel shipPanel = new JPanel(new GridLayout(25, 25, 1, 1));
        shipPanel.setBackground(Color.WHITE);
        shipPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        for (int rows = 0; rows < SIZEOFALPHABET; rows++) {
            for (int columns = 0; columns < SIZEOFALPHABET; columns++) {
                shipPanel.add(board[rows][columns]);
            }
        }

        return shipPanel;
    }

    public JPanel enemyGameGrid() {

        JPanel enemyBoardPanel = new JPanel(new GridLayout(25, 25, 1, 1));

        for (int rows = 0; rows < SIZEOFALPHABET; rows++) {
            for (int columns = 0; columns < SIZEOFALPHABET; columns++) {
                clientBoard[rows][columns] = new BattleshipBoard();
            }
        }

        enemyBoardPanel.setBackground(Color.WHITE);
        enemyBoardPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        for (int rows = 0; rows < SIZEOFALPHABET; rows++) {
            for (int columns = 0; columns < SIZEOFALPHABET; columns++) {
                enemyBoardPanel.add(clientBoard[rows][columns]);

            }
        }

        return enemyBoardPanel;
    }

    public void initiatePlayerGUI() {

        this.setTitle("BattleShip Player #" + playerID); //set title to battleship

        if(playerID == 1){
            System.out.println("Player 1 connected, begin setting up your board");

            otherPlayer = 2;
        } else{
            System.out.println("You're player 2, begin setting up your board as well");
            otherPlayer = 1;
            Thread turnBase = new Thread(new Runnable() {
                @Override
                public void run() {
                    updateTurn();
                }
            });
            turnBase.start();
            sendMessage.setEnabled(false); //because you're waiting for player1's turn, you cant send a message to declare it's their turn

        }

        JPanel playerPanel = new JPanel();
        playerPanel.setLayout(new BorderLayout()); //need to make new BorderLayout in order to create side by side panels
        playerPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        setBackground(Color.white);
        setResizable(true);

        playerPanel.add(xGrid(), BorderLayout.NORTH); //sets the Letter grid to the Top
        playerPanel.add(yGrid(), BorderLayout.WEST); //sets the Numeric grid to the Left

        for (int rows = 0; rows < SIZEOFALPHABET; rows++) {
            for (int columns = 0; columns < SIZEOFALPHABET; columns++) {// registers each grid of the board as a battleship mouselistener
                board[rows][columns] = new BattleshipBoard();
            }
        }

        playerPanel.add(clientGameGrid(), BorderLayout.CENTER); //sets it as the central focus of the jFrame
        JPanel chatPanel = new JPanel(new GridLayout(3, 1, 1, 1)); //will take the send button and textArea
        chatPanel.add(sendMessageLabel);
        chatBox.addActionListener(this);
        chatPanel.add(chatBox);
        sendMessage.addActionListener(this); //this button will be designed to send the message in TEXTAREA to the client.
        chatPanel.add(sendMessage);
        sendMessage.setEnabled(false);
        chatPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        playerPanel.add(chatPanel, BorderLayout.SOUTH);
        add(playerPanel, BorderLayout.WEST);

        pack();
        setVisible(true);

    } //this is thinking hey, the left side is yours, the right side is the enemy

    public void setUpEnemyGUI() {

        JPanel enemyPanel = new JPanel();
        enemyPanel.setLayout(new BorderLayout());
        enemyPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JPanel enemyBoardPanel = enemyGameGrid();

        enemyPanel.add(xGrid(), BorderLayout.NORTH);
        enemyPanel.add(yGrid(), BorderLayout.WEST);

        enemyPanel.add(enemyBoardPanel, BorderLayout.CENTER);

        //NEEDS TO FIX SCROLLBAR

        JPanel chatPanel = new JPanel(new GridLayout(3, 1, 1, 1)); //will take the send button and textArea
        chatPanel.add(receivedLabel);
        enemyPanel.add(chatPanel, BorderLayout.SOUTH);
        clientMessages.setEditable(false);


        add(enemyPanel, BorderLayout.EAST);
        chatPanel.add(clientMessages);
        chatPanel.add(messageHistory);
        //chatPanel.add(receivedLabel);

        pack();
    } //this is initializing the right side of the panel, aka enemy

    public JPanel addShips(){
        JPanel shipButtons = new JPanel();

        //sets aircraft
        aircraftButton = new JButton("Aircraft Carrier"); //5 board placements
        aircraftButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String buttonPressed = e.getActionCommand();
                if (buttonPressed.equals(aircraftButton.getText())) {

                    if(!battleShipButton.isEnabled()){
                        battleShipButton.setEnabled(true);
                    }
                    if(!submarineButton.isEnabled()){
                        submarineButton.setEnabled(true);
                    }
                    if(!destroyerButton.isEnabled()){
                        destroyerButton.setEnabled(true);
                    }
                    if(!patrolBoatButton.isEnabled()){
                        patrolBoatButton.setEnabled(true);
                    }

                    aircraftButton.setEnabled(false);

                }
            }
        });

        battleShipButton = new JButton("Battleship");
        battleShipButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String buttonPressed = e.getActionCommand();
                if (buttonPressed.equals(battleShipButton.getText())) {
                    if(!aircraftButton.isEnabled()){
                        aircraftButton.setEnabled(true);
                    }
                    if(!submarineButton.isEnabled()){
                        submarineButton.setEnabled(true);
                    }
                    if(!destroyerButton.isEnabled()){
                        destroyerButton.setEnabled(true);
                    }
                    if(!patrolBoatButton.isEnabled()){
                        patrolBoatButton.setEnabled(true);
                    }

                    battleShipButton.setEnabled(false);
                }
            }
        });

        submarineButton = new JButton("Submarine");
        submarineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String buttonPressed = e.getActionCommand();
                if (buttonPressed.equals(submarineButton.getText())) {
                    if(!aircraftButton.isEnabled()){
                        aircraftButton.setEnabled(true);
                    }
                    if(!battleShipButton.isEnabled()){
                        battleShipButton.setEnabled(true);
                    }
                    if(!destroyerButton.isEnabled()){
                        destroyerButton.setEnabled(true);
                    }
                    if(!patrolBoatButton.isEnabled()){
                        patrolBoatButton.setEnabled(true);
                    }
                    submarineButton.setEnabled(false);
                }
            }
        });
        destroyerButton = new JButton("Destroyer");
        destroyerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String buttonPressed = e.getActionCommand();
                if (buttonPressed.equals(destroyerButton.getText())) {
                    if(!aircraftButton.isEnabled()){
                        aircraftButton.setEnabled(true);
                    }
                    if(!battleShipButton.isEnabled()){
                        battleShipButton.setEnabled(true);
                    }
                    if(!submarineButton.isEnabled()){
                        submarineButton.setEnabled(true);
                    }
                    if(!patrolBoatButton.isEnabled()){
                        patrolBoatButton.setEnabled(true);
                    }

                    destroyerButton.setEnabled(false);
                }
            }
        });
        patrolBoatButton = new JButton("Patrol Boat");
        patrolBoatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String buttonPressed = e.getActionCommand();
                if (buttonPressed.equals(patrolBoatButton.getText())) {
                    if(!aircraftButton.isEnabled()){
                        aircraftButton.setEnabled(true);
                    }
                    if(!battleShipButton.isEnabled()){
                        battleShipButton.setEnabled(true);
                    }
                    if(!submarineButton.isEnabled()){
                        submarineButton.setEnabled(true);
                    }
                    if(!destroyerButton.isEnabled()){
                        destroyerButton.setEnabled(true);
                    }
                    patrolBoatButton.setEnabled(false);
                }
            }
        });

        shipButtons.setLayout(new GridLayout(1, 5));

        shipButtons.add(aircraftButton);
        shipButtons.add(battleShipButton);
        shipButtons.add(submarineButton);
        shipButtons.add(destroyerButton);
        shipButtons.add(patrolBoatButton);
        return shipButtons;
    }

    public void setPlayerGrid() { //this part should update the grid based off of player selection on ship placement, and then we can register ship hits and misses

        JPanel shipButtons = addShips();

        add(shipButtons, BorderLayout.NORTH);
        pack();
        JPanel readyPanel = new JPanel();
        JButton ready = new JButton("Ready to Play!");
        ready.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (shipPresence()) {
                    readyCheck = true;
                    ready.setText("Waiting for Opponent...");
                    ready.setEnabled(false);
                    remove(readyPanel);
                    remove(shipButtons);
                    sendMessage.setEnabled(true);
                    pack();
                    setUpEnemyGUI();
                }
            }
        });
        readyPanel.add(ready);
        add(readyPanel, BorderLayout.SOUTH);

    }

    public synchronized void actionPerformed(ActionEvent e) {
        Object userAction = e.getSource();

        if (userAction == sendMessage) {

            if (chatBox.getText().equals("your turn")) {
                chatBox.setText(""); //this should reset the textbox after the button is clicked
                Thread turnBase = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        updateTurn();
                    }
                });
                turnBase.start();
                sendMessage.setEnabled(false);
            }
        } else {
            chatBox.setText(""); //this should reset the text box after the button is clicked
        }

    }

    public void connectToServer(){
        client = new BattleshipClient();
    }

    public void updateTurn(){
        boolean turnInfo = client.receiveTurnState();
        int xCoord = client.receieveXCoord();
        int yCoord = client.receiveYCoord();
        System.out.println("your enemy has shot their turn, at the xCoord "+ xCoord +" " + yCoord);

        enemyAttack(yCoord,xCoord); //receives the client's coordinates and does the following

        //checks to see if there is a shipPlaced at that location of the double array
        //if there is, mark both the clientBoard and the player2's board in order to register the hit and set shipPlaced to false
        System.out.println("It's your turn");
        turnState = turnInfo;
        sendMessage.setEnabled(true);
    }

    public void enemyAttack(int yCoord, int xCoord){

        if(board[yCoord][xCoord].shipPlaced){
            int HIT = 0;
            board[yCoord][xCoord].setBackground(shots[HIT]); // for hit
            board[yCoord][xCoord].shipPlaced = false;
        }else if(!board[yCoord][xCoord].shipPlaced){ //if shipPlaced was false, then we set the shot on the board as a miss.
            board[yCoord][xCoord].setBackground(shots[MISS]);
        }

    }

    //create a forloop that counts up the amount of ships present and places that value as a player value and the other player would get it as a enemy value.

    private boolean checkWin(){
        if (shipPresence()){ // if there's ships present on your board, you're the winner
            JFrame winScreen = new JFrame();
            winScreen.setTitle("You won!!!!");
            JLabel winnerLabel = new JLabel("YOU'VE WON!!!");
            winScreen.add(winnerLabel, BorderLayout.CENTER);
            return true;
        }else if(!shipPresence()){
            JFrame winScreen = new JFrame();
            winScreen.setTitle("You lost :(");
            JLabel winnerLabel = new JLabel("You lost :(");
            winScreen.add(winnerLabel, BorderLayout.CENTER);
            return true;
        }

        return false; //false meaning the game is still going on

    }

    public boolean shipPresence(){ //use this function to make sure we don't have the game begin with no ships, and to check win condition.
        for (int rows = 0; rows < SIZEOFALPHABET; rows++){
            for (int columns = 0; columns < SIZEOFALPHABET; columns++){
                if (board[rows][columns].shipPlaced){
                    return true;
                }
            }
        }
        return false;
    }

    private class BattleshipClient {

        private Socket clientSocket;
        private final int port = 7777;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;

        public BattleshipClient(){
            System.out.println("Client side");
            try{
                clientSocket = new Socket("Localhost", port); // connects to the port of 7777
                dataIn = new DataInputStream(clientSocket.getInputStream());
                dataOut = new DataOutputStream(clientSocket.getOutputStream());
                playerID = dataIn.readInt(); //reads player ID
                turnState = dataIn.readBoolean(); //sets the turnState to what the server sends out.
                System.out.println("Connected to the server as Player #" + playerID + ".");
                System.out.println("Your turn is: " + turnState);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendTurnInformation(boolean yourTurn, int yCoord, int xCoord){
            try{
                dataOut.writeBoolean(yourTurn); //sends out turn what your turn was
                dataOut.writeInt(xCoord); //sends the value of the xCoordinate of the array
                dataOut.writeInt(yCoord); //sends the value of the yCoordinate of the array
                dataOut.flush();//sends the information
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        public boolean receiveTurnState(){
            boolean turn = false;

            try{
                turn = dataIn.readBoolean();
                System.out.println("Player " + otherPlayer + " has shot their turned");
            }catch (IOException e){
                e.printStackTrace();
            }
            return turn;
        }
        public int receieveXCoord(){
            int xCoord = -1;
            try{
                xCoord = dataIn.readInt();
            }catch (IOException e){
                e.printStackTrace();
            }
            return xCoord;
        }
        public int receiveYCoord(){
            int yCoord = -1;
            try{
                yCoord = dataIn.readInt();
            }catch (IOException e){
                e.printStackTrace();
            }
            return yCoord;
        }

    }

    class BattleshipBoard extends JPanel implements MouseListener {
        private final int X_OFFSET = 21; //the amount of spaces each column in X is spaced out
        private final int Y_OFFSET = 19; //amount of spaces Y is spaced out
        private boolean shipPlaced = false;
        Dimension dimension = new Dimension(20, 20); //dimensions of the squares

        BattleshipBoard() {
            setOpaque(true);
            setBackground(Color.darkGray); //default board color
            addMouseListener(this); //sets a mouselistener so that way we can edit the board
            this.setPreferredSize(dimension);
        }


        //these verify and update methods will always be board cause this is player based ship editing
        public void updateBoard(Ships shipEntered, Color enteredColor) {
            for (int column = 0; column < shipEntered.getSize(); column++) { //find a way to get constants via class declaration
                if (getX() / X_OFFSET + shipEntered.getSize() > SIZEOFALPHABET) { //if the ship size added to the Xcoordinate is greater than the ArraySize, we'll face the ship placement to the left
                    if (!board[getY() / Y_OFFSET][getX() / X_OFFSET - column].shipPlaced) {
                        board[getY() / Y_OFFSET][getX() / X_OFFSET - column].setBackground(enteredColor); //subtracting columns means we'll be going to the left instead
                        board[getY() / 19][getX() / X_OFFSET - column].shipPlaced = true;

                    }
                } else if (getX() / X_OFFSET + shipEntered.getSize() < SIZEOFALPHABET) { //adding will set the objects to the right
                    if (!board[getY() / Y_OFFSET][getX() / X_OFFSET + column].shipPlaced) {
                        board[getY() / Y_OFFSET][getX() / X_OFFSET + column].setBackground(enteredColor); //It accesses it Y first then X, it simulates X-Y coordinates backwards for the grid!!!!!!
                        board[getY() / 19][getX() / X_OFFSET + column].shipPlaced = true;
                    }
                }
            }
        }

        public void verifyBoard(Ships shipEntered, Color enteredColor) {
            for (int column = 0; column < shipEntered.getSize(); column++) { //find a way to get constants via class declaration
                if (getX() / X_OFFSET + shipEntered.getSize() > SIZEOFALPHABET) { //if the ship size added to the Xcoordinate is greater than the ArraySize, we'll face the ship placement to the left
                    if(!board[getY() / Y_OFFSET][getX() / X_OFFSET - column].shipPlaced) {
                        board[getY() / Y_OFFSET][getX() / X_OFFSET - column].setBackground(enteredColor); //subtracting columns means we'll be going to the left instead
                    }
                } else if (getX() / X_OFFSET + shipEntered.getSize() <= SIZEOFALPHABET){ //adding will set the objects to the right
                    if(!board[getY() / Y_OFFSET][getX() / X_OFFSET + column].shipPlaced) {
                        board[getY() / Y_OFFSET][getX() / X_OFFSET + column].setBackground(enteredColor); //It accesses it Y first then X, it simulates X-Y coordinates backwards for the grid!!!!!!
                    }
                }
            }
        }


        public void verifyButtonStates(){
            if (!aircraftButton.isEnabled() && !shipPlaced) { // if the aircraft is pressed, and no ship has been placed
                verifyBoard(aircraftCarrier, aircraftCarrier.getShipColor());
            }

            if (!battleShipButton.isEnabled() && !shipPlaced){
                verifyBoard(battleship, battleship.getShipColor());
            }

            if (!submarineButton.isEnabled() && !shipPlaced){
                verifyBoard(submarine, submarine.getShipColor());
            }

            if (!destroyerButton.isEnabled() && !shipPlaced){
                verifyBoard(destroyer, destroyer.getShipColor());
            }

            if (!patrolBoatButton.isEnabled() && !shipPlaced){
                verifyBoard(patrolBoat, patrolBoat.getShipColor());
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) { //we're gonna use this function to set up the ships and also take shots

        }

        @Override
        public void mouseEntered(MouseEvent e) {
//            System.out.println(getX()/X_OFFSET + " " + getY()/Y_OFFSET);  //use this to calculate clientBoard coordinates
            if(!readyCheck){
                verifyButtonStates();
            }

        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (!aircraftButton.isEnabled() && !shipPlaced) { //allows the change of the ship to go through
                updateBoard(aircraftCarrier, aircraftCarrier.getShipColor());
                aircraftButton.setEnabled(true);
            }

            if (!battleShipButton.isEnabled() && !shipPlaced){
                updateBoard(battleship, battleship.getShipColor());
                battleShipButton.setEnabled(true);

            }

            if (!submarineButton.isEnabled() && !shipPlaced){
                updateBoard(submarine, submarine.getShipColor());
                submarineButton.setEnabled(true);

            }

            if (!destroyerButton.isEnabled() && !shipPlaced){
                updateBoard(destroyer, destroyer.getShipColor());
                destroyerButton.setEnabled(true);

            }

            if (!patrolBoatButton.isEnabled() && !shipPlaced){
                updateBoard(patrolBoat, patrolBoat.getShipColor());
                patrolBoatButton.setEnabled(true);
            }


            if (turnState && readyCheck && sendMessage.isEnabled()) { //if it's your turn and the game has begun, enable turn phases
                int yCoord = getY() / Y_OFFSET; //gets the y coordinate and divides it by offset to set to array coordinates
                int xCoord = getX() / X_OFFSET;



                client.sendTurnInformation(turnState, yCoord, xCoord); //but after this check it'll determine if it hit or not
                //this function allows us to send where the person clicked on the grid, sets it to access arrays by dividing the coordinates by its offsets
                //then sends those coordinates through the sendTurnInformation
                clientBoard[yCoord][xCoord].setBackground(shots[MISS]);// will always set it to blue when placed
                turnState = false; //allows the player to not be able to make rapid fire shots in one go
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

            if (!aircraftButton.isEnabled() && !shipPlaced) { //when the aircraft button is pressed, and they didn't register a ship placement, go through this function
                //resets the board to default color because they didn't set anything
                verifyBoard(aircraftCarrier, Color.darkGray); //passes in default board so that way it doesn't stay unless mousepress
            }

            if (!battleShipButton.isEnabled() && !shipPlaced){
                verifyBoard(battleship, Color.darkGray);
            }

            if (!submarineButton.isEnabled() && !shipPlaced){
                verifyBoard(submarine, Color.darkGray);
            }

            if (!destroyerButton.isEnabled() && !shipPlaced){
                verifyBoard(destroyer, Color.darkGray);
            }

            if (!patrolBoatButton.isEnabled() && !shipPlaced){
                verifyBoard(patrolBoat, Color.darkGray);
            }
        }
    }
}