package server;


import java.util.*;
import static util.Log.*;


import client.*


public class LexerParser {
	static String findRemProg(String[] program, int i, boolean preservematches) {
		String remprg=""
		for (j in (i..<program.length))
		{
			def l=program[j].trim()
			if (l.startsWith("matchDone"))
				return remprg
			else if (!preservematches && ["matchManual","matchAll","matchPerfectStranger","matchHistoryClear","matchAutoRun"].any{l.startsWith(it)})
				remprg+="\n"
			else
				remprg+=program[j]+"\n"
		}
		return remprg
	}



	public static Vector<Function> parseProgram(String program, int executestartlinenum) throws ErrorMessage
	{
		Vector<Function> ret=new Vector<Function>()
    	String[] progline=program.split('\n')
        mainparseloop: for (int line=0; line<progline.length; line++)
        {
        	String s=progline[line]
        	// info "line = "+executestartlinenum+"->"+line+" : "+s

        	try
        	{
    			// Test for if/while
    			if (s.indexOf("(")>=0 && !s.trim().startsWith('//'))
    			{
	                String command=s.substring(0,s.indexOf("(")).trim(), 
						param=s.substring(s.indexOf("(")+1, s.lastIndexOf(")")).trim()

						
					
	    	        if (command in ['matchHistoryClear'])
	    	        {		
	    	        	ret.add(new Function(name:"matchHistoryClear",linenum:line,c:{ ee->ee.session.matchingNum=0}));
						continue mainparseloop
	    	        }
	    	        else if (command in ['matchDone'])
	    	        {		
	    	        	ret.add(new Function(name:"matchDone",linenum:line,c:{ ee->ee.session.clearGroups()}));
						continue mainparseloop
	    	        }	    	        
	    	       
	    	        else if (command in ['matchAll','matchStranger','matchPerfectStranger','matchManual'])
	    	        {
	    	        	String block=findRemProg(progline,line+1,false)
	    	        	int blocklength=block.split("\n").length;
	    	        	Vector<Function> subprog=parseProgram(block,line+1);

	    	        	synchronized(ServerClientThread.subjectPool)
	    	        	{
							switch(command)
							{
								case "matchManual":
				    	        	ret.add(new Function(name:"matchManual",linenum:line,c:{       		ee->
				    	        		if (!ee.session.running) return
				    	        		def params=client.Utils.splitByComma(param)
										def x=params.collect{(String)evaluateExpression(it).execute(new ExecutionEnvironment(null,null,ee.session.experimenter,"",ee.session.varspace,null,ee.session))}
										def _subject=x[0], role=x[2], _group=x[1]
										def group=ee.session.groups.get(_group)
										
										if (!group)
										{
											group=new Group(session:ee.session,name:_group,linenum:line+1)
											ee.session.groups.put(_group,group)
										}
								
										def subject=ServerClientThread.getAvailableSubjects(ee.experimenter.subinfo.realm).find{it.subinfo.username.equalsIgnoreCase(_subject)}
										
										if (!subject) 
											warning("subject not found: "+_subject)
										else
											group.assignSubjectToExperiment(subject, role,subprog,ee.session.varspace).start()
				    	        	}, stringrep:{return "matchManual {"+subprog.collect({it.toString()}).join("\n")+"\n}"}));
									break

								case "matchAll":
			    	        		ret.add(new Function(name:"matchAll",linenum:line,c:{ ee->
			    	        			if (!ee.session.running) return
			    	        			if (ServerClientThread.getAvailableSubjects(ee.experimenter.subinfo.realm).isEmpty()) throw new RuntimeException("No subjects available for matching")
						    	        	String[] roles=param.split(",")
											int groupcount=(int)(ServerClientThread.getAvailableSubjects(ee.experimenter.subinfo.realm).size()/roles.length)

											def subjectthread=[]		
											for (g in 1..groupcount)
											{
												String groupname=""+g
												Group group=new Group(session:ee.session,name:groupname,linenum:line+1)
												ee.session.groups.put(groupname,group)
											   for (role in roles)
											   {
													subjectthread<<group.assignSubjectToExperiment(ServerClientThread.getAvailableSubjects(ee.experimenter.subinfo.realm)[0],
														role,subprog,ee.session.varspace)
												}
											}
											subjectthread*.start()
										}, stringrep:{return "matchAll {"+subprog.collect({it.toString()}).join("\n")+"\n}"}));
			    	        		break

			    	        	case "matchStranger":
			    	        		ret.add(new Function(name:"matchStranger",linenum:line,c:{ ee->
										if (!ee.session.running) return		    	        			
			    	        			if (ServerClientThread.getAvailableSubjects(ee.experimenter.subinfo.realm).isEmpty()) throw new RuntimeException("No subjects available for matching")
						    	        	String[] roles=param.split(",")
											int groupcount=(int)(ServerClientThread.getAvailableSubjects(ee.experimenter.subinfo.realm).size()/roles.length)

											def subjectthread=[]		
											for (g in 1..groupcount)
											{
												String groupname=""+g
												Group group=new Group(session:ee.session,name:groupname,linenum:line+1)
												ee.session.groups.put(groupname,group)
									
											   for (role in roles)
													subjectthread<<group.assignSubjectToExperiment(ServerClientThread.getAvailableSubjects(ee.experimenter.subinfo.realm).randomElement,
														role,subprog,ee.session.varspace)
											}
											subjectthread*.start()
										}, stringrep:{return "matchStranger {"+subprog.collect({it.toString()}).join("\n")+"\n}"}));
			    	        		break

			    	        	case "matchPerfectStranger":
			    	        		ret.add(new Function(name:"matchPerfectStranger",linenum:line,c:{ ee->
										if (!ee.session.running) return
			    	        			if (ServerClientThread.getAvailableSubjects(ee.experimenter.subinfo.realm).isEmpty()) throw new RuntimeException("No subjects available for matching")

					    	        	String[] roles=param.split(",")
					    	        	int availableSubjects=ServerClientThread.getAvailableSubjects(ee.experimenter.subinfo.realm).size()
										int redsubjectcount=availableSubjects-availableSubjects%roles.length;

										info "PSM for $redsubjectcount/${roles.length}"
										try {
											Vector<int[]> assignments=new File("pstables/"+redsubjectcount+"_"+roles.length+".dat").withObjectInputStream { it.readObject() }		

											if (ee.session.matchingNum>=assignments.size())
												throw "Too many perfect matching attempts. Use matchHistoryClear()"
											int[] assignment=assignments.get(ee.session.matchingNum)

											for (j in (0..<(int)(availableSubjects/roles.length)))
												ee.session.groups.put(""+(j+1),new Group(session:ee.session,name:""+(j+1),linenum:line));
				
											def subjectthread=[]
											for (j in 0..<redsubjectcount)
											{
												ServerClientThread sct=ServerClientThread.subjectPool.grep{it.subinfo.realm.equals(ee.experimenter.subinfo.realm)}[j]
												Group g=ee.session.groups.get(""+((int)(assignment[j])+1))
												String role=roles[g.subjects.size()]

												subjectthread<<g.assignSubjectToExperiment(sct, role,subprog,ee.session.varspace)
											}
											subjectthread*.start()


											ee.session.matchingNum++
										}
										catch(Exception e) {
											e.printStackTrace()
										}
											


									}, stringrep:{return "matchPerfectStranger {"+subprog.collect({it.toString()}).join("\n")+"\n}"}));
			    	        		break
			    	        }

							boolean furtherManualMatches=findRemProg(progline,line+1,true).split("\n").any({it.trim().startsWith("matchManual")})
		   	       			line+=(command=="matchManual"&&furtherManualMatches)?0:blocklength
		    	        	continue mainparseloop
			    	    }
	    	        }
	    	        else if (command in ['if','while','for'])
	    	        {
	    	        	String remprg=(line+1..<progline.length).collect{progline[it]}.join('\n');
						String block=client.Utils.getMatchingLevelString(remprg.trim(),false, false);
	    	        	int blocklength=block.count('\n')+1

						switch(command)
						{
							case "if":
		    	        		Function para=evaluateExpression(param)
		    	        		Vector<Function> subprog=parseProgram(block,executestartlinenum+line+1);
								ret.add(new Function(name:"if",linenum:executestartlinenum+line,c:{ ee->
									if (para.execute(ee)!=0)
										for (Function f:subprog)
											f?.execute(ee);
								}, stringrep:{return "if {"+subprog.collect({it.toString()}).join("\n")+"\n}"}));
								break
								
							case "while":
								Function para=evaluateExpression(param)
								Vector<Function> subprog=parseProgram(block,executestartlinenum+line+1);
								ret.add(new Function(name:"while",linenum:executestartlinenum+line,c:{ ee->
									while (para.execute(ee)!=0 && !ee.cancel)
										for (Function f:subprog)
											f?.execute(ee);
								}, stringrep:{return "while {"+subprog.collect({it.toString()}).join("\n")+"\n}"}));
								break
									
							case "for":
								String[] param2=param.split(';')
								if (param2.length!=3) throw new Exception("Invalid for statement: $param");
								Function[] para=[parseLine(param2[0], 0),
										evaluateExpression(param2[1]),
										parseLine(param2[2], 0)]
								
								Vector subprog=parseProgram(block,executestartlinenum+line+1);
	
								ret.add(new Function(name:'for',linenum:executestartlinenum+line,c:{ ee->
									
									para[0].execute(ee)
									while (para[1].execute(ee)!=0 && !ee.cancel)
									{
										for (Function f:subprog)
											f?.execute(ee);
										para[2].execute(ee)
									}
								}, stringrep:{return "for {"+subprog.collect({it.toString()}).join("\n")+"\n}"}
								));
								break
						}
	    	        	
	    	        	line+=blocklength
	    	        	continue mainparseloop
	    	        }
    			}

    			def temp=parseLine(s,executestartlinenum+line)
    			if (temp!=null)
    				ret.add(temp);
        	}
        	catch(ErrorMessage e)
        	{
        		error(e);
        		throw e;
        	}
        	catch(Exception e)
        	{
        		error(e);
        		throw new ErrorMessage(executestartlinenum+line, s, e.getMessage(),new java.util.Date());
        	}
        }

        return ret;
	}

	
	
	
	static String stripQuotation(String val)
	{
		return (val).substring(1,(val).length()-1);
	}


