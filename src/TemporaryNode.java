// IN2011 Computer Networks
// Coursework 2023/2024
//
// Submission by
// Nazifa Chowdhury
// 220051752
// nazifa.chowdhury@city.ac.uk


import java.io.*;
import java.math.BigInteger;
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


    @Override
    public boolean start(String startingNodeName, String startingNodeAddress) {
        try {
            socket = new Socket(startingNodeAddress.split(":")[0], Integer.parseInt(startingNodeAddress.split(":")[1]));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

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
        // Implement this!
        // Return true if the store worked
        // Return false if the store failed

        try {

            int keyLines = key.split("\n", -1).length;
            int valueLines = value.split("\n", -1).length;
            String request = "PUT? " + keyLines + " " + valueLines + "\n" + key + "\n" + value + "\n";
            writer.write(request);
            writer.flush();

            String response = reader.readLine();

            return "SUCCESS".equals(response);
        } catch (Exception e) {
            System.err.println("IO error during 'store': " + e.getMessage());
            return false;
        }


    }

//    @Override
//    public String get(String key) {
//        // Implement this!
//        // Return the string if the get worked
//        // Return null if it didn't
//
//        try {
//            int keyLines = key.split("\n").length;
//
//            String request = "GET? " + keyLines + "\n" + key;
//            writer.write(request);
//            writer.flush();
//
//            String ci = reader.readLine();
//            System.out.println("REPLY: " + ci);
//            if (ci.startsWith("VALUE")) {
//                String[] response = ci.split(" ");
//                String string = "";
//                int l = Integer.parseInt(response[1]);
//                for (int i = 0; i < l; i++) {
//                    String k = reader.readLine();
//                    string = string + k + "\n";
//
//                }
//                return string;
//            }
//        } catch (Exception e) {
//            System.err.println("IO error during 'get': " + e.getMessage());
//        }
//        return null;
//
//    }




        public boolean sendEchoRequest() {
        try {
            writer.write("ECHO?\n");
            writer.flush();
            String response = reader.readLine();

            System.out.println(response);
            return response.equals("OHCE");

        } catch (IOException e) {
            System.err.println("Error sending ECHO request: " + e.getMessage());
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
            return false;
        }
    }


    public Map<String, String> sendNearestRequest(String hashID) {
        Map<String, String> nearestNodes = new HashMap<>();
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
                    nearestNodes.put(nodeName,nodeAddr);
                    System.out.println(nodeName + " " + nodeAddr);
               }
            }
        } catch (IOException e) {
            System.err.println("Error sending NEAREST? request: " + e.getMessage());
        }
        return nearestNodes;
    }


//    @Override
//    public String get(String key) {
//        try {
//            int keyLines = key.split("\n").length;
//            writer.write("GET? " + keyLines + "\n" + key);
//            writer.flush();
//
//            String response = reader.readLine();
//            System.out.println("Response: " + response);
//
//            if (response.startsWith("VALUE")) {
//                StringBuilder valueBuilder = new StringBuilder();
//                String[] parts = response.split(" ");
//                int valueLines = Integer.parseInt(parts[1]);
//                for (int i = 0; i < valueLines; i++) {
//                    valueBuilder.append(reader.readLine()).append('\n');
//                }
//                return valueBuilder.toString();
//            } else if (response.equals("NOPE")) {
//                Map<String, String> nearestNodes = sendNearestRequest(HashID.bytesToHex(HashID.computeHashID(key)));
//                //loop through map
//                // try connect to each node
//                // send GET
//                for (Map.Entry<String, String> entry : nearestNodes.entrySet()) {
//                    String nodeAddress = entry.getValue();
//                    String nodeName = entry.getKey();
//
//                    try (Socket socket = new Socket(nodeAddress.split(":")[0], Integer.parseInt(nodeAddress.split(":")[1]));
//                         BufferedWriter nodeWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//                         BufferedReader nodeReader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
//                        //send start
//
//                        nodeWriter.write("START 1 " + nodeName + "\n");
//                        nodeWriter.flush();
//
//                        String startResponse = nodeReader.readLine();
//                        System.out.println("Start response: " + startResponse);
//                        if (startResponse != null && startResponse.startsWith("START")) {
//
//                            nodeWriter.write("GET? " + keyLines + "\n" + key);
//                            nodeWriter.flush();
//
//                            String nodeResponse = nodeReader.readLine();
//                            if (nodeResponse.startsWith("VALUE")) {
//                                StringBuilder nodeValueBuilder = new StringBuilder();
//                                String[] parts = nodeResponse.split(" ");
//
//                                int valueLines = Integer.parseInt(parts[1]);
//                                for (int i = 0; i < valueLines; i++) {
//                                    nodeValueBuilder.append(nodeReader.readLine()).append('\n');
//                                }
//                                return nodeValueBuilder.toString();
//                                }
//                            }
//                        } catch(IOException e){
//                            System.err.println("Failed to connect to node at " + nodeAddress);
//                        }
//                    }
//
//                //Need to find the minimimum distance between each of the nearest nodes and the hashKey
//                //Once that is found -> go to that node
//                //Start the cycle again once it is found.
//                }
//
//        } catch (Exception e) {
//                System.err.println("Error during 'get': " + e.getMessage());
//            }
//            return null;
//        }
//


