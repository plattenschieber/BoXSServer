package client;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.text.*;
import java.util.*;

import javax.swing.JFileChooser;

public class Utils {

	public static String getNiceString(Object evaluateExpression) {
		return getNiceString(evaluateExpression,"en");
	}

	public static String getNiceString(Object evaluateExpression, String locale) {
		if (evaluateExpression instanceof String) return (String)evaluateExpression;
		else
		{
			double d=0;
			if (evaluateExpression instanceof Double) d=(Double)evaluateExpression;
			if (evaluateExpression instanceof Integer) d=(Integer)evaluateExpression;
			if (d==Math.round(d))
			{
				return Long.toString(Math.round(d));
			}
			else
			{
				NumberFormat df = NumberFormat.getInstance(new Locale(locale));
				df.setMaximumIntegerDigits(50);
				df.setGroupingUsed(false);
				df.setMinimumFractionDigits(2);
				df.setMaximumFractionDigits(8);
				return df.format((Double)d);
			}
		}
	}


	public static void makeComponentShot(Component c)
	{
		makeComponentShot(c,c.getSize().width,c.getSize().height);

	}


	public static void makeComponentShot(Component c, int width,
			int height) {
		JFileChooser jfc=new JFileChooser();
		jfc.showSaveDialog(c);

		BufferedImage bi=new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		Graphics g=bi.createGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, width,height);
		c.paint(g);
		g.dispose();

		try
		{
			javax.imageio.ImageIO.write(bi, "PNG", jfc.getSelectedFile());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}	// TODO Auto-generated method stub

	}
	
	public static Vector<String>  splitByComma(String param) 
	{

		Vector<String> paramparts=new Vector<String>();
        try
		{
        String tempparam=param;
        while (!tempparam.isEmpty())
        {
        	String nextpart=getMatchingLevelString(tempparam, true,false);
        	paramparts.add(nextpart);
        	tempparam=tempparam.substring(nextpart.length()).trim();
        	if (!tempparam.isEmpty() && tempparam.charAt(0)==',')
            	tempparam=tempparam.substring(1);
        }
		}
		catch(Exception e)
		{
			e.printStackTrace();
			;
		}
        return paramparts;

	}
	
	
	public static String getMatchingLevelString(String s, boolean untilcomma, boolean untilequals) throws Exception
	{
		String brackets="";
		for (int i=0; i<s.length(); i++)
		{
			char lastchar=0;
			if (brackets.length()>0)
				lastchar=brackets.charAt(brackets.length()-1);

			switch(s.charAt(i))
			{
			case ' ':
				continue;
			case '\"':
				if (lastchar=='\"')
					brackets=brackets.substring(0,brackets.length()-1);
				else	
					brackets+='\"';
				break;
			case '(':
				if (lastchar!='\"')
					brackets+='(';
				break;
			case '[':
				if (lastchar!='\"')
					brackets+='[';
				break;
			case '{':
				if (lastchar!='\"')
					brackets+='{';
				break;

			case ')':
				if (lastchar!='\"')
				{
					if (lastchar!='(')
						throw new Exception("Brackets do not balance.");
					else
						brackets=brackets.substring(0,brackets.length()-1);
				}
				break;
			case ']':
				if (lastchar!='\"')
				{
					if (lastchar!='[')
						throw new Exception("Brackets do not balance.");
					else
						brackets=brackets.substring(0,brackets.length()-1);
				}
				break;
			case '}':
				if (lastchar!='\"')
				{
					if (lastchar!='{')
						throw new Exception("Brackets do not balance.");
					else
						brackets=brackets.substring(0,brackets.length()-1);
				}
				break;
				
			case ',':
				if (untilcomma)
				{
					if (brackets.isEmpty())
						return s.substring(0,i);
				}
				break;

			case '=':
				if (untilequals)
				{
					if (brackets.isEmpty())
					{
						return s.substring(0,i);
					}
				}
			}

			if (!untilcomma && !untilequals && brackets.isEmpty() ) 
				return s.substring(1,i);
		}

		if (untilcomma) 
			return s;
		else
			throw new Exception("Brackets do not balance.");
	}


	public static double toDouble(String string) {
		if (string.charAt(0)=='"')
			string=string.substring(1, string.length()-1);
		return Double.parseDouble(string);
	}

}
