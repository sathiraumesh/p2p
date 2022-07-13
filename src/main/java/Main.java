import node.Constants;
import node.Credential;
import node.Node;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Credential bootStrapServerCredential = new Credential(
                Constants.IP_BOOTSTRAP_SERVER,
                Constants.PORT_BOOTSTRAP_SERVER,
                Constants.USERNAME_BOOTSTRAP_SERVER
        );


        Credential nodeCredential1 = new Credential(
                "127.0.0.1",
                6000,
                "node1"
        );

        Credential nodeCredential2 = new Credential(
                "127.0.0.1",
                6001,
                "node2"
        );

        Credential nodeCredential3 = new Credential(
                "127.0.0.1",
                6003,
                "node3"
        );

        Credential nodeCredential4 = new Credential(
                "127.0.0.1",
                6004,
                "node4"
        );

        Credential [] credentials = new Credential[]{nodeCredential1, nodeCredential2, nodeCredential3, nodeCredential4};
        ArrayList<Node> nodes = new ArrayList<>();
        for(int i = 0; i<credentials.length; i++){
            Node node = new Node(bootStrapServerCredential, credentials[i]);
            node.register();
            nodes.add(node);
            Thread.sleep(2000);
        }

        nodes.stream().forEach(node -> node.Join());

        nodes.get(3).Leave();

        Thread.sleep(2000);
        nodes.get(3).register();
        nodes.get(3).Search("American_Idol");
        Thread.sleep(10000);
        for(int i = 0; i<nodes.size(); i++){
            nodes.get(i).unRegister();
            Thread.sleep(2000);
        }
    }
}
