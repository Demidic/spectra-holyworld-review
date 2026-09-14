package com.jagrosh.discordipc.entities.pipe;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.entities.Callback;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.entities.User;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
public class WindowsPipe extends Pipe
{
    private RandomAccessFile file;
    WindowsPipe(IPCClient ipcClient, HashMap<String, Callback> callbacks, String location)
    {
        super(ipcClient, callbacks);
        try {
            this.file = new RandomAccessFile(location, "rw");
        } catch (FileNotFoundException e) {
        }
    }
    @Override
    public void write(byte[] b) throws IOException {
        file.write(b);
    }
    @Override
    public Packet read() throws IOException, JSONException {
        while(file.length() == 0 && status == PipeStatus.CONNECTED)
        {
            try {
                Thread.sleep(50);
            } catch(InterruptedException ignored) {}
        }
        if(status==PipeStatus.DISCONNECTED)
            throw new IOException("Disconnected!");
        if(status==PipeStatus.CLOSED)
            return new Packet(Packet.OpCode.CLOSE, null);
        Packet.OpCode op = Packet.OpCode.values()[Integer.reverseBytes(file.readInt())];
        int len = Integer.reverseBytes(file.readInt());
        byte[] d = new byte[len];
        file.readFully(d);
        Packet p = new Packet(op, new JSONObject(new String(d)));
        if(listener != null)
            listener.onPacketReceived(ipcClient, p);
        if ("READY".equals(p.getJson().optString("evt", null))) {
            JSONObject data = p.getJson().getJSONObject("data");
            JSONObject user = data.getJSONObject("user");
        }
        return p;
    }
    @Override
    public void close() throws IOException {
        send(Packet.OpCode.CLOSE, new JSONObject(), null);
        status = PipeStatus.CLOSED;
        file.close();
    }
}
