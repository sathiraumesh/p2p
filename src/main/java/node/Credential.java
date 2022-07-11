package node;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Credential {

    private String ip;
    private int port;
    private String username;
}
