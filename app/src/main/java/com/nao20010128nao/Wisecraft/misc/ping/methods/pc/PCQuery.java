package com.nao20010128nao.Wisecraft.misc.ping.methods.pc;

import android.annotation.*;
import android.util.*;
import com.google.gson.*;
import com.nao20010128nao.Wisecraft.misc.*;
import com.nao20010128nao.Wisecraft.misc.compat.*;
import com.nao20010128nao.Wisecraft.misc.ping.methods.*;

import java.io.*;
import java.net.*;

public class PCQuery implements PingHost {
    private Gson gson = Utils.newGson();
    private String host;
    private int port;
    private long lastPing;

    public PCQuery(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /* handshake->Request->statJson->ping */
    private void writeHandshake(DataOutputStream out, String host, int port)
        throws IOException {
        ByteArrayOutputStream handshake_bytes = new ByteArrayOutputStream();
        DataOutputStream handshake = new DataOutputStream(handshake_bytes);

        handshake.writeByte(PingerUtils.PACKET_HANDSHAKE);
        PingerUtils.writeVarInt(handshake, PingerUtils.PROTOCOL_VERSION);
        PingerUtils.writeVarInt(handshake, host.length());
        handshake.writeBytes(host);
        handshake.writeShort(port);
        PingerUtils.writeVarInt(handshake, PingerUtils.STATUS_HANDSHAKE);

        PingerUtils.writeVarInt(out, handshake_bytes.size());
        out.write(handshake_bytes.toByteArray());

    }

    private void writeRequest(DataOutputStream out) throws IOException {
        out.writeByte(0x01); // Size of packet
        out.writeByte(PingerUtils.PACKET_STATUSREQUEST);
    }

    @TargetApi(9)
    private String getStatJson(DataInputStream in) throws IOException {
        PingerUtils.readVarInt(in); // Size
        int id = PingerUtils.readVarInt(in);

        PingerUtils.io(id == -1, "Server prematurely ended stream.");
        PingerUtils.io(id != PingerUtils.PACKET_STATUSREQUEST,
            "Server returned invalid packet.");

        int length = PingerUtils.readVarInt(in);
        PingerUtils.io(length == -1, "Server prematurely ended stream.");
        PingerUtils.io(length == 0, "Server returned unexpected value.");

        byte[] data = new byte[length];
        in.readFully(data);
        String json = new String(data, CompatCharsets.UTF_8);
        return json;
    }

    private void doPing(DataOutputStream out, DataInputStream in)
        throws IOException {

        out.writeByte(0x09);
        out.writeByte(PingerUtils.PACKET_PING);
        out.writeLong(System.currentTimeMillis());

        PingerUtils.readVarInt(in); // Size
        int id = PingerUtils.readVarInt(in);
        PingerUtils.io(id == -1, "Server prematurely ended stream.");
        PingerUtils.io(id != PingerUtils.PACKET_PING, "Server returned invalid packet.");
    }

    // ///////
    public PCQueryResult fetchReply() throws IOException {
        Socket sock = null;
        try {
            sock = new Socket(host, port);
            sock.setSoTimeout(5000);
            DataInputStream dis = new DataInputStream(sock.getInputStream());
            DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
            long t = System.currentTimeMillis();
            writeHandshake(dos, host, port);
            writeRequest(dos);
            String s = getStatJson(dis);
            Log.i("ping_pc", s);
            lastPing = System.currentTimeMillis() - t;
            PCQueryResult result = new RawJsonReply(s);
            // Reply/Reply19 is going to be removed in the future
            // They uses fixed Json structure, and they causing me confused and having low quality
            result.setRaw(s);
            return result;
        } finally {
            if (sock != null)
                sock.close();
        }
    }

    public void doPingOnce() throws IOException {
        Socket sock = null;
        try {
            sock = new Socket(host, port);
            sock.setSoTimeout(5000);
            DataInputStream dis = new DataInputStream(sock.getInputStream());
            DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
            doPing(dos, dis);
        } finally {
            if (sock != null)
                sock.close();
        }
    }

    @Override
    public long getLatestPingElapsed() {
        return lastPing;
    }
}
