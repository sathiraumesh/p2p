package node.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import node.Credential;

@AllArgsConstructor
@Data
public class Request extends Message {
    protected Credential credential;
    protected String command;
    public String parseRequestAsString() {
        command += " " + credential.getIp() + " " + credential.getPort() + " " + credential.getUsername();
//        System.out.println(super.getMessageAsString(command));
        return super.getMessageAsString(command);
    }
}
