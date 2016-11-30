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
 * @author Michelle Bowman, Sara Ogaz
 */
public class EchoServer {
    
       
    
        public static void main(String[] args) throws IOException{          
            //adjust depending on machine, filename
            String filename = "/Users/ogaz/NetBeansProjects/JavaSockets/src/javasockets/filename.txt";
            int portNumber = 21252;
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
            
                       
            
            //setup file stream and Datagram buffer           
            try{
                FileInputStream  fileinputstream = new FileInputStream(filename);
                long fsize = fileinputstream.getChannel().size();              
                InputStream buff = new FileInputStream("filename.txt");
                
                remainder = (int) fsize % M;
                full_packets = (int) fsize / M;
                outbuffer = new DatagramPacket[full_packets+1];
                
                
                // first x number of full packets
                for(int i=0; i<full_packets; i++){
                    byte[] bytes = new byte[M+2];
                    buff.read(bytes, 0, M);
                    bytes[M] = (byte) S;
                    bytes[M=1] = (byte) i;  
                    DatagramPacket pack = new DatagramPacket(bytes, bytes.length, InetAddress.getLocalHost(), portNumber);
                    outbuffer[i] = pack;                   
                }
                
                
                // last datagram
                byte[] bytes = new byte[remainder + 2];
                buff.read(bytes, 0, remainder);                
                bytes[remainder] = (byte) S;
                bytes[remainder+1] = (byte) remainder;                   
                DatagramPacket pack = new DatagramPacket(bytes, bytes.length, InetAddress.getLocalHost(), portNumber);
                outbuffer[M] = pack;  
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();    
            } 
            
                                    
            
            try {
                ServerSocket serverSocket = 
                    new ServerSocket (portNumber);// Use 21252 as port number.
                Socket clientSocket = serverSocket.accept();
                PrintWriter out =
                    new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
  
                //When server gets hello message, it prints hello received and port number sent by client
                String inline;
                while ((inline = in.readLine()) != null) {
                    System.out.println("Hello received.");
                    String udpPort = in.readLine(); // Read UDP port number from client.
                    int udpPortNumber = Integer.parseInt(udpPort);                
                    System.out.println("The UDP port number is " + udpPortNumber + ".");
                    UDPthing newUDP = new UDPthing(T,udpPortNumber,full_packets, outbuffer);           
                }                              
            } catch (IOException e) {
                System.out.println("Exception caught when trying to listen on port "
                    + portNumber + " or listening for a connection");
                System.out.println(e.getMessage());
            }
                                                                                                    
        }
}
        
        
        
            
class UDPthing {
                
            
        UDPthing(int T, int portnumber, int full_packets, DatagramPacket[] outbuffer) throws IOException{
            
            int myportnumber = portnumber+1;
            int okTimer = T;
            
            if (portnumber == 65535){
                myportnumber = 25000;
            }
                                               
            //setup socket
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
                        mySock.receive(gack);
                        System.out.print(String.valueOf(portnumber));
                        System.out.print("\nOK\n");
                        
                        //sending ok message
                        String ok = "OK";
                        byte[] ok_arr = ok.getBytes();
                        DatagramPacket myAck = new DatagramPacket(ok_arr, ok_arr.length, InetAddress.getLocalHost(), portnumber);
                        mySock.send(myAck);
                        break;

                    } 
                    catch (SocketTimeoutException e) {   
                        System.out.print(String.valueOf(portnumber));
                        System.out.print("\nresending\n");                                    
                            
                        for(int i=0; i< full_packets+1; i++){
                            DatagramPacket packet = outbuffer[i];
                            mySock.send(packet);
                        }
                                                   
                        T = T*2;
                        mySock.setSoTimeout(T);
                        continue;                                                 
                    }
                }                               
                
                
                mySock.setSoTimeout(okTimer);  
                
                while (true) {
                    byte[] incoming = new byte[256];
                    DatagramPacket gack = new DatagramPacket(incoming, incoming.length);
                    try {
                        mySock.receive(gack);
                    }catch (SocketTimeoutException e){
                        okTimer = 2*okTimer;  
                        System.out.print(String.valueOf(portnumber));
                        System.out.print("\nOK timed out, restarting timer\n");
                        mySock.setSoTimeout(okTimer);
                        continue;
                    }
                    
                    try {
                        mySock.receive(gack);
                        System.out.print(String.valueOf(portnumber));
                        System.out.print("\nDuplicate ACK recieved\n");
                        okTimer = okTimer * 2;                                               
                    } catch (SocketTimeoutException e){
                        System.out.print(String.valueOf(portnumber));
                        System.out.print("\nDone\n");
                        mySock.close();
                    }
                    mySock.setSoTimeout(okTimer);
                }
                                                                                                                  
                                                
            } catch (SocketException ex){
                ex.printStackTrace();
            }
        }
}
        

