package client;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExperimentSessionData implements Serializable
{
	private static final long serialVersionUID = 1L;
	public Map<String, Object> varspace;
	public int num;
	public boolean running;
}
