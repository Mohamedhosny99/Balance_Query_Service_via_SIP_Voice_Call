package udp.classes;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class CDR {
    

    private String callingNumber;
    private String calledNumber;
    private int duration;
    private String timeStart;
    private String timeend;
    private String callStatus;
    private double balance;




    public CDR(String callingNumber, String calledNumber, int duration, String timestart , String timeend, String callStatus , double balance) {
        this.callingNumber = callingNumber;
        this.calledNumber = calledNumber;
        this.duration = duration;
        this.callStatus = callStatus;
        this.timeStart = timestart; // Assuming timeStart is the same as timestamp
        this.timeend = timeend; // Assuming timeend is the same as timestamp
        this.balance =  balance; // Default balance
    }
    
    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
    public String getCallingNumber() {
        return callingNumber;
    }

    public String getCalledNumber() {
        return calledNumber;
    }

    public int getDuration() {
        return duration;
    }

    public String getTimeStart() {
        return timeStart;
    }
    public String getTimeEnd() {
        return timeend;
    }
    public String getCallStatus() {
        return callStatus;
    }

    @Override
    public String toString() {
        return callingNumber + "," + calledNumber + "," + timeStart  +", "+timeend  +", "+ duration  + "," + callStatus + "," + balance;
    }


  // make CDR file 

public void exportToFile(String filePath, boolean append) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, append))) {
            writer.write(this.toString());
            writer.newLine();
        } catch (IOException e) {
            throw new IOException("Error writing CDR to file: " + e.getMessage(), e);


}
}
 
}