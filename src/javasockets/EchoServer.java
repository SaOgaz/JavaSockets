/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.Scanner;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.InetAddress;
/**
 *
 * @author Michelle Bowman, Anne Frederick, Valentina Hyman, Sara Ogaz, Laurel Valenti
 */
public class EchoServer {
    
       
    
        public static void main(String[] args) throws IOException{          
            //adjust depending on machine, filename
            String filename = "/Users/ogaz/NetBeansProjects/JavaSockets/src/javasockets/stestfile.txt";
            int portNumbert = 21252;
            int remainder;
            int full_packets=0;
            DatagramPacket[] outbuffer=null;
            
            // User enters timeout T and size M of all packets. Server checks tht M <=S
            Scanner reader = new Scanner(System.in);
            System.out.println("Enter an integer timeout T in ms (e.g., 100 means 100 ms): ");
            int T = reader.nextInt();// Reads the timeout T.
                
            System.out.println("Enter the size M of all packets (except the last) in bytes (e.g., 1200 means 1200 bytes): ");//Michelle
            int M = reader.nextInt();// Reads the size M of all packets, except the last.
            
            long S = new File(filename).length();// Get the File Size.
            
            while (M > S) {//If total packet size M is greater than file size S, prompt user to enter new value.
                System.out.println("Packet size (M) is too big.");
                System.out.println("Please enter a packet size less than " + S + " bytes:");
                M = reader.nextInt();
            } 
            
            System.out.println("You have entered " + T + " ms for the timeout " +
                "and " + M + " for the size of all packets in bytes.");
            System.out.println("The file size is " + S + " bytes. Thus size of all packets is <= file size.");
            
                       
            
                                    
            
            try {
                ServerSocket serverSocket = 
                    new ServerSocket (portNumbert);// Use 21252 as port number.
                while(true){
                    Socket clientSocket = serverSocket.accept();
                
                    PrintWriter out =
                        new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
  
                    //When server gets hello message, it prints hello received and port number sent by client
                    String inline;
                    inline = in.readLine();
                    System.out.println("\n\nHello received.");
                    String udpPort = in.readLine(); // Read UDP port number from client.
                    int udpPortNumber = Integer.parseInt(udpPort);                
                    System.out.println("The UDP port number is " + udpPortNumber + ".");
                    
                    int Sint = (int) S;
                    
                    //setup file stream and Datagram buffer           
                    try{
                        FileInputStream  fileinputstream = new FileInputStream(filename);
                        long fsize = fileinputstream.getChannel().size(); 
                        
                        InputStream buff = new FileInputStream(filename);
                
                        remainder = (int) fsize % M;
                        full_packets = (int) fsize / M;
                        outbuffer = new DatagramPacket[full_packets+1];
                
                        int seq_tracker = 0;
                
                        // first x number of full packets
                        for(int i=0; i<full_packets; i++){
                            byte[] bytes = new byte[M+8];
                            buff.read(bytes, 0, M);                                                        
                            
                            bytes[0+M] = (byte) ((Sint >> 24) & 0xFF);
                            bytes[1+M] = (byte) ((Sint >> 16) & 0xFF);
                            bytes[2+M] = (byte) ((Sint >> 8) & 0xFF);
                            bytes[3+M] = (byte) (Sint & 0xFF);                               
                            
                            
                                                        
                            bytes[4+M] = (byte) ((i >> 24) & 0xFF);
                            bytes[5+M] = (byte) ((i >> 16) & 0xFF);
                            bytes[6+M] = (byte) ((i >> 8) & 0xFF);
                            bytes[7+M] = (byte) (i & 0xFF);
                            
                            DatagramPacket pack = new DatagramPacket(bytes, bytes.length, InetAddress.getLocalHost(), udpPortNumber);
                            outbuffer[i] = pack;
                            seq_tracker=i;
                        }

                        seq_tracker = seq_tracker+1;
                        
                        // last datagram
                        byte[] bytes = new byte[remainder + 8];
                        buff.read(bytes, 0, remainder);  
                        
                        bytes[0+remainder] = (byte) ((Sint >> 24) & 0xFF);
                        bytes[1+remainder] = (byte) ((Sint >> 16) & 0xFF);
                        bytes[2+remainder] = (byte) ((Sint >> 8) & 0xFF);
                        bytes[3+remainder] = (byte) (Sint & 0xFF);    
                        
                        bytes[4+remainder] = (byte) ((seq_tracker >> 24) & 0xFF);
                        bytes[5+remainder] = (byte) ((seq_tracker >> 16) & 0xFF);
                        bytes[6+remainder] = (byte) ((seq_tracker >> 8) & 0xFF);
                        bytes[7+remainder] = (byte) (seq_tracker & 0xFF);
                        
                                          
                        DatagramPacket pack = new DatagramPacket(bytes, bytes.length, InetAddress.getLocalHost(), udpPortNumber);
                        outbuffer[full_packets] = pack;  
                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();    
                    }                                                           
                    
                    
                    UDPthing newUDP = new UDPthing(T,udpPortNumber,full_packets, outbuffer);           
                }                              
            } catch (IOException e) {
                System.out.println("Exception caught when trying to listen on port "
                    + portNumbert + " or listening for a connection");
                System.out.println(e.getMessage());
            }
                                                                                                    
        }
}
        
        
        
            
class UDPthing {
                
            
        UDPthing(int T, int sportnumber, int full_packets, DatagramPacket[] outbuffer) throws IOException{
            
            int myportnumber = sportnumber+1;
            int okTimer = T;
            
            if (sportnumber == 65535){
                myportnumber = 25000;
            }
                                               
            //setup socket
            String ok = "OK";
            byte[] ok_arr = ok.getBytes();
            DatagramPacket myAck = new DatagramPacket(ok_arr, ok_arr.length, InetAddress.getLocalHost(), sportnumber);
                        
            try{
                DatagramSocket mySock = new DatagramSocket(myportnumber);
                for(int i=0; i< full_packets+1; i++){
                    DatagramPacket packet = outbuffer[i];
                    mySock.send(packet); 
                }
                mySock.setSoTimeout(T);
                
                while (true) {
                    byte[] incoming = new byte[256];
                    DatagramPacket gack = new DatagramPacket(incoming, incoming.length);
                    try {
                        //recieve first ack
                        mySock.receive(gack);
                        System.out.println(String.valueOf(sportnumber)+": OK");
                        
                        //sending ok message
                        mySock.send(myAck);
                        mySock.setSoTimeout(okTimer);
                        break;

                    } 
                    catch (SocketTimeoutException e) {   
                        System.out.println(String.valueOf(sportnumber)+": resending");                                
                            
                        for(int i=0; i< full_packets+1; i++){
                            DatagramPacket packet = outbuffer[i];
                            mySock.send(packet);
                        }
                                                   
                        T = T*2;
                        mySock.setSoTimeout(T);
                        continue;                                                 
                    }
                }                               
                                                                           
                 
                while (true) {
                    try {
                        //recieve second ack
                        byte[] incoming = new byte[256];
                        DatagramPacket gack = new DatagramPacket(incoming, incoming.length);
                        mySock.receive(gack);
                        System.out.println(String.valueOf(sportnumber)+": Dublicate ACK recieved");
                        okTimer = okTimer * 2;
                        mySock.send(myAck);
                        mySock.setSoTimeout(okTimer);
                        //loop back to beginning
                        
                        
                    } catch (SocketTimeoutException e){
                        System.out.print(String.valueOf(sportnumber)+": Done");
                        mySock.close();
                        break;
                    }
                    
                }
                                                                                                                  
                                                
            } catch (SocketException ex){
                ex.printStackTrace();
            }
        }
}
        

