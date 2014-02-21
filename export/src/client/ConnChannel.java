package client;

import static util.Log.*;
import java.io.*;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class ConnChannel extends Thread{
	private static final long PINGPONGINTERVAL = 30*1000;
	boolean sending=false;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	
	final Timer pingpongtimer=new Timer();
	
	public ConnChannel(ObjectOutputStream _out, ObjectInputStream _in) throws IOException
	{
		out=_out;
		in=_in;
	}
	
	public void close()
	{
		
	//	new Exception("Connection closed").printStackTrace();
		info("Connection closed");

		try
		{
			pingpongtimer.cancel();
		}
		catch (Exception e) {
			;
		}
		
		
		try
		{
			in.close();
		}
		catch (Exception e) {
			;
		}
		try
		{
			out.flush();
			out.close();
		}
		catch (Exception e) {
			;
		}
		
		out=null;
		in=null;
	}
	
	Date lastPong=new Date();
	
	
	
	@Override
	public void run() {
		try
		{
			pingpongtimer.schedule(new TimerTask() {
				public void run()
				{
					send(new SyncObject());
					long timesincepong=new Date().getTime() - lastPong.getTime();
					if (timesincepong>2*PINGPONGINTERVAL)
					{
						warning("No Pong received, closing! timesincepong=[" + timesincepong + "]");
						close();
					}
				}
			}, PINGPONGINTERVAL,PINGPONGINTERVAL);
			
			while(true)
			{
				try{
					Object o=in.readObject();

					if (o instanceof SyncObject)
						lastPong=new Date();
					else
						receive(o);
				}
				catch(EOFException e)
				{
					close();
					return;
				}
				catch(SocketException e)
				{
					close();
					return;
				}
			}

		}
		catch (Exception e)
		{
			error(e);
		}
		close();
	}
	
	public abstract void receive(Object o);
	
	final ConcurrentLinkedQueue<Object> sendQueue=new ConcurrentLinkedQueue<Object>();
	
	public void send(Object o)
	{
			new SendThread(o).start();
	}
	
	
	class SendThread extends Thread
	{ 
		Object o;
		SendThread(Object _o) {o=_o;}
		@Override
		public void run() {
			super.run();

			sendQueue.add(o);
			doSend();
		}
	}
	
	private synchronized void doSend()
	{
		if (!sending) 
		{
			sending=true;
			while (!sendQueue.isEmpty())
			{
				try{
				out.writeObject(sendQueue.poll());
				out.flush();
				}
				catch(Exception e)
				{
					;
				}
			}
			sending=false;
		}
	}
	
	
	
}
