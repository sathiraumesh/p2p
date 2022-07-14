package node.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import node.Credential;

import java.util.List;

@Data
@AllArgsConstructor
public class SearchResultResponse extends Message{

    private String command;
    private Credential credential;
    private int numOfFiles;
    private int hops;
    private List<String> fileList;

    public String parseResponseAsString() {
        command += " " +numOfFiles + " " + credential.getIp() + " " + credential.getPort()  +" "+ hops;
        for (String fileName: fileList) {
            command += " " + fileName;
        }
        return super.getMessageAsString(command);
    }
}
