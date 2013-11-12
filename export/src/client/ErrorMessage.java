package client;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;

public class ErrorMessage extends Exception implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	static transient JFrame frame;
	static transient JList errorlist; 
	static transient Vector<ErrorMessage> errors=new Vector<ErrorMessage>();

	public ErrorMessage(int _line, String _errorline, String _errormessage, Date _time)
	{
		errormsg=_errormessage;
		errorline=_errorline;
		errorlinenum=_line;
		time=_time;
	}

	Date time;
	String errormsg;
	String errorline;
	int errorlinenum;
	
	public void add()
	{
		errors.add(this);
	}

	public static void show()
	{
		if (frame==null)
		{
			frame=new JFrame("BoXS Error messages");
			frame.setSize(800, 600);
			errorlist=new JList();
			frame.add(new JScrollPane(errorlist));
		}
		
		DefaultListModel lm=new DefaultListModel();
		for (ErrorMessage e:errors)
		{
			lm.addElement(e);
		}
		
		errorlist.setModel(lm);
		frame.setVisible(true);

	}
	
	@Override
	public String toString() {
		return "<html><font size=\"4\" face=\"Arial\">"+new SimpleDateFormat("dd.MM.yy hh:mm:ss").format(time)+
				": Line "+errorlinenum+" {"+errorline+"}<br><b>"+errormsg+"</b>";
		
	}

}
