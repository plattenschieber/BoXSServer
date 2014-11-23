package client;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import imd.eyetracking.lib.Lctigaze.LctigazeDll;
import imd.eyetracking.lib._stEgControl;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

import static util.Log.info;

public class EyetrackerInterface
{
	private DatagramSocket ourSocket;
	private String host;
	private int portreceive, portsend;
	private FileOutputStream fos;
	private BufferedOutputStream bos;
	private Thread relaythread;
	private boolean initialised;
	private boolean started;
    long startTime;
    int triggerCount = 0;
	private TrackerType trackerType = TrackerType.SMI; // default tracker type, if not set explicitly
	private LctigazeDll lctigaze;
	private _stEgControl pstEgControl;
	
	public enum TrackerType {
	    SMI, EYEGAZE
	}

	public void setTrackerType(TrackerType type)
	{
		this.trackerType = type;
	}
	
	public TrackerType getTrackerType()
	{
		return this.trackerType;
	}

	public void initialise(String _host, int _portsend, int _portreceive)
	{
		if (initialised)
			return;
		initialised = true;
		
		// different initialisations
		if (this.trackerType == TrackerType.SMI)
		{
			host = _host;
			portreceive = _portreceive;
			portsend = _portsend;
			info("ET-initialise: " + host + ", " + portsend + ", " + portreceive);

			try
			{
				ourSocket = new DatagramSocket(new InetSocketAddress(host,portreceive));
			} catch (SocketException e)
			{
				e.printStackTrace();
			}
		}
		else if (this.trackerType == TrackerType.EYEGAZE)
        {		
            // Set Eyegaze control settings 
            System.setProperty("jna.library.path", "C:\\Eyegaze\\");
            lctigaze = LctigazeDll.INSTANCE;
            pstEgControl = new _stEgControl();
            //fill in the control structure values
            pstEgControl.iNDataSetsInRingBuffer=5000;
            pstEgControl.bTrackingActive=1;
            pstEgControl.bEgCameraDisplayActive=0;		
            pstEgControl.iScreenHeightPix=Toolkit.getDefaultToolkit().getScreenSize().height;
            pstEgControl.iScreenWidthPix=Toolkit.getDefaultToolkit().getScreenSize().width;
            pstEgControl.iEyeImagesScreenPos=1;
            pstEgControl.iCommType=LctigazeDll.EG_COMM_TYPE_LOCAL;
            lctigaze.EgInit(pstEgControl.byReference());
        }
        else // we can't initialise a nonexisting eyetracker 
        {
            }
		
		info("initialise done");
	}


	public void start(int frequency, String _filename) throws IOException
	{
		if (started)
			return;
		started = true;
		
		String filename = System.getProperty("user.home") + "/" + _filename;
		info("start @" + frequency + " to " + filename);

		if (this.trackerType == TrackerType.SMI)
		{		
			// Open File
			new File(filename).createNewFile();
			fos = new FileOutputStream(filename);
			bos = new BufferedOutputStream(fos);
	
			// Start streaming
			send("ET_FRM \"%DX %DY %SX %SY\"");
			send("ET_STR 120");
	
			relaythread = new Thread() {
				public void run()
				{
					String s;
					try
					{
						while (!isInterrupted())
						{
							s = receive();
							if (s != null)
							{
								String[] parameters = s.split("[\t ]");
								info(s);
								if (parameters[0].equals("ET_SPL"))
								{
									try
									{
										bos.write((s + "\n").getBytes("ASCII"));
									} catch (UnsupportedEncodingException e)
									{
										e.printStackTrace();
									} catch (IOException e)
									{
										e.printStackTrace();
									}
								}
							}
						}
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				};
			};
			relaythread.start();
		}
		else if (this.trackerType == TrackerType.EYEGAZE)
		{
			filename = System.getProperty("user.home").toString() + "/testJeronim.log";
			String mode = "w";
			String triggerData = "Trigger XX";
			Pointer m = new Memory(mode.length() + 1);
			Pointer f = new Memory(filename.length() +1 );
			Pointer t = new Memory(triggerData.length()+ 1);
			m.setString(0, mode);
			f.setString(0, filename);
			lctigaze.EgLogFileOpen(pstEgControl, f,m);
			lctigaze.EgLogWriteColumnHeader(pstEgControl);
			lctigaze.EgLogStart(pstEgControl);
		}
		
		info("start done");

	}

	public void stop()
	{
		info("stop");
		
		if (this.trackerType == TrackerType.SMI)
		{
			relaythread.interrupt();
			try
			{
				send("ET_EST");
				bos.close();
				fos.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		else if (this.trackerType == TrackerType.EYEGAZE)
		{
            lctigaze.EgLogStop(pstEgControl);
            lctigaze.EgLogFileClose(pstEgControl);
            pstEgControl.bTrackingActive = 0;
            lctigaze.EgExit(pstEgControl);
		}
		
		info("stop done");
	}

	public void trigger(String s)
	{
		info("trigger " + s);
		
		if (this.trackerType == TrackerType.EYEGAZE)
		{
			
			
		}
		else if (this.trackerType == TrackerType.SMI)
		{
			try
			{
				bos.write((s + "\n").getBytes("ASCII"));
			} catch (UnsupportedEncodingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		info("trigger done");
	}

	public synchronized void send(String s) throws IOException
	{
		byte[] buf = (s + "\n").getBytes("ASCII");
		ourSocket.send(new DatagramPacket(buf, buf.length,
				new InetSocketAddress(host, portsend)));
		info("Sent: " + s);
	}

	public synchronized void clear() throws IOException
	{
		while (receive() != null)
			;
	}

	public synchronized String receive() throws IOException
	{
		byte[] buf = new byte[1024];
		DatagramPacket dp = new DatagramPacket(buf, buf.length);
		ourSocket.setSoTimeout(5);
		try{
			ourSocket.receive(dp);
			String data = new String(buf, 0, dp.getLength()).trim();
			//info("Received: " + data);
			return data;
		}catch(SocketTimeoutException ste)
		{
			return null;
		}
	}

	public void destroy()
	{
		try
		{
			if (bos != null)
				bos.close();
		} catch (IOException e)
		{
			;
		}
		try
		{
			if (fos != null)
				fos.close();
		} catch (IOException e)
		{
			;
		}
		ourSocket.close();
		if (relaythread != null)
			relaythread.interrupt();
	}
}
