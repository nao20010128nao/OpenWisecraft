package com.nao20010128nao.Wisecraft.rcon;

public interface KeyChain
{
	public boolean isPasswordStored(String ip,int port);
	public String[] getRecentPassword(String ip,int port);
}
