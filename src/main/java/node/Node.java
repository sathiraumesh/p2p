package node;

import lombok.Data;
import node.request.Request;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;

@Data
public class Node extends Thread implements NodeOperations{

    private Credential credential;
    private Credential bootStrapSeverCredential;
    private List<String> fileList;
    private List<Credential> routingTable;
    private DatagramSocket socket;

    public Node(Credential bootStrapSeverCredential, Credential nodeCredentials) {
        this.bootStrapSeverCredential = bootStrapSeverCredential;
        this.credential = nodeCredentials;
        start();
    }

    @Override
    public void start() {
        try {
            socket = new DatagramSocket(credential.getPort());
        } catch(SocketException e) {
            e.printStackTrace();
        }
        new Thread(this).start();
    }

    @Override
    public void register() {
        Request registerRequest = new Request(credential, Constants.Command.REG);
        String msg = registerRequest.parseRequestAsString();
        try {
            socket.send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, InetAddress.getByName(bootStrapSeverCredential.getIp()), bootStrapSeverCredential.getPort()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unRegister() {
        Request unRegisterRequest = new Request(credential, Constants.Command.UNREG);
        String msg = unRegisterRequest.parseRequestAsString();
        try {
            socket.send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, InetAddress.getByName(bootStrapSeverCredential.getIp()), bootStrapSeverCredential.getPort()));
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void run(){
        System.out.println("Node " + credential.getUsername() + " on port " +credential.getPort());
        byte buffer[];
        DatagramPacket packet;
        while (true) {
            buffer = new byte[65536];
            packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                Credential senderCredentials = new Credential(packet.getAddress().getHostAddress(), packet.getPort(), null);
                System.out.println(message);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void close() {
        socket.close();
    }
}
