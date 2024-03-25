// IN2011 Computer Networks
// Coursework 2023/2024
//
// Submission by
// YOUR_NAME_GOES_HERE
// YOUR_STUDENT_ID_NUMBER_GOES_HERE
// YOUR_EMAIL_GOES_HERE


import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

// DO NOT EDIT starts
interface FullNodeInterface {
    public boolean listen(String ipAddress, int portNumber);
    public void handleIncomingConnections(String startingNodeName, String startingNodeAddress);
}
// DO NOT EDIT ends


public class FullNode implements FullNodeInterface {
    private ServerSocket socket;
    private int portNumber;
    private final HashMap<String, String> keyValueStore = new HashMap<>();
    private final HashMap<String, String> networkMap = new HashMap<>();

    private final Map<String, String[]> nodeInfos = new HashMap<>();

    private List<String> nearbyNodeAddresses = new ArrayList<>();

    private final HashMap<String, String> nodeHashes = new HashMap<>();

    private String currentNodeHash;


    @Override
    public boolean listen(String ipAddress, int portNumber) {
        this.portNumber = portNumber;
        String currentNodeInfo = ipAddress + ":" + portNumber;
        try {
            socket = new ServerSocket(portNumber);
            System.out.println("FullNode listening on " + ipAddress + ":" + portNumber);
            ExecutorService executor = Executors.newCachedThreadPool();

            while (true) {
                Socket clientSocket = socket.accept();
                executor.submit(() -> handleClient(clientSocket));

                currentNodeHash = HashID.hashString(currentNodeInfo + "\n");
            }
        } catch (IOException e) {
            System.err.println("Could not listen on " + ipAddress + ":" + portNumber);
            return false;
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {
            String line;
            String nodeName = "nazifa.chowdhury@city.ac.uk:NodeName";
            int highestSupportedProtocolVersion = 1;

            while ((line = reader.readLine()) != null) {
                System.out.println("Request: " + line);
                String[] parts = line.split(" ");
                switch (parts[0]) {
                    case "START":
                        String startResponse = String.format("START %d %s\n", highestSupportedProtocolVersion, nodeName);
                        writer.write(startResponse);
                        writer.flush();
                        break;
                    case "PUT?":
                        handlePutRequest(line, reader, writer);
                        break;
                    case "GET?":
                        handleGetRequest(line, reader, writer);
                        break;
                    case "NOTIFY?":
                        handleNotifyRequest(reader, writer);
                        break;
                    case "NEAREST?":
                        handleNearestRequest(parts, writer);
                        break;
                    case "ECHO?":
                        writer.write("OHCE\n");
                        writer.flush();
                        break;
                    case "END":
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling client connection: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing the connection: " + e.getMessage());
            }
        }
    }

    private void handleGetRequest(String line, BufferedReader reader, BufferedWriter writer) throws Exception {
        int keyLines = Integer.parseInt(line.split(" ")[1]);
        StringBuilder keyBuilder = new StringBuilder();
        String currentLine;
        for (int i = 0; i < keyLines; i++) {
            currentLine = reader.readLine();
            keyBuilder.append(currentLine + "\n");
            System.out.println(currentLine);
        }
        String key = keyBuilder.toString();
        String hex = HashID.hashString(key);
        String value = keyValueStore.get(hex);
        if (value == null) {
            writer.write("NOPE\n");
            writer.flush();
            return;
        }
        int values = value.split("\n").length;
        writer.write("VALUE " + values + "\n" + value);
        writer.flush();
    }


    private void handlePutRequest(String line, BufferedReader reader, BufferedWriter writer) throws IOException {
        int keyLines = Integer.parseInt(line.split(" ")[1]);
        StringBuilder keyBuilder = new StringBuilder();
        String currentLine;
        for (int i = 0; i < keyLines; i++) {
            currentLine = reader.readLine();
            keyBuilder.append(currentLine + "\n");
            System.out.println(currentLine);
        }
        String key = keyBuilder.toString();

        String hashedKey = HashID.hashString(key);

        int valueLines = Integer.parseInt(line.split(" ")[2]);
        StringBuilder valueBuilder = new StringBuilder();
        for (int i = 0; i < valueLines; i++) {
            currentLine = reader.readLine();
            valueBuilder.append(currentLine + "\n");
            System.out.println(currentLine);
        }
        String value = valueBuilder.toString();

        keyValueStore.put(hashedKey, value);
        writer.write("SUCCESS\n");
        writer.flush();
    }

    private void handleNotifyRequest(BufferedReader reader, BufferedWriter writer) throws IOException {
        String nodeName = reader.readLine();
        String nodeAddress = reader.readLine();

        if (nodeName != null && nodeAddress != null) {
            networkMap.put(nodeName, nodeAddress);
            System.out.println("Node notified: " + nodeName + " at " + nodeAddress);


            writer.write("NOTIFIED\n");
            writer.flush();
            notifyNearbyNodes(nodeName, nodeAddress);

        } else {
            writer.write("ERROR Invalid NOTIFY? format\n");
            writer.flush();
        }
    }



    public void notifyNearbyNodes(String nodeName, String nodeAddress) {
        for (String address : nearbyNodeAddresses) {
            try (Socket socket = new Socket(address.split(":")[0], Integer.parseInt(address.split(":")[1]));
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

                String notifyMessage = "NOTIFY?\n" + nodeName + "\n" + nodeAddress + "\n";
                writer.write(notifyMessage);
                writer.flush();

                System.out.println("Notified nearby node at " + address);
            } catch (IOException e) {
                System.err.println("Failed to notify nearby node at " + address + ": " + e.getMessage());
            }
        }
    }

    private void handleNearestRequest(String[] parts, BufferedWriter writer) throws IOException {
        if (parts.length == 2) {
            String hashIDRequest = parts[1];
            try {
                List<Map.Entry<String, String>> closestNodes = getClosestNodes(hashIDRequest);

                System.out.println("Current network map: " + networkMap);
                System.out.println("Closest nodes to hashID " + hashIDRequest + ": " + closestNodes);

                writer.write("NODES " + closestNodes.size() + "\n");
                for (Map.Entry<String, String> node : closestNodes) {
                    writer.write(node.getKey() + "," + node.getValue() + "\n");
                }
                writer.flush();
            } catch (Exception e) {
                writer.write("ERROR processing NEAREST? request\n");
                writer.flush();
            }
        } else {
            writer.write("ERROR Invalid NEAREST? format\n");
            writer.flush();
        }
    }


    private List<Map.Entry<String, String>> getClosestNodes(String hashID) {
        List<Map.Entry<String, String>> eligibleNodes = networkMap.entrySet().stream()
                .filter(entry -> !nodeHashes.get(entry.getKey()).equals(currentNodeHash))
                .collect(Collectors.toList());

        eligibleNodes.sort(Comparator.comparingInt(e -> hashDistance(hashID, nodeHashes.get(e.getKey()))));
        return eligibleNodes.subList(0, Math.min(3, eligibleNodes.size()));
    }


    private int hashDistance(String hashID1, String hashID2) {
        BigInteger b1 = new BigInteger(hashID1, 16);
        BigInteger b2 = new BigInteger(hashID2, 16);
        return b1.xor(b2).bitCount();
    }




    @Override
    public void handleIncomingConnections(String startingNodeName, String startingNodeAddress) {
        // Implement this!

        System.out.println("Attempting to connect to the network via " + startingNodeName + " at " + startingNodeAddress);
        try {
            String[] parts = startingNodeAddress.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid starting node address format. Expected format: IP:Port");
            }
            String ip = parts[0];
            int port = Integer.parseInt(parts[1]);

            Socket socket = new Socket(ip, port);
            System.out.println("Connected to the starting node " + startingNodeName + " at " + startingNodeAddress);

            Writer writer = new OutputStreamWriter(socket.getOutputStream());
            String notifyMessage = "NOTIFY\n" + startingNodeName + "\n" + startingNodeAddress + ":" + this.portNumber + "\n";
            writer.write(notifyMessage);
            writer.flush();
            System.out.println("NOTIFY message sent to " + startingNodeName);

            socket.close();
        } catch (IOException e) {
            System.err.println("Could not connect to starting node at " + startingNodeAddress + ": " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number in starting node address: " + startingNodeAddress);
        }
    }


    public static void main(String[] args) {
        FullNode node = new FullNode();
        if (node.listen("localhost", 1400)) {
            System.out.println("Full Node listening on localhost:1400");
            node.handleIncomingConnections("nazifa.chowdhury@city.ac.uk:YourNodeName", "127.0.0.1:1400");


                }
            }
        }



