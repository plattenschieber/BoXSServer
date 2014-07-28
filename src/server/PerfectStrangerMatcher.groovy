package server;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;
import static util.Log.*;

public class PerfectStrangerMatcher {

	private static Vector<int[]> getPermutations(int subjectcount, int rolecount)
	{
		if (subjectcount%rolecount!=0) throw new IllegalArgumentException("illegal subject/role ratio");
		Vector<int[]> ret=new Vector<int[]>();
		int[] temp=new int[subjectcount];
		for (int i=0; i<subjectcount; i++)
			temp[i]=i/rolecount;
		ret.add(temp);
		getPermutations(ret,subjectcount,(int)(subjectcount/rolecount),rolecount,new int[0],subjectcount);
		return ret;
	}

	private static void getPermutations(final Vector<int[]> ret, final int subjectcount,
			final int teamcount, final int rolecount, final int[] history, final int left)
	{
		if (left==0)
		{
			ret.add(history);
			return;
		}

		x: for (int i=0; i<teamcount; i++)
		{
			int countedi=0;
			// test ob zu gro�e teams...
			for (int j=0; j<history.length; j++)
			{
				if (history[j]==i)
				{
					countedi++;
					if (countedi>=rolecount) continue x;
				}
			}

			// test ob �berschneidung
			int testpos=history.length;
			for (int k in (0..<testpos))
			{
				for (int[] otherMatch:ret)
				{
					if (otherMatch[k]==otherMatch[testpos] && history[k]==i)
					{
						continue x;
					}
				}
			}

			for (int k=0; k<testpos; k++)
			{
				// und jedes andere Subject...
				for (int j=0; j<k; j++)
				{
					// pr�fe ob sie sich gesehen haben...
					for (final int[] otherMatch:ret)
					{
						if (otherMatch[k]==otherMatch[j] && history[k]==history[j])
						{
							continue x;
						}
					}
				}
			}


			int[] h=new int[history.length+1];
			for (int j=0; j<history.length; j++)
				h[j]=history[j];

			h[h.length-1]=i;

			getPermutations(ret, subjectcount, teamcount,rolecount, h, left-1);
		}
	}


	public static void main(String[] args)
	{
		int sc=12;
//		for (int sc=Integer.parseInt(args[0]); sc<=Integer.parseInt(args[1]); sc++)
		lrc: for (int rc=2; rc<sc; rc++)
		{
			//if (sc%rc!=0) continue;
			info "\ncalculating SC="+sc+"  RC="+rc
			
			try {  
				Vector<int[]> allAssignments=getPermutations(sc,rc);
				okay "SC="+sc+"  RC="+rc+"  #="+allAssignments.size()
				info allAssignments
				//new File("pstables/"+sc+"_"+rc+".dat").withObjectOutputStream {it.writeObject(allAssignments) }
			} catch(IllegalArgumentException iae)
			{
				break lrc;
			}
		}
	}

}
