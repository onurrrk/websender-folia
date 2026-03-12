package tc.yigit.shared;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;

import tc.yigit.events.EventOutput;

public class SocketServer extends Thread {
    
    public static int port = SocketConfig.port;
    public static ServerSocket listenSock = null;
    
    public static DataInputStream in = null;
    public static DataOutputStream out = null;
    public static Socket sock = null;
    
    public SocketServer(){
    }
    
    @Override
    public void run(){
        try{
            listenSock = new ServerSocket(port);
            
            while(true){
                Socket clientSock = listenSock.accept();
                
                new Thread(() -> {
                    boolean connect_status = false;
                    try{
                        DataInputStream localIn = new DataInputStream(clientSock.getInputStream());
                        DataOutputStream localOut = new DataOutputStream(clientSock.getOutputStream());
                        connect_status = true;
                        
                        if(localIn.readByte() == 1){
                            int random_code = new SecureRandom().nextInt();
                            
                            localOut.writeInt(random_code);
                            boolean success = UtilSocket.readString(localIn, false).equals(UtilSocket.hash(random_code + SocketConfig.password));
                            
                            if(success){
                                localOut.writeInt(1);
                                UtilSocket.createLog(SocketConfig.succesfullyLogin);
                            }else{
                                localOut.writeInt(0);
                                UtilSocket.createLog(SocketConfig.wrongPassword);
                                connect_status = false;
                            }
                        }else{
                            UtilSocket.createLog(SocketConfig.wrongData);
                            connect_status = false;
                        }
                        
                        while(connect_status){
                            final byte packetNumber = localIn.readByte();
                            
                            if(packetNumber == 2){
                                String command = UtilSocket.readString(localIn, true);
                                
                                EventOutput event = UtilSocket.getManager().callOnCommandEvent(clientSock, command);
                                if(event.isCancelled()){
                                    localOut.writeInt(0);
                                    localOut.flush();
                                    continue;
                                }
                                
                                UtilSocket.sendCommand(event.getMessage(), localOut);                            
                            }else if(packetNumber == 3){
                                connect_status = false;
                            }else if(packetNumber == 4){
                                String message = UtilSocket.readString(localIn, true);
                                EventOutput event = UtilSocket.getManager().callOnMessageEvent(clientSock, message);
                                
                                localOut.writeInt(event.isCancelled() ? 0 : 1);
                                localOut.flush();
                            }else{
                                UtilSocket.createLog("Packet not found!");
                            }
                        }
                        
                        localOut.flush();
                        localOut.close();
                        localIn.close();
                    }catch(IOException ex){
                        UtilSocket.createLog(ex.getMessage());
                    }
                    
                    try {
                        if(clientSock != null) clientSock.close();
                    } catch (IOException ex) {
                        UtilSocket.createLog(ex.getMessage());
                    }
                }).start();
            }
        }catch(IOException ex){
            UtilSocket.createLog(ex.getMessage());
        }
    }
}