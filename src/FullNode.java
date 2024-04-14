// IN2011 Computer Networks
// Coursework 2023/2024
//
// Submission by
// Nazifa Chowdhury
// 220051752
// nazifa.chowdhury@city.ac.uk


import org.w3c.dom.Node;

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
    private String name = "naz@city.ac.uk:impl1,fullNode";
    private final HashMap<String, String> keyValueStore = new HashMap<>();
    private final HashMap<Integer, ArrayList<NodeInfo>> networkMap = new HashMap<>();

    private List<String> nearbyNodeAddresses = new ArrayList<>();

    private final HashMap<String, String> nodeHashes = new HashMap<>();

    private String currentNodeHash;

    private String currentNodeInfo;

    @Override
    public boolean listen(String ipAddress, int portNumber) {
        this.portNumber = portNumber;
        currentNodeInfo = ipAddress + ":" + portNumber;

        networkMap.put(0, new ArrayList<>());
        NodeInfo n = new NodeInfo(name, currentNodeInfo);
        networkMap.get(0).add(n);
        System.out.println(networkMap.get(0).get(0).nodeName);
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
            e.printStackTrace();
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
                System.out.println(line);
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
                        writer.write("END INVALID REQUEST\n");
                        writer.flush();
                        System.out.println("COMMUNICATION ENDED");
                        clientSocket.close();
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling client connection: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing the connection: " + e.getMessage());
                e.printStackTrace();
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


    private void handlePutRequest(String line, BufferedReader reader, BufferedWriter writer) throws Exception {
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
//        System.out.println(nodeIsNearby(hashedKey));
        if (nodeIsNearby(hashedKey)){
            keyValueStore.put(hashedKey, value);
            writer.write("SUCCESS\n");
        } else {
            writer.write("FAILED\n");
        }
        writer.flush();

    }
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private boolean nodeIsNearby(String hashedKey) throws Exception {
        byte[] currentHash = HashID.computeHashID(name+"\n");
        byte[] keyHash = hexStringToByteArray(hashedKey);
        int distance = hashDistance(currentHash, keyHash);

//        System.out.println(distance);
        ArrayList<NodeInfo> nearest3 = new ArrayList<>();
//        System.out.println(networkMap.get(0));
        for(int i = distance; i >= 0 && nearest3.size() < 3; i--){
            ArrayList<NodeInfo> c = networkMap.get(i);
//            System.out.println("C"+i+": " + c);
            if(c == null){
                continue;
            }
            for (int j = 0; j < 3 && nearest3.size() < 3; j++) {
                try{
                    nearest3.add(c.get(j));
                }catch (IndexOutOfBoundsException e){
                    break;
                }
            }
        }

//        System.out.println(nearest3);
        for (NodeInfo n: nearest3) {
            if(n.nodeName.equals(name)){
                return true;
            }
        }

        return false;
    }

    private void handleNotifyRequest(BufferedReader reader, BufferedWriter writer) throws Exception {
        String nodeName = reader.readLine();
        String nodeAddress = reader.readLine();

        if (nodeName != null && nodeAddress != null) {
            updateNetworkMap(nodeName, nodeAddress);
//            System.out.println("Node notified: " + nodeName + " at " + nodeAddress);


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
                e.printStackTrace();
                System.err.println("Failed to notify nearby node at " + address + ": " + e.getMessage());
            }
        }
    }

    private void handleNearestRequest(String[] parts, BufferedWriter writer) throws IOException {
        if (parts.length == 2) {
            String hashIDRequest = parts[1];
            try {
                List<NodeInfo> closestNodes = getClosestNodes(hashIDRequest);

                System.out.println("Current network map: " + networkMap);
                System.out.println("Closest nodes to hashID " + hashIDRequest + ": " + closestNodes);

                writer.write("NODES " + closestNodes.size() + "\n");
                for (NodeInfo node : closestNodes) {
                    writer.write(node.nodeName+"\n"+node.nodeAddress+"\n");
                }
                writer.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            writer.write("ERROR Invalid NEAREST? format\n");
            writer.flush();
        }
    }

    private List<NodeInfo> getClosestNodes(String hashID) throws Exception {
        byte[] hash = hexStringToByteArray(hashID);
        byte[] hashName = HashID.computeHashID(name+"\n");

        int distance = hashDistance(hash, hashName);

        ArrayList<NodeInfo> nodes = new ArrayList<>();
        for (int i = distance; i >= 0; i--){
            List<NodeInfo> n = networkMap.get(i);
            if(n == null){
                continue;
            }
            for(NodeInfo ni : n){
                nodes.add(ni);
                if(nodes.size() == 3){
                    break;
                }
            }
        }
        return nodes;
    }

    public synchronized void updateNetworkMap(String nodeName, String nodeAddress) throws Exception {
        byte[] nameHash = HashID.computeHashID(name+"\n");
        byte[] nodeHash = HashID.computeHashID(nodeName+"\n");
        int distance = hashDistance(nodeHash, nameHash);
        if (!networkMap.containsKey(distance)) {
            networkMap.put(distance, new ArrayList<>());
        }
        if (networkMap.size() == 3) {
            networkMap.get(distance).remove(0);
        }
//        System.out.println("Added node: " + nodeName + " at " + nodeAddress);

        networkMap.get(distance).add(new NodeInfo(nodeName, nodeAddress));
    }



    private int hashDistance(byte[] hash1, byte[] hash2) {
        int distance = 256;
        for (int i = 0; i < hash1.length; i++) {
            int diff = 0xff & (hash1[i] ^ hash2[i]);
            int calc = Integer.numberOfLeadingZeros(diff) - 24;
            distance -= calc;
            if (diff != 0) {
                break;
            }
        }
        return distance;
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
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number in starting node address: " + startingNodeAddress);
            e.printStackTrace();
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



