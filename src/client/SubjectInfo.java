package client;

import java.io.Serializable;
import java.net.SocketAddress;

public class SubjectInfo implements Serializable, Comparable<SubjectInfo>
{
	private static final long serialVersionUID = 1L;

	public int num;
	public String username;
	public String realm;
	public String experimentGroup, experimentRole;
	public boolean inExperiment;

	public boolean isSuspended;

	public transient SocketAddress socket;

	public boolean isExperimenter()
	{
		return username.startsWith("exp:");
	}
	

	@Override
	public String toString()
	{
		return username+" "+(inExperiment?experimentGroup+"."+experimentRole:"");
	}
/*
	public SubjectInfo clone()
	{
		SubjectInfo cl=new SubjectInfo();
		cl.num=num;
		cl.username=username;
		cl.realm=realm;
		cl.inExperiment=inExperiment;
		cl.experimentGroup=experimentGroup;
		cl.experimentRole=experimentRole;
		cl.isSuspended=isSuspended;
		return cl;
	}
*/
	@Override
	public int compareTo(SubjectInfo o) {
		return username.compareTo(o.username);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		SubjectInfo b=(SubjectInfo)obj;
		return b.username.equals(username) && b.realm.equals(realm);
	}

}
