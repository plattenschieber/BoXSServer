package client;
import java.awt.BorderLayout;

import javax.swing.JFrame;



public class ClientFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	public ClientFrame(String username)
	{
		setSize(600,400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		ClientApplet ca=new ClientApplet();
		add(ca,BorderLayout.CENTER);
		ca.realm="test";
		ca.username=username;
		ca.init();
		ca.start();
		setVisible(true);
		setExtendedState(JFrame.MAXIMIZED_BOTH);

	}

	public static void main(String args[])
	{
		new ClientFrame(args[0]);
	}
}
