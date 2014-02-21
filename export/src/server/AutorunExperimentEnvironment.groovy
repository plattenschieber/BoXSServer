package server;

import static util.Log.*;
import java.util.concurrent.ConcurrentHashMap;

public class AutorunExperimentEnvironment extends ExecutionEnvironment
{
    public AutorunExperimentEnvironment(Vector<Function> _prg, ServerClientThread _sct, ServerClientThread _experimenter, String _role,
    		Map<String, Object> _varspace, AutorunExperiment _exp)
    {
    	super(_prg, _sct, _experimenter, _role, _varspace, null,_exp)
    }
    	
    	void run()
    	{
			sct.experimentParser=this
			sct.experiment=session
			info "Start autorun experiment for "+sct.subinfo.username
			super.run()
			session.sendSessionDataByMail("session complete for "+sct.subinfo.username,_varspace)
			close()
    	}

    	void close()
    	{
			info "End autorun experiment for "+sct.subinfo.username

			varspacePut("_finished", new Double(1))
			sct.experimentParser=null
			sct.experiment=null
			sct.updates.clear()
			sct.updates.add("finish")
			sct.update()
		}
}
