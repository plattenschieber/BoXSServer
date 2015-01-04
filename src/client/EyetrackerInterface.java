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
	private boolean initialised;
	private boolean started;
    long startTime;
    int triggerCount = 0;
	private TrackerType trackerType; 
	private LctigazeDll lctigaze;
	private _stEgControl pstEgControl;
	
    // do some basic initialisation on construction
    public EyetrackerInterface()
    {
        this.trackerType = TrackerType.UNKNOWN;
    }

	public enum TrackerType {
	    SMI, EYEGAZE, UNKNOWN
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
        switch(this.trackerType)
		{
            case SMI:
                host = _host;
                portreceive = _portreceive;
                portsend = _portsend;
                info("initialise SMI: " + host + ", " + portsend + ", " + portreceive);

                try
                {
                    ourSocket = new DatagramSocket(new InetSocketAddress(host,portreceive));
                } catch (SocketException e)
                {
                    e.printStackTrace();
                }
                break;

            case EYEGAZE:
                // Set Eyegaze control settings 
                // TODO files should be in same folder?! otherwise documentation is needed! 
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
                info("initialise Eyegaze");
                break;

            // we can't initialise a nonexisting eyetracker
            case UNKNOWN:
            default: 
                info("Please choose an Eyetracker via 'EyetrackerInitialise'");
                initialised = false; 
                return;
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
			// Start streaming
	
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
			try
			{
				send("ET_EST");
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

	public void destroy()
	{
		ourSocket.close();
	}
}
