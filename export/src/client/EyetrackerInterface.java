package client;

import java.awt.*;
import java.io.*;
import java.net.*;
import static util.Log.*;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JPanel;

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
    private Timer timer;
    private String filename;

	public void initialise(String _host, int _portsend, int _portreceive)
	{
		if (initialised)
			return;
		initialised = true;

		host = _host;
		portreceive = _portreceive;
		portsend = _portsend;
                timer = new Timer();
		info("ET-initialise: " + host + ", " + portsend + ", "
				+ portreceive);

		try
		{
			ourSocket = new DatagramSocket(new InetSocketAddress(host,
					portreceive));
		} catch (SocketException e)
		{
			e.printStackTrace();
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
		new CalibrateThread(ca).start();
	}

	public void start(int frequency, String _filename) throws IOException
	{
		if (started)
			return;
		started = true;
		filename = System.getProperty("user.home") + "/" + _filename;
		info("start @" + frequency + " to " + filename);

		// Open File
		new File(filename).createNewFile();
		fos = new FileOutputStream(filename);
		bos = new BufferedOutputStream(fos);

		// Start streaming
		send("ET_FRM \"%TU %DX %DY %SX %SY\"");
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
							//info(s);
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
		send("ET_REC");
		info("start done");

	}

	public void stop()
	{
		info("stopping eyetracker and saving data");
		relaythread.interrupt();
		try
		{
			send("ET_EST");
			send("ET_STP");			
			send("ET_SAV \"C:\\eye_Data\\BoXS\\" + filename + ".idf\" \"description\" \"username\" \"OVR\"");
			bos.close();
			fos.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		info("stopping done, data saved successfully");
	}

    public void trigger(final String s)
    {
        int triggerRate;
        try {
            triggerRate = Integer.parseInt(s);
        } catch (NumberFormatException e)
        {
            triggerRate = 0;
            info ("Could not parse triggerRate. Setting triggerRate to 0.");
        }
        if (triggerRate == 0) {
            sendTrigger(0);
        }
        else //if (triggerRate > 0) 
        {
            timer.scheduleAtFixedRate(new TimerTask() {
                int triggerCount = 0;
                @Override
                public void run() {
                    sendTrigger(triggerCount);
                    info ("Sending of " + triggerCount + " trigger done.");

                    // cancel trigger sending after 200 times
                    if (triggerCount++ == 200)
                    {
                        this.cancel();
                        info("trigger done");
                    }
                }
            }, 0, triggerRate);
        }

    }
    
    private void sendTrigger(int triggerCount) 
    {
        try {
            send("ET_AUX \"" + triggerCount + ". Trigger from BoXS\"");
            send("ET_INC");
            //info("ONE Trigger send");
            bos.write(("Trigger: " + System.nanoTime() + " " + triggerCount + "\n").getBytes("ASCII"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
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

