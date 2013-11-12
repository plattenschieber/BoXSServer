package server;

import java.util.concurrent.ConcurrentHashMap;

public class AutorunExperiment
{
	String program
	int counter=0
	ConcurrentHashMap<String, Object> varspace=new ConcurrentHashMap<String, Object>()
	ServerClientThread experimenter

	def start()
	{
		new Session(experimenter,program,varspace).start()
		++counter
	}
}
