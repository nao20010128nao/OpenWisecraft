package com.nao20010128nao.Wisecraft.misc;

//Server class for <=3.2

import com.google.gson.annotations.*;

public class OldServerW3_2 {
    @SerializedName("ip")
    public String ip;
    @SerializedName("port")
    public int port;
    @SerializedName("mode")
    public int mode;//0 is PE, 1 is PC

    @Override
    public int hashCode() {
        return ip.hashCode() ^ port ^ mode;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OldServerW3_2)) {
            return false;
        }
        OldServerW3_2 os = (OldServerW3_2) o;
        return os.ip.equals(ip) & os.port == port & os.mode == mode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (/*ip.matches(Constant.IPV6_PATTERN)*/false) {
            sb.append('[').append(ip).append(']');//IPv6
        } else {
            sb.append(ip);//IPv4
        }
        sb.append(':').append(port);
        return sb.toString();
    }

    public OldServerW3_2 cloneAsServer() {
        OldServerW3_2 s = new OldServerW3_2();
        cloneInto(s);
        return s;
    }

    public void cloneInto(OldServerW3_2 dest) {
        dest.ip = ip;
        dest.port = port;
        dest.mode = mode;
    }
}
