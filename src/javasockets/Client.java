
import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 * @author Anne
 */
public class Client {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        int fileSize; //second to last int in byte[] packet; the total file size
        int packetNum;//last int in byte[] packet; the num of the packet
        byte[][] packetsBuffered = new byte[8192][8192];
        //the file being written to by the client
        String outputfile = "/Users/ogaz/NetBeansProjects/JavaSockets/src/javasockets/testtestfile.txt";
        int fileSum = 0;//the number of bytes received by the client so far
        int packet_counter = 0;
        
        try{
            
            /////////////////////////////////////////////TCP CONNECTION/////////
            //New tcpClient Object
            tcpClient tcpClient = new tcpClient();

            //portReceive is the random UDP port number generated from the Client
            //this is the portReceive on which the client will receive packets
            int portReceive = tcpClient.createReceivePort();

            //creates the port by which the client will send packets to the server
            int portSend = tcpClient.createSendPort(portReceive);

            //this function will open a TCP connection to the server, then closes
            tcpClient.clientRunTCP(portReceive);
            /////////////////////////////////////////////////////END TCP////////
            
            ///////////////////////////////////////////////UDP CONNECTION///////
            //new client UDP object
            udpClient udpClient = new udpClient();
            
            //create UDP Datagram socket
            DatagramSocket sSocket = new DatagramSocket(portReceive);
                               
            
            /**********************************LOOP A TO RECEIVE PACKETS*******/
            byte packetReceived[];//the incoming packet
            DatagramPacket dpack;
            int fileTot = 0;
            int endnum = 0;
            boolean used[] = new boolean[8192];
            Arrays.fill(used, false);
            
            do{
                //receive a packet from server on portReceive; returns byte[] to packetReceived[]
                dpack = udpClient.receivePacket(sSocket);                
                packetReceived = dpack.getData();
                
                endnum = endnum +1;
                
                
                //extract file size from packet[]                                              
                int dataLength = dpack.getLength() - 8;//gives only the datalength, 0-dataLength
                
                byte[] sizeBytes = Arrays.copyOfRange(packetReceived, dataLength, dataLength+4);
                fileTot =  ((sizeBytes[0] & 0xFF) << 24) | ((sizeBytes[1] & 0xFF) << 16)
                            | ((sizeBytes[2] & 0xFF) << 8) | (sizeBytes[3] & 0xFF);
                
                
                //extract the packet number from packet[]
                byte[] seqBytes = Arrays.copyOfRange(packetReceived, dataLength+4, dataLength+8);                
                packetNum = ((seqBytes[0] & 0xFF) << 24) | ((seqBytes[1] & 0xFF) << 16)
                                | ((seqBytes[2] & 0xFF) << 8) | (seqBytes[3] & 0xFF);

                //check to see if packets already received and stored in packetsBuffered[]
                //store the packet in array at i = packetNum
                if(used[packetNum] == false){
                    packetsBuffered[packetNum] = Arrays.copyOfRange(packetReceived, 0, dpack.getLength()-8);
                    fileSum = fileSum + dpack.getLength()-8;
                    used[packetNum] = true;
                    packet_counter += 1;
                }

                
            }while (fileSum < fileTot);

            //when fileSum = fileSize, send ACK
            udpClient.sendAck(portSend, sSocket);
            System.out.println("ACK Sent. Port " + portReceive);
            /**********************************************END LOOP A**********/

            /***LOOP B - check if next packet is OK, if not, send ACK**********/        
            while(true){
                DatagramPacket okPacket = udpClient.receivePacket(sSocket);
                String check = new String(okPacket.getData(),0,okPacket.getLength());
                if(check.equals("OK")){

                    System.out.println("Done. Port " + portReceive);
                    break;
                }
                else{
                    udpClient.sendAck(portSend, sSocket);
                    System.out.println("ACK Resent. Port " + portReceive);
                }
            }
            /******************************************************************/
            /////////////////////////////////////////////END UDP CONNECTION/////


            //translate byte[][] to one byte[]
            ByteArrayOutputStream blah = new ByteArrayOutputStream();
            
            try{
                for(int i=0; i<packet_counter; i++){
                    blah.write(packetsBuffered[i]);              
                }
            }catch (Exception e){
                System.out.println(e);
                }
            
            byte[] finalByteArr = blah.toByteArray();            
            
            //write file
            try{
                PrintWriter output = new PrintWriter(outputfile);
                output.print(new String(finalByteArr));    
                output.close();
            }catch (Exception e){
                System.out.printf("Error while writing packetsReceived[] to file\n", e);
            }
            ////////////////////////////////END WRITING TO FILE/////////////////

        }
        catch (SocketException e){
            System.out.printf("Error in main class\n", e);
        }

    }
    
    public static class tcpClient {
        int tport = 21252;
    
        private final int min = 16384;
        private final int  max = 65535;

        //returns a random  number to use as the UDP portReceive on which 
        //the client will receive packets from server
        public int createReceivePort(){

           Random rand = new Random();
           int randomNum = rand.nextInt((max-min + 1) + min);
            return randomNum;
        }

        public int createSendPort(int portNum){
            //create new port number to send packet from client to server
                    int portNumSend;
                    if(portNum == 65535){
                        portNumSend = 25000;
                    }
                    else{
                        portNumSend = portNum+1; //increment portReceive by 1 to send to localhost
                    }
                    
                    return portNumSend;
        }
        
        //creates a new socket
        //sends initial hello message to server
        public void clientRunTCP(int portReceive){

                System.out.println("Client Running...");
                System.out.println("UDP Port Number: " + portReceive);
                try {
                    
                    //create socket to localhost, portNumSend
                    Socket clientSocket = new Socket("localhost", tport);
                    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

                    //this will write "Hello" to the server, and send the portReceive
                    outToServer.writeBytes("Hello! Here is the port number I want to receive the packets on: \n");
                    outToServer.writeBytes(Integer.toString(portReceive));
                    
                    //close TCP connection
                    clientSocket.close();
                    

                } catch (Exception e) {
                    System.out.printf(null, e);
                }
            }
    }
    
    public static class udpClient{
        
        //sends an ACK packet to server
        public void sendAck(int portsend, DatagramSocket clientSocket){

            try {

                InetAddress IPAddress = InetAddress.getLocalHost();
                //sends ACK packet containing the string ack
                String ack = "ACK";
                byte[] ack_arr = ack.getBytes();
                DatagramPacket ackPack = new DatagramPacket(ack_arr, ack_arr.length, IPAddress, portsend);
                clientSocket.send(ackPack);

            }catch (Exception e) {
                    System.out.printf("Error with sending UDP ACK\n", e);
                }

        }
        
        //receive packets from server, returns byte[]
        public DatagramPacket receivePacket(DatagramSocket clientSocket){
            
            byte[] receiveData = new byte[8192];//buffer for the incoming packet 
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try{
                            
                clientSocket.receive(receivePacket);
                
               
                
            }catch (Exception e) {
                    System.out.printf("Error in receivePacket method\n", e);
                }
            //return the byte[]
            return receivePacket;    
        }
            
    
    }
}