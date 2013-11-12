package client;

import java.util.*;

public class FileExporter {

	public static final String getExportString(Map<String, Object> varspace)
	{
		String exportString="";

		LinkedList<String> players=new LinkedList<String>();
		LinkedList<String> varnames=new LinkedList<String>();
		LinkedList<String> sysvarnames=new LinkedList<String>();
		
		

		for (String s:varspace.keySet())
		{
			String _player = s.substring(0,s.lastIndexOf('.'));
			if (_player.equals("Global")) continue;
			if (!players.contains(_player))
				players.add(_player);

			String _varname= s.substring(s.lastIndexOf('.')+1,s.length());
			if (!varnames.contains(_varname) && !sysvarnames.contains(_varname) && !_varname.equals("group")
					&& !_varname.equals("role")&& !_varname.equals("username"))
			{
				if (_varname.startsWith("_"))
					sysvarnames.add(_varname);
				else
					varnames.add(_varname);
			}
		}

		Collections.sort(players);
		Collections.sort(varnames);
		varnames.addFirst("role");
		varnames.addFirst("group");
		varnames.addFirst("username");
		varnames.removeAll(sysvarnames);
		varnames.addAll(sysvarnames);


		
		
		
		// Table header
		for(String vn:varnames)
			exportString+=vn+"\t";
		exportString+="\n";

		// Table content
		for(String pn:players)
		{
			for(String vn:varnames)
			{
				String _varname=pn+"."+vn;
				if (varspace.containsKey(_varname)) {
					Object o=varspace.get(_varname);
					exportString+=Utils.getNiceString(o).replaceAll("\n", "|")+"\t";
				}
				else
					exportString+="\t";
			}
			
			exportString+="\n";
		}
		return exportString;
	}
}
