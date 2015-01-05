package client;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
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
	private TrackerType trackerType; 
	private LctigazeDll lctigaze;
	private _stEgControl pstEgControl;
    private CharsetEncoder encoder;
    private String filename;
	
    // do some basic initialisation on construction
    public EyetrackerInterface()
    {
        this.trackerType = TrackerType.UNKNOWN;
    }

	public enum TrackerType {
	    SMI, EYEGAZE, TET, UNKNOWN
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

        // SMI and TET need extra communication handling
        host = _host;
        portreceive = _portreceive;
        portsend = _portsend;
		
		// different initialisations
        switch(this.trackerType)
		{
            case SMI:
            case TET:
                info("initialise SMI/TET: " + host + ", " + portsend + ", " + portreceive);

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


	public void start(int frequency, String _filename) 
	{
		if (started)
			return;
		started = true;
		
		filename = System.getProperty("user.home") + "/BoXS_EyeData/" + _filename + "_" + System.currentTimeMillis();

        switch(this.trackerType)
		{
            case SMI:
                info("start @" + frequency + " to " + filename);
                sendSMI("ET_REC");
                break;
		    case EYEGAZE:
                // setup parameters
                String mode = "w";
                // add 5 chars: 1 for '\0' and the rest for '.log' 
                ByteBuffer f = ByteBuffer.allocate(filename.length() + 1 + 4);
                ByteBuffer m = ByteBuffer.allocate(mode.length() + 1);
                try {
                    f = encoder.encode(CharBuffer.wrap(filename + ".log"));
                    m = encoder.encode(CharBuffer.wrap(mode));
                }
                catch (IOException e){
                    e.printStackTrace();
                }
                // transfer parameters to eyetracker API
                lctigaze.EgLogFileOpen(pstEgControl, f, m);
                lctigaze.EgLogWriteColumnHeader(pstEgControl);
                lctigaze.EgLogStart(pstEgControl);
                break;
            default:
                break;
		}
		
		info("start done");
	}

	public void stop()
	{
		info("stop eyetracker");
		
        switch(this.trackerType)
		{
            case SMI:
                    sendSMI("ET_STP");			
                    sendSMI("ET_SAV \"" + filename + ".idf\" \"description\" \"username\" \"OVR\""); 
                break;
		    case EYEGAZE:
                lctigaze.EgLogStop(pstEgControl);
                lctigaze.EgLogFileClose(pstEgControl);
                pstEgControl.bTrackingActive = 0;
                lctigaze.EgExit(pstEgControl);
                break;
            default:
                break;
		}
		
		info("stop done");
	}

	public void trigger(String s)
	{
		info("trigger " + s);
		
        switch(this.trackerType)
		{
            case SMI:
                    sendSMI("ET_INC");
                    sendSMI("ET_REM \"" + s + "\"");
                break;
            case EYEGAZE:
                try {
                    lctigaze.EgLogAppendText(pstEgControl, encoder.encode(CharBuffer.wrap(s)));
                } 
                catch (IOException e){ 
                    e.printStackTrace();
                }
                break;
            default:
                break;
		}

		info("trigger done");
	}

	public void sendSMI(String s)
	{
        try {
            byte[] buf = (s + "\n").getBytes("ASCII");
            ourSocket.send(new DatagramPacket(buf, buf.length,
				new InetSocketAddress(host, portsend)));
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
		info("Sent: " + s);
	}

	public void destroy()
	{
		// only allocated ressources need to be freed
        switch(this.trackerType)
		{
            case SMI:
                ourSocket.close();
                break;
            case EYEGAZE:
                lctigaze.EgExit(pstEgControl.byReference());
                break;
            default:
                break;
        }
	}
}
