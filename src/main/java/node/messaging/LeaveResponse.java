package node.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class LeaveResponse extends Message{

    private String command;
    private int code;

    public String parseResponseAsString() {
        command += " " + code;
//        System.out.println(super.getMessageAsString(command));
        return super.getMessageAsString(command);
    }
}
