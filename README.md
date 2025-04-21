# UDPVoice - VoIP Application

UDPVoice is a Java-based Voice over IP (VoIP) application that enables real-time voice communication between users. The application uses UDP for audio transmission and TCP for control messages and call management.

## Features

- Real-time voice communication
- User authentication via MSISDN (phone number)
- Balance management for calls
- Call Detail Records (CDR) generation
- Database integration for user accounts and balance tracking
- Call duration tracking and minute-by-minute balance deduction

## Requirements

- Java 8 or higher
- PostgreSQL database
- Audio input/output devices

## Setup

### Database Setup

1. Create a PostgreSQL database named `balance_db`
2. Create a users table with the following schema:
   ```sql
   CREATE TABLE users (
     id SERIAL PRIMARY KEY,
     msisdn VARCHAR(15) UNIQUE NOT NULL,
     balance INTEGER NOT NULL
   );
   ```
3. Insert sample users with their MSISDN and balance:
   ```sql
   INSERT INTO users (msisdn, balance) VALUES ('1234567890', 100);
   INSERT INTO users (msisdn, balance) VALUES ('0987654321', 100);
   ```

### Running the Application

#### Receiver (Call Recipient)

Start the receiver first:

```bash
java -cp ".:./lib/*" udp.Receiver
```

The receiver will listen on UDP port 5000 for audio and TCP port 5001 for control messages.

#### Sender (Caller)

Start the sender with your MSISDN and the recipient's MSISDN:

```bash
java -cp ".:./lib/*" udp.Sender 1234567890 0987654321
```

Where:
- `1234567890` is your MSISDN
- `0987654321` is the recipient's MSISDN

## Using the Application

1. The sender initiates a call by providing their MSISDN and the recipient's MSISDN
2. The system verifies both users exist and checks the sender's balance
3. If verification is successful, the call begins
4. Audio is captured from the sender's microphone and transmitted to the receiver
5. The receiver plays the audio through the speakers
6. The sender can end the call at any time by typing 'q' and pressing Enter
7. A CDR record is generated and stored when the call ends
8. The sender's balance is updated based on the call duration (5 units per minute)

## Project Structure

- `src/udp/Sender.java` - The calling party implementation
- `src/udp/Receiver.java` - The receiving party implementation
- `src/udp/classes/CDR.java` - Call Detail Record implementation
- `src/udp/classes/DBConnection.java` - Database connection handler
- `src/udp/classes/user.java` - User entity class

## Troubleshooting

- Ensure both the sender and receiver are on the same network
- Verify that ports 5000 and 5001 are not blocked by a firewall
- Check database connection settings in DBConnection.java
- Ensure audio devices are properly connected and configured

## Future Enhancements

- Support for multiple concurrent calls
- Encryption for secure communication
- Conference calling features
- Audio quality improvements
- User interface for easier interaction 