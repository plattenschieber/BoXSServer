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
	private boolean calibrated;
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
		else // this.trackerType = EYEGAZE
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
		
		info("initialise done");
	}

	class CalibrateThread extends Thread
	{
		private final ClientApplet ca;
		private int[][] calibrationpoints;
		private int currentcalibrationpoint;

		public CalibrateThread(ClientApplet ca) {
			this.ca = ca;
		}

		@Override
		public void run()
		{
			super.run();
			ca.remove(ca.mainScrollPanel);

			calibrationpoints = new int[100][2];
			currentcalibrationpoint = 0;
			JPanel pnl = null;

			try
			{
				send("ET_CPA 2 0");
				clear();
				send("ET_CAL 5");
				String r;

				while ((r = receive()) != null)
				{
					String[] parameters = r.split("[\t ]");
					if (parameters[0].equals("ET_PNT"))
					{
						int num = Integer.parseInt(parameters[1]);
						calibrationpoints[num][0] = Integer
								.parseInt(parameters[2]);
						calibrationpoints[num][1] = Integer
								.parseInt(parameters[3]);
					}
					if (parameters[0].equals("ET_CHG"))
					{
						currentcalibrationpoint = Integer
								.parseInt(parameters[1]);
					}
				}

				// Panel erzeugen
				pnl = new JPanel() {
					@Override
					protected void paintComponent(Graphics g)
					{
						super.paintComponent(g);
						int x = calibrationpoints[currentcalibrationpoint][0], y = calibrationpoints[currentcalibrationpoint][1];
						info("show point "
										+ currentcalibrationpoint + " @ " + x
										+ "," + y);

						g.setColor(Color.red);
						((Graphics2D) g).setStroke(new BasicStroke(3));
						g.drawOval(x - 10, y - 10, 20, 20);
					}
				};
				ca.add(pnl, BorderLayout.CENTER);
				ca.validate();
				ca.invalidate();
				ca.repaint();

				boolean finished = false;

				main: while (!finished)
				{
					try
					{
						Thread.sleep(1500);
					} catch (InterruptedException e)
					{
						e.printStackTrace();
					}

					send("ET_ACC");

					try
					{
						Thread.sleep(1500);
					} catch (InterruptedException e)
					{
						e.printStackTrace();
					}

					String s;
					while ((s = receive()) != null)
					{
						String[] parameters = s.split("[\t ]");

						if (parameters[0].equals("ET_CHG"))
						{
							currentcalibrationpoint = Integer
									.parseInt(parameters[1]);
							pnl.repaint();
							continue main;
						} else if (parameters[0].equals("ET_FIN"))
						{
							info("fertig");
							finished = true;
						}
					}
				}
			}

			catch (IOException e)
			{
				e.printStackTrace();
			} finally
			{

				if (pnl != null)
					ca.remove(pnl);
				ca.add(ca.mainScrollPanel);
				ca.validate();
				ca.invalidate();
				ca.repaint();

				info("calibrate done");

			}

		}
	}

	public void calibrate(ClientApplet ca)
	{
		if (calibrated)
			return;
		calibrated = true;

		info("calibrate");
		
		if (this.trackerType == TrackerType.EYEGAZE)
			lctigaze.EgCalibrate2(pstEgControl, 1);
		else
			new CalibrateThread(ca).start();
		
		info("calibrate done");
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