    public static Function parseLine(String _line, int linenum) throws Exception
    {
        String line=_line.trim();
        if (line.isEmpty() || line.startsWith("//")) return null

        boolean assignment=false;

        int para=0;
		line.chars.each { 
				if (it=='(') para++;
				if (it==')') para--;
				if (para==0 && it=='=')
					assignment=true;
			}

        if (assignment)
        {
        	String _vname=client.Utils.getMatchingLevelString(line, false, true);
        	String exp=line.substring(_vname.length()+1);
        	_vname=_vname.trim();
        	exp=exp.trim();
			def eexp=evaluateExpression(exp)
			
			return new Function(name:"var",c:{ ee->
					String vname=ee.resolveVarname(_vname);
					Object o=eexp.execute(ee);
			
					StringTokenizer st=new StringTokenizer(vname,".");
					String[] temp=new String[st.countTokens()];
					int i=0;
			
					while(st.hasMoreElements())
						temp[i++]=st.nextToken();
			
					// Variablen
					switch(temp.length)
					{
					case 1: //   test
						ee.checkVarname(vname);
						if (vname.startsWith("\$"))
							ee.varspacePut(vname, o);
						else
							ee.varspacePut(ee.group, ee.role, vname, o);
						break;
					case 2: //   A.test
						ee.checkVarname(temp[1]);
						if (temp[0].equalsIgnoreCase("opponent"))
							ee.varspacePut(ee.group, ee.opponentRole, temp[1], o);
						else if (temp[0].equalsIgnoreCase("*"))
						{
							if (ee.group && ee.group.subjects)
								for (String r:ee.group.subjects.keySet())
									ee.varspacePut(ee.group, r, temp[1], o);
						}
						else
							ee.varspacePut(ee.group, temp[0], temp[1], o);
						break;
					case 3: //   1.A.test
						ee.checkVarname(temp[2]);
						if (temp[0].equalsIgnoreCase("*") && temp[1].equalsIgnoreCase("*"))
						{	// *.*.test
							for (Group g:ee.group.session.groups.values())
								for (String r:g.subjects.keySet())
									ee.varspacePut(g, r, temp[2], o);
						}
						else if (temp[0].equalsIgnoreCase("*"))
						{	// *.x.test
							for (Group g:ee.group.session.groups.values())
								ee.varspacePut(g, temp[1], temp[2], o);
						}
						else if (temp[1].equalsIgnoreCase("*"))
						{	// *.x.test
							for (String r:ee.group.subjects.keySet())
								ee.varspacePut(ee.session.groups.get(temp[0]), r, temp[2], o);
						}
						else
						{
							ee.varspacePut(ee.session.groups.get(temp[0]), temp[1], temp[2], o);
						}
						break;
					}
				}
				
				);
			
			
        }
        else
        {
        	if (!line.trim().isEmpty())
        	{
	            String command=line.substring(0,line.indexOf("(")).trim(), 
						param=line.substring(line.indexOf("(")+1, line.lastIndexOf(")")).trim();
	
	            Vector<String> paramparts=client.Utils.splitByComma(param);
	
				
				boolean hasVarPar=false
	
				switch(command)
				{
					case "debug":
						return new Function(name:command,c:{
									ee,ist,soll -> 
									if (ist==soll) okay "debug okay: $ist = $soll"
									else error " *** ERROR: $ist != $soll ***"
								},l:evaluateExpression(paramparts.get(0)),r:evaluateExpression(paramparts.get(1)))
					case "assert":
						String errormessage=(paramparts.size()==2)?stripQuotation(paramparts.get(1)):null;
						return new Function(name:command,c:{ee -> 
							if (ee==null || ee.sct==null) return;
							ee.assertions.add new Assertion(evaluateExpression(paramparts.get(0)), errormessage!=null?errormessage:(ee.locale.equals("de")?"Ungültige Eingabe":"Invalid input"))})
					case "locale":
						return new Function(name:command,c:{ee -> ee.locale=param; info "locale="+ee.locale})
					case "disableInputHistory":
						return new Function(name:command,c:{ee -> ee.inputHistory=false})
					case "enableInputHistory":
						return new Function(name:command,c:{ee -> ee.inputHistory=true})
					
					case ["inputNumber","inputNumberNC","inputString","inputStringNC",
						"choice","choiceNC","choiceRandomize","choiceRandomizeNC","button"
						,"checkbox","slider"]:
						hasVarPar=true
					case ["display","video","style","button","manualLayout","automaticLayout","clear",
						"enableMouseTracking","disableMouseTracking",
						"eyetrackerInitialise","eyetrackerCalibrate","eyetrackerStart",
						"eyetrackerStop","eyetrackerTrigger","recordKeys"]:
						Vector<Function> functions=new Vector<Function>();
						paramparts.eachWithIndex { s,n ->
							//functions.add ((n==0 && hasVarPar)?new Function(name:"string",c:{s}):evaluateExpression(s)) }  
							functions.add ((n==0 && hasVarPar)?new Function(name:"varsearch",c:{it.resolveVarname(s)}):evaluateExpression(s)) }  
						
						if (command in ["choiceRandomize","choiceRandomizeNC"] )
						{
							Vector<Function> g=new Vector<Function>();
							g.add(functions.get(0));
							functions.remove(0);
							functions.sort {Math.random()}
							g.addAll(functions)
							command=command.replaceAll("Randomize", "")
							functions=g;
						}
//						Function f=evaluateExpression(paramparts[0])
						return new Function(linenum:linenum, name:command, c:{ee->
							if (ee==null || ee.sct==null) return;
							if (hasVarPar)
							{
								// Default-Wert bestimmen
								Function f=paramparts[0]?evaluateExpression(paramparts[0]):null
								String varname=ee.resolveVarname((String)f.execute(ee))
								Object varval=ee.varspaceGet(varname)
								if (varval)
									ee?.sct.updates.add(linenum+"@setdefault("+varname+",\""+Utils.getNiceString(varval, ee.locale).replaceAll("\n", "\\\\n")+"\")")
							}

							if (hasVarPar)
							{
								Function f=functions.get(0)
								String name=f.execute(ee)
							}

							if(name.equals("recordKeys"))
								ee.varspaceGet("_lastkey",0)

							String output=functions.collect{'\"'+Utils.getNiceString(it.execute(ee),ee.locale)+'\"'}.join(',')
							ee?.sct.updates.add(linenum+"@"+command+"("+output+")");
						})
						
					case "wait":
						Function[] f=[
							(paramparts.size()>=1)?evaluateExpression(paramparts.get(0)):new Function(name:"string",c:{ee->ee.locale.equals("de")?"Weiter...":"Continue..."}),
							(paramparts.size()>=2)?evaluateExpression(paramparts.get(1)):new Function(name:"string",c:{ee -> ee.locale.equals("de")?"Bitte warten":"Please wait"}),
							(paramparts.size()>=3)?evaluateExpression(paramparts.get(2)):new Function(name:"0",c:{0}),
							(paramparts.size()>=4)?evaluateExpression(paramparts.get(3)):new Function(name:"0",c:{0}),
							(paramparts.size()>=5)?evaluateExpression(paramparts.get(4)):new Function(name:"0",c:{0}),
							(paramparts.size()>=6)?evaluateExpression(paramparts.get(5)):new Function(name:"0",c:{0})]
		
						return new Function(linenum:linenum,name:command,c:{ ee ->
							if (ee.sct!=null)
							{
								ee.varspacePut("_continue"+(linenum), new Double(0));
								String sline=linenum+"@wait("+(linenum)+","+f[0].execute(ee)+","+f[1].execute(ee)+",1"+
									","+f[2].execute(ee)+","+f[3].execute(ee)+","+f[4].execute(ee)+","+f[5].execute(ee)+")";
								ee.sct.updates.add(sline);
		
								String temp=ee.checkAssertions();
								ee.sct.send(new ServerSideValidInfo(temp));
								ee.sct.update();
		
								while(((Double)ee.varspaceGet("_continue"+(linenum))).doubleValue()==0  && !ee.cancel)
								{
									try{Thread.sleep(200)}catch(Exception e){;}
								}
							}
							ee.assertions.clear();
						})
						
					case "waitTime":
						return new Function(linenum:linenum,name:command,c:{ ee ->
								String temp=ee.checkAssertions()
								ee.sct.send(new ServerSideValidInfo(temp))
								ee.sct.update()
								Double time=(Double)evaluateExpression(paramparts.get(0)).execute(ee)
							   try{Thread.sleep(time.longValue())}catch(Exception e){}
								ee.assertions.clear() // ?
								"ok"
						})
						
					case "waitForExperimenter":
						Function[] f=[evaluateExpression(param)]
						return new Function(linenum:linenum,name:command,c:{ ee ->
							if (ee.sct!=null)
							{
								String temp=ee.checkAssertions();
								ee.sct.send(new ServerSideValidInfo(temp));
		
								ee.sct.update();
								while(!ee.sct.experiment.experimenter.ready && !ee.cancel)
								{
									try{Thread.sleep(200)}catch(Exception e){}
								}
							}
							ee.assertions.clear(); // ?
							"ok"
						})
					case "waitForPlayers":
			        	Function[] f=[
			        			(paramparts.size()>=1)?evaluateExpression(paramparts.get(0)):new Function(name:"string",c:{ee->ee.locale.equals("de")?"Weiter...":"Continue..."}),  
			        			(paramparts.size()>=2)?evaluateExpression(paramparts.get(1)):new Function(name:"string",c:{ee->ee.locale.equals("de")?"Bitte warten":"Please wait"}),
			        				(paramparts.size()>=3)?evaluateExpression(paramparts.get(2)):new Function(name:"0",c:{0}),
			    	        		(paramparts.size()>=4)?evaluateExpression(paramparts.get(3)):new Function(name:"0",c:{0}),
			    	    	        (paramparts.size()>=5)?evaluateExpression(paramparts.get(4)):new Function(name:"0",c:{0}),
			    	    	    	(paramparts.size()>=6)?evaluateExpression(paramparts.get(5)):new Function(name:"0",c:{0}),
			        		]
		
		       			return new Function(linenum:linenum,name:command,c:{ p ->
								if (p==null || p.sct==null) return;
					            if (p.sct!=null)
					            {
					            	// Zielnummer ermitteln
									int waituntilcount=1;
									Object o=p.varspaceGet(p.group, p.role, "_continue"+linenum)
									if (o!=null && o instanceof Double) waituntilcount+=(Double)o;
			
				            		p.sct.updates.add(linenum+"@wait("+linenum+","+f[0].execute(p)+","+f[1].execute(p)+
										","+waituntilcount+
				            			","+f[2].execute(p)+","+f[3].execute(p)+","+f[4].execute(p)+","+f[5].execute(p)+")");
			             
					            	String temp=p.checkAssertions();
					            	p.sct.send(new ServerSideValidInfo(temp));
				            		p.sct.update();
				
					                boolean playernotfinished=true;
					                while(playernotfinished && !p.cancel)
					                {
				                    	playernotfinished=false;
				                    	try{Thread.sleep(200)} catch (InterruptedException e){;}
										for (String player:p.group.subjects.keySet())
				                    	{
				                    		if (p.varspaceGet(player, "_continue"+linenum)==null)
				                    			playernotfinished=true;
				                    		else if (((Double)p.varspaceGet(player, "_continue"+linenum)).doubleValue()<waituntilcount)
				                    			playernotfinished=true;
				                    	}
					                }
					            }
					            p.assertions.clear(); // ?
					            "ok";
							})
				}
        	}
        }

        throw new Exception("Command not recognized.");
    }


	

