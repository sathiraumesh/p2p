package node.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import node.Credential;

@Data
@AllArgsConstructor
public class DownLoadRequest extends Message {
    private String filename;
    private Credential credential;

    protected String command;
    public String parseRequestAsString() {
        command += " " + credential.getIp() + " " + credential.getPort() + " " + filename;
        return super.getMessageAsString(command);
    }
}
