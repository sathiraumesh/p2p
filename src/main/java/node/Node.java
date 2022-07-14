package node;

import lombok.Data;
import node.cache.CacheItem;
import node.cache.NodeCache;
import node.messaging.*;

import java.io.*;
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
    private int joinedNumberOfNodes = 0;
    private String filesToDownload;

    private Log nodeLogs;
    public Node(Credential bootStrapSeverCredential, Credential nodeCredentials) {
        this.bootStrapSeverCredential = bootStrapSeverCredential;
        this.credential = nodeCredentials;
        this.routingTable = new HashMap<>();
        this.fileList = new ArrayList<>(createFileList());
        this.filesToDownload = null;
        this.nodeLogs = new Log(new ArrayList<>(), nodeCredentials);
        this.nodeLogs.addLogItem("Starting node");
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
            System.out.println(credential.getUsername() +" Cannot Join Network No Neighbours "+ credential.getIp() + " " + credential.getPort());
            System.out.println("");
        }
    }

    @Override
    public void leave() {
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
    public void search(String filename) {
        SearchRequest searchRequest = new SearchRequest(filename, credential, Constants.Command.SEARCH, 0);
        String msg = searchRequest.parseRequestAsString();
        filesToDownload = filename;
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
                nodeLogs.addLogItem(message);
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
                    processSearchOK(tokenizer, senderNodeCredentials);
                    break;
                case Constants.Command.DOWNLOAD:
                    processDownload(tokenizer, senderNodeCredentials);
                    break;
                case Constants.Command.DOWNLOADOK:
                    processDownloadOkay(tokenizer, senderNodeCredentials);
                    break;
                default:
                    System.out.println("Cannot Process Request" + command);
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
            System.out.println("Number of nodes to join: " + routingTable.size());
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
        System.out.println(credential.getUsername() + " Unregister Successful");
        System.out.println("");
    }

    private void processJOIN(StringTokenizer rest, Credential senderCredentials){
        System.out.println("Joining the network");
        JoinResponse response = new JoinResponse(Constants.Command.JOINOK, 0);
        String msg = response.parseResponseAsString();
        try {
            socket.send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, InetAddress.getByName(senderCredentials.getIp()), senderCredentials.getPort()));
        }catch (IOException e){
            e.printStackTrace();
        }

        System.out.println(" ");
    }

    private void processJOINOK(StringTokenizer rest, Credential senderCredentials){
        System.out.println(credential.getUsername() +" Making Join Okay Live");
        routingTable.put(senderCredentials, true);
        joinedNumberOfNodes +=1;
        System.out.println(credential.getUsername()+ " Joined node count: " + joinedNumberOfNodes);
        System.out.println("");
    }
    private void processLEAVE(StringTokenizer rest, Credential senderCredentials) {
        System.out.println("Processing Leave");
        JoinResponse response = new JoinResponse(Constants.Command.LEAVEOK, 0);
        String msg = response.parseResponseAsString();
        try {
            socket.send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, InetAddress.getByName(senderCredentials.getIp()), senderCredentials.getPort()));
        }catch (IOException e){
            e.printStackTrace();
        }

        System.out.println("");
    }

    private void processLEAVEOK(StringTokenizer rest, Credential senderCredentials) {
        System.out.println("Processing Leave Okay");
        routingTable = new HashMap<>();
        cache = new NodeCache(new ArrayList<>());
        System.out.println(credential.getUsername() +" Leave Successfully");
        System.out.println("");
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
                System.out.println(" ");
                return;
            }
            searchNeighbours(fileName, senderIP, senderPort, hops);

        }else {

            System.out.println("Sending matching file names");
            SearchResultResponse response = new SearchResultResponse(Constants.Command.SEARCHOK, credential, fileSearchList.size(), hops, fileSearchList);
            String msg = response.parseResponseAsString();
            System.out.println(msg);
            try {
                socket.send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, InetAddress.getByName(senderIP), senderPort));
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        System.out.println(" ");
    }

    List<String> searchForFiles(String fileName) {
        Pattern pattern = Pattern.compile(fileName);
        return fileList.stream().filter(pattern.asPredicate()).collect(Collectors.toList());
    }
    private void searchNeighbours(String filename, String ip, int port, int hops){
        SearchRequest searchRequest = new SearchRequest(filename, new Credential(ip, port, null), Constants.Command.SEARCH, hops + 1);
        String msg = searchRequest.parseRequestAsString();
        System.out.println("Search neighbours");
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
        System.out.println("");
    }

    public void processSearchOK(StringTokenizer rest, Credential senderCredentials){
        System.out.println("Processing Search OK");
        int numberOfFiles = Integer.parseInt(rest.nextToken());
        String senderIP = rest.nextToken();
        int senderPort = Integer.parseInt(rest.nextToken());
        int hops = Integer.parseInt(rest.nextToken());

        if (numberOfFiles == Constants.Codes.Search.ERROR_NODE_UNREACHABLE){
            System.out.println("Node Unreachable");

        } else if (numberOfFiles == Constants.Codes.Search.ERROR_OTHER) {
            System.out.println("Some Other Error occurred");

        } else if (numberOfFiles == 0) {
            System.out.println("no matching results found");

        } else if (numberOfFiles >=1) {
            while (rest.hasMoreElements()){
                String file = rest.nextToken();
                if (filesToDownload!= null && filesToDownload.equalsIgnoreCase(file)){
                    System.out.println("contains the file");
                    sendDownloadRequest(file, senderIP, senderPort);
                }
            }
        }
        System.out.println("");
    }

    public void sendDownloadRequest(String file, String senderIP, int senderPort ){
        DownLoadRequest downLoadRequest= new DownLoadRequest(file, credential, Constants.Command.DOWNLOAD);
        String msg = downLoadRequest.parseRequestAsString();
        System.out.println(msg);
        try {
            socket.send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, InetAddress.getByName(senderIP), senderPort));
            filesToDownload = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("");
    }
    public void processDownload(StringTokenizer rest, Credential senderCredentials){
        new Thread(()-> {
            System.out.println("Downloading file");
            DownloadResponse response = new DownloadResponse(Constants.Command.DOWNLOADOK, "filecontents");
            String msg = response.parseResponseAsString();
            System.out.println(msg);
            try {
                socket.send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, InetAddress.getByName(credential.getIp()), credential.getPort()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println();
        }).start();
    }

    public void processDownloadOkay(StringTokenizer rest, Credential senderCredentials){
        System.out.println("Processing Downloaded file");
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
        System.out.println(" ");
        System.out.println("File List : " + Arrays.toString(subFileList.toArray()));
        return subFileList;
    }

    public void close(){
        socket.close();
    }
}
