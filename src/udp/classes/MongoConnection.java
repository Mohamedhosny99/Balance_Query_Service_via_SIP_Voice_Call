package udp.classes;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoConnection {
    private static MongoClient mongoClient;
    private static MongoDatabase database;

    public static MongoDatabase getDatabase() {
        if (database == null) {
            String connectionString = "mongodb://localhost:27017";
            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase("SIP");
        }
        return database;
    }


    public static void insertCDR(String cdrData) {
        try {
            MongoDatabase db = getDatabase();
            // Parse the CDR data manually
            String[] parts = cdrData.split(",");
            if (parts.length >= 7) {
                Document cdrDocument = new Document()
                    .append("callingNumber", parts[0].trim())
                    .append("calledNumber", parts[1].trim())
                    .append("timeStart", parts[2].trim())
                    .append("timeEnd", parts[3].trim())
                    .append("duration", Integer.parseInt(parts[4].trim()))
                    .append("status", parts[5].trim())
                    .append("balance", Double.parseDouble(parts[6].trim()))
                    .append("timestamp", new java.util.Date());
                
                db.getCollection("CDRs").insertOne(cdrDocument);
            }
        } catch (Exception e) {
            System.err.println("Error inserting CDR: " + e.getMessage());
        }
    }


    public static String generateFileName(String msisdn, String timestamp) {
        return msisdn + "_" + timestamp.replace(':', '-').replace(' ', '_') + ".CSV";
    }



    public static void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
