package com.jagrosh.discordipc.entities.pipe;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.Callback;
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.RandomAccess;
import java.util.UUID;
public abstract class Pipe {
    private static final int VERSION = 1;
    PipeStatus status = PipeStatus.CONNECTING;
    IPCListener listener;
    private DiscordBuild build;
    final IPCClient ipcClient;
    private final HashMap<String,Callback> callbacks;
    Pipe(IPCClient ipcClient, HashMap<String, Callback> callbacks)
    {
        this.ipcClient = ipcClient;
        this.callbacks = callbacks;
    }
    public static Pipe openPipe(IPCClient ipcClient, long clientId, HashMap<String,Callback> callbacks,
                                DiscordBuild... preferredOrder) throws NoDiscordClientException
    {
        if(preferredOrder == null || preferredOrder.length == 0)
            preferredOrder = new DiscordBuild[]{DiscordBuild.ANY};
        Pipe pipe = null;
        Pipe[] open = new Pipe[DiscordBuild.values().length];
        for(int i = 0; i < 10; i++)
        {
            try
            {
                String location = getPipeLocation(i);
                pipe = createPipe(ipcClient, callbacks, location);
                pipe.send(Packet.OpCode.HANDSHAKE, new JSONObject().put("v", VERSION).put("client_id", Long.toString(clientId)), null);
                Packet p = pipe.read();
                pipe.build = DiscordBuild.from(p.getJson().getJSONObject("data")
                        .getJSONObject("config")
                        .getString("api_endpoint"));
                if(pipe.build == preferredOrder[0] || DiscordBuild.ANY == preferredOrder[0])
                {
                    break;
                }
                open[pipe.build.ordinal()] = pipe;
                open[DiscordBuild.ANY.ordinal()] = pipe;
                pipe.build = null;
                pipe = null;
            }
            catch(IOException | JSONException ex)
            {
                pipe = null;
            }
        }
        if(pipe == null)
        {
            for(int i = 1; i < preferredOrder.length; i++)
            {
                DiscordBuild cb = preferredOrder[i];
                if(open[cb.ordinal()] != null)
                {
                    pipe = open[cb.ordinal()];
                    open[cb.ordinal()] = null;
                    if(cb == DiscordBuild.ANY)
                    {
                        for(int k = 0; k < open.length; k++)
                        {
                            if(open[k] == pipe)
                            {
                                pipe.build = DiscordBuild.values()[k];
                                open[k] = null;
                            }
                        }
                    }
                    else pipe.build = cb;
                    break;
                }
            }
            if(pipe == null)
            {
                throw new NoDiscordClientException();
            }
        }
        for(int i = 0; i < open.length; i++)
        {
            if(i == DiscordBuild.ANY.ordinal())
                continue;
            if(open[i] != null)
            {
                try {
                    open[i].close();
                } catch(IOException ex) {
                }
            }
        }
        pipe.status = PipeStatus.CONNECTED;
        return pipe;
    }
    private static Pipe createPipe(IPCClient ipcClient, HashMap<String, Callback> callbacks, String location) {
        String osName = System.getProperty("os.name").toLowerCase();
        try {
            RandomAccessFile file = new RandomAccessFile(location, "rw");
        } catch (FileNotFoundException e) {
            return new Pipe(ipcClient, callbacks) {
                @Override
                public Packet read() throws IOException, JSONException {
                    return new Packet(Packet.OpCode.CLOSE, new JSONObject());
                }
                @Override
                public void write(byte[] b) throws IOException {
                }
                @Override
                public void close() throws IOException {
                }
            };
        }
        if (osName.contains("win"))
        {
            return new WindowsPipe(ipcClient, callbacks, location);
        }
        else
        {
            throw new RuntimeException("Unsupported OS: " + osName);
        }
    }
    public void send(Packet.OpCode op, JSONObject data, Callback callback)
    {
        try
        {
            String nonce = generateNonce();
            Packet p = new Packet(op, data.put("nonce",nonce));
            if(callback!=null && !callback.isEmpty())
                callbacks.put(nonce, callback);
            write(p.toBytes());
            if(listener != null)
                listener.onPacketSent(ipcClient, p);
        }
        catch(IOException ex)
        {
            status = PipeStatus.DISCONNECTED;
        }
    }
    public abstract Packet read() throws IOException, JSONException;
    public abstract void write(byte[] b) throws IOException;
    private static String generateNonce()
    {
        return UUID.randomUUID().toString();
    }
    public PipeStatus getStatus()
    {
        return status;
    }
    public void setStatus(PipeStatus status)
    {
        this.status = status;
    }
    public void setListener(IPCListener listener)
    {
        this.listener = listener;
    }
    public abstract void close() throws IOException;
    public DiscordBuild getDiscordBuild()
    {
        return build;
    }
    private final static String[] unixPaths = {"XDG_RUNTIME_DIR","TMPDIR","TMP","TEMP"};
    private static String getPipeLocation(int i)
    {
        if(System.getProperty("os.name").contains("Win"))
            return "\\\\?\\pipe\\discord-ipc-"+i;
        String tmppath = null;
        for(String str : unixPaths)
        {
            tmppath = System.getenv(str);
            if(tmppath != null)
                break;
        }
        if(tmppath == null)
            tmppath = "/tmp";
        return tmppath+"/discord-ipc-"+i;
    }
}
