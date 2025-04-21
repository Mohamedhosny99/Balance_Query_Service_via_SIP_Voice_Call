package udp;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar UDPVoice.jar [receiver|sender] [args...]");
            return;
        }

        String command = args[0];
        String[] remainingArgs = new String[args.length - 1];
        System.arraycopy(args, 1, remainingArgs, 0, args.length - 1);

        switch (command) {
            case "receiver":
                Receiver.main(remainingArgs);
                break;
            case "sender":
                try {
                    Sender.main(remainingArgs);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                System.out.println("Unknown command: " + command);
                System.out.println("Available commands: receiver, sender");
        }
    }
} 