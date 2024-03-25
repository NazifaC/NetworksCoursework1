// IN2011 Computer Networks
// Coursework 2023/2024
//
// Submission by
// Nazifa Chowdhury
// 220051752
// nazifa.chowdhury@city.ac.uk


import java.io.*;
import java.net.Socket;


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
            System.out.println("Sending: " + startMessage);
            writer.write(startMessage);
            writer.flush();

            String response = reader.readLine();
            System.out.println("Response: " + response);

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

    @Override
    public String get(String key) {
        // Implement this!
        // Return the string if the get worked
        // Return null if it didn't

        try {
            int keyLines = key.split("\n").length;

            String request = "GET? " + keyLines + "\n" + key + "\n";
            writer.write(request);
            writer.flush();

            String ci = reader.readLine();
            System.out.println(ci);
            String[] response = ci.split(" ");
            if (ci.startsWith("VALUE")) {
                String string = "";
                int l = Integer.parseInt(response[1]);
                for (int i = 0; i < l; i++) {
                    String k = reader.readLine();
                    string = string + k + "\n";

                }
                return string;
            }
        } catch (Exception e) {
            System.err.println("IO error during 'get': " + e.getMessage());
        }
        return null;

    }


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


    public void sendNearestRequest(String hashID) {
        try {
            String nearestRequest = "NEAREST? " + hashID + "\n";
            System.out.println("Sending: " + nearestRequest);
            writer.write(nearestRequest);
            writer.flush();

            String responseHeader = reader.readLine();
            System.out.println("Response: " + responseHeader);

            if (responseHeader != null && responseHeader.startsWith("NODES")) {
                int numberOfNodes = Integer.parseInt(responseHeader.split(" ")[1]);
                for (int i = 0; i < numberOfNodes * 2; i++) {
                    String nodeInfo = reader.readLine();
                    System.out.println(nodeInfo);
                }
            }
        } catch (IOException e) {
            System.err.println("Error sending NEAREST? request: " + e.getMessage());
        }
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




