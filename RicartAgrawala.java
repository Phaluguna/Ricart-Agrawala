import java.io.*;
import java.net.*;
import java.util.*;

public class RicartAgrawala implements Runnable {
    public int nodeNo;// Node number of the process
    public int port; // port number from which this node listens to incoming messages
    public ServerSocket ss; // server socket object to accept messages
    public List<Integer> SocketList = new ArrayList<Integer>();// list of port numbers of nodes to which we need to send messages
    public List<String> IpAddr = new ArrayList<String>();// list of all the ip addresses / domain names of nodes to which we need to send messages
    public int num_of_nodes;// total number of nodes including the current node participating
    public int outstandingReplies;// numeber of replies pending to this node after it sends a request to all the other nodes
    public int clock;// internal clock of this node 
    public boolean clockLock;//lock variable to flag that the clock is betimeing accessed so no other events can access clock at the same 
    public boolean requesting = false;// flag to indicate that request msg has been sent
    public boolean criticalSec = false;// flag to indicate that the node is currently executing critical section
    public int defferedCount = 0;// total number of deffered messages
    public List<Integer> DeferQueue = new ArrayList<Integer>();// port numbers of the nodes whose requests have been deffered
    public List<InetAddress> DeferIPQueue = new ArrayList<InetAddress>();//domain names  of the nodes whose requests have been deffered
    public List<String> IpAddrList = new ArrayList<String>();// IP addresses of all the nodes including this node
    public List<Integer> PortNoList = new ArrayList<Integer>();// port numbers of all the nodes including this node
    public List<Integer> NodeNoList = new ArrayList<Integer>();// node numbers numbers of all the nodes including this node
    public int masterPortNo;// port number of node 0
    public String masterIPaddr;// domain name of node 0
    public int criticalSec_exe_count;// number of time this node has executed critical section
    public int heartBeatCount = 0;// number of heart beats received by node 0
    public int RelaxingNodes = 0;// number of nodes who have executed the critical section 40 times 

    //to count the number of messages sent , received and exchanged
    public int sentRequestCount =0;
    public int sentReplyCount = 0;
    public int ReceivedRequestCount =0;
    public int ReceivedReplyCount = 0;
    public int msgExchanged = 0;
    //to track time..
    public long startTime;
    public long endTime;
    
