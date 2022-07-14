package node.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import node.Credential;

@Data
@AllArgsConstructor
public class JoinResponse extends Message{
    private String command;
    private int code;
    public String parseResponseAsString() {
        command += " " + code;
        return super.getMessageAsString(command);
    }
}
