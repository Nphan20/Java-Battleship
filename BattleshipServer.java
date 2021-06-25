import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class BattleshipServer {

    private ServerSocket serverSocket;
    private final int port = 7777;
    private int numPlayers;
    private ServerSideConnection player1;
    private ServerSideConnection player2;
    private boolean player1Turn; //boolean values to be placed into each instance of the program
    private boolean player2Turn;
    private int player1yCoord; //these will be the array element that'll be searched
    private int player1xCoord; //to see if a ship is placed
    private int player2yCoord; //these will be the array element that'll be searched
    private int player2xCoord; //to see if a ship is placed

    public BattleshipServer(){
        System.out.println("Beginning Server. . .");
        numPlayers = 0; //starts off with no players
        player1Turn = true; //player one has their turn first
        player2Turn = false; // player two goes second
        try{
            serverSocket = new ServerSocket(port);
        }catch (IOException ex){
            ex.printStackTrace();
        }
    }

    public void acceptConnection(){
        try {
            System.out.println("waiting for server connection...");

            while (numPlayers < 2) {
                Socket player = serverSocket.accept();
                numPlayers++; //with each connection we accept we increment number of players, max is 2 so we get out of this loop after
                System.out.println("Player #" + numPlayers + " has connected to server");
                ServerSideConnection serverSideConnection = new ServerSideConnection(player, numPlayers);
                if(numPlayers == 1){
                    player1 = serverSideConnection; //sets up connection as player 1
                }else{
                    player2 = serverSideConnection; //sets up player 2
                }
                Thread thread = new Thread(serverSideConnection); //each application needs a thread
                thread.start();
            }
            System.out.println("Max players reached, beginning game.");

        }catch (IOException ex){
            ex.printStackTrace();
        }
    }



    private class ServerSideConnection implements Runnable{

        private final Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;
        private final int playerID;

        public ServerSideConnection(Socket socket, int id){
            this.socket = socket;
            playerID = id;
            try{
                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try{
                dataOut.writeInt(playerID); //sends which window is which player
//                dataOut.writeBoolean();
                if (playerID == 1) {
                    dataOut.writeBoolean(player1Turn);//sends the value of true to player1, allowing them to shoot first
                }else if(playerID == 2) {
                    dataOut.writeBoolean(player2Turn); // sends the value of false to player2, making them shoot second
                }
                dataOut.flush();

                while(true){ //adjust while loop to be like, while gameWon = false;
                    if(playerID == 1) {
                        player1Turn = dataIn.readBoolean();
                        System.out.println("Player 1 has shot");
                        player1yCoord = dataIn.readInt();
                        player1xCoord = dataIn.readInt();
                        System.out.println("The yCoord is: " + player1yCoord + " and the xCoord is: " + player1xCoord);
                        player2.sendTurnInformation(player1Turn, player1yCoord, player1xCoord);
                    }else{
                        player2Turn = dataIn.readBoolean();
                        System.out.println("Player 2 has shot");
                        player2yCoord = dataIn.readInt();
                        player2xCoord = dataIn.readInt();
                        System.out.println("The yCoord is: " + player2yCoord + " and the xCoord is: " + player2xCoord);
                        player1.sendTurnInformation(player2Turn, player2yCoord, player2xCoord);

                    }
                }

            }catch (IOException e){
                e.printStackTrace();
            }
        }

        public void sendTurnInformation(boolean turn, int yCoord, int xCoord){
            try{
                dataOut.writeBoolean(turn);
                dataOut.writeInt(yCoord);
                dataOut.writeInt(xCoord);

                dataOut.flush();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        BattleshipServer server = new BattleshipServer();
        server.acceptConnection();
    }

}