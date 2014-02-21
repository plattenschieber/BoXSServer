package server;

import java.io.*;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.*
import static util.Log.*;

import com.sun.imageio.plugins.common.SubImageInputStream;

import static client.ServerCommand.*;
import client.*;

public class ServerClientThread extends ConnChannel implements Comparable<ServerClientThread>
{
	static int DEFAULT_MINIMUM_UPDATEDELAY, globalcount=0
	static ConcurrentLinkedQueue<ServerClientThread> subjectPool=new ConcurrentLinkedQueue<ServerClientThread>(), 
		experimenterPool=new ConcurrentLinkedQueue<ServerClientThread>(), 
		suspendedPool=new ConcurrentLinkedQueue<ServerClientThread>()
	static final Map<String, AutorunExperiment> autorunExperiments=new ConcurrentHashMap<String, AutorunExperiment>()
	static final Hashtable<String, String> realmpasswords=new Hashtable<String, String>()
	
	public LinkedList<String> lastUpdates=new LinkedList<String>()
	LinkedList<String> updates=new LinkedList<String>()

	boolean wasclosed=false, updatethreadRunning=false, details=false, ready=false
	int minimumUpdatedelay=200
	long lastupdate=0

	SubjectInfo subinfo=new SubjectInfo()
	Session experiment
	ExecutionEnvironment experimentParser


	
	public ServerClientThread(ObjectOutputStream out, ObjectInputStream _in, SocketAddress adr) throws IOException
	{
		super(out, _in)
		minimumUpdatedelay=DEFAULT_MINIMUM_UPDATEDELAY
		subinfo.num=globalcount++
		subinfo.socket=adr
		new Timer().runAfter(1000*60*60*12) {warning "Closing due to timeout"; 	close()}
	}

	
	public void receive(Object _o)
	{
		Object[] __temp=(Object[])_o
		Object o=__temp[1]
		
		switch(	(ServerCommand)__temp[0])
		{
			case LOGIN:
				String[] s=(String[])o
				subinfo.username = s[0]
				subinfo.realm = s[1]
				login(s[2])
				break
				
			case START_EXPERIMENT:
				if (!experiment || !experiment.running)
					new Session(this,(String)o,new ConcurrentHashMap<String, Object>(),true).start()
				break

			case START_EXPERIMENT_AUTORUN:
				if (autorunExperiments.containsKey(subinfo.realm))
				{
					info "Removing autorun experiment"
					autorunExperiments.remove(subinfo.realm)
				}
				else
				{
					info "Adding autorun experiment"
					autorunExperiments.put(subinfo.realm, new AutorunExperiment((String)o,this))
				}
				break
				
			case SUBMIT_VALUE:
				Object[] temp=(Object[])o
				if (!experimentParser) break
				String ret=experimentParser.submitValue((String)temp[0],temp[1],((Long)temp[2]).longValue())
				send(new ServerSideValidInfo(ret))
				notifyExperimenter()
				break

			case SET_READY:
				ready=((String)o).equals("yes")
				break
				
			case SET_DETAILS:
				details=((Integer)o)==1
				break

			case SEND_EXPERIMENT:
				log "received send request: experiment="+experiment
				experiment?.sendSessionDataByMail("send requested");
				break;
				
			case KILL_SUBJECT:
				SubjectInfo si=(SubjectInfo)o;
				log "Kill subject "+si.username
				ServerClientThread killsct=ServerClientThread.subjectPool.find { si.username==it.subinfo.username }
				killsct?.close()
				ServerClientThread.suspendedPool.remove(killsct)
				break

			case CANCEL_EXPERIMENT:
				experiment?.sendSessionDataByMail("experiment cancelled")
				experiment?.cancelExperiment()
				if (autorunExperiments.containsKey(subinfo.realm))
				{
					info "Removing autorun experiment"
					autorunExperiments.remove(subinfo.realm)
				}
				break
		}
		
		if (subinfo.isExperimenter()) update()
		
	}
	


	
	def existsBySubjectname(realmName, subjectName)
	{
		subjectPool.any{it.subinfo.realm==realmName && it.subinfo.username==subjectName}
	}
	
	
	
	public void login(String password)
	{
		synchronized(subjectPool)
		{
			// Settings für Experimenter
			if (subinfo.isExperimenter())
			{
				minimumUpdatedelay=1000;
				if (realmpasswords.containsKey(subinfo.realm))
				{
					if (!realmpasswords.get(subinfo.realm).equals(password))
					{
						warning "Password for realm incorrect. Disconnecting"
						close(); return
					}
				} 
				else
				{
					realmpasswords.put(subinfo.realm, password);
				}
				
				// Alte Experimenter killen
				experimenterPool.grep{it.subinfo.realm.equals(subinfo.realm)}.each{it.close()}
			}
			else if (subinfo.username=="new" && !autorunExperiments.containsKey(subinfo.realm))
			{
				subinfo.username=''+((2..10000).find{!existsBySubjectname(subinfo.realm, ''+it)});
			}
			else if (existsBySubjectname(subinfo.realm, subinfo.username))
			{
				subjectPool.
					findAll{it.subinfo.realm==subinfo.realm && it.subinfo.username==subinfo.username}.
					each{warning "Killing duplicate subject: $it"; it.close()}
			}
			
			
			okay ""+new java.util.Date()+"#"+subinfo.num+" Login: "+ subinfo.username + ":" + subinfo.realm+"@"+subinfo.socket+" Exp: "+subinfo.isExperimenter()
	
			// Testen, ob Experiment vorliegt
			ServerClientThread matchingSuspendedThread = suspendedPool.find {it.subinfo==subinfo}
			
			if (matchingSuspendedThread/* && matchingSuspendedThread.experimentParser*/)
			{
				okay ""+new java.util.Date()+"#"+subinfo.num+" Reconnecting to experiment"
				suspendedPool.remove(matchingSuspendedThread)
				experiment=matchingSuspendedThread.experiment
				experimentParser=matchingSuspendedThread.experimentParser
				experimentParser?.sct=this
			
				if (!subinfo.isExperimenter())
				{
					for (group in experiment.groups.values())
						group.subjects.each{role,sct->
							if (sct == matchingSuspendedThread)
							{
								subinfo.experimentGroup=group.name
								subinfo.experimentRole=role
								subinfo.inExperiment=true
								group.subjects.put(role, this)
							}
						}
	
					updates.addAll(matchingSuspendedThread.lastUpdates)
					experiment.varspace.put(subinfo.username+"._suspended", new Double(0))
				}
			}
	
	
			if (subinfo.isExperimenter())
				experimenterPool.add(this)
			else
			{
				subjectPool.add(this)
				subjectPool.sort()
				notifyExperimenter()
			}
	
			// Autorun??
			if (!matchingSuspendedThread && autorunExperiments.containsKey(subinfo.realm))
			{
				autorunExperiments.get(subinfo.realm).start(this)
			}
			
			update()
		}
	}

	
	def notifyExperimenter() {
		experimenterPool.grep{it.subinfo.realm==subinfo.realm}*.update()
	}

	
	
