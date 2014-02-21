package server;


import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import static util.Log.*;

public class Group{
	public Session session
	public String name
	public Map<String, ServerClientThread> subjects=new ConcurrentHashMap<String, ServerClientThread>()
	int linenum


	public ExecutionEnvironment assignSubjectToExperiment(
			ServerClientThread sct, String role, Vector<Function> subjectProgram, 
			Map<String, Object> varspace)
	{

		okay "#"+sct.subinfo.num+" Subject "+sct.subinfo.username+" ->  Group "+name+" Role "+role
		subjects.put(role,sct)
		sct.experiment=session
		sct.experimentParser=new ExecutionEnvironment(subjectProgram,sct,session.experimenter,
				role,varspace, this,session)
		sct.experimentParser
	}

	public void writeMatchingHistory()
	{
		subjects.values()*.experimentParser*.writeMatchingHistory(linenum)
	}

}
