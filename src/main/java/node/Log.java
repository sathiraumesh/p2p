package node;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Log {
    private List<LogItem> log;
    private Credential node;
    public void addLogItem(String msg){
        log.add(new LogItem(msg, node));
    }
}
