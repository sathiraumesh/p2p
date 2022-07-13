package node.cache;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CacheItem {
    private String ip;
    private int port;
    private String Command;
    private  String filename;
}
