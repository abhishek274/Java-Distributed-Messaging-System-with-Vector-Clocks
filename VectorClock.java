import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class VectorClock {
    static int vectorClock[] = {0, 0, 0};
    static String nodeList[] = {"A", "B", "C"};
    static int socketPorts[] = {8080, 8081, 8082};

    static HashMap<String, Integer> nodePortMap = new HashMap<String, Integer>();
    static String curNode;
    static int curPort;
    static int nodeIndex;

    public static void main(String[] args) throws InterruptedException, IOException {


        //map nodes and ports
        nodePortMap.put("A", 0);
        nodePortMap.put("B", 1);
        nodePortMap.put("C", 2);


        ServerSocket socket = null;
        while (true) {

            try {
                int num = (new Random()).nextInt(3); // Taking 3 random values
                nodeIndex = num;
                curNode = nodeList[num];
                curPort = socketPorts[num];
                socket = new ServerSocket(curPort);

                break;
            } catch (BindException e) {
                // Handle exceptions that may occur when binding to a port that is already in use.
            }

        }

        System.out.println("Node " + curNode + " started on port " + curPort + " has been initiated");


        new Thread(new MessageReceiver(nodePortMap, vectorClock, socket, curNode, curPort)).start(); //calling receiver thread

        new Thread(new MessageSender(nodePortMap, vectorClock, curNode, socketPorts)).start(); //calling sender thread

    }
}


class MessageSender implements Runnable {
    private String curNode;
    private int[] socketPorts;
    private Map<String, Integer> nodePortMap;
    private int vectorClock[];

    MessageSender(Map<String, Integer> processMap, int vectorClock[],
                  String currentProcess, int portList[]) {
        this.nodePortMap = processMap;
        this.vectorClock = vectorClock;
        this.curNode = currentProcess;
        this.socketPorts = portList;
    }

    @Override
    public void run() {
        int nodeIndex = nodePortMap.get(curNode);
        System.out.println("This is Node- " + curNode);
        System.out.println("1. Send Message");
        System.out.println("2. Broadcast Message");
        while (true) {
            try {
                System.out.print("Enter your choice : "); //enter choice for unicast or broadcast
                Scanner sc = new Scanner(System.in);
                int choice = Integer.parseInt(sc.nextLine());
                if (choice == 1) {
                    System.out.println("Enter Node name to whom you want to send a message:");
                    String sendTo = sc.nextLine();
                    int receiverPort = socketPorts[nodePortMap.get(sendTo)]; //fetching receiver port from hash map
                    System.out.println("Establishing connection with node running on port:" + receiverPort);
                    Socket socket = new Socket("127.0.0.1", receiverPort); //fetching receiver port
                    System.out.print("Enter the message you want to send:");
                    String msg = sc.nextLine();// reading message
                    System.out.println(curNode + "'s Vector Clock before Sending message " + " [A, B, C] : [" + vectorClock[0] + ", " + vectorClock[1] + ", " + vectorClock[2] + "]"); //printing vector time
                    System.out.println("===============================================================");
                    vectorClock[nodeIndex] = vectorClock[nodeIndex] + 1; // incrementing vector clock
                    ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                    outputStream.writeObject(curNode);
                    outputStream.writeObject(msg);
                    outputStream.writeObject(vectorClock);
                    outputStream.flush();
                    socket.close();
                    System.out.println(curNode + "'s Vector Clock After Sending message " + " [A, B, C] : [" + vectorClock[0] + ", " + vectorClock[1] + ", " + vectorClock[2] + "]"); //printing vector time
                    System.out.println("===============================================================");
                } else if(choice == 2) { // for broadcasting
                    System.out.print("Enter the message you want to broadcast:");
                    String data = sc.nextLine(); // message to broadcast
                    int currentPort = socketPorts[nodePortMap.get(curNode)];//fetching current node from hashmap

                    List<Integer> otherPorts = new ArrayList<Integer>();//list to store all the ports
                    for (int i = 0; i < socketPorts.length; i++) {
                        if (socketPorts[i] != currentPort) {
                            otherPorts.add(socketPorts[i]);//adding ports other that current port to send message
                        }
                    }
                    String allPorts = otherPorts.stream().map(String::valueOf).collect(Collectors.joining(","));
                    System.out.println("Broadcasting the message to Nodes running on ports : " + allPorts);

                    for (Integer port : otherPorts) {
                        int receiverPort = port;
                        System.out.println("Establishing connection with node running on port:" + receiverPort);
                        Socket socket = new Socket("127.0.0.1", receiverPort);// opening socket connection
                        System.out.println(curNode + " Vector Clock before Sending message " + " [A, B, C] : [" + vectorClock[0] + ", " + vectorClock[1] + ", " + vectorClock[2] + "]"); //printing vector time
                        System.out.println("===============================================================");
                        vectorClock[nodeIndex] = vectorClock[nodeIndex] + 1;// incrementing vector clock
                        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                        outputStream.writeObject(curNode);
                        outputStream.writeObject(data);
                        outputStream.writeObject(vectorClock);
                        outputStream.flush();
                        System.out.println(curNode + " Vector Clock After Sending message " + " [A, B, C] : [" + vectorClock[0] + ", " + vectorClock[1] + ", " + vectorClock[2] + "]"); //printing vector time
                        System.out.println("===============================================================");
                        socket.close();
                        System.out.println();
                    }
                }
                else {
                    System.out.println("Enter correct choice");
                }

            } catch (Exception e) {
                // can handle exceptions for socket issues
            }

        }

    }

}

class MessageReceiver implements Runnable {

    private String curNode;
    private Map<String, Integer> nodePortMap;
    private int vectorClock[];
    private ServerSocket socket;
    private int curPort;

    public MessageReceiver(Map<String, Integer> nodePortMap, int vectorClock[], ServerSocket socket, String curNode, int curPort) {
        this.nodePortMap = nodePortMap;
        this.vectorClock = vectorClock;
        this.socket = socket;
        this.curNode = curNode;
        this.curPort = curPort;
    }

    @Override
    public void run() {

        try {
            while (true) {
                Socket socket = this.socket.accept();//to connect to sender
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                String senderNode = String.valueOf(inputStream.readObject());
                String data = String.valueOf(inputStream.readObject());
                int senderVectorClock[] = (int[]) inputStream.readObject();
                System.out.println("Node - " + senderNode + " : sent" + data);
                System.out.println(curNode + "'s Vector Clock before Receiving data " + " [A, B, C] : [" + vectorClock[0] + ", " + vectorClock[1] + ", " + vectorClock[2] + "]"); //printing vector time
                System.out.println("===============================================================");
                int tempClock[] = new int[3];
                int index = nodePortMap.get(curNode);//fetching current node from hash map
                vectorClock[index] += 1;// incrementing vector clock
                for (int i = 0; i < senderVectorClock.length; i++) {
                    tempClock[i] = senderVectorClock[i];
                    vectorClock[i] = vectorClock[i] > tempClock[i] ? vectorClock[i] : tempClock[i];//updating vector clock with max value
                }
                System.out.println(curNode + "'s Vector Clock After Receiving data " + " [A, B, C] : [" + vectorClock[0] + ", " + vectorClock[1] + ", " + vectorClock[2] + "]"); //printing vector time
                System.out.println("===============================================================");
                System.out.println();
                inputStream.close();
            }

        } catch (Exception e) {
            // can handle exceptions for socket issues
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // can handle exceptions for socket issues
            }
        }

    }
}