	public static Function evaluateExpression(String _exp) throws Exception
    {
      String rem=_exp.trim()
		
		while (rem.startsWith("(") && rem.endsWith(")"))
			rem=rem.substring(1,rem.size()-1)
			
        if (rem.isEmpty()) return null
					
		def patterns=[:], patternsnocomp=[:]
	
		patterns[/strlen\((.*)\)/]=["strlen", {ee,a -> a.length() }]
		patterns[/strucase\((.*)\)/]=["strucase", {ee,a -> a.toUpperCase() }]
		patterns[/strlcase\((.*)\)/]=["strlcase", {ee,a -> a.toLowerCase() }]
		
		patterns[/randomUniformInteger\((.*),(.*)\)/]=["randomUniformInteger", { ee,a,b ->  
			((int)a)+new Random().nextInt((int)Math.round(b-a+1)) }]
		patterns[/randomUniform\(\)/]=["randomUniform", { new Random().nextDouble() }]
		patterns[/randomGauss\(\)/]=["randomGauss", { new Random().nextGaussian() }]
		
		patterns[/exp\((.*)\)/]=["exp", {ee,a -> Math.exp(a) }]
		patterns[/abs\((.*)\)/]=["abs", {ee,a -> Math.abs(a) }]
		patterns[/sin\((.*)\)/]=["sin", {ee,a -> Math.sin(a) }]
		patterns[/cos\((.*)\)/]=["cos", {ee,a -> Math.cos(a) }]
		patterns[/tan\((.*)\)/]=["tan", {ee,a -> Math.tan(a) }]
		patterns[/log\((.*)\)/]=["log", {ee,a -> Math.log(a) }]
		patterns[/sqrt\((.*)\)/]=["sqrt", {ee,a -> a**0.5 }]
		patterns[/round\((.*)\)/]=["round", {ee,a -> (double)Math.round(a) }]
		patterns[/round1\((.*)\)/]=["round1", {ee,a -> (double)Math.round(a*10)/10 }]
		patterns[/round2\((.*)\)/]=["round2", {ee,a -> (double)Math.round(a*100)/100 }]
		
		patternsnocomp[/sum\((.*)\)/]=["sum", {ee,varname -> 
			ee.group.subjects.keySet().collect { player -> ee.varspaceGet(player, varname) }.sum()}]
		patternsnocomp[/mean\((.*)\)/]=["mean", {ee,varname ->
			ee.group.subjects.keySet().collect { player -> ee.varspaceGet(player, varname) }.mean()}]
		patternsnocomp[/median\((.*)\)/]=["median", {ee,varname ->
			ee.group.subjects.keySet().collect { player -> ee.varspaceGet(player, varname) }.median()}]
		patternsnocomp[/min\((.*)\)/]=["min", {ee,varname ->
			ee.group.subjects.keySet().collect { player -> ee.varspaceGet(player, varname) }.min()}]
		patternsnocomp[/max\((.*)\)/]=["max", {ee,varname ->
			ee.group.subjects.keySet().collect { player -> ee.varspaceGet(player, varname) }.max()}]
					
		
		patterns[/(.+)&&(.+)/]=["&&", {ee,a,b -> a&&b?1:0 }]
		patterns[/(.+)\|\|(.+)/]=["||", {ee,a,b -> a||b?1:0 }]
		
		patterns[/(.+)==(.+)/]=["==", {ee,a,b -> a==b?1:0 }]
		patterns[/(.+)!=(.+)/]=["!=", {ee,a,b -> a!=b?1:0 }]
		patterns[/(.+)>=(.+)/]=[">=", {ee,a,b -> a>=b?1:0 }]
		patterns[/(.+)<=(.+)/]=["<=", {ee,a,b -> (a<=b)?1:0 }]
		patterns[/(.+)>(.+)/] =[">",  {ee,a,b -> a>b?1:0  }]
		patterns[/(.+)<(.+)/] =["<",  {ee,a,b -> a<b?1:0  }]

		
		patterns[/(.+?)\+(.+)/]=patterns[/(.+)\+(.+)/]=["+", {ee,a,b ->	
				if (a instanceof Number && b instanceof Number) a+b
					else (Utils.getNiceString(a,ee.locale)+Utils.getNiceString(b,ee.locale)) }]

		patterns[/(.+)\-(.+)/]=["-",{ee,a,b ->	a-b }]
		patterns[/(.+)\*(.+)/]=["*",{ee,a,b ->	a*b }]
		patterns[/(.+)\/(.+)/]=["/",{ee,a,b ->	(Double)(a/b) }]
		patterns[/(.+)%(.+)/]=["%",	{ee,a,b ->	a%b }]
		patterns[/(.+)\^(.+)/]=["^", {ee,a,b ->	a**b }]

		patterns[/time/]=["time", {(Double)new Date().getTime() }]
		patterns[/timestring/]=["timestring", {new Date().toLocaleString(); }]
		patterns[/PI/]=["PI", {(Double)Math.PI }]

				
		
		// Operationen
		for (def y:patterns.entrySet())
		{
			def p=y.key, c=y.value
			if (rem ==~ p)
			{
				def x=(rem =~ p)[0]
				Function ret=new Function(name:c[0],c:c[1])
				
				boolean okay=true
				if (x instanceof ArrayList)
				{
					
					if (x.size()>=2)
					{
						if (hasBalancedBrackets(x[1])) 
							ret.l=evaluateExpression(x[1])
						else 
							okay=false
					}
					if (x.size()>=3)
					{
						if (hasBalancedBrackets(x[2]))
							ret.r=evaluateExpression(x[2])
						else
							okay=false
					}
				}

				if (okay)
				 	return ret
			}
		}
		
		
		// Operationen ohne Parameterkompilation
		for (def y:patternsnocomp.entrySet())
		{
			def p=y.key, c=y.value
			if (rem ==~ p)
			{
				def x=(rem =~ p)[0]
				Function ret=new Function(name:c[0],c:c[1])
				
				boolean okay=true
				if (x instanceof ArrayList)
				{
					
					if (x.size()>=2)
					{
						if (hasBalancedBrackets(x[1]))
							ret.l=new Function(name:x[1],c:{x[1]})
						else
							okay=false
					}
					if (x.size()>=3)
					{
						if (hasBalancedBrackets(x[2]))
							ret.r=new Function(name:x[2],c:{x[2]})
						else
							okay=false
					}
				}
				if (okay) return ret
			}
		}
		
		
		// Zahlen
		if (rem.length()>0 && (isNumberCharacter(rem.charAt(0))
				|| (rem.charAt(0)=='-')))
		{
			String num="";

			while (rem.length()>0 && (isNumberCharacter(rem.charAt(0))
				|| (rem.charAt(0)=='-'&&num.isEmpty())))
			{
				num+=rem.charAt(0);
				rem=rem.substring(1);
			}

			double d=Double.parseDouble(num)
			return new Function(name:"$d",c:{(Double)d})
		}

		
		// Strings
    	if (rem.startsWith("\""))
        {
        	int end=1;
        	while (true)
        	{
        		end=rem.indexOf("\"",end);
        		if (end==-1) throw new Exception("Syntax error: '"+rem+"' in '"+_exp+"'");
        		if (rem.charAt(end-1)=='\\')
        		{
        			rem=rem.substring(0,end-1)+rem.substring(end);
        		}
        		else
        		{
        			break;
        		}
        		end++;
        	}
			return new Function(name:"string",c:{rem.substring(1,end)})
        }
		

        if (isVariableCharacter(rem.charAt(0)))
        {	// Variable

        	String temp1=getVarnameFromMixedString(rem);
        	if (temp1==null) throw new Exception("Invalid varname "+rem);
        	StringTokenizer st=new StringTokenizer(temp1,".");
        	String[] temp=new String[st.countTokens()];
        	int i=0;

        	while(st.hasMoreElements())
        		temp[i++]=st.nextToken();

			def group=null,role=null,name=null
				
        	// Variablen suchen
        	switch(temp.length)
        	{
        		case 1: // vname
					name=temp[0]; break;	
        		case 2: // role.vname
					role=temp[0]; name=temp[1]; break;	
        		case 3: // group.role.vname
					group=temp[0]; role=temp[1]; name=temp[2]; break;	
        	}
			
			
			return new Function(name:"var",c:{ee->
				String vname=ee.resolveVarname(name);
				Object ret;
				String _role=role;

				if (_role!=null && _role.equals("opponent"))
				{
					_role=ee.opponentRole
				}

				
				if (vname.startsWith('$'))
					ret=ee.varspaceGet(vname);
				else if (_role!=null && group!=null)
					ret=ee.varspaceGet(ee.session.groups.get(group),_role,vname);
				else if (_role!=null)
					ret=ee.varspaceGet(_role, vname);
				else
					ret=ee.varspaceGet(vname);
				if (ret==null) ret=0d;
				//log "get var $group . $_role . $vname for ee $ee -> $ret"
				return ret;
				
				})
			
        }

		throw new Exception("Line could not be parsed: "+_exp);
    }
	

	static boolean isVariableCharacter(char c)
	{
		c in ('a'..'z') || c in ('A'..'Z') || c in ('0'..'9') || c=='_' || c=='.' || c=='$'
	}

	static boolean isNumberCharacter(char c)
	{
		return  c in ('0'..'9')  || c=='.';
	}

	static boolean hasBalancedBrackets (String s)
	{
		s.findAll(/\(/).size() == s.findAll(/\)/).size()  && s.findAll(/\[/).size() == s.findAll(/\]/).size() && s.findAll(/"/).size()%2==0
	}
	
	public static String getVarnameFromMixedString(String orig) throws Exception {

		String rem=orig;
		int finalpos=0;
		while (rem.length()>0)
		{
			if (isVariableCharacter(rem.charAt(0)))
			{
				rem=rem.substring(1);
				finalpos++;
			}
			else if (rem.charAt(0)=='[')
			{
				String temp=client.Utils.getMatchingLevelString(rem,false,false);
				rem=rem.substring(temp.length()+2);
				finalpos+=temp.length()+2;
			}
			else if (finalpos!=0)
				return orig.substring(0,finalpos);
			else
				return null;
		}
		return orig.substring(0,finalpos);
	}

	
}
