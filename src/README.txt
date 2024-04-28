README.txt

Project Build and Run Instructions:
1.Open a terminal within the directory of this file, and ensure to compile it using havac*.java
2. In the terminal run any of the following code to, and it should successfully work as expected:
java CmdLineStore martin.brain@city.ac.uk:martins-implementation-1.0,fullNode-20000 10.0.0.164:20000 nazifa.chowdhury@city.ac.uk Working
java CmdLineGet martin.brain@city.ac.uk:martins-implementation-1.0,fullNode-20000 10.0.0.164:20000 nazifa.chowdhury@city.ac.uk
java CmdLineFullNode martin.brain@city.ac.uk:martins-implementation-1.0,fullNode-20000 10.0.0.164:20000 10.0.0.205 20000

Completed Functionality:
I believe the Temporary node does successfully connect to the network, and can identify and fina a full node within its vicinity. The full node works expected from the requirements, handling PUT? ECHO? and GET? correctly