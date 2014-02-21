package server;

import static util.Log.*;
import java.util.concurrent.ConcurrentHashMap;

public class AutorunExperiment extends Session
{
	Vector<Function> parsedProgram
	int counter=0

	AutorunExperiment(String _program, ServerClientThread _experimenter)
	{
		super(_experimenter, _program, null, true)
		parsedProgram=LexerParser.parseProgram(_program,1)
	}

	void start(ServerClientThread sct)
	{
		++counter
		if (sct.subinfo.username.toLowerCase().equals("new"))
			sct.subinfo.username=''+counter
		

		String groupname=sct.subinfo.username
		AutorunExperimentEnvironment ee=new AutorunExperimentEnvironment(parsedProgram, sct, experimenter,sct.subinfo.username,new ConcurrentHashMap<String, Object>(),this)
		ee.start()
	}
}