	public void update()
	{
		if (subinfo.isExperimenter())
		{
			long time=new java.util.Date().getTime()
			if (time>lastupdate+minimumUpdatedelay)
			{
				lastupdate=time
				_update()
			}
			else
			{
				if (!updatethreadRunning)
				{
					updatethreadRunning=true
					new Timer().runAfter(minimumUpdatedelay,{updatethreadRunning=false; update()})
				}
			}
		}
		else
		{
			_update()
		}	
	}
	
	
	private synchronized void _update()
	{
		if (subinfo.isExperimenter())
		{	
			Object ret=new Object[3];
			// Experimentdaten
			if (experiment!=null && experiment.varspace!=null)
			{
				ExperimentSessionData esd=((Object[])ret)[0]=new ExperimentSessionData();
				esd.num=experiment.num;
				esd.varspace=new ConcurrentHashMap<String, Object>(experiment.varspace);
				if (!details)
				{
					Object[] temp=esd.varspace.keySet().toArray();
					
					for (Object s:temp)
						if (((String)s).startsWith("_"))
								esd.varspace.remove(s);
				}
				esd.running=experiment.running;
			}

			// Subjektdaten
			Vector<SubjectInfo> subinfos=new Vector<SubjectInfo>()
	
			subinfos.addAll (subjectPool.grep{it.subinfo.realm==subinfo.realm}.collect {
				new SubjectInfo(num:it.subinfo.num, username: it.subinfo.username,  
					inExperiment:it.experiment!=null,isSuspended:false	) })
			
			subinfos.addAll (suspendedPool.grep{it.subinfo.realm==subinfo.realm}.collect {
				new SubjectInfo(num:it.subinfo.num, username: it.subinfo.username,
					inExperiment:it.experiment!=null,isSuspended:true ) })

			((Object[])ret)[1]=subinfos
			// Autorun-Experiment?
			((Object[])ret)[2]=new Boolean(autorunExperiments.containsKey(subinfo.realm))

			send(ret);
		}
		else if (!updates.isEmpty())
		{
			lastUpdates.clear()
			lastUpdates.addAll(updates)
			String s=(experimentParser!=null?"0@locale("+experimentParser.locale+")\n":"")+updates.join('\n')

			send(s)
			updates.clear()
			notifyExperimenter()
		}
	}
	
	
	public void close()
	{	
		if (!wasclosed)
		{
			wasclosed=true;
			info ""+new java.util.Date()+"#"+subinfo.num+" Disconnect: "+ subinfo.username + ":" + subinfo.realm+"@"+subinfo.socket
			if (experiment==null)
			{
				info ""+new java.util.Date()+"#"+subinfo.num+" Closing connection"
				subjectPool.remove(this);
				experimenterPool.remove(this);
			}
			else if (subinfo.isExperimenter() || (experimentParser!=null ))
			{
				warning ""+new java.util.Date()+"#"+subinfo.num+" Disconnected during experiment!"
				if (!subinfo.isExperimenter() && experiment.varspace!=null)
					experiment.varspace.put(subinfo.username+"._suspended", new Double(1));
				subjectPool.remove(this);
				experimenterPool.remove(this);
				suspendedPool.add(this);
				
				new Timer().runAfter(1000*60*60,{	
					if (suspendedPool.contains(this))
					{
						warning ""+new java.util.Date()+"Removing suspended sct from list"
						suspendedPool.remove(this);
					}
				})
			}
			
			notifyExperimenter();
		}
		super.close();
	}

	public static def getAvailableSubjects(String realm)
	{
		subjectPool.grep{it.experiment==null && it.subinfo.realm.equals(realm)}
	}

	@Override
	public int compareTo(ServerClientThread o)
	{
		return subinfo.username.compareTo(o.subinfo.username);
	}
	
	static String getInfoString() {
		[["Date",new Date()],["Port",Server.port],["Mem",Runtime.getRuntime().freeMemory()],
		["SubP",subjectPool.size()],
		["ExpP",experimenterPool.size()],["SusP",suspendedPool.size()],
		["Autorun",autorunExperiments.size()],["ConnC",globalcount]].collect(
			{name,val -> "$name=$val"}).join(" ")
	}
}