    //===================================================================================================================================
    // This is the constructor... it creates the ServerSocket object and assigns a port to listen for messages from other nodes
    public MutEx(String[] args) throws Exception {
        nodeNo = Integer.parseInt(args[0]);// assign the node number
        clockLock = false;
        clock = 0;
        criticalSec_exe_count = 0;

        // read from the file which has the info about participating nodes in the format ===> <node number>:<domain name>:<port number>
        File file = new File("nodeList.txt");
        FileReader reader = new FileReader(file);
        BufferedReader br = new BufferedReader(reader);
                
        String line;
        while ((line = br.readLine()) != null) {
            String tokens[] = line.split(":");
            IpAddrList.add(tokens[1]);
            PortNoList.add(Integer.parseInt(tokens[2]));
            NodeNoList.add(Integer.parseInt(tokens[0]));
        }

        num_of_nodes = NodeNoList.size();
        masterPortNo = PortNoList.get(0);
        masterIPaddr = IpAddrList.get(0);

        for(int i = 0 ; i < num_of_nodes ; i++){
            if(NodeNoList.get(i) != nodeNo){
                SocketList.add(PortNoList.get(i));// add port numbers of all the other nodes...
                IpAddr.add(IpAddrList.get(i));// add domain names  of all the other nodes...
            }
            else if(NodeNoList.get(i) == nodeNo)// add the port number of this node...
            {
                port = PortNoList.get(i);
            }
        }
        
        try{
            ss = new ServerSocket(port);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    //===================================================================================================================================
    // nodes other than node 0 sends the heart beat message "THUMP_THUMP" to node 0
    public boolean sendHeartBeat(){
        try{
                Socket s = new Socket(masterIPaddr,masterPortNo);
                PrintWriter writer = new PrintWriter(s.getOutputStream(), true);
                writer.println("THUMP_THUMP:"+nodeNo);
                System.out.println("sent a heart beat...");
                s.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return true;
    }
    //===================================================================================================================================
    //node 0 listens to heart beats from all the other nodes. It lets the execution continue only when it hears "THUMP_THUMP" from all the nodes
    public boolean listenToHB(){
        while(true){
            try{
                Socket skt = ss.accept();
                heartBeatCount++;
                System.out.println("::its ALIVE...hallelujah !!!");
                skt.close();
                if(heartBeatCount == SocketList.size()){
                    break;
                }
            }
            catch(Exception e){
                System.out.print(""+e);
            }
        }
        return true;
    }
    //===================================================================================================================================
    // node 0 sends this message to all the other nodes once it has heard heart beats from them
    public boolean uMayProceded(){
        for(int i = 0 ; i < SocketList.size() ; i++)
        {
            try{
                Socket s = new Socket(IpAddr.get(i),SocketList.get(i));
                PrintWriter writer = new PrintWriter(s.getOutputStream(), true);
                writer.println("BEGIN:"+ clock);//  format of msg->  ----- REQUEST:timestamp:processID ----
                s.close(); 
            }
            catch(Exception e){
                System.out.print("");
            }
            System.out.println("Sent execution PERMISSION to :"+ SocketList.get(i));
        }
        return true;
    }
    //===================================================================================================================================
    // nodes other than node 0 begins the execution of sending requests only after they have received permission to continue from node 0 
    public boolean receivedPerm(){
        while(true){
            try{
                Socket skt = ss.accept();
                InputStreamReader iReader = new InputStreamReader(skt.getInputStream());
                BufferedReader reader = new BufferedReader(iReader);
                String message = reader.readLine();
                
                String tokens[] = message.split(":");
                String messageType = tokens[0];
                System.out.println("Received message --> "+messageType+":: from node 0 to start...");
                break;
            }
            catch(Exception e){
                System.out.print("");
            }
        }
        return true;
    }
    //===================================================================================================================================
    // the node sends requests messages to all the other nodes
    public boolean sendRequest(){
        outstandingReplies = SocketList.size();
        clockLock = true;
        clock++;
        
        requesting = true;
        for(int i = 0 ; i < SocketList.size() ; i++)
        {   
            
            boolean scanning=true;
            while(scanning){
                try{
                    Thread.sleep(100);
                    Socket s = new Socket(IpAddr.get(i),SocketList.get(i));
                    scanning = false;
                    PrintWriter writer = new PrintWriter(s.getOutputStream(), true);
                    writer.println("REQUEST:"+ clock +":"+ nodeNo+":"+port);//  format of msg->  ----- REQUEST:timestamp:processID ----
                    System.out.println("sent request to : "+SocketList.get(i));
                    sentRequestCount++;
                    msgExchanged++;
                }
                catch(Exception e){
                    System.out.print(""+e);
                }
            }
            System.out.println("sent REQUEST to : " + SocketList.get(i) + "|||| message = REQUEST( " + clock + " , " + nodeNo+ "  )");
        }
        clockLock = false;
        while(outstandingReplies > 0){
			try{
				Thread.sleep(5);
			}
			catch(Exception e){
                System.out.print(""+e);	
			}
		}

        return true;
    }
    //===================================================================================================================================
    // send the reply to node from which the request message was received
    public boolean sendReply(InetAddress ip ,int ServerPortNum){
        try{
            Socket s = new Socket(ip,ServerPortNum);// last change
            PrintWriter writer = new PrintWriter(s.getOutputStream(), true);
            writer.println("REPLY:"+ clock +":"+ nodeNo+":"+port);//  format of msg->  ----- REPLY : nodeNum ----
            System.out.println("sent REPLY to : " + ServerPortNum + "|||| message = REPLY( " + clock + " , " + nodeNo+ "  )");
            sentReplyCount++;
            msgExchanged++;
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return true;
    }
    //===================================================================================================================================
    // msg handling is done here..
    public void run() {
        String message;
        BufferedReader reader;
        while(true) {
            try{
                Socket s = ss.accept();
                InputStreamReader iReader = new InputStreamReader(s.getInputStream());
                reader = new BufferedReader(iReader);
                message = reader.readLine();
                
                String tokens[] = message.split(":");
                String messageType = tokens[0];
                int c = Integer.parseInt(tokens[1]);// clock value or the RelaxingNodes count from master...
                int n = Integer.parseInt(tokens[2]);// node number
                int p = Integer.parseInt(tokens[3]);// port number of the server from which the msg arrived
                InetAddress ip = s.getInetAddress();
                int new_clock;
                while(true){
                    if( clockLock == true){
                        System.out.print("");
                    }
                    else if(clockLock == false){
                        clockLock = true;
                        new_clock = Math.max(clock,c)+1;
                        clockLock = false;
                        break;
                    }
                }

                System.out.println("Message received ==> "+messageType+"( "+c+" : "+n+" ) @ time = "+clock);
                if(messageType.equals("REQUEST")){
                    ReceivedRequestCount++;
                    msgExchanged++;
                    if(criticalSec == true){
                        //defer request...
                        DeferQueue.add(p);
                        DeferIPQueue.add(ip);
                        clock=new_clock;
                        System.out.println("REQUEST deffered ");
                        defferedCount++;// may be unwanted var....??
                        
                    }
                    else{
                        if(requesting == true && ( clock < c || (clock == c && nodeNo < n ))){
                            //defer the reply and add it to queue
                            DeferQueue.add(p);
                            DeferIPQueue.add(ip);
                            System.out.println("REQUEST deffered ");
                            defferedCount++;
                            clock=new_clock;
                        }
                        else{
                            // send reply back....
                            clock=new_clock;
                            sendReply(ip,p);
                        }
                    }
                }
                //------------------------------------------------------------------------------------------------------------------------------------------------
                else if(messageType.equals("REPLY")){
                    msgExchanged++;
                    ReceivedReplyCount++;
                    //Received a reply -- decrement outstanding replies
                    outstandingReplies--;
                    clock=new_clock;
                }
                //------------------------------------------------------------------------------------------------------------------------------------------------
                else if(messageType.equals("PHEW_AM_DONE")){
                    RelaxingNodes++;
                    System.out.println("COMPLETION message from : "+ n);
                }
                //------------------------------------------------------------------------------------------------------------------------------------------------
                else if(messageType.equals("YOU_MAY_LEAVE")){
                    System.out.println("Exiting the thread of node "+ n);
                    IOException e = new IOException();
                    throw e;
                }
                //------------------------------------------------------------------------------------------------------------------------------------------------
                else if(messageType.equals("END_ALL")){
                    System.out.println("closing node 0");
                    IOException e = new IOException();
                    throw e;
                }
            }
            catch(IOException e){
                System.out.print("");
                break;
            }
        }   
        System.out.println("\nTotal number of requests sent = "+sentRequestCount);
        System.out.println("Total number of replies sent = "+sentReplyCount);
        System.out.println();
        System.out.println("Total number of requests received = "+ReceivedRequestCount);
        System.out.println("Total number of replies received = "+ReceivedReplyCount+"\n");
        
    }

    //===================================================================================================================================

    public boolean initiate() {
        startTime = System.currentTimeMillis();
        sendRequest();
        endTime = System.currentTimeMillis();;
        //enter critical section..
        System.out.println("Entered critical section...");
        try{
            criticalSec = true;
            Thread.sleep(1000);
            clockLock = true;
            File file = new File("Criticalsection.txt");
            FileWriter writer = new FileWriter(file,true);
            writer.write("Critical Section = "+(criticalSec_exe_count+1)+" ||| Entering node: "+nodeNo+" : @ local clock value = "+clock+" ||| messages exchanged "+msgExchanged+" ||| Time ealpsed in milliseconds = "+(endTime-startTime)+"\n");
            criticalSec_exe_count++;
            clock = clock + 1;
            writer.close();
            clockLock = false;
        }
        catch(Exception e){
            e.printStackTrace();
        }
        //exit critical section
        requesting = false;

        System.out.println("Exit critical section...");// send all defered replies...
        criticalSec = false;

        int size = DeferQueue.size();
        while(size != 0){
            clock++;
            sendReply(DeferIPQueue.get(0) ,DeferQueue.get(0));
            DeferQueue.remove(0);
            DeferIPQueue.remove(0);
            size--;
        }
        return true;
    }
    //===================================================================================================================================
    public static void main(String[] args) 
	{
        try{
            MutEx sock = new MutEx(args);
            System.out.println("created server socket.....");

            if(sock.nodeNo != 0){
                //nodes send a heart beat to master node...
                sock.sendHeartBeat();
                sock.receivedPerm();//received permission from master to continue execution....
            }
            else if(sock.nodeNo == 0){

                File file = new File("Criticalsection.txt");
                file.createNewFile();
                FileWriter writer = new FileWriter(file);
                writer.write("\n");
                writer.close();
                System.out.println("File created....");
                //lesten to heart beat
                sock.listenToHB();
                sock.uMayProceded();// give permission to other nodes to begin execution..
            }

            Thread.sleep(1000);
            Thread t1 = new Thread(sock);
            t1.start();
            
            System.out.println("Threads started...");
            Thread.sleep(500);
            
            //begin execution of requesting critical section..
            int count = 0;
            for(int i = 0 ; i < 40 ; i++){
                if(count  <= 20){
                    sock.initiate();
                    count++;
                    Thread.sleep(500);
                }
                else{
                    if(sock.nodeNo % 2 == 0){
                        sock.initiate();
                        count++;
                        Thread.sleep(500);
                    }
                    else{
                        sock.initiate();
                        count++;
                        Thread.sleep(4500);
                    }
                }
                sock.msgExchanged = 0; 
            }
            System.out.println("# critical section executions : "+ sock.criticalSec_exe_count);
            Thread.sleep(1000);

            //send a msg to master when node has completed its full critical setion execution cycles.... untill then wait..!!!
            
            if(sock.nodeNo != 0 && sock.criticalSec_exe_count == 40){
                while(true){
                    try{
                            Socket lastSoc = new Socket(sock.masterIPaddr,sock.masterPortNo);
                            PrintWriter writer = new PrintWriter(lastSoc.getOutputStream(), true);
                            writer.println("PHEW_AM_DONE:"+ sock.clock +":"+ sock.nodeNo+":"+sock.port);// completion message...
                            System.out.println("Sent completion msg from :" + sock.port + " to server: " + sock.masterPortNo);
                            lastSoc.close();
                            break;  
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }
                
            }

            if(sock.nodeNo == 0){// send messages to the nodes that have completed 40 iterations... this msg is send only after all the other nodes have completed 40 iterations.
                while(true){
                    System.out.print("");
                    if(sock.RelaxingNodes == sock.SocketList.size()){
                        //
                        try{
                            for(int i = 0  ; i < sock.SocketList.size() ; i++){
                                Socket leaveSock = new Socket(sock.IpAddr.get(i),sock.SocketList.get(i));
                                PrintWriter writer = new PrintWriter(leaveSock.getOutputStream(), true);
                                writer.println("YOU_MAY_LEAVE:"+ sock.RelaxingNodes +":"+ sock.nodeNo+":"+sock.port);
                                leaveSock.close();
                            }
                            break;
                        }
                        catch(Exception e){
                            System.out.print("");
                        }
                    }
                }
                // finally stop the node 0.... this terminates the execution of the program by closing the threads...
                Socket finalSock = new Socket(sock.masterIPaddr,sock.masterPortNo);
                PrintWriter writer = new PrintWriter(finalSock.getOutputStream(), true);
                writer.println("END_ALL:"+ 1 +":"+ sock.nodeNo+":"+sock.port);
                finalSock.close();
            }
            
        }
        catch(Exception e){
            e.printStackTrace();
        }
	}   
}
