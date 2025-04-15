package udp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sound.sampled.*;
import udp.classes.CDR;

public class Receiver {
    private static final int PORT = 5000;
    private static final int SAMPLE_RATE = 44100;
    private static final int SAMPLE_SIZE = 16;
    private static final int BUFFER_SIZE = 1024;
    private static final int TCP_PORT = 5001;
    private static volatile boolean callEnded = false;

    public static void main(String[] args) {
        DatagramSocket udpSocket = null;
        SourceDataLine speakers = null;
        Connection conn = null;
        ServerSocket tcpServer = null;
        
        try {
            // Set up TCP server first
            tcpServer = new ServerSocket(TCP_PORT);
            System.out.println("TCP Server listening on port " + TCP_PORT);
            
            // Set up audio system
            AudioFormat format = setupAudioFormat();
            speakers = setupSpeakers(format);

            // Set up UDP socket for audio
            udpSocket = new DatagramSocket(PORT);
            System.out.println("UDP Server listening on port " + PORT);
            
            // Accept TCP connection for control messages
            Socket tcpSocket = tcpServer.accept();
            BufferedReader tcpIn = new BufferedReader(
                new InputStreamReader(tcpSocket.getInputStream())
            );

            // Receive initial message
            String initialMessage = tcpIn.readLine();
            System.out.println("Received initial message: " + initialMessage);

            // Parse MSISDN information
            String[] parts = initialMessage.split("from | to ");
            if (parts.length >= 3) {
                String callerMsisdn = parts[1].trim();
                String receiverMsisdn = parts[2].trim();
                System.out.println("Caller MSISDN: " + callerMsisdn);
                System.out.println("Receiver MSISDN: " + receiverMsisdn);
            }

            // Start audio reception
            byte[] buffer = new byte[BUFFER_SIZE];
            System.out.println("Starting audio reception...");
            
            // Create TCP listener thread for control messages
            Thread controlThread = new Thread(() -> {
                try {
                    String controlMessage;
                    while ((controlMessage = tcpIn.readLine()) != null) {
                        if (controlMessage.startsWith("CDR:")) {
                            String cdrData = controlMessage.substring(4);
                            System.out.println("Received CDR: " + cdrData);
                            // Process CDR data
                            processCDR(cdrData);
                        } else if (controlMessage.equals("END_CALL")) {
                            System.out.println("Received end call signal");
                            callEnded = true;  // Set flag
                            break;
                        }
                    }
                    System.out.println("Control thread exiting");
                } catch (IOException e) {
                    System.out.println("TCP connection error: " + e.getMessage());
                    callEnded = true;  // Set flag on error too
                }
            });
            controlThread.start();

            // Main audio loop
            while (!Thread.interrupted() && !callEnded) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.setSoTimeout(500);  // Set timeout to check callEnded flag periodically
                    udpSocket.receive(packet);
                    speakers.write(packet.getData(), 0, packet.getLength());
                } catch (SocketTimeoutException e) {
                    // This is expected, just recheck the callEnded flag
                    System.out.println("Checking if call ended: " + callEnded);
                }
            }
            
            System.out.println("Main audio loop exited");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanupResources(speakers, udpSocket, tcpServer, conn);
        }
    }

    private static void processCDR(String cdrData) {
        try {
            String[] parts = cdrData.split(",");
            if (parts.length >= 7) {
                CDR cdr = new CDR(
                    parts[0].trim(), // callingNumber
                    parts[1].trim(), // calledNumber
                    Integer.parseInt(parts[4].trim()), // duration
                    parts[2].trim(), // timeStart
                    parts[3].trim(), // timeEnd
                    parts[5].trim(), // callStatus
                    Double.parseDouble(parts[6].trim()) // balance
                );
                cdr.exportToFile("cdr_records.csv", true);
            }
        } catch (Exception e) {
            System.err.println("Error processing CDR: " + e.getMessage());
        }
    }

    private static void cleanupResources(SourceDataLine speakers, DatagramSocket udpSocket, 
                                       ServerSocket tcpServer, Connection conn) {
        if (speakers != null) {
            speakers.drain();
            speakers.close();
        }
        if (udpSocket != null) udpSocket.close();
        if (tcpServer != null) {
            try {
                tcpServer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static AudioFormat setupAudioFormat() {
        return new AudioFormat(
            SAMPLE_RATE,
            SAMPLE_SIZE,
            2,  // stereo
            true,  // signed
            false  // little endian
        );
    }

    private static SourceDataLine setupSpeakers(AudioFormat format) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(info);
        speakers.open(format);
        speakers.start();
        return speakers;
    }
}

