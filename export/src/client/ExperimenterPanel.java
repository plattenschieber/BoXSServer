package client;

import static util.Log.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.URI;
import java.util.Collections;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.StyledEditorKit;

public class ExperimenterPanel extends JPanel
{
	// FIXME
	private static final String HELP_ADRESS = "boxs.uni-bonn.de/documentation";
	private static final ProgDocument PROG_DOCUMENT = new ProgDocument();
	private static final Font EDITOR_FONT = new Font("Courier New",Font.PLAIN,14);
	private static final long serialVersionUID = 991868469330470152L;
	private JButton btnStart;
	JToggleButton btnReady, btnAutorun;
	JButton btnNew,btnSave,btnSaveAs,btnLoad,btnHelp,btnUndo,btnRedo,btnZoomIn,btnZoomOut,btnSS;
	JLabel lblFilename=new JLabel();

	private JButton btnExport, btnClose, btnSend=new JButton("send"),btnASend=new JButton("autosend");
	private JToggleButton btnDetails=new JToggleButton("more");


	JEditorPane editorArea;

	JScrollPane jsp=new JScrollPane();

	JPanel leftPanel=new JPanel(), rightPanel=new JPanel();

	JSplitPane jsplitpane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

	JList jSubjectList=new JList();

	JPanel centerStuff=new JPanel();



	private JButton makeIconButton(String tooltip, String iconfile)
	{
		JButton temp=new JButton(loadImage(iconfile));
		temp.setToolTipText(tooltip);
		return temp;
	}

	ClientApplet clientApplet;
	
	String filename;

	public ExperimenterPanel(final ClientApplet _clientApplet)
	{
		clientApplet=_clientApplet;
		
		btnStart=makeIconButton("Start","start.png");
		btnReady=new JToggleButton("Ready");
		btnAutorun=new JToggleButton("Autorun");
		btnNew=makeIconButton("New Program","new.png");
		btnSave=makeIconButton("Save Program","save.png");
		btnSaveAs=makeIconButton("Save Program as...","saveas.png");
		btnLoad=makeIconButton("Load Program","open.png");
		btnHelp=makeIconButton("Open online help","help.png");
		btnUndo=makeIconButton("Undo last action","undo.png");
		btnRedo=makeIconButton("Redo last action","redo.png");
		btnZoomIn=makeIconButton("Zoom in","zoomin.png");
		btnZoomOut=makeIconButton("Zoom out","zoomout.png");
		btnSS=new JButton("ss");
		btnExport=makeIconButton("Export experiment data to CSV","export.png");
		btnClose=makeIconButton("Close","cancel.png");

		
		btnReady.setIcon(loadImage("ready.png"));
		btnReady.setText(null);
		btnReady.setToolTipText("Experimenter ready (important only for waitForExperimenter()-command)");

		btnAutorun.setIcon(loadImage("autorun.png"));
		btnAutorun.setText(null);
		btnAutorun.setToolTipText("Autorun (Launch experiment automatically when clients connect)");

		setLayout(new BorderLayout());
		add(jsplitpane,BorderLayout.CENTER);



		btnStart.setEnabled(false);
		btnClose.setEnabled(false);
		btnExport.setEnabled(false);
		btnReady.setEnabled(false);

		jsplitpane.add(leftPanel);
		jsplitpane.add(rightPanel);


		// Links
		leftPanel.setLayout(new BorderLayout());

		JToolBar buttonPanel=new JToolBar();
		leftPanel.add(buttonPanel,BorderLayout.NORTH);
		buttonPanel.setFloatable(false);



		btnNew.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				editorArea.setText("");
			}
		});
		buttonPanel.add(btnNew);

		lblFilename.setOpaque(false);
		btnLoad.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser jfc=new JFileChooser();
				jfc.setFileFilter(new FileNameExtensionFilter("BoXS Program","boxs","BOXS"));
