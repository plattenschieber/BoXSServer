package server;

import java.util.*;

import static util.Log.*;
import client.ErrorMessage;
import client.Utils;


public class ExecutionEnvironment extends Thread
{
    public final Map<String, Object> _varspace

    public Vector<Function> prg
    public ServerClientThread sct, experimenter
    public String role
    public Group group
    // http://ftp.ics.uci.edu/pub/ietf/http/related/iso639.txt
    public String locale="en"
    public Session session
	public boolean cancel=false, inputHistory=true
	Vector<Assertion> assertions=new Vector<Assertion>()


    public ExecutionEnvironment(Vector<Function> _prg, ServerClientThread _sct, ServerClientThread _experimenter, String _role,
    		Map<String, Object> __varspace, Group _group, Session _session)
    {
    	prg=_prg; sct=_sct; role=_role; group=_group; _varspace=__varspace; session=_session; experimenter=_experimenter;
    	
		if (sct!=null)
    	{
	    	sct.subinfo.experimentGroup=group.name;
	    	sct.subinfo.experimentRole=role;
	    	if (_varspace!=null && role!=null)
	    	{
	    		varspacePut("role", role)
	    		varspacePut("group", group.name)
	    		varspacePut("username", sct.subinfo.username)
	    		varspacePut("_finished", new Double(0))
	    	}
    	}

		MirkoGroovyLibrary.scheduleAt 1000*60*60*12,{cancel=true}
    }


	public void writeMatchingHistory(int linenum) {

    	// Matchhistory
    	String otherRoles=null;
    	for (ServerClientThread or:group.subjects.values())
    	{
    		if (!or.subinfo.username.equals(sct.subinfo.username))
    		{
    			if (otherRoles==null) otherRoles=or.subinfo.username;
    			else otherRoles+=","+or.subinfo.username;
    		}
    	}

    	if (otherRoles!=null)
    	{
	    	String matchHistory;
	    	Object _matchHistory=_varspace.get(sct.subinfo.username+"._matchinghistory");
	    	if (_matchHistory!=null)
	    		matchHistory=(String)_matchHistory+","+otherRoles;
	    	else
	    		matchHistory=otherRoles;
	    	_varspace.put(sct.subinfo.username+"._matchinghistory"+linenum,matchHistory);
    	}
	}


    public void varspacePut(Group _group, String _role, String varname, Object value)
    {
    	_varspace.put( getVarname(_group, _role, varname), value)
    }
	public void varspacePut(String _role, String varname, Object value)
	{
		_varspace.put( getVarname(group, _role, varname), value)
	}
	public void varspacePut(String varname, Object value)
	{
		_varspace.put( getVarname(group, role, varname), value)
	}

    public Object varspaceGet(Group group, String role, String varname)
    {
    	_varspace.get(getVarname(group, role, varname));
    }
    public Object varspaceGet(String role, String varname)
    {
    	_varspace.get(getVarname(group, role, varname));
    }
	public Object varspaceGet(String varname)
	{
    	_varspace.get(getVarname(group, role, varname));
	}



	
	private String getVarname(Group _group=group, String _role=role, String varname)
	{
		if(varname.startsWith('\$')) return varname.substring(1);
		//if(!_group) return randomSubjectname()+"."+varname;
		if(!_group) return "."+varname;
		return _group.subjects.get(_role)?.subinfo?.username + "." + varname;
	}


	
	public String randomSubjectname()
	{
		for (String k:_varspace.keySet())
			if (k.indexOf(".")!=-1)
				return k.substring(0,k.indexOf("."));
		return "";
	}
	
	

    public void run()
    {
		for (Function f:prg)
		{
			if (cancel) return;
        	
	    	try {
    			if (f!=null)
    			{
    				info "execute "+f
    				f.execute(this);
    			}
			} catch (Exception e) {
				session.experimenter.send(new ErrorMessage(f.linenum, f.toString(), e.getMessage(), new java.util.Date()));
				e.printStackTrace();
			}
		}
    }
	

    void checkVarname(String vname) throws Exception {
    	if (vname in ["role","group","username"])
    		throw new Exception("Cannot assign value to system variable "+vname);
	}


	String getOpponentRole() {
		group.subjects.keySet().find {it!=role}  
	}



	public void cancel()
	{
		cancel=true
		interrupt()
	}


	


	public String submitValue(String _name, Object val, long time) throws Exception
	{
		// Resolve name
		String name=resolveVarname(_name.trim());
		if (name.startsWith("\"")) 
			name=name.substring(1,name.length()-1).trim();
		
	//	if (name.startsWith("_continue") && varspaceGet(name) && varspaceGet(name) instanceof Double)
		//	val=(Double)val+(Double)varspaceGet(name);
		
		// Historie schreiben
		Object history=varspaceGet("_inputhistory_"+name);
		String h="";
		if (history!=null)
			h=(String)history+",";
		h+=""+time+".\""+Utils.getNiceString(val, locale)+"\"";
		if (inputHistory && !name.startsWith("_clientdisplaytime"))
		{
			if (h.length()>5000) h="[error: >5000, too big]";
			varspacePut("_inputhistory_"+name,h);
		}
	
		varspacePut(name,val);
		return checkAssertions();
	}

	public String checkAssertions()
	{
		String ret=null
		for (Assertion a:assertions)
			try
			{
				Object res=a.function.execute(this)
				if (res!=1) ret=a.errormessage
			}
			catch(Exception e)
			{
				ret="Unknown error"
			}

		return ret
	}

	String resolveVarname(String vname) throws Exception{
		try
		{
			if (vname.indexOf('[')!=-1)
			{ // Array
				String basevarname=vname.substring(0,vname.indexOf('['));
				String remainder=vname.substring(vname.indexOf('['));

				boolean waschanged=false;
				while (!remainder.isEmpty() && remainder.startsWith("["))
				{
					String eval=client.Utils.getMatchingLevelString(remainder,false,false);
					Object evaluateExpression = LexerParser.evaluateExpression(eval).execute(this);
					String o
					if (evaluateExpression instanceof Double)
						o=""+((Double)evaluateExpression).intValue();
					else
						o=evaluateExpression.toString();

					basevarname+="["+o+"]";
					remainder=remainder.substring(eval.length()+2);
					waschanged=true;
				}
				if (waschanged) basevarname=basevarname+remainder;
				return basevarname;
			}
			return vname;
		}
		catch(Exception e)
		{
			throw new Exception("Unable to resolve variable name "+vname);
		}
	}
	
}
