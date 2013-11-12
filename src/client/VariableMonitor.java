package client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.*;
import java.util.Timer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class VariableMonitor extends JPanel
{
	public static final Color LIGHTGRAY = new Color(235,235,235);

	private static final long serialVersionUID = -3736483082882072329L;

	Timer t=new Timer();

	JTable jt=new JTable();

	Map<String, Object> varspace;
	LinkedList<String> varnames,players;

	public VariableMonitor()
	{
		setLayout(new BorderLayout());
		add(new JScrollPane(jt),BorderLayout.CENTER);




		jt.setTableHeader(null);
		jt.setModel(new DefaultTableModel()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public int getColumnCount()
			{
				if (players==null) return 1;
				return players.size()+1;
			}

			@Override
			public int getRowCount()
			{
				if (varnames==null) return 1;
				return varnames.size();
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex)
			{
				try
				{
					if (players==null || varnames==null) return "?";
					if (columnIndex==0)
					{
						return varnames.get(rowIndex);
					}
	
					String _varname=players.get(columnIndex-1)+"."+varnames.get(rowIndex);
					if (varspace.containsKey(_varname)) {
						Object object = varspace.get(_varname);
						return object;
					}
				}
				catch(Exception e)
				{
					;
				}
				return "";
			}

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return String.class;
			}

		});


		jt.setCellSelectionEnabled(false);
	}

	final DefaultTableCellRenderer dtcr=new DefaultTableCellRenderer()
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			String string;

			if (value==null)
				string="";
			else if (value instanceof String && column>0)
				string="\""+value+"\"";
			else
				string = Utils.getNiceString(value);



			JLabel temp=new JLabel(string)
			{

				private static final long serialVersionUID = 1L;

				@Override
				protected void paintComponent(Graphics g) {
					((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					super.paintComponent(g);
				}
			};


			temp.setForeground(varnames.get(row).startsWith("_")?Color.gray:Color.black);



			temp.setHorizontalTextPosition(JLabel.CENTER);
			temp.setToolTipText(value.toString());
			temp.setHorizontalAlignment(JLabel.CENTER);

			temp.setBackground((row==0||column==0)?LIGHTGRAY:Color.white);
			temp.setOpaque(true);
			return temp;
		}
	};

/*
	public String getExportString()
	{
		String exportString="";

		// Table header
		for(int var=0; var<jt.getModel().getRowCount(); var++)
			exportString+=jt.getModel().getValueAt(var, 0)+"\t";
		exportString+="\n";

		// Table content
		for(int player=1; player<jt.getModel().getColumnCount(); player++)
		{
			for(int var=0; var<jt.getModel().getRowCount(); var++)
			{
				exportString+=jt.getModel().getValueAt(var, player)+"\t";
			}
			exportString+="\n";
		}
		return exportString;
	}

*/
	//HashMap<Double, Boolean> errorShown=new HashMap<Double,Boolean>();



	public void updateData(Map<String, Object> _varspace)
	{
		varspace=_varspace;
		varnames=new LinkedList<String>();
		LinkedList<String> sysvarnames=new LinkedList<String>();
		players=new LinkedList<String>();

		for (String s:varspace.keySet())
		{
			if (s.startsWith("$")) continue;
			String _player = s.substring(0,s.lastIndexOf('.'));
			if (_player.equals("Global")) continue;
			if (!players.contains(_player))
				players.add(_player);

			String _varname= s.substring(s.lastIndexOf('.')+1,s.length());
			if (!varnames.contains(_varname) && !sysvarnames.contains(_varname) && !_varname.equals("group")
					&& !_varname.equals("role")&& !_varname.equals("username"))
			{
				if (_varname.startsWith("_"))
					sysvarnames.add(_varname);
				else
					varnames.add(_varname);
			}
		}


		// Fehler suchen
		/*for (String p:players)
		{
			if (varspace.get(p+"._errormsg")==null) continue;
			String errormsg=(String)varspace.get(p+"._errormsg");
			String errorline=(String)varspace.get(p+"._errorline");
			Double errorlinenum=(Double)varspace.get(p+"._errorlinenum");

			if (errorShown.get(errorlinenum)==null)
			{
				String message="<html><font size=\"4\" face=\"Arial\">Line "+(int)errorlinenum.doubleValue()+": "+errorline+"<br>"+errormsg;
				JOptionPane.showMessageDialog(null, message, "Runtime error", JOptionPane.ERROR_MESSAGE);
				errorShown.put(errorlinenum, true);
			}
		}*/


		Collections.sort(players);
		Collections.sort(varnames);
		varnames.addFirst("role");
		varnames.addFirst("group");
		varnames.addFirst("username");
		varnames.removeAll(sysvarnames);
		varnames.addAll(sysvarnames);


		((DefaultTableModel)jt.getModel()).fireTableStructureChanged();
		jt.setDefaultRenderer(String.class, dtcr);
	}


}
