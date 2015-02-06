package client;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import imd.eyetracking.lib.Lctigaze.LctigazeDll;
import imd.eyetracking.lib._stEgControl;

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
	    SMI, EYEGAZE, UNKNOWN
	}

	public void setTrackerType(String type)
	{
		if (type.equals("SMI"))
			this.trackerType = TrackerType.SMI;
		else if (type.equals("EYEGAZE"))
			this.trackerType = TrackerType.EYEGAZE;
		else 
			info(type + " is not a valid option for eyetrackerSetType");
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
                // initialise encoder
                encoder = Charset.forName("UTF-8").newEncoder();
                info("initialised Eyegaze");
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


	@SuppressWarnings("deprecation")
	public void start(int frequency, String _filename) 
	{
		if (started)
			return;
		started = true;
		
		filename = System.getProperty("user.home") + "/BoXS_EyeData/";
		new File(filename).mkdirs();
		filename.concat(_filename + "_" + System.currentTimeMillis());

        switch(this.trackerType)
		{
            case SMI:
                sendSMI("ET_REC");
                info("starting SMI eye tracker @" + frequency + " with logfile " + filename);
                break;
		    case EYEGAZE:
                // transfer parameters to eyetracker API
                lctigaze.EgLogFileOpen(pstEgControl.byReference(), filename.concat(".log"), "w");
                lctigaze.EgLogWriteColumnHeader(pstEgControl.byReference());
                lctigaze.EgLogStart(pstEgControl.byReference());
                info("starting EYEGAZE eye tracker with logfile " + filename);
                break;
            default:
                break;
		}
        info("eyetracker started"); 
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
                    lctigaze.EgLogMark(pstEgControl.byReference());
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
