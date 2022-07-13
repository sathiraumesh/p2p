package node.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import node.Credential;

@AllArgsConstructor
@Data
public class SearchRequest extends Message {

    private String filename;
    private Credential credential;

    protected String command;
    private int hops;
    public String parseRequestAsString() {
        command += " " + credential.getIp() + " " + credential.getPort() + " " + filename +" "+ hops;
//        System.out.println(super.getMessageAsString(command));
        return super.getMessageAsString(command);
    }
}
