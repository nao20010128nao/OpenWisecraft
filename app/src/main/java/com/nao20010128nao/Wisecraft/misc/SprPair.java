package com.nao20010128nao.Wisecraft.misc;

import com.nao20010128nao.Wisecraft.*;
import com.nao20010128nao.Wisecraft.misc.ping.methods.*;

import java.io.*;

public class SprPair extends Duo<ServerPingResult, ServerPingResult> implements ServerPingResult {
    public SprPair(){}
    public SprPair(ServerPingResult a,ServerPingResult b){
        super(a,b);
    }
    @Override
    public byte[] getRawResult() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.write(PingSerializeProvider.doRawDump(a));
            dos.write(PingSerializeProvider.doRawDump(b));
            dos.flush();
        } catch (IOException e) {
            WisecraftError.report("SprPair#getRawResult", e);
        }
        return baos.toByteArray();
    }
}
