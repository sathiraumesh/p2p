package node;

import java.util.StringTokenizer;

public class requestParser {

    public static void parse(String message, Credential senderCredentials){
        System.out.println("Message received: " + "Message");
        StringTokenizer tokenizer = new StringTokenizer(message, " ");
        String command = tokenizer.nextToken();
        System.out.println(command);
    }
}
