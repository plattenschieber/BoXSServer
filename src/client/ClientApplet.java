package client;
import java.applet.Applet;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

public class ClientApplet extends Applet {
	private static final long serialVersionUID = 1L;
	public JPanel mainPanel=new JPanel();
	public JScrollPane mainScrollPanel;
	public ClientConnection cc;
	public boolean experimenter, showStartScreen=true, stopped=false;
	Date lastResize=new Date();
	boolean hasbeenconnected=false;

	JLabel msgLabel=new JLabel("Starting");
	String host="localhost", username, realm, password; 
	int port=58000;

	
	Timer fadetimer=null;
	
	void updateMessage(String message, boolean fade)
	{
		if (fadetimer!=null)
			fadetimer.cancel();
		
		msgLabel.setText(message);
		msgLabel.setVisible(!message.isEmpty());
		validate();
		
		if (fade)
		{
			fadetimer=new Timer();
			fadetimer.schedule(new TimerTask() {
				@Override
				public void run()
				{
					updateMessage("", false);
				}
			}, 2000);
		}
	}

	
	@Override
	public void start()
	{
		super.start();
		try
		{
			//UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); //TODO: Remove (?)
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e1)
		{
			;
		}
		setLayout(new BorderLayout());
		add(msgLabel, BorderLayout.SOUTH);
		
		try{
			if (getParameter("host")!=null) host=getParameter("host");
			if (getParameter("port")!=null) port=Integer.parseInt(getParameter("port"));
			if (getParameter("username")!=null) username=getParameter("username");
			if (getParameter("realm")!=null) realm=getParameter("realm");
			if (getParameter("showstartscreen")!=null) showStartScreen=Boolean.parseBoolean(getParameter("showstartscreen"));
		} catch(Exception e)
		{
			;
		}


		if (username==null)
		{
			String text=JOptionPane.showInputDialog(null, "Please enter your user name", username);
			username=(text!=null)?text:"";
		}
		if (realm==null)
		{
			String text=JOptionPane.showInputDialog(null, "Please enter the realm", realm);
			realm=(text!=null)?text:"";
		}

		experimenter=username.startsWith("exp");

		if (experimenter)
		{
			String text=JOptionPane.showInputDialog(null, "Please enter your password", password);
			password=(text!=null)?text:"";
		}

		if (experimenter)
			setExperimenter();
		else
			setSubject();
		
		
		updateMessage("Connecting to server...",false);
		
		connect(false);
	}


	void setSubject()
	{
		mainPanel=new SubjectPanel(this);
		mainScrollPanel=new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(mainScrollPanel,BorderLayout.CENTER);
	}


	public void setExperimenter()
	{
		mainPanel=new ExperimenterPanel(this);
		add(mainPanel,BorderLayout.CENTER);
		
	}
	
	
	void connect(final boolean reconnect)
	{
		if (stopped) return;
		final ClientApplet thisca=this;

		// connect
		new Thread()
		{
		public void run() {
			while (true)
			{
				// Connect
				try
				{
					Socket sa=new Socket(host, port);
					ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(sa.getInputStream()));
					ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(sa.getOutputStream()));
					oos.flush();
					cc=new ClientConnection(oos, ois,thisca);
					cc.start();
					updateMessage("Login",false);

					cc.loginToServer(username, realm, password, reconnect);
					if (!experimenter)
						((SubjectPanel)mainPanel).executeUpdate("");
					updateMessage("Connected to server!",true);
					hasbeenconnected=true;
					return;
				}
				catch(Exception e)
				{
					e.printStackTrace();
					updateMessage("Error logging in to server "+host+":"+port+" "+e.getLocalizedMessage(),false);
				}
			}
		}
		}.start();
	}

	
	
	

	@Override
	public void stop()
	{
		stopped=true;
		if (mainPanel instanceof SubjectPanel && ((SubjectPanel)mainPanel).eyetrackerInterface!=null)
			((SubjectPanel)mainPanel).eyetrackerInterface.destroy();
			
		super.stop();
		if (cc!=null)
			cc.close();
	}
}
