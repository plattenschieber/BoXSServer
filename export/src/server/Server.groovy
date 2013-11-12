package server;

import java.io.*;
import java.lang.reflect.Array
import java.net.ServerSocket;
import java.util.Date;
import static util.Log.*;


public class Server {
	static int count=0, port=58000
	
	public static void main(String[] args)
	{
		MirkoGroovyLibrary.execute()
		MirkoGroovyLibrary.scheduleAtFixedRate 0, 1000*60*2, { log ServerClientThread.getInfoString() }

		if (args.length>=1)
		{
			info args[0]
			if (args[0].indexOf("enableMail")!=-1)
			{
				SmtpSend.active=true
				info "Mail account active"
			}
			
			if (args.length>=2)
			{
				final String keepaliveaddress=args[1];
				info keepaliveaddress
				Runtime.getRuntime().addShutdownHook{
					SmtpSend.sendMail(keepaliveaddress,"I am dead :( :( :(!","... not alive, not alive...\n"+
						ServerClientThread.getInfoString(), null);	}
				
				MirkoGroovyLibrary.scheduleAtFixedRate 1000, 1000*60*60*24, { 
					SmtpSend.sendMail(keepaliveaddress,"I am alive! Connections: "+ServerClientThread.globalcount,
						"... still alive, still alive...\n"+ServerClientThread.getInfoString(), null) }
			}

			if (args.length>=3)
				port=Integer.parseInt(args[2]);

			if (args.length>=4)
				ServerClientThread.DEFAULT_MINIMUM_UPDATEDELAY=Integer.parseInt(args[3]);
		}
		
		info "port="+port
		
		Thread.start
		{
			ServerSocket ss=new ServerSocket(port)
			while (true) ss.accept (true, { 
						count++
						okay(""+new java.util.Date()+" Connection accepted from "+it.getRemoteSocketAddress());
						def oos=new ObjectOutputStream(new BufferedOutputStream(it.getOutputStream()))
						oos.flush()
						def ois=new ObjectInputStream(new BufferedInputStream(it.getInputStream()))
						
						if (oos!=null && ois!=null)
						new ServerClientThread(oos,ois, it.getRemoteSocketAddress()).run()
						
				} )
		}
	}

}
