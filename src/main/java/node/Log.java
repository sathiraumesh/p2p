package node;

import lombok.Data;

import java.util.Date;

@Data
public class Log {

    private String searchQuery;
    private Date triggeredTime;
    private Date deliveryTime;
    private int hopsRequired;
    private Credential servedNode;
}
