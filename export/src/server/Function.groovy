package server;

import groovy.lang.Closure;

public class Function {
	int linenum=-1
	String name
	Closure c
	Closure stringrep=null
	public Function l,r

	Object execute(ExecutionEnvironment ee)
	{
		if (linenum!=-1)
			ee.varspacePut( "_linenum", new Double(linenum));
		
		if (l==null)
			c.call(ee)
		else if (r==null)
			c.call(ee,l.execute(ee))
		else
			c.call(ee,l.execute(ee),r.execute(ee))
	}
	
	public String toString() {
		if (stringrep!=null)
			return stringrep.call()
		else
		{
			Vector<Function> f=new Vector<Function>()
			if (l!=null) f.add(l)
			if (r!=null) f.add(r)
			return name+(l?("("+f.join(",")+")"):"")
		}
	}
}
