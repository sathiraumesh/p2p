import node.Constants;
import node.Credential;
import node.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        Credential bootStrapServerCredential = new Credential(
                Constants.IP_BOOTSTRAP_SERVER,
                Constants.PORT_BOOTSTRAP_SERVER,
                Constants.USERNAME_BOOTSTRAP_SERVER
        );


        List<Node> nodes = nodeList(bootStrapServerCredential);

        for (Node node : nodes) {
            node.unRegister();
            Thread.sleep(1000);
        }

        for (Node node : nodes) {
            node.register();
            Thread.sleep(1000);
        }

        nodes.stream().forEach(node -> node.Join());
        nodes.get(getRandomNodeNumber(10, 0)).search("American_Idol");
        nodes.get(getRandomNodeNumber(10, 0)).search("Microsoft_Office_2010");
        nodes.get(getRandomNodeNumber(10, 0)).search("Hacking_for_Dummies");
        nodes.get(getRandomNodeNumber(10, 0)).leave();

        for (Node node : nodes) {
            Thread.sleep(1000);
            System.out.println("node logs");
            node.getNodeLogs().getLog().forEach(l -> System.out.println(l));
            System.out.println("---------------");
            System.out.println(" ");
        }

    }

    public static List<Node> nodeList(Credential bootStrapServerCredential) throws InterruptedException {

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

        Credential nodeCredential5 = new Credential(
                "127.0.0.1",
                6005,
                "node5"
        );

        Credential nodeCredential6 = new Credential(
                "127.0.0.1",
                6006,
                "node6"
        );

        Credential nodeCredential7 = new Credential(
                "127.0.0.1",
                6007,
                "node7"
        );

        Credential nodeCredential8 = new Credential(
                "127.0.0.1",
                6008,
                "node8"
        );

        Credential nodeCredential9 = new Credential(
                "127.0.0.1",
                6009,
                "node9"
        );

        Credential nodeCredential10 = new Credential(
                "127.0.0.1",
                6010,
                "node10"
        );
        Credential [] credentials = new Credential[]{
                nodeCredential1, nodeCredential2, nodeCredential3, nodeCredential4, nodeCredential5, nodeCredential6, nodeCredential7, nodeCredential8, nodeCredential9, nodeCredential10};
        ArrayList<Node> nodes = new ArrayList<>();
        for(int i = 0; i<credentials.length; i++){
            Node node = new Node(bootStrapServerCredential, credentials[i]);
            nodes.add(node);
            Thread.sleep(1000);
        }
        return nodes;
    }

    private static int getRandomNodeNumber(int upperbound, int lowerbound) {
        Random rand = new Random();
        return rand.nextInt(upperbound-lowerbound) + lowerbound;

    }
}
