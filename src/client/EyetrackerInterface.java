package client;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import imd.eyetracking.lib.Lctigaze.LctigazeDll;
import imd.eyetracking.lib._stEgControl;
import java.util.TimerTask;
import java.util.Timer;

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
    private Timer timer;
	
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
                send("ET_REC");
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
            case TET:
                // Send the obligatory connect request message
                String REQ_CONNECT = "{\"values\":{\"push\":true,\"version\":1},\"category\":\"tracker\",\"request\":\"set\"}"; 
                send(REQ_CONNECT);

                // launch a seperate thread to parse incoming data
                incomingThread = new Thread(ListenerLoop);
                incomingThread.Start();

                // start a timer that sends a heartbeat every 250ms.
                // The minimum interval required by the server can be read out 
                // in the response to the initial connect request.   
                String REQ_HEATBEAT = "{\"category\":\"heartbeat\",\"request\":null}";
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() { send(REQ_HEATBEAT); } }, 0, 250);
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
                    send("ET_STP");			
                    send("ET_SAV \"" + filename + ".idf\" \"description\" \"username\" \"OVR\""); 
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
                    send("ET_INC");
                    send("ET_REM \"" + s + "\"");
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

	private void send(String s)
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

    private void ListenerLoop()
    {
        boolean isRunning = true;
        StreamReader reader = new StreamReader(socket.GetStream());

        while (isRunning)
        {
            String response = "";

            try
            {
                response = reader.ReadLine();

                JObject jObject = JObject.Parse(response);

                Packet p = new Packet();
                p.RawData = json;

                p.Category = (String)jObject["category"];
                p.Request = (String)jObject["request"];
                p.StatusCode = (String)jObject["statuscode"];

                JToken values = jObject.GetValue("values");

                if (values != null)
                {
                    /* 
                       We can further parse the Key-Value pairs from the values here.
                       For example using a switch on the Category and/or Request 
                       to create Gaze Data or CalibrationResult objects and pass these 
                       via separate events.
                       */
                }

                // Raise event with the data
                if(OnData != null)
                    OnData(this, new ReceivedDataEventArgs(p));
            }
            catch (Exception ex)
            {
                info("Error while reading response: " + ex.Message);
            }
        }
    }
}