//				jfc.setFileFilter(new FileNameExtensionFilter("Text file","txt","TXT"));
				try
				{if (filename!=null)
					jfc.setSelectedFile(new File(filename));
				} catch(Exception ex)
				{
					ex.printStackTrace();
				}
				if (jfc.showOpenDialog(clientApplet)==JFileChooser.APPROVE_OPTION)
				{
					try
					{
						filename=jfc.getSelectedFile().getAbsolutePath();
						lblFilename.setText(filename.indexOf("/")!=-1?filename.substring(filename.lastIndexOf("/")+1):filename);
						btnSave.setEnabled(true);
						BufferedReader br=new BufferedReader(new FileReader(jfc.getSelectedFile()));

						String f=null;

						while(true)
						{
							String l=br.readLine();
							if (l==null) break;
							if (f!=null) f+="\n"+l;
							else f=l;
						}
						br.close();

						editorArea.setText(f);
						updateLineNumbers();
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
					}
				}



			}
		});
		buttonPanel.add(btnLoad);


		btnSave.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				if (filename!=null)
				{
					try
					{
						FileWriter fw=new FileWriter(filename);
						fw.write(editorArea.getText());
						fw.close();
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
					}
				}



			}
		});
		buttonPanel.add(btnSave);
		btnSave.setEnabled(false);


		btnSaveAs.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser jfc=new JFileChooser();
				
				try
				{if (filename!=null)
					jfc.setSelectedFile(new File(filename));
				} catch(Exception ex)
				{
					ex.printStackTrace();
				}
				
				jfc.setFileFilter(new FileNameExtensionFilter("BoXS Program","boxs","BOXS"));

				if (jfc.showSaveDialog(clientApplet)==JFileChooser.APPROVE_OPTION)
				{
					try
					{
						String fname=jfc.getSelectedFile().getAbsolutePath();
						if (!fname.endsWith(".boxs"))
							fname=fname+".boxs";
						filename=fname;
						lblFilename.setText(filename.indexOf("/")!=-1?filename.substring(filename.lastIndexOf("/")+1):filename);
						btnSave.setEnabled(true);
						
						FileWriter fw=new FileWriter(filename);
						fw.write(editorArea.getText());
						fw.close();
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
					}
				}



			}
		});
		buttonPanel.add(btnSaveAs);

		buttonPanel.add(lblFilename);


		buttonPanel.add(new JSeparator());

		buttonPanel.add(btnZoomIn);
		btnZoomIn.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e)
			{
				PROG_DOCUMENT.size+=2;
				editorArea.setFont(new Font("Courier New",Font.PLAIN,PROG_DOCUMENT.size));
				PROG_DOCUMENT.updateStyles();
				PROG_DOCUMENT.processChangedLines(0, 0);
				updateLineNumbers();
			}
		});

		buttonPanel.add(btnZoomOut);
		btnZoomOut.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e)
			{
				PROG_DOCUMENT.size-=2;
				editorArea.setFont(new Font("Courier New",Font.PLAIN,PROG_DOCUMENT.size));
				PROG_DOCUMENT.updateStyles();
				PROG_DOCUMENT.processChangedLines(0, 0);
				updateLineNumbers();
			}
		});



		buttonPanel.add(btnUndo);
		btnUndo.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e)
			{
				synchronized (PROG_DOCUMENT.undo)
				{
					if (PROG_DOCUMENT.undo.canUndo())
						PROG_DOCUMENT.undo.undo();
				}
			}
		});

		buttonPanel.add(btnRedo);
		btnRedo.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e)
			{
				synchronized (PROG_DOCUMENT.undo)
				{
					if (PROG_DOCUMENT.undo.canRedo())
						PROG_DOCUMENT.undo.redo();
				}
			}
		});

		buttonPanel.add(btnHelp);
		btnHelp.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e)
			{
				showHelp();
			}
		});





		centerStuff.setLayout(new BoxLayout(centerStuff,BoxLayout.X_AXIS));

		//editorArea.setPreferredSize(new Dimension(500,200));


		lineNumbers = new JPanel()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);
				((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);



				String[] split = editorArea.getText().split("\n");
				double totallines=split.length;
				if (editorArea.getText().endsWith("\n"))
					totallines++;
				double totalheight=((double)editorArea.getPreferredSize().height-4);


				double y=-(double)(editorArea.getFont().getSize())*0.25d+3;
				g.setFont(editorArea.getFont());
				for (int i=0; i<totallines; i++)
				{
					y+=totalheight/totallines;
					g.drawString(""+(i+1), g.getFont().getSize()/3,(int)y-2);
				}
			}

		};
		lineNumbers.setFont(EDITOR_FONT);
		lineNumbers.setBackground(new Color(230,230,230));
		lineNumbers.setForeground(new Color(150,150,150));
		lineNumbers.setOpaque(true);
		updateLineNumbers();



		centerStuff.add(lineNumbers);

		editorArea=new JEditorPane()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g)
			{
		        Graphics2D g2 = (Graphics2D) g;
		        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
		          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
		          RenderingHints.VALUE_RENDER_QUALITY);

		        super.paintComponent(g2);
			}
		};

		centerStuff.add(editorArea);


		if (clientApplet.getParameter("screenshot")!=null)
		{
			btnSS.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					Utils.makeComponentShot(centerStuff,centerStuff.getPreferredSize().width,editorArea.getMinimumSize().height);
				}
			});
			buttonPanel.add(btnSS);
		}




		editorArea.setFont(EDITOR_FONT);

		editorArea.setEditorKitForContentType("text/java", new StyledEditorKit());
		editorArea.setContentType("text/java");
		editorArea.setDocument(PROG_DOCUMENT);
		editorArea.enableInputMethods(true);


		editorArea.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e)
			{
				lineNumbers.repaint();
			}
		});
		leftPanel.add(jsp, BorderLayout.CENTER);
		jsp.setViewportView(centerStuff);



		// rechts
		rightPanel.setLayout(new BorderLayout());

		JPanel temp=new JPanel();
		temp.setLayout(new BorderLayout());

		jSubjectList.setBackground(VariableMonitor.LIGHTGRAY);
		jSubjectList.setOpaque(true);
		jSubjectList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		JScrollPane slsp = new JScrollPane(jSubjectList,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		slsp.setBorder(new TitledBorder("Available Subjects"));
		slsp.setPreferredSize(new Dimension(50,80));
		temp.add(slsp,BorderLayout.CENTER);

		jSubjectList.setCellRenderer(new DefaultListCellRenderer()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {

				try
				{
				final SubjectInfo si=(SubjectInfo)value;
				JLabel lbl=new JLabel(si.username);
				lbl.setPreferredSize(new Dimension(40,20));
				lbl.setOpaque(true);
				Color c=new Color(220,230,255);
				if (si.inExperiment) c=new Color(220,255,220);
				if (si.isSuspended) c=new Color(255,220,220);
				lbl.setBackground(c);
				lbl.setHorizontalAlignment(JLabel.CENTER);
				lbl.setToolTipText("CTRL-ALT-Click to disconnect client");
				return lbl;
				}
				catch(Exception e)
				{
					return new JLabel("WTF");
				}
			}
		});
		
		jSubjectList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				if (e.isControlDown() && e.isAltDown())
				{
			     int index = jSubjectList.locationToIndex(e.getPoint());
			     ListModel dlm = jSubjectList.getModel();
			     try
			     {
			    	 Object item = dlm.getElementAt(index);
			    	 final SubjectInfo si=(SubjectInfo)item;
			    	 jSubjectList.ensureIndexIsVisible(index);
						clientApplet.cc.send(ServerCommand.KILL_SUBJECT, si);
			     }
			     catch(Exception ex)
			     {
			    	 ex.printStackTrace();
			     }
				}
			     
			}
		});
		
		
		
		rightPanel.add(temp,BorderLayout.SOUTH);


		rightPanel.setMinimumSize(new Dimension(0,0));
		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				super.componentResized(e);
				jsplitpane.setDividerLocation(0.75d);

			}
		});





		// Execution Toolbar
		JToolBar bottomExecutionToolbar=new JToolBar();
		JToolBar executionToolbar=new JToolBar();
		bottomExecutionToolbar.setFloatable(false);
		executionToolbar.setFloatable(false);
		temp.add(bottomExecutionToolbar, BorderLayout.NORTH);



		bottomExecutionToolbar.add(btnStart);
		btnStart.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e)
			{
				clientApplet.cc.send(ServerCommand.START_EXPERIMENT, editorArea.getText());
			}
		});


		executionToolbar.add(btnReady);
		btnReady.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e)
			{
				clientApplet.cc.send(ServerCommand.SET_READY, btnReady.isSelected()?"yes":"no");
			}
		});

		
		bottomExecutionToolbar.add(btnAutorun);
		btnAutorun.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e)
			{
				clientApplet.cc.send(ServerCommand.START_EXPERIMENT_AUTORUN, editorArea.getText());
			}
		});



		bottomExecutionToolbar.add(btnClose);
		btnClose.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (sp!=null)
					clientApplet.cc.send(ServerCommand.CANCEL_EXPERIMENT, new Integer(sp.esd.num));
			}
		});


		executionToolbar.add(btnExport);
		btnExport.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e)
			{
				final JFileChooser fc = new JFileChooser();
				if  (fc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
				{
					String filename=fc.getSelectedFile().getAbsolutePath();
					if (!filename.toLowerCase().endsWith(".csv"))
						filename=filename+".csv";
					String exportString = FileExporter.getExportString(sp.vm.varspace);

					try
					{
						FileWriter fw=new FileWriter(filename);
						fw.write(exportString);
						fw.close();
					}
					catch (IOException e1)
					{
						e1.printStackTrace();
					}
				}

			}
		});



		executionToolbar.add(btnSend);
		btnSend.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e)
			{
				clientApplet.cc.send(ServerCommand.SEND_EXPERIMENT, new Integer(1));
			}
		});

		executionToolbar.add(btnDetails);
		btnDetails.setToolTipText("Display more variables (may slow down connection and display, use with care!)");
		btnDetails.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e)
			{
				clientApplet.cc.send(ServerCommand.SET_DETAILS, new Integer(btnDetails.isSelected()?1:0));
			}
		});


		rightPanel.add(executionToolbar, BorderLayout.NORTH);

		sp=new SessionPanel(null,clientApplet.cc);
		rightPanel.add(sp,BorderLayout.CENTER);
	}


	private void updateLineNumbers() {
		int width=PROG_DOCUMENT.size*3;
		lineNumbers.setMinimumSize(new Dimension(width,20));
		lineNumbers.setPreferredSize(new Dimension(width,20));
		lineNumbers.setMaximumSize(new Dimension(width,Integer.MAX_VALUE));
		lineNumbers.setFont(EDITOR_FONT);
		lineNumbers.repaint();
	}


	//JPanel sessionPanelPlace=new JPanel();
	SessionPanel sp=null;


	public String[][] serverComments;
	private JPanel lineNumbers;

	boolean hasdata=false;

	@SuppressWarnings("unchecked")
	public void updateData(Object o)
	{
		ExperimentSessionData esd=null;
		if (((Object[])o)[0]!=null)
		{
			esd=(ExperimentSessionData)((Object[])o)[0];
		}
		Vector<SubjectInfo> subs=(Vector<SubjectInfo>)((Object[])o)[1];
		Collections.sort(subs);

		if (esd!=null && esd.running) hasdata=true;
		//btnClose.setEnabled(esd!=null && esd.running);
		btnClose.setEnabled(true);
		btnExport.setEnabled(hasdata);
		btnReady.setEnabled(esd!=null && esd.running);
		btnStart.setEnabled((esd==null || (esd!=null && !esd.running))
				&& !subs.isEmpty());


		sp.updateData(esd);

		boolean autorun=((Boolean)((Object[])o)[2]);
		btnAutorun.setSelected(autorun);

		jSubjectList.setListData(subs);
		int maxrows=(rightPanel.getSize().width-30)/40;
		if (maxrows<=0) maxrows=1;
		jSubjectList.setVisibleRowCount(subs.size()/maxrows+1);

		Vector<SubjectInfo> available=new Vector<SubjectInfo>();
		for (SubjectInfo si:subs)
		{
			if (!si.inExperiment)
				available.add(si);
		}
	}


	void showHelp() {
		try {
			Desktop.getDesktop().browse(new URI(HELP_ADRESS));
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}



	public ImageIcon loadImage(String name) {
		String path = name;
		int MAX_IMAGE_SIZE = 2400;  //Change this to the size of
									 //your biggest image, in bytes.
		int count = 0;
		BufferedInputStream imgStream = new BufferedInputStream(
		   this.getClass().getResourceAsStream(path));
		byte buf[] = new byte[MAX_IMAGE_SIZE];
		try {
			count = imgStream.read(buf);
			imgStream.close();
		} catch (java.io.IOException ioe) {
			error("Couldn't read stream from file: " + path);
			return null;
		}
		if (count <= 0) {
			error("Empty file: " + path);
			return null;
		}
		return new ImageIcon(Toolkit.getDefaultToolkit().createImage(buf));
	}

}
