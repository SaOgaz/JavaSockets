package client;

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
        byte[][] packetsBuffered = null;
        //the file being written to by the client
        String outputfile = "/Users/ogaz/NetBeansProjects/JavaSockets/src/javasockets/ctestfile.txt";
        int fileSum = 0;//the number of bytes received by the client so far

        
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
            tcpClient.clientRunTCP(portReceive, portSend);
            /////////////////////////////////////////////////////END TCP////////
            
            ///////////////////////////////////////////////UDP CONNECTION///////
            //new client UDP object
            udpClient udpClient = new udpClient();
            //create UDP Datagram socket
            DatagramSocket clientSocket = new DatagramSocket();

            //send an ACK to the server via the port portSend
            udpClient.sendAck(portSend, clientSocket);

            /**********************************LOOP A TO RECEIVE PACKETS*******/
            byte packetReceived[];//the incoming packet
            do{
                //receive a packet from server on portReceive; returns byte[] to packetReceived[]
                packetReceived = udpClient.receivePacket(clientSocket);

                //extract file size from packet[]
                int fileByte = packetReceived[packetReceived.length - 2];
                byte size[] = Arrays.copyOfRange(packetReceived, fileByte, fileByte);
                fileSize = size[0];
                int packetSize = fileSize - 2;

                //extract the packet number from packet[]
                int packetNumByte = packetReceived[packetReceived.length-1];
                byte packetNumber[] = 
                        Arrays.copyOfRange(packetReceived, packetNumByte, packetNumByte);
                packetNum = packetNumber[0];

                //check to see if packets already received and stored in packetsBuffered[]
                //store the packet in array at i = packetNum
                if(packetsBuffered[packetNum] != null){
                    packetsBuffered[packetNum] = packetReceived;
                    fileSum = fileSum + packetSize;

                }
            }while (fileSum < fileSize);

            //when fileSum = fileSize, send ACK
            udpClient.sendAck(portSend, clientSocket);
            System.out.print("ACK Sent. Port number" + portReceive);
            /**********************************************END LOOP A**********/

            /***LOOP B - check if next packet is OK, if not, send ACK**********/
            while(true){
                byte okPacket[] = udpClient.receivePacket(clientSocket);
                String check = Arrays.toString(okPacket);
                if("OK".equals(check)){

                    System.out.print("Done. Port " + portReceive);
                    break;
                }
                else{
                    udpClient.sendAck(portSend, clientSocket);
                    System.out.print("ACK Resent. Port " + portReceive);
                }
            }
            /******************************************************************/
            /////////////////////////////////////////////END UDP CONNECTION/////


            ///////////////////////////////write packetsBuffered[] to file//////
            FileOutputStream output = new FileOutputStream(outputfile);
            try{
                for(int i=0; i<=packetsBuffered.length; i++){
                    output.write(packetsBuffered[i]);
                }
            }catch (Exception e){
                System.out.printf("Error while writing packetsReceived[] to file\n", e);
            }
            ////////////////////////////////END WRITING TO FILE/////////////////

        }
        catch (SocketException | FileNotFoundException e){
            System.out.printf("Error in main class\n", e);
        }

    }
    
    public static class tcpClient {
    
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
                        portNumSend = portNum++; //increment portReceive by 1 to send to localhost
                    }
                    
                    return portNumSend;
        }
        
        //creates a new socket
        //sends initial hello message to server
        public void clientRunTCP(int portReceive, int portNumSend){

                System.out.print("Client Running...");
                System.out.print("UDP Port Number: " + portReceive);
                try {
                    
                    //create socket to localhost, portNumSend
                    Socket clientSocket = new Socket("localhost", portNumSend);
                    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

                    //this will write "Hello" to the server, and send the portReceive
                    outToServer.writeBytes("Hello! Here is the port number I want to receive the packets on: \n");
                    outToServer.write(portReceive);
                    
                    //close TCP connection
                    clientSocket.close();

                } catch (Exception e) {
                    System.out.printf(null, e);
                }
            }
    }
    
    public static class udpClient{
        
        //sends an ACK packet to server
        public void sendAck(int portSend, DatagramSocket clientSocket){

            try {

                InetAddress IPAddress = InetAddress.getLocalHost();
                //sends ACK packet containing the string ack
                String ack = "ACK";
                byte[] ack_arr = ack.getBytes();
                DatagramPacket ackPack = new DatagramPacket(ack_arr, ack_arr.length, IPAddress, portSend);
                clientSocket.send(ackPack);

            }catch (Exception e) {
                    System.out.printf("Error with sending UDP ACK\n", e);
                }

        }
        
        //receive packets from server, returns byte[]
        public byte[] receivePacket(DatagramSocket clientSocket){
            
            byte[] receiveData = new byte[1024];//buffer for the incoming packet
            try{
                
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);
                receiveData = receivePacket.getData();
               
                
            }catch (Exception e) {
                    System.out.printf("Error in receivePacket method\n", e);
                }
            //return the byte[]
                return receiveData;
        }
            
    
    }
}