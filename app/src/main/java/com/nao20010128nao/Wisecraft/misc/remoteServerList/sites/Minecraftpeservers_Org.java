package com.nao20010128nao.Wisecraft.misc.remoteServerList.sites;

import com.nao20010128nao.Wisecraft.misc.remoteServerList.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Parser class for "minecraftpeservers.org"
 */
public class Minecraftpeservers_Org implements ServerListSite {

    public Minecraftpeservers_Org() {
        // TODO 自動生成されたコンストラクター・スタブ
    }

    @Override
    public boolean matches(URL url) {
        // TODO 自動生成されたメソッド・スタブ
        return url.getHost().equalsIgnoreCase("minecraftpeservers.org");
    }

    @Override
    public boolean hasMultipleServers(URL url) {
        // TODO 自動生成されたメソッド・スタブ
        if (url.getPath().replace("/", "").equals("")
            | url.getPath().replace("/", "").toLowerCase().startsWith("index"))
            return true;
        if (url.getPath().replace("/", "").toLowerCase().startsWith("server"))
            return false;
        return false;
    }

    @Override
    public List<MslServer> getServers(URL url) throws IOException {
        // TODO 自動生成されたメソッド・スタブ
        if (url.getPath().replace("/", "").toLowerCase().startsWith("server")) {
            // Single server page
            Document page = Jsoup.connect(url.toString()).userAgent("Mozilla").get();
            String ip = page.select("html > body > #single > div > #left > table > tbody > tr > td > span").get(2)
                .html();
            return Arrays.asList(MslServer.makeServerFromString(ip, true));
        }
        if (url.getPath().replace("/", "").equals("")
            | url.getPath().replace("/", "").toLowerCase().startsWith("index")) {
            List<MslServer> list = new ArrayList<>();
            Document page = Jsoup.connect(url.toString()).userAgent("Mozilla").get();
            Elements elems = page.select("html > body > #main > div > table > tbody > tr > td > div > p");
            for (Element e : elems) {
                String ip = e.html().substring(29);
                list.add(MslServer.makeServerFromString(ip, true));
            }
            return list;
        }
        return null;
    }
}
