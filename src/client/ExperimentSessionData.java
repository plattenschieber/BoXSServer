package client;

import java.io.Serializable;
import java.util.Map;

public class ExperimentSessionData implements Serializable
{
	private static final long serialVersionUID = 1L;
	public Map<String, Object> varspace;
	public int num;
	public boolean running;
}
