package client;

import java.io.Serializable;

public class ServerSideValidInfo implements Serializable{
	private static final long serialVersionUID = 1L;
	String ssvi;
	
	public ServerSideValidInfo(String _ssvi) {
		ssvi=_ssvi;
	}
}
