package client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.JButton;

public class ContinueButtonListener implements ActionListener
{
	JButton jb;
	ClientConnection cc;
	String name, pressedtext;
	Date starttime;
	SubjectPanel ca;
	int targetvalue;

	public ContinueButtonListener(SubjectPanel _ca, JButton _jb, ClientConnection _cc, String _name, int _targetvalue, String _pressedtext, Date _startTime)
	{
		ca=_ca;
		jb=_jb;
		cc=_cc;
		name=_name;
		targetvalue = _targetvalue;
		starttime=_startTime;
		pressedtext=_pressedtext;
	}

	@Override
	public void actionPerformed(ActionEvent arg0)
	{
		ca.waitButtonPressed=true;
		ca.checkFulfilled();
		final Object[] o={"_continue"+name,new Double(targetvalue),new Long(new Date().getTime() - starttime.getTime())};
		// Button nicht mehr freigeben...
		ca.setEnabled(false);
		cc.send(ServerCommand.SUBMIT_VALUE, o);
		jb.setText(pressedtext);
		if (jb.getWidth() < jb.getPreferredSize().width)
			jb.setSize(jb.getPreferredSize());
//		jb.setEnabled(false);
	}

}
