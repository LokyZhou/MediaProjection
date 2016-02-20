package com.example.zzr.mediaprojection;

import android.os.Handler;
import android.os.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by Administrator on 2016/1/26.
 */
public class ServerThread implements Runnable {
    private Socket socket = null;
    private BufferedReader bf = null;
    private Handler handler;
    public ServerThread(Socket socket) throws IOException{
        this.socket = socket;
        bf = new BufferedReader(new InputStreamReader(socket.getInputStream(),"utf-8"));
    }

    @Override
    public void run() {
        try{
            String content = null;
            while((content = bf.readLine()) != null){
                /**
                 * process the content here;
                 */
                if("QUIT".equals(content)){
                    socket.close();
                    MainActivity.socketList.remove(socket);
                }else if (String.valueOf(MainActivity.TEST).equals(content)){
                    Message msg = new Message();
                    msg.what = MainActivity.socket_msg;
                    msg.obj = content;
                    handler.sendMessage(msg);

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
