package com.nao20010128nao.Wisecraft.asfsls.misc.serverList.sites;

import com.nao20010128nao.Wisecraft.asfsls.misc.serverList.MslServer;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * A server list site.<br>
 * Shouldn't be used directly.
 *
 * @see com.nao20010128nao.Wisecraft.asfsls.misc.serverList.ServerAddressFetcher
 */
public interface ServerListSite {
    /**
     * Checks this class supports the URL.
     */
    boolean matches(URL url);

    /**
     * Checks there's more than one servers in a URL.
     */
    boolean hasMultipleServers(URL url) throws IOException;

    /**
     * Finds Minecraft multiplayer IP & port from a URL.
     *
     * @return A list that was found servers contains. Immutable.
     */
    List<MslServer> getServers(URL url) throws IOException;
}