//    @Override
//    public String get(String key) {
//        try {
//            int keyLines = key.split("\n").length;
//            writer.write("GET? " + keyLines + "\n" + key);
//            writer.flush();
//
//            String response = reader.readLine();
//            System.out.println("Response: " + response);
//
//            if (response.startsWith("VALUE")) {
//                return processValueResponse(reader, response);
//            }
//            else if (response.equals("NOPE")) {
//                return processNopeResponse(key);
//            }
//        } catch (Exception e) {
//            System.err.println("Error during 'get': " + e.getMessage());
//        }
//        return null;
//    }
//
//    private String processValueResponse(BufferedReader reader, String response) throws IOException {
//        StringBuilder valueBuilder = new StringBuilder();
//        String[] parts = response.split(" ");
//        int valueLines = Integer.parseInt(parts[1]);
//        for (int i = 0; i < valueLines; i++) {
//            valueBuilder.append(reader.readLine()).append('\n');
//        }
//        return valueBuilder.toString().trim();
//    }
//
//    private String processNopeResponse(String key) throws Exception {
//        Map<String, String> nearestNodes = sendNearestRequest(HashID.bytesToHex(HashID.computeHashID(key + "\n")));
//
//        String closestNodeAddress = null;
//        String closestNodeName = null;
//        int minDistance = Integer.MAX_VALUE;
//
//        BigInteger keyHash = new BigInteger(HashID.bytesToHex(HashID.computeHashID(key + "\n")), 16);
//
//        for (Map.Entry<String, String> entry : nearestNodes.entrySet()) {
//            String nodeName = entry.getKey();
//            String nodeAddress = entry.getValue();
//            BigInteger nodeHash = new BigInteger(HashID.bytesToHex(HashID.computeHashID(nodeName + "\n")), 16);
//            int distance = keyHash.xor(nodeHash).bitCount();
//
//            if (distance < minDistance) {
//                minDistance = distance;
//                closestNodeAddress = nodeAddress;
//                closestNodeName = nodeName;
//            }
//        }
//
//        if (closestNodeAddress != null) {
//            System.out.println("Attempting to get value from the closest node: " + closestNodeName + " at " + closestNodeAddress);
//            return attemptGetValueFromNode(key, closestNodeName, closestNodeAddress);
//        }
//
//        return null;
//    }
//
////    private String processNopeResponse(String key) throws Exception {
////        Map<String, String> nearestNodes = sendNearestRequest(HashID.bytesToHex(HashID.computeHashID(key + "\n")));
////
////        //find minimum of the 3 nodes relative to the key
////        //once thats found, connect to that node
////        //once connected, continue cycle
////        for (Map.Entry<String, String> entry : nearestNodes.entrySet()) {
////            String nodeName = entry.getKey();
////            String nodeAddress = entry.getValue();
////            System.out.println("Trying nearest node " + nodeName + " at " + nodeAddress);
////
////            String result = attemptGetValueFromNode(key, nodeName, nodeAddress);
////            if (result != null) {
////                return result;
////            }
////        }
////        return null;
////    }
//
//    private String attemptGetValueFromNode(String key, String nodeName, String nodeAddress) {
//        try (Socket nodeSocket = new Socket(nodeAddress.split(":")[0], Integer.parseInt(nodeAddress.split(":")[1]));
//             BufferedWriter nodeWriter = new BufferedWriter(new OutputStreamWriter(nodeSocket.getOutputStream()));
//             BufferedReader nodeReader = new BufferedReader(new InputStreamReader(nodeSocket.getInputStream()))) {
//
//            nodeWriter.write("START 1 " + nodeName + "\n");
//            nodeWriter.flush();
//
//            String startResponse = nodeReader.readLine();
//            System.out.println("Start response from " + nodeName + ": " + startResponse);
//
//            int keyLines = key.split("\n").length;
//            nodeWriter.write("GET? " + keyLines + "\n" + key);
//            nodeWriter.flush();
//
//            String response = nodeReader.readLine();
//            System.out.println("Response from " + nodeName + ": " + response);
//            if (response.startsWith("VALUE")) {
//                return processValueResponse(nodeReader, response);
//            }
//        } catch (IOException e) {
//            System.err.println("Failed to connect to nearest node at " + nodeAddress + ": " + e.getMessage());
//        } catch (Exception e) {
//            System.err.println("Error processing response from " + nodeName + ": " + e.getMessage());
//        }
//        return null;
//    }

    @Override
    public String get(String key) {
        try {
            return getRecursive(key, new HashSet<>());
        } catch (Exception e) {
            System.err.println("Error during 'get': " + e.getMessage());
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
            Map<String, String> nearestNodes = sendNearestRequest(HashID.bytesToHex(HashID.computeHashID(key + "\n")));
            for (Map.Entry<String, String> entry : nearestNodes.entrySet()) {
                if (!attemptedNodes.contains(entry.getValue())) {
                    attemptedNodes.add(entry.getValue());
                    String result = attemptGetValueFromNode(key, entry.getKey(), entry.getValue(), attemptedNodes);
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

    private String attemptGetValueFromNode(String key, String nodeName, String nodeAddress, Set<String> attemptedNodes) {
        try {
            try (Socket nodeSocket = new Socket(nodeAddress.split(":")[0], Integer.parseInt(nodeAddress.split(":")[1]));
                 BufferedWriter nodeWriter = new BufferedWriter(new OutputStreamWriter(nodeSocket.getOutputStream()));
                 BufferedReader nodeReader = new BufferedReader(new InputStreamReader(nodeSocket.getInputStream()))) {

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
                    Map<String, String> nearestNodes = sendNearestRequest(HashID.bytesToHex(HashID.computeHashID(key + "\n")));
                    for (Map.Entry<String, String> entry : nearestNodes.entrySet()) {
                        String nextNodeName = entry.getKey();
                        String nextNodeAddress = entry.getValue();
                        if (!attemptedNodes.contains(nextNodeAddress)) {
                            attemptedNodes.add(nextNodeAddress);
                            String result = attemptGetValueFromNode(key, nextNodeName, nextNodeAddress, attemptedNodes);
                            if (result != null) {
                                return result;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to connect to node at " + nodeAddress + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error during retrieval from node " + nodeName + ": " + e.getMessage());
        }
        return null;
    }



    public static void main(String[] args) {
        TemporaryNode node = new TemporaryNode();
        if (node.start("nazifa.chowdhury@city.ac.uk:YourNodeName", "127.0.0.1:1400")) {
            System.out.println("Connected to the network.");
            boolean storeSuccess = node.store("Welcome", "Hello\nWorld!");
            System.out.println((storeSuccess ? "SUCCESS" : "UNSUCCESSFUL") + "\n");

            String value = node.get("Welcome");
            System.out.println(value);

            boolean echoSuccess = node.sendEchoRequest();
            System.out.println(echoSuccess ? "ECHO successful" : "ECHO failed");

            boolean notifySuccess = node.notifyOtherNode("testNode@example.com:NodeName", "127.0.0.1:1400");
            System.out.println(notifySuccess);

            String hashID = "exampleHashID";
            node.sendNearestRequest(hashID);

            try {

                System.out.println(node.reader.readLine());
            } catch (Exception e) {
                e.getMessage();
            }

            try {
                node.socket.close();
            } catch (Exception e) {
                System.err.println("Exception while closing socket: " + e);
            }
        } else {
            System.out.println("Failed to connect to the network.");
        }
    }
}




