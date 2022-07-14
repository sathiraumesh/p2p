package node.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DownloadResponse extends Message{
    private String command;
    private String fileContent;
    public String parseResponseAsString() {
        command += " " + fileContent;
        return super.getMessageAsString(command);
    }
}
