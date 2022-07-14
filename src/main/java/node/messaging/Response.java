package node.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import node.Credential;

@AllArgsConstructor
@Data
public class Response extends Message{
    private Credential credential;
    private String command;
    public String parseResponseAsString() {
        command += " " + credential.getIp() + " " + credential.getPort() + " " + credential.getUsername();

        return super.getMessageAsString(command);
    }
}
