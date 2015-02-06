package client;

import static util.Log.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.SwingUtilities;

public class ClientConnection extends ConnChannel {

	ClientApplet ca;
	
	ClientConnection(ObjectOutputStream _out, ObjectInputStream _in, ClientApplet _ca) throws IOException
	{
		super(_out, _in);
		ca=_ca;
	}



	public void loginToServer(final String username, final String realm, final String password, final boolean exact) throws Exception
	{
		String[] s=new String[4];
		s[0]=username;
		s[1]=realm;
		s[2]=password;
		s[3]=exact?"exact":"assign";
		send(ServerCommand.LOGIN,s);
	}

	public void send(final ServerCommand dt, final Object content)
	{
		Object[] temp=new Object[2];
		temp[0]=dt; temp[1]=content;
		send(temp);
	}
	
	boolean updatingstuff=false;

	@Override
	public void receive(final Object o) 
	{
		if (ca.experimenter)
		{
			if (o instanceof ErrorMessage)
			{
				((ErrorMessage)o).add();
				ErrorMessage.show();
			}
			else
			{
				if (!updatingstuff)
				{
					updatingstuff=true;
					SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
							((ExperimenterPanel)ca.mainPanel).updateData(o);
							updatingstuff=false;
						}
					});
				}
				else
					warning("Discarding data package");
			}
		}
		else
		{

			if (o instanceof String)
			{
				info("received from server: "+o);
				((SubjectPanel)ca.mainPanel).executeUpdate((String)o);
			}
			else if (o instanceof ServerSideValidInfo)
				((SubjectPanel)ca.mainPanel).setServerSideValid(((ServerSideValidInfo)o));

		}
	}

	public void close()
	{
		super.close();
		ca.updateMessage("Client disconnected. (Usually due to conflicting username or incorrect password.)",false);
		
		try {
			sleep(2000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		if ( !ca.hasbeenconnected)
			ca.connect(true);
	}
}
