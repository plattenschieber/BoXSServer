package server;


import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import static util.Log.*;

import client.ErrorMessage;
import client.FileExporter;



public class Session extends Thread
{
	public boolean cancel=false, running=true
	static int count=0
	public int num=count++, matchingoffset=1, matchingNum=0
	static def MATCHINGCOMMANDS=["matchManualmatchManual","matchAll","matchPerfectStranger","matchStranger","matchDone","matchHistoryClear"]
	
	ServerClientThread experimenter
	String[] program
	public ConcurrentHashMap<String, Object> varspace
	Date startTime=new Date()
	Map<String, Group> groups=new ConcurrentHashMap<String, Group>()


	Session(ServerClientThread _experimenter, String _program, ConcurrentHashMap<String, Object> _varspace)
	{
		varspace=_varspace
		experimenter=_experimenter
		experimenter.experiment=this
		
		program=_program.split('\n')
		if (!program.any{ l-> MATCHINGCOMMANDS.any{l.trim().startsWith(it)} })
			program=["matchAll(A)",*program]
		else 
			program=["",*program]
	}

	
	
	
	void run() {
		super.run()
		log "Session started"

		Vector<Function> parsedProgram=LexerParser.parseProgram(program.join("\n"),1);

		for (f in parsedProgram)
			info ""+f

		ExecutionEnvironment ee=new ExecutionEnvironment(parsedProgram, null, experimenter, "sessionmaster",varspace,null,this)

		try
		{
			ee.run()
		}
		catch(ErrorMessage em)
		{
			experimenter.send(em)
			em.printStackTrace()
		}
		catch(Exception pe)
		{
			experimenter.send(new ErrorMessage(0, "?", pe.getMessage(), new Date()))
			pe.printStackTrace();
		}

		clearGroups()

		okay "Session finished"
		cancelExperiment()
		running=false
		experimenter.update()

		sendSessionDataByMail "session complete"
	}

	
	
	def clearGroups()
	{
		if (!groups.isEmpty())
		{
			groups.values()*.writeMatchingHistory()
			
			info "MatchDone! Num="+groups.values().size()
			for (g in new LinkedList<Group>(groups.values()))
			{
				info "Waiting for group "+g.name+" to finish"
	
				for (j in 0..10)
				for (sct in g.subjects.values())
				{
					try
					{
						sct.experimentParser?.join()
					}
					catch (InterruptedException e)
					{
						//e.printStackTrace()
					}
					finally
					{
						sct.experiment=null
					}
				}
				info "group "+g.name+" finished"
			}
	
			groups.clear();
		}
	}
	
	
	
	
	def sendSessionDataByMail(String title)
	{
		if (experimenter.subinfo.username.length()>4)
		{
			// Export stuff and send it by mail
			String filename="export/"+experimenter.subinfo.realm+"-"+new Date().getTime()+".csv",
				   usermailaddr=experimenter.subinfo.username.substring(4).replaceAll("%40", "@")

			if (usermailaddr.indexOf("@")==-1 || usermailaddr.indexOf(".")==-1)
			{
			}
			else if (SmtpSend.active)
			{
				try {
					new File(filename).withWriter { it.write(FileExporter.getExportString(varspace)) }
					
					SmtpSend.sendMail(usermailaddr, 
						"Your experiment using the Bonn Experiment System: $title", 
						"Hello!\n\nThank you for using the Bonn Experiment System. Here is your data!\nPlease remember that you *must* cite the BoXS in your publication as \"Mirko Seithe (2010): Introducing the Bonn Experiment System (Discussion Paper)\".  \n\nPlease do not answer to this mail. If you have questions please visit boxs.uni-bonn.de", 
						filename);
					okay "Mail sent successfully to "+usermailaddr
					
				}
				catch(Exception e) { error(e) }
				
			}
		}
	}
	
	


	public void cancelExperiment() {
		cancel=true; running=false
		info "Cancel/Finish Experiment"

		ServerClientThread.subjectPool.grep{it.subinfo.realm==experimenter.subinfo.realm}.each{
			it.experimentParser?.varspacePut("_finished", new Double(1))
			it.experimentParser?.cancel()
			it.experimentParser=null
			it.experiment=null
			it.updates.clear()
			it.updates.add("finish")
			it.update()
		}
		ServerClientThread.suspendedPool.removeAll(ServerClientThread.suspendedPool.grep{it.subinfo.realm==experimenter.subinfo.realm})

//		for (ServerClientThread s:g.subjects.values())

/*
		for (g in groups.values())
		for (ServerClientThread s:g.subjects.values())
		{
			s.experimentParser?.cancel();
			if (ServerClientThread.suspendedPool.contains(s))
				ServerClientThread.suspendedPool.remove(s);
		}*/
	}
}