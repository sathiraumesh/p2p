package node.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import node.Credential;

@AllArgsConstructor
@Data
public class Request extends Message {
    private Credential credential;

    private String command;
    public String parseRequestAsString() {
        command += " " + credential.getIp() + " " + credential.getPort() + " " + credential.getUsername();
        System.out.println(super.getMessageAsString(command));
        return super.getMessageAsString(command);
    }
}
