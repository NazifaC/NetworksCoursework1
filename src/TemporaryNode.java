// IN2011 Computer Networks
// Coursework 2023/2024
//
// Submission by
// Nazifa Chowdhury
// 220051752
// nazifa.chowdhury@city.ac.uk


import javax.swing.plaf.synth.SynthOptionPaneUI;
import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;


// DO NOT EDIT starts
interface TemporaryNodeInterface {
    public boolean start(String startingNodeName, String startingNodeAddress);
    public boolean store(String key, String value);
    public String get(String key);
}
// DO NOT EDIT ends


public class TemporaryNode implements TemporaryNodeInterface {

    public Socket socket;
    private BufferedReader reader;

    private BufferedWriter writer;
    private String startingNodeName;

    private String startingNodeAddress;


    @Override
    public boolean start(String startingNodeName, String startingNodeAddress) {
        try {
            socket = new Socket(startingNodeAddress.split(":")[0], Integer.parseInt(startingNodeAddress.split(":")[1]));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            this.startingNodeName= startingNodeName;
            this.startingNodeAddress= startingNodeAddress;

            String startMessage = "START 1 " + startingNodeName + "\n";
            writer.write(startMessage);
            writer.flush();

            String response = reader.readLine();

            if (response != null && response.startsWith("START")) {
                return true;
            } else {
                System.out.println("Unexpected or no response from FullNode.");
                return false;
            }

        } catch (IOException e) {
            System.err.println("Could not start TemporaryNode: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }




    @Override
    public boolean store(String key, String value) {
        try {
            return storeRecursive(key, value, new HashSet<>());
        } catch (Exception e) {
            System.err.println("Error during 'get': " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private boolean storeRecursive(String key, String value, Set<String> attemptedNodes) throws Exception {
        int keyLines = key.split("\n").length;
        int valueLines = value.split("\n").length;
        String request = "PUT? " + keyLines + " " + valueLines + "\n" + key + value;
        writer.write(request);
        writer.flush();

        String response = reader.readLine();
        System.out.println("Response: " + response);

        if (response.startsWith("SUCCESS")) {
            return true;
        } else if (response.equals("FAILED")) {
            ArrayList<NodeInfo> nearestNodes = sendNearestRequest(HashID.bytesToHex(HashID.computeHashID(key + "\n")), writer, reader);
            for (NodeInfo nodeInfo: nearestNodes) {
                String nextNodeName = nodeInfo.nodeName;
                String nextNodeAddress = nodeInfo.nodeAddress;

                if (!attemptedNodes.contains(nextNodeAddress)) {
                    attemptedNodes.add(nextNodeAddress);
                    String result = attemptStoreAtNode(key,value,nextNodeName, nextNodeAddress);
                    if (result != null) {
                        return false;
                    }
                }
            }
        }
        return false;
    }




        private String attemptStoreAtNode(String key, String value, String nodeName, String nodeAddress) throws Exception {
        NodeInfo minNode = new NodeInfo(nodeName, nodeAddress);
        Socket nodeSocket = new Socket(InetAddress.getByName(nodeAddress.split(":")[0]), Integer.parseInt(nodeAddress.split(":")[1]));
        Writer nodeWriter = new OutputStreamWriter(nodeSocket.getOutputStream());
        BufferedReader nodeReader = new BufferedReader(new InputStreamReader(nodeSocket.getInputStream()));
        while (true) {
            nodeWriter.write("START 1 " + nodeName + "\n");
            nodeWriter.flush();

            String startResponse = nodeReader.readLine();
            System.out.println("Start response from " + nodeName + ": " + startResponse);

            int keyLines = key.split("\n", -1).length;
            int valueLines = value.split("\n", -1).length;
           nodeWriter.write("PUT? " + keyLines + " " + valueLines + "\n" + key + value);
           nodeWriter.flush();

            String response = nodeReader.readLine();
            System.out.println("Response from " + nodeName + ": " + response);

            if (response.startsWith("SUCCESS")) {
                return "SUCCESS";
            } else if (response.equals("FAILED")) {

                byte[] hash = HashID.computeHashID(key);
                String hashHex = HashID.bytesToHex(hash);
                ArrayList<NodeInfo> nearestNodes = sendNearestRequest(hashHex, nodeWriter, nodeReader);
                for (NodeInfo n: nearestNodes) {
                    System.out.println("NEAREST: " + n.nodeName);
                }
                for (NodeInfo nodeInfo: nearestNodes) {
                    byte[] h1 = HashID.computeHashID(minNode.nodeName+"\n");
                    byte[] h2 = HashID.computeHashID(nodeInfo.nodeName+"\n");
                    int distanceMin = hashDistance(hash, h1);
                    int distanceName = hashDistance(h2, hash);

                    if(distanceMin > distanceName){
                        System.out.println(nodeInfo.nodeName);
                        minNode = nodeInfo;
                    }
                }

                System.out.println("MIN: " + minNode.nodeName);

                if(Objects.equals(minNode.nodeName, nodeName)){
                    return null;
                }


                nodeSocket.close();

                nodeSocket = new Socket(InetAddress.getByName(minNode.nodeAddress.split(":")[0]), Integer.parseInt(minNode.nodeAddress.split(":")[1]));
                nodeWriter = new OutputStreamWriter(nodeSocket.getOutputStream());
                nodeReader = new BufferedReader(new InputStreamReader(nodeSocket.getInputStream()));


            }
        }
    }


    public boolean sendEchoRequest() {
        try {
            writer.write("ECHO?\n");
            writer.flush();
            String response = reader.readLine();

            if (response == null) {
                System.err.println("No response received, the connection might have been closed.");
                return false;
            }

            System.out.println(response);
            return response.equals("OHCE");
        } catch (IOException e) {
            System.err.println("Error sending ECHO request: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean notifyOtherNode(String nodeName, String nodeAddress) {
        try {
            String notifyMessage = "NOTIFY?\n" + nodeName + "\n" + nodeAddress + "\n";
            writer.write(notifyMessage);
            writer.flush();

            String response = reader.readLine();
            System.out.println(response);

            return "NOTIFIED".equals(response);
        } catch (IOException e) {
            System.err.println("Error sending NOTIFY request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public ArrayList<NodeInfo> sendNearestRequest(String hashID, Writer writer, BufferedReader reader) {
        ArrayList<NodeInfo> nearestNodes = new ArrayList<>();
        try {
            String nearestRequest = "NEAREST? " + hashID + "\n";
            System.out.println("Sending: " + nearestRequest);
            writer.write(nearestRequest);
            writer.flush();

            String responseHeader = reader.readLine();
            System.out.println("Response: " + responseHeader);
            if (responseHeader != null && responseHeader.startsWith("NODES")) {
                int numberOfNodes = Integer.parseInt(responseHeader.split(" ")[1]);
                for (int i = 0; i < numberOfNodes; i++) {
                    String nodeName = reader.readLine();
                    String nodeAddr = reader.readLine();
                    nearestNodes.add(new NodeInfo(nodeName,nodeAddr));
                    System.out.println(nodeName + " " + nodeAddr);
               }
            }
        } catch (IOException e) {
            System.err.println("Error sending NEAREST? request: " + e.getMessage());
            e.printStackTrace();
        }
        return nearestNodes;
    }



    @Override
    public String get(String key) {
        try {
            return getRecursive(key, new HashSet<>());
        } catch (Exception e) {
            System.err.println("Error during 'get': " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


    private String getRecursive(String key, Set<String> attemptedNodes) throws Exception {
        int keyLines = key.split("\n").length;
        writer.write("GET? " + keyLines + "\n" + key);
        writer.flush();

        String response = reader.readLine();
        System.out.println("Response: " + response);

        if (response.startsWith("VALUE")) {
            return processValueResponse(reader, response);
        } else if (response.equals("NOPE")) {
            ArrayList<NodeInfo> nearestNodes = sendNearestRequest(HashID.bytesToHex(HashID.computeHashID(key + "\n")), writer, reader);
            for (NodeInfo nodeInfo: nearestNodes) {
                String nextNodeName = nodeInfo.nodeName;
                String nextNodeAddress = nodeInfo.nodeAddress;

                if (!attemptedNodes.contains(nextNodeAddress)) {
                    attemptedNodes.add(nextNodeAddress);
                    String result = attemptGetValueFromNode(key, nextNodeName, nextNodeAddress, attemptedNodes);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }


    private String processValueResponse(BufferedReader reader, String response) throws IOException {
        StringBuilder valueBuilder = new StringBuilder();
        String[] parts = response.split(" ");
        int valueLines = Integer.parseInt(parts[1]);
        for (int i = 0; i < valueLines; i++) {
            valueBuilder.append(reader.readLine()).append('\n');
        }
        return valueBuilder.toString().trim();
    }

    private String attemptGetValueFromNode(String key, String nodeName, String nodeAddress, Set<String> attemptedNodes) throws Exception {
        NodeInfo minNode = new NodeInfo(nodeName,nodeAddress);
        NodeInfo oldNode = new NodeInfo(nodeName,nodeAddress);
        Socket nodeSocket = new Socket(InetAddress.getByName(nodeAddress.split(":")[0]), Integer.parseInt(nodeAddress.split(":")[1]));
        Writer nodeWriter = new OutputStreamWriter(nodeSocket.getOutputStream());
        BufferedReader nodeReader = new BufferedReader(new InputStreamReader(nodeSocket.getInputStream()));
        ArrayList<String> connectedNodes = new ArrayList<>();
        connectedNodes.add(nodeName);
        while (true) {
                    nodeWriter.write("START 1 " + nodeName + "\n");
                    nodeWriter.flush();

                    String startResponse = nodeReader.readLine();
                    System.out.println("Start response from " + nodeName + ": " + startResponse);

                    int keyLines = key.split("\n").length;
                    nodeWriter.write("GET? " + keyLines + "\n" + key);
                    nodeWriter.flush();

                    String response = nodeReader.readLine();
                    System.out.println("Response from " + nodeName + ": " + response);

                    if (response.startsWith("VALUE")) {
                        return processValueResponse(nodeReader, response);
                    } else if (response.equals("NOPE")) {

                        byte[] hash = HashID.computeHashID(key);
                        String hashHex = HashID.bytesToHex(hash);
                        ArrayList<NodeInfo> nearestNodes = sendNearestRequest(hashHex, nodeWriter, nodeReader);
                        for (NodeInfo n: nearestNodes) {
                            System.out.println("NEAREST: " + n.nodeName);
                        }
                        for (NodeInfo nodeInfo: nearestNodes) {
                            byte[] h1 = HashID.computeHashID(minNode.nodeName+"\n");
                            byte[] h2 = HashID.computeHashID(nodeInfo.nodeName+"\n");
                            int distanceMin = hashDistance(hash, h1);
                            int distanceName = hashDistance(h2, hash);

                            if(distanceMin > distanceName){
                                System.out.println(nodeInfo.nodeName);
                                oldNode = minNode;
                                minNode = nodeInfo;

                            }
                        }

                        System.out.println("l: "+oldNode.nodeName);
                        System.out.println("m: "+minNode.nodeName);

                        System.out.println("MIN: " + minNode.nodeName);

                        byte[] h1 = HashID.computeHashID(oldNode.nodeName+ "\n");
                        byte[] h2 = HashID.computeHashID(minNode.nodeName+ "\n");
                        int distance1 = hashDistance(hash, h1);
                        int distance2 = hashDistance(hash, h2);
                        System.out.println("this is:" + distance1);
                        System.out.println("this is:" + distance2);

                        if(connectedNodes.contains(minNode.nodeName)){
                            return null;
                        }

                        nodeSocket.close();

                        nodeSocket = new Socket(InetAddress.getByName(minNode.nodeAddress.split(":")[0]), Integer.parseInt(minNode.nodeAddress.split(":")[1]));
                        nodeWriter = new OutputStreamWriter(nodeSocket.getOutputStream());
                        nodeReader = new BufferedReader(new InputStreamReader(nodeSocket.getInputStream()));

                        connectedNodes.add(minNode.nodeName);

                    }
        }
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

    public static void main(String[] args) {
        TemporaryNode node = new TemporaryNode();
        if (node.start("nazifa.chowdhury@city.ac.uk:YourNodeName", "127.0.0.1:1400")) {
            System.out.println("Connected to the network.");
            boolean storeSuccess = node.store("Welcome", "Hello\nWorld!");
            System.out.println((storeSuccess ? "SUCCESS" : "FAILED") + "\n");

            String value = node.get("Welcome\n");
            System.out.println(value);

            boolean echoSuccess = node.sendEchoRequest();
            System.out.println(echoSuccess ? "ECHO successful" : "ECHO failed");

            boolean notifySuccess = node.notifyOtherNode("testNode@example.com:NodeName", "127.0.0.1:1400");
            System.out.println(notifySuccess);

            String hashID = "0f003b106b2ce5e1f95df39fffa34c2341f2141383ca46709269b13b1e6b4832";

            try {

                System.out.println(node.reader.readLine());
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                node.socket.close();
            } catch (Exception e) {
                System.err.println("Exception while closing socket: " + e);
                e.printStackTrace();
            }
        } else {
            System.out.println("Failed to connect to the network.");
        }
    }
}




