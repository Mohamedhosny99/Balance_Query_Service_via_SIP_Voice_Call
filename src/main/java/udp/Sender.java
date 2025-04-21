package udp;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.sql.Connection;
import java.sql.PreparedStatement;  // Changed from com.sun.jdi.connect.spi.Connection
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import javax.sound.sampled.*;
import udp.classes.CDR;
import udp.classes.DBConnection;

public class Sender {
    private static final int PORT = 5000;
    private static final String RECEIVER_IP = "localhost"; 
    private static final int SAMPLE_RATE = 44100;
    private static final int SAMPLE_SIZE = 16;
    private static final int BUFFER_SIZE = 1024;
    private static int balance = 0;
    private static Socket tcpSocket; // Store the TCP socket as a class field
    private static volatile boolean stopAudio = false; // Flag to signal audio thread to stop

    public static void main(String[] args) throws SQLException {

        // if (args.length < 1 && args[0] == null) {
        //     System.out.println("Please provide MSISDN as a command line argument.");
        //     System.out.println("Usage: java Sender <MSISDN>");
        //     return;
        // }
        String mymsisdn = args[0];
        String rmsisdn = args[1]; 
        System.out.println("SMSISDN: " + mymsisdn  + "RMSISDN: " + rmsisdn);

        
         String timestamp = new java.util.Date().toString(); 
          System.out.println("Timestamp: " + timestamp);   
          DBConnection connection = new DBConnection("jdbc:postgresql://localhost:5432/balance_db", "postgres", "123");
          Connection conn =  (Connection) connection.connect();
          try {
                
                System.out.println("Connected to database successfully.");
                
                String query = "SELECT balance FROM users WHERE msisdn = ?";
                PreparedStatement preparedStatement = ((java.sql.Connection) conn).prepareStatement(query);
                preparedStatement.setString(1, mymsisdn);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    
                    System.out.println("User found with MSISDN: " + mymsisdn);
                    Sender.balance = resultSet.getInt("balance");
                    System.out.println("Balance for MSISDN " + mymsisdn + ": " + Sender.balance);
                  // check rmsisdn in the database
                    String query2 = "SELECT balance FROM users WHERE msisdn = ?";
                    PreparedStatement preparedStatement2 = ((java.sql.Connection) conn).prepareStatement(query2);
                    preparedStatement2.setString(1, rmsisdn);
                    ResultSet resultSet2 = preparedStatement2.executeQuery();
                    if (resultSet2.next()) {
                        System.out.println("Reciver user  found with MSISDN: " + rmsisdn);
                  

                           try {
                               System.out.println("Starting audio transmission...");
                                    AudioFormat format = new AudioFormat(
                                        SAMPLE_RATE,
                                        SAMPLE_SIZE,
                                        2,
                                        true, // signed
                                        false // little endian
                                    );


                                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                                    TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
                                    microphone.open(format);
                                    microphone.start();


                                    DatagramSocket socket = new DatagramSocket();
                                    InetAddress address = InetAddress.getByName(RECEIVER_IP);
 
                                    // Send initial message to receiver by TCP
                                    try {
                                        Socket tcpSocket = new Socket("localhost", 5001);
                                        PrintWriter out = new PrintWriter(tcpSocket.getOutputStream(), true);
                                        String initialMessage = "Hello, this is a test message from " + mymsisdn + " to " + rmsisdn;
                                        out.println(initialMessage);
                                        System.out.println("Sent initial message via TCP: " + initialMessage);
                                        
                                        // Keep the socket open for later CDR and END_CALL messages
                                        // Store it as a class field to use later
                                        Sender.tcpSocket = tcpSocket;
                                    } catch (IOException e) {
                                        System.err.println("Could not connect to receiver: " + e.getMessage());
                                        throw e;
                                    }

                                    // Audio buffer
                                    byte[] buffer = new byte[BUFFER_SIZE];
                                    long startTime = System.currentTimeMillis();
                                    final int[] lastPrintedMinute = {-1};
                                    // Create a separate thread for audio transmission
                                    Thread audioThread = new Thread(() -> {
                                        try {
                                            while (!Thread.interrupted() && !stopAudio) {
                                                try {
                                                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                                                    // Check if we should stop before sending
                                                    if (Thread.interrupted() || stopAudio) {
                                                        System.out.println("Audio thread stop flag detected");
                                                        break;
                                                    }
                                                    DatagramPacket packet = new DatagramPacket(
                                                        buffer,
                                                        bytesRead,
                                                        address,
                                                        PORT
                                                    );
                                                    socket.send(packet);
                                                    long elapsedMillis = System.currentTimeMillis() - startTime;
                                                    int elapsedMinutes = (int) (elapsedMillis / 60000);
                                                    
                                                    if (elapsedMinutes > lastPrintedMinute[0]) {
                                                        System.out.println(elapsedMinutes + " minute(s) elapsed");
                                                        lastPrintedMinute[0] = elapsedMinutes;
                                                        balance -= 5; // Deduct balance every minute
                                                        
                                                        if (balance <= 0) {
                                                            System.out.println("Call ended: Insufficient balance");
                                                            return;
                                                        }
                                                    }
                                                } catch (IOException e) {
                                                    if (Thread.interrupted() || stopAudio) {
                                                        System.out.println("Audio thread interrupted while in I/O");
                                                        break;
                                                    }
                                                    e.printStackTrace();
                                                }
                                            }
                                            System.out.println("Audio thread exiting");
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    });

                                    // Start the audio thread
                                    audioThread.start();

                                    // Listen for user input to end call
                                    System.out.println("Press 'Q' and Enter to end the call");
                                    Scanner scanner = new Scanner(System.in);
                                    while (scanner.hasNext()) {
                                        if (scanner.nextLine().equalsIgnoreCase("q")) {
                                            System.out.println("Ending call...");
                                            // Signal audio thread to stop
                                            stopAudio = true;
                                            // Stop the audio thread
                                            audioThread.interrupt();
                                            
                                            long totalMillis = System.currentTimeMillis() - startTime;
                                            int totalMinutes = (int) (totalMillis / 60000);
                                            String endTime = new java.util.Date().toString(); // Get the end time
                                            CDR cdr = new CDR(mymsisdn, rmsisdn, totalMinutes, timestamp, endTime, "Success", balance);
                                            System.out.println("Call ended. " + cdr.toString());
                                            
                                            // Export CDR to file
                                            try {
                                                cdr.exportToFile("cdr_records.csv", true);
                                            } catch (IOException e) {
                                                System.err.println("Error exporting CDR to file: " + e.getMessage());
                                            }

                                            // Send CDR and end call message via TCP
                                            try {
                                                PrintWriter tcpOut = new PrintWriter(Sender.tcpSocket.getOutputStream(), true);
                                                
                                                // Send CDR record
                                                tcpOut.println("CDR:" + cdr.toString());
                                                System.out.println("Sent CDR via TCP: " + cdr.toString());
                                                
                                                // Send end call message
                                                tcpOut.println("END_CALL");
                                                System.out.println("Sent END_CALL signal via TCP");
                                                
                                                // Add a delay to ensure the receiver processes the END_CALL message
                                                Thread.sleep(1000);
                                                
                                                // Wait for audio thread to exit - with timeout
                                                System.out.println("Waiting for audio thread to exit...");
                                                audioThread.join(3000);
                                                if (audioThread.isAlive()) {
                                                    System.out.println("Audio thread did not exit in time, forcing shutdown");
                                                }
                                                
                                                // Close resources in the correct order
                                                System.out.println("Closing resources...");
                                                microphone.stop();
                                                microphone.close();
                                                
                                                // Make sure we don't close the socket until the audio thread is done with it
                                                System.out.println("Stopping audio completely before closing sockets...");
                                                Thread.sleep(500); // Short wait to ensure audio thread sees stopAudio flag
                                                
                                                // Now close network resources
                                                socket.close();
                                                scanner.close();
                                                Sender.tcpSocket.close(); // Close the TCP socket too
                                                System.out.println("All resources closed. Exiting...");
                                                System.exit(0); // Force exit
                                                return;

                                            } catch (IOException e) {
                                                System.err.println("Error sending TCP messages: " + e.getMessage());
                                                System.exit(1); // Force exit on error
                                            } catch (InterruptedException e) {
                                                System.err.println("Interrupted while waiting: " + e.getMessage());
                                                System.exit(1); // Force exit on error
                                            }
                                        }
                                    }

                                    // Cleanup
                                    audioThread.join();
                                    microphone.stop();
                                    microphone.close();
                                    socket.close();
                                    scanner.close();


                           }catch (Exception e) {
                                   e.printStackTrace();
                              CDR cdr = new CDR(mymsisdn, rmsisdn, 0, timestamp, "Failed", "Failure reason", 0.0);
                               cdr.setBalance(0);
                               System.out.println(cdr.toString());
                                return;
                               }



                    } else {
                        System.out.println("No user found with MSISDN: " + rmsisdn);
                        CDR cdr = new CDR(mymsisdn, rmsisdn, 0, timestamp, "Failed", "Failure reason", 0.0);
                        cdr.setBalance(0);

                    }


                }else {
                    System.out.println("No user found with MSISDN: " + mymsisdn);
                }
             
                

                
              } catch (SQLException e) {
                System.out.println("Database connection error: " + e.getMessage());

                CDR cdr = new CDR(mymsisdn, rmsisdn, 0, timestamp, "Failed", "Failure reason", 0.0);
                cdr.setBalance(0);

          }


}
}
