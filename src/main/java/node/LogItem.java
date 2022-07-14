package node;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data

public class LogItem {
    private String msg;
    private Instant triggeredTime ;
    private Credential servedNode;

    public LogItem(String msg, Credential credential){
        this.triggeredTime = Instant.now();
        this.msg = msg;
        this.servedNode = credential;
    }
    @Override
    public String toString() {
        return "LogItem{" +
                ", msg='" + msg + '\'' +
                ", triggeredTime=" + triggeredTime +
                ", servedNode=" + servedNode.getUsername() +
                '}';
    }
}

