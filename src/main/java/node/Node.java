package node;

import com.sun.corba.se.impl.orbutil.closure.Constant;
import lombok.Data;
import node.cache.CacheItem;
import node.cache.NodeCache;
import node.messaging.JoinResponse;
import node.messaging.Request;
import node.messaging.Response;
import node.messaging.SearchRequest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
public class Node extends Thread implements NodeOperations{

    private Credential credential;
    private Credential bootStrapSeverCredential;
    private List<String> fileList;
    private Map<Credential, Boolean> routingTable;
    private DatagramSocket socket;

    private List<String> FileList = new ArrayList<>();

    private NodeCache cache = new NodeCache(new ArrayList<>());

    public Node(Credential bootStrapSeverCredential, Credential nodeCredentials) {
        this.bootStrapSeverCredential = bootStrapSeverCredential;
        this.credential = nodeCredentials;
        this.routingTable = new HashMap<>();
        this.fileList = new ArrayList<>(createFileList());
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

    @Override
    public void Join() {
        Request joinRequest = new Request(credential, Constants.Command.JOIN);
        String msg = joinRequest.parseRequestAsString();
        if (routingTable.size() > 0){
            for (Map.Entry<Credential, Boolean> entry : routingTable.entrySet()) {
                Credential senderCredentials = entry.getKey();
                try {
                    socket.send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, InetAddress.getByName(senderCredentials.getIp()), senderCredentials.getPort()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }else{
            System.out.println("Cannot Join Network No Neighbours"+ credential.getIp() + credential.getUsername() + credential.getPort());
        }

    }

    @Override
    public void Leave() {
        Request joinRequest = new Request(credential, Constants.Command.LEAVE);
        String msg = joinRequest.parseRequestAsString();
        for (Map.Entry<Credential, Boolean> entry : routingTable.entrySet()) {
            Credential senderCredentials = entry.getKey();
            try {
                socket.send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, InetAddress.getByName(senderCredentials.getIp()), senderCredentials.getPort()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        unRegister();
    }

    @Override
    public void Search(String filename) {
        SearchRequest searchRequest = new SearchRequest(filename, credential, Constants.Command.SEARCH, 0);
        String msg = searchRequest.parseRequestAsString();
        for (Map.Entry<Credential, Boolean> entry : routingTable.entrySet()) {
            Credential senderCredentials = entry.getKey();
            try {
                socket.send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, InetAddress.getByName(senderCredentials.getIp()), senderCredentials.getPort()));
                cache.addCacheItem(senderCredentials.getIp(), senderCredentials.getPort(), Constants.Command.SEARCH, filename);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                handleRequest(senderCredentials, message);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void handleRequest(Credential senderNodeCredentials, String message){
        System.out.println(message);
            StringTokenizer tokenizer = new StringTokenizer(message, " ");

            String length = tokenizer.nextToken();
            String command = tokenizer.nextToken();

            switch (command) {
                case Constants.Command.REGOK:
                    processREGOK(tokenizer);
                    break;
                case Constants.Command.UNREGOK:
                    processUNROK(tokenizer);
                    break;
                case Constants.Command.JOIN:
                    processJOIN(tokenizer, senderNodeCredentials);
                    break;
                case Constants.Command.JOINOK:
                    processJOINOK(tokenizer, senderNodeCredentials);
                    break;
                case Constants.Command.LEAVE:
                    processLEAVE(tokenizer, senderNodeCredentials);
                    break;
                case Constants.Command.LEAVEOK:
                    processLEAVEOK(tokenizer, senderNodeCredentials);
                    break;
                case Constants.Command.SEARCH:
                    processSearch(tokenizer, senderNodeCredentials);
                    break;
                case Constants.Command.SEARCHOK:
                    processSearch(tokenizer, senderNodeCredentials);
                default:
                    System.out.println("Cannot Process Request");
            }
    }

    private void processREGOK(StringTokenizer rest){
        int numOfNodes = Integer.parseInt(rest.nextToken());

        if (!(numOfNodes == Constants.Codes.Register.ERROR_CANNOT_REGISTER ||
                numOfNodes == Constants.Codes.Register.ERROR_DUPLICATE_IP ||
                numOfNodes == Constants.Codes.Register.ERROR_ALREADY_REGISTERED ||
                numOfNodes == Constants.Codes.Register.ERROR_COMMAND)){
            for (int i = 0; i< numOfNodes; i++){
                String ip = rest.nextToken();
                int port = Integer.parseInt(rest.nextToken());
                routingTable.put(new Credential(ip, port, null), false);
            }

            System.out.println(routingTable.size());
        }else {
            System.out.println("Error in command: " + numOfNodes);
        }
    }

    private void processUNROK(StringTokenizer rest){
        int codeNumber = Integer.parseInt(rest.nextToken());
        if(codeNumber == Constants.Codes.Register.ERROR_COMMAND){
            System.out.println("Error in command: " + codeNumber);
            return;
        }
        System.out.println("Unregister Successful");
    }

    private void processJOIN(StringTokenizer rest, Credential senderCredentials){
        System.out.println("Sending Join Okay");
        JoinResponse response = new JoinResponse(Constants.Command.JOINOK, 0);
        String msg = response.parseResponseAsString();
        try {
            socket.send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, InetAddress.getByName(senderCredentials.getIp()), senderCredentials.getPort()));
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void processJOINOK(StringTokenizer rest, Credential senderCredentials){
        System.out.println("Making Join Okay Live");
        routingTable.put(senderCredentials, true);
    }
    private void processLEAVE(StringTokenizer rest, Credential senderCredentials) {
        JoinResponse response = new JoinResponse(Constants.Command.LEAVEOK, 0);
        String msg = response.parseResponseAsString();
        try {
            socket.send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, InetAddress.getByName(senderCredentials.getIp()), senderCredentials.getPort()));
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void processLEAVEOK(StringTokenizer rest, Credential senderCredentials) {
        System.out.println("Leave Successfully");
    }

    private void processSearch(StringTokenizer rest, Credential senderCredentials){
        String senderIP = rest.nextToken();
        int senderPort = Integer.parseInt(rest.nextToken());
        String fileName = rest.nextToken();
        int hops = Integer.parseInt(rest.nextToken());

        System.out.println("Processing search");

        List<String> fileSearchList = searchForFiles(fileName);

        if (fileSearchList.isEmpty()){
            boolean messageAlreadySent = cache.getCacheItemList().contains(new CacheItem(senderIP, senderPort, Constants.Command.SEARCH, fileName));

            if (messageAlreadySent){
                System.out.println("Already sent not sending again");
                return;
            }

            searchNeighbours(fileName, senderIP, senderPort, hops);
        }else {

        }
    }

    List<String> searchForFiles(String fileName) {
        Pattern pattern = Pattern.compile(fileName);
        return fileList.stream().filter(pattern.asPredicate()).collect(Collectors.toList());
    }
    private void searchNeighbours(String filename, String ip, int port, int hops){
        SearchRequest searchRequest = new SearchRequest(filename, new Credential(ip, port, null), Constants.Command.SEARCH, hops + 1);
        String msg = searchRequest.parseRequestAsString();
        System.out.println("From search neighbours");
        System.out.println(msg);
        for (Map.Entry<Credential, Boolean> entry : routingTable.entrySet()) {
            Credential senderCredentials = entry.getKey();
            try {
                socket.send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, InetAddress.getByName(senderCredentials.getIp()), senderCredentials.getPort()));
                cache.addCacheItem(senderCredentials.getIp(), senderCredentials.getPort(), Constants.Command.SEARCH, filename);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void processSearchOK(StringTokenizer rest, Credential senderCredentials){

    }
    public List<String> createFileList() {
        ArrayList<String> fileList = new ArrayList<>();
        fileList.add("Adventures_of_Tintin");
        fileList.add("Jack_and_Jill");
        fileList.add("Glee");
        fileList.add("The_Vampire Diarie");
        fileList.add("King_Arthur");
        fileList.add("Windows_XP");
        fileList.add("Harry_Potter");
        fileList.add("Kung_Fu_Panda");
        fileList.add("Lady_Gaga");
        fileList.add("Twilight");
        fileList.add("Windows_8");
        fileList.add("Mission_Impossible");
        fileList.add("Turn_Up_The_Music");
        fileList.add("Super_Mario");
        fileList.add("American_Pickers");
        fileList.add("Microsoft_Office_2010");
        fileList.add("Happy_Feet");
        fileList.add("Modern_Family");
        fileList.add("American_Idol");
        fileList.add("Hacking_for_Dummies");
        Collections.shuffle(fileList);
        List<String> subFileList = fileList.subList(0, 5);
        System.out.println("File List : " + Arrays.toString(subFileList.toArray()));
        return subFileList;
    }
}
