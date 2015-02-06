package client;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.ConcurrentLinkedQueue;
import static util.Log.*;

import javax.media.*;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.*;
import javax.swing.text.html.parser.ParserDelegator;


public class SubjectPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final String HTML_FOOTER = "</body></html>";

	String locale="en";
	EyetrackerInterface eyetrackerInterface;
	Thread mousetrackingthread=null;
	long mousetrackingstarttime;

	String errormessage=null;

	Font FONT;
	Date lastUpdateTime=null;
	HashMap<Component, Rectangle> manualLayoutCache=new HashMap<Component, Rectangle>();
	HashMap<JScrollPane,JTextArea> jsp_to_jta=new HashMap<JScrollPane, JTextArea>();
	HashMap<JButton,String> waitButtonsOkayText=new HashMap<JButton, String>();

	boolean serverSideValid=false, automaticlayout=true, updating=false, 
			internalresize=false,isUpdating=false,layoutspecified=false,waitButtonPressed; 

	ConcurrentLinkedQueue<String> updates=new ConcurrentLinkedQueue<String>();
	HashMap<String, String> defaults=new HashMap<String, String>();
	String customstyle=null;
	HashMap<Integer, Component> componentCache=new HashMap<Integer, Component>();
	Component firstcomp=null;
	LinkedList<DocumentListener> oldDocumentListeners=new LinkedList<DocumentListener>();
	LinkedList<JButton> waitButtons=new LinkedList<JButton>();
	Vector<Object> inputFulfilled=new Vector<Object>();
	Timer t=null;
	final ClientApplet clientApplet;

	
	void initFontsize() {
		int fontsize=Math.min(14, getWidth()/35);
		if (fontsize<8) fontsize=8;
		FONT = new Font("Arial",Font.PLAIN,fontsize);
	}

	
	public SubjectPanel(final ClientApplet clientApplet) {
		this.clientApplet = clientApplet;
		initFontsize();
		setBackground(Color.white);
		setOpaque(true);
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		 this.requestFocusInWindow();


		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				super.componentResized(e);
				initFontsize();
/*				
					if (!isUpdating)
					{
						isUpdating=true;
						if (t!=null)
						{
							t.cancel();
							t=null;
						}
						initFontsize();
						if (!internalresize) 
							{
							//executeUpdate(null);
							}

						t=new Timer();
						t.schedule(new TimerTask()
						{
							@Override
							public void run()
							{
								SwingUtilities.invokeLater(new Runnable()
								{
									public void run() {
										isUpdating=false;
									};
								});
							}

						}, 500);
					}*/
			}

		});
	}



	void executeUpdate(final String _update) {
		
		synchronized (updates)
		{
			
			if (_update!=null)
				updates.add(_update);
	
			if (updates.isEmpty()) return;
			
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					
					@Override
					public void run() {
						updating=true;
						try{
						if (!updates.isEmpty()) 
							__executeSubjectUpdate(updates.poll());
						}catch(Exception e)
						{
							e.printStackTrace();
						}
						updating=false;
					}
				});
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		/*
		if (!updating)
		{
			updating=true;
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					if (!updates.isEmpty()) 
						__executeSubjectUpdate(updates.poll());
					updating=false;
				}
			});
		}
		else
		{
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					executeUpdate(null);
				}
			}, 500);
		}*/
	}

	void __executeSubjectUpdate(String _update)
	{
		boolean recordkeys=false;
		 info("update: "+_update);
		layoutspecified=false;
		defaults.clear();
		String update=_update;
		
		boolean changed=false, majorchange=false;
		waitButtonPressed=false;
		
		if (update.equals("") || update.startsWith("finish"))
		{
			if (eyetrackerInterface!=null)
				eyetrackerInterface.destroy();
			customstyle=null;
			if (clientApplet.showStartScreen)
				update="-1@display(\"<br><h1>Bonn Experiment System</h1>Framework for conducting laboratory and field experiments<br>in economic and social science<br><br><br><br><table width=100%><tr><td>Dr. Mirko Seithe<br>Bonn Graduate School of Economics<br>University of Bonn</td><td> <img src='http://boxs.uni-bonn.de/bonn.png'></td></tr></table>\")";
			else
				update="-1@display(\"<br>Please wait...\")";
			componentCache.clear();
		}


		JButton btnSS=new JButton("Screenshot");
		final Component x=this;
		if (clientApplet.getParameter("screenshot")!=null)
		{
			btnSS.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					Utils.makeComponentShot(x);
				}
			});
			add(btnSS, BorderLayout.SOUTH);
		}

		firstcomp=null;
		

		Vector<Component> desiredComponents=new Vector<Component>();
		inputFulfilled.clear();

		waitButtons.clear();
		int linenum=0;
		String[] updatelines=update.split("\n");
		for (String _line:updatelines)
		{
			linenum=Integer.parseInt(_line.substring(0,_line.indexOf("@")));
			String line=_line.substring(_line.indexOf("@")+1);
			try
			{
				String command=line.substring(0,line.indexOf("(")).trim();
				String param=line.substring(line.indexOf("(")+1, line.lastIndexOf(")")).trim();
				Vector<String> paramparts=client.Utils.splitByComma(param);
				lastUpdateTime=new Date();

				if (command.equals("clear"))
				{
					desiredComponents.removeAllElements();
					manualLayoutCache.clear();
					removeAll();
					componentCache.clear();
				}
				
				if (command.equals("display"))
				{
					if (!layoutspecified) setLayoutAutomatic(true);
					String content=paramparts.get(0);
					if (content.startsWith("\""))
						content=content.substring(content.indexOf("\"")+1, content.lastIndexOf("\""));


					JLabel jl;
					if (componentCache.containsKey(linenum) && componentCache.get(linenum) instanceof JLabel
							&& !desiredComponents.contains(componentCache.get(linenum)))
					{
						jl=(JLabel)componentCache.get(linenum);
						int temp=jl.getPreferredSize().height;
						jl.setText("<html><head>"+getStyle()+"</head><body>"+content+HTML_FOOTER);
						jl.validate();
						jl.doLayout();
						if (temp!=jl.getPreferredSize().height)
							changed=true;
					}
					else
					{
						jl=new JLabel("<html><head>"+getStyle()+"</head><body>"+content+HTML_FOOTER);
						makeBorder(jl);
						jl.setFont(FONT);
						componentCache.put(linenum, jl);
					}
					
					if(!automaticlayout&&paramparts.size()>=5)
					{
						manualLayoutCache.put(jl,new Rectangle(
								(int)Utils.toDouble(paramparts.get(1)), 
								(int)Utils.toDouble(paramparts.get(2)), 
								(int)Utils.toDouble(paramparts.get(3)), 
								(int)Utils.toDouble(paramparts.get(4))));
					}

					desiredComponents.add(jl);
				}


				else if (command.equals("video"))
				{
					String content=param.substring(1,param.length()-1);
					URL url=new URL(clientApplet.getDocumentBase(),content);
					String extname=url.toExternalForm();
					MediaLocator mloc=new MediaLocator(extname);

					try {
						Player p=Manager.createRealizedPlayer(mloc);
						Component video=p.getVisualComponent();
						desiredComponents.add(video);
						p.start();
					} catch (Exception e2) {
						info("there is a problem with the video player");
					}
				}



				else if (command.equals("wait"))
				{
					if (!layoutspecified) setLayoutAutomatic(true);
					String content=paramparts.get(1), pressedcontent=paramparts.get(2);
					if (content.startsWith("\""))
						content=param.substring(param.indexOf("\"")+1, param.lastIndexOf("\""));
					if (pressedcontent.startsWith("\""))
						pressedcontent=param.substring(param.indexOf("\"")+1, param.lastIndexOf("\""));

					JButton jb;
					String text="<html>"+getStyle()+"<body>"+content+HTML_FOOTER;

					if (componentCache.containsKey(linenum) && componentCache.get(linenum) instanceof JButton)
					{
						jb=(JButton)componentCache.get(linenum);
						jb.setText(text);
						for (ActionListener al:jb.getActionListeners())
							jb.removeActionListener(al);
					}
					else
					{
						jb=new JButton(text);
						jb.setFont(FONT);
						jb.setMaximumSize(new Dimension(FONT.getSize()*20,FONT.getSize()+8));
						jb.setAlignmentX(Component.LEFT_ALIGNMENT);
						componentCache.put(linenum, jb);
					}
					
					jb.addActionListener(new ContinueButtonListener(this,jb,clientApplet.cc,paramparts.get(0).trim(),
							(int)Utils.toDouble(paramparts.get(3)),pressedcontent.trim(), lastUpdateTime));

					enableFocusTraversal(jb);
						
					if(!automaticlayout&&paramparts.size()>=8)
					{
						manualLayoutCache.put(jb,new Rectangle(
								(int)Utils.toDouble(paramparts.get(4)), 
								(int)Utils.toDouble(paramparts.get(5)), 
								(int)Utils.toDouble(paramparts.get(6)), 
								(int)Utils.toDouble(paramparts.get(7))));
					}

					
					desiredComponents.add(jb);
					waitButtonsOkayText.put(jb,text);
					waitButtons.add(jb);
				}

				else if (command.equals("inputNumber") || command.equals("inputString") || command.equals("inputNumberNC") || command.equals("inputStringNC"))
				{
					boolean number=command.equals("inputNumber") || command.equals("inputNumberNC") ;
					if (!layoutspecified) setLayoutAutomatic(true);
					JPanel jp=null;
					JScrollPane jsp;

					if (automaticlayout)
					{
						if (componentCache.containsKey(linenum) && componentCache.get(linenum) instanceof JPanel
								&& ((JPanel)componentCache.get(linenum)).getComponent(0) instanceof JScrollPane
								&& !desiredComponents.contains(componentCache.get(linenum)))
						{
							jp=(JPanel)componentCache.get(linenum);
							jsp=(JScrollPane)jp.getComponent(0);
						}
						else
						{
							jp=new JPanel(new BorderLayout());
							JTextArea jtf=new JTextArea();
							jtf.setFont(FONT);
							jp.setAlignmentX(Component.LEFT_ALIGNMENT);
							jtf.setAlignmentX(Component.LEFT_ALIGNMENT);
						//	jtf.setPreferredSize(new Dimension(FONT.getSize()*20,(int)(FONT.getSize()*2)));
							jtf.setLineWrap(true);
							jp.setMaximumSize(new Dimension(Math.min(600, getWidth()-100),(int)(FONT.getSize()*2)));
							jp.setBackground(Color.white);
							jp.setOpaque(true);
							jsp=new JScrollPane(jtf, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
							jsp.setMaximumSize(new Dimension(Math.min(600, getWidth()-100),(int)(FONT.getSize()*2)));
							jsp.setPreferredSize(new Dimension(Math.min(600, getWidth()-100),(int)(FONT.getSize()*2)));
							jsp_to_jta.put(jsp, jtf);
							//jsp.setBorder(new LineBorder(Color.black,1));
							jp.add(jsp, BorderLayout.WEST);
							jtf.setFocusTraversalKeysEnabled(true);
							enableFocusTraversal(jtf); 
							if (number) disableenter(jtf); 
							 
						
							componentCache.put(linenum, jp);
						}
					}
					else
					{
						if (componentCache.containsKey(linenum) && componentCache.get(linenum) instanceof JScrollPane
								&& !desiredComponents.contains(componentCache.get(linenum)))
						{
							jsp=(JScrollPane)componentCache.get(linenum);
						}
						else
						{
							JTextArea jtf=new JTextArea();
							jtf.setFont(FONT);
							jtf.setAlignmentX(Component.LEFT_ALIGNMENT);
							jtf.setLineWrap(true);
							jsp=new JScrollPane(jtf, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
							jsp_to_jta.put(jsp, jtf);
							if (number) disableenter(jtf); 
							componentCache.put(linenum, jsp);
						}	
					}
					InputTextFieldHandler itfh;
					boolean valid=false;
					DocumentListener remdl=null;
					JTextArea jtf =jsp_to_jta.get(jsp);
					enableFocusTraversal(jtf);
					for (DocumentListener dl:oldDocumentListeners)
					{
						if (dl instanceof InputTextFieldHandler)
						{
							InputTextFieldHandler itfh2=(InputTextFieldHandler)dl;
							if (jtf==itfh2.jtf)
							{
								valid=itfh2.valid;
								remdl=dl;
							}
						}
					}

					
					String varname=paramparts.firstElement().substring(1,paramparts.firstElement().length()-1);

					if (command.equals("inputNumber") || command.equals("inputNumberNC"))
						itfh=new InputTextFieldHandler(jtf,varname,Double.class,lastUpdateTime);
					else
						itfh=new InputTextFieldHandler(jtf,varname,String.class,lastUpdateTime);

					if (defaults.containsKey(varname) && !isDisplayed(jsp) && !isDisplayed(jp)  )
					{
						jtf.setText(defaults.get(varname));
						itfh.upd();
					}
					
					itfh.valid=valid;
					
					for (DocumentListener dl:oldDocumentListeners)
						jtf.getDocument().removeDocumentListener(dl);
					if (remdl!=null)
						oldDocumentListeners.remove(remdl);

					jtf.getDocument().addDocumentListener(itfh);
					oldDocumentListeners.add(itfh);

					if (command.equals("inputNumber") || command.equals("inputString"))
						inputFulfilled.add(itfh);
					
					if(!automaticlayout&&paramparts.size()>=5)
					{			
						desiredComponents.add(jsp);
						
						Rectangle rectangle = new Rectangle(
								(int)Utils.toDouble(paramparts.get(1)), 
								(int)Utils.toDouble(paramparts.get(2)), 
								(int)Utils.toDouble(paramparts.get(3)), 
								(int)Utils.toDouble(paramparts.get(4)));
						jsp.setPreferredSize(new Dimension(rectangle.width, rectangle.height));
						jsp.setMaximumSize(new Dimension(rectangle.width, rectangle.height));
						manualLayoutCache.put(jsp,rectangle);
					}
					else
						desiredComponents.add(jp);
				}

				else if (command.equals("choice") || command.equals("choiceNC"))
				{
					if (!layoutspecified) setLayoutAutomatic(true);
					JPanel jp;					
					int optioncount=paramparts.size();
					if (!automaticlayout)
						optioncount-=4;
					
					if (componentCache.containsKey(linenum) && componentCache.get(linenum) instanceof JPanel && !desiredComponents.contains(componentCache.get(linenum)))
					{
						jp=(JPanel)componentCache.get(linenum);
					}
					else
					{
						jp=new JPanel();
						jp.setLayout(new BoxLayout(jp,BoxLayout.X_AXIS));
						makeBorder(jp);
						
						String var=null;
						ButtonGroup bg=new ButtonGroup();

						jp.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
						

						boolean defex=false;
						
						for (int i=0; i<optioncount; i++)
						{
							String _val=paramparts.get(i);
							if (var==null) var=_val;
							else
							{
								Object val;
								val=_val.trim();
								if (((String)val).startsWith("\""))
									val=stripQuotation((String)val);
								else
									val=Utils.toDouble(((String)val));


								if (val.toString().startsWith("_"))
								{
									JLabel jl=new JLabel("<html>"+getStyle()+"<body>"+val.toString().substring(1)+HTML_FOOTER);
									jl.setFont(FONT);
									jl.setBackground(Color.white);
									jl.setAlignmentX(Component.LEFT_ALIGNMENT);
									jp.add(jl);
									
								}
								else
								{
									JRadioButton jrb=new JRadioButton("<html>"+getStyle()+"<body>"+val.toString()+HTML_FOOTER);
									jrb.setFont(FONT);
									jrb.setBackground(Color.white);
									jrb.setAlignmentX(Component.LEFT_ALIGNMENT);
									jrb.addActionListener(new RBListener(var,val,lastUpdateTime));
									jrb.setFocusTraversalKeysEnabled(true);

									bg.add(jrb);
									jp.add(jrb);
									enableFocusTraversal(jrb);
									
									String varname=stripQuotation(paramparts.firstElement());
									if (defaults.containsKey(varname) && !isDisplayed(jp))
									{
										defex=true;
										Object o=defaults.get(varname);
										if (((String)o).equals(val.toString()))
										{
											jrb.setSelected(true);
										}
									}

								}
							}
						}

						jp.setAlignmentX(Component.LEFT_ALIGNMENT);
						jp.setMaximumSize(new Dimension(getWidth(),(int)(FONT.getSize()*2*2)));
						jp.setMinimumSize(new Dimension(0,(int)(FONT.getSize()*2)));
						jp.setBackground(Color.white);
						jp.setOpaque(true);
						jp.invalidate();
						jp.validate();
						jp.doLayout();

						if (command.equals("choice"))
							inputFulfilled.add(bg);
						if (defex)
							checkFulfilled();

						componentCache.put(linenum, jp);
					}
					

					if(!automaticlayout&&optioncount>=0)
					{
						manualLayoutCache.put(jp,new Rectangle(
								(int)Utils.toDouble(paramparts.get(paramparts.size()-4)), 
								(int)Utils.toDouble(paramparts.get(paramparts.size()-3)), 
								(int)Utils.toDouble(paramparts.get(paramparts.size()-2)), 
								(int)Utils.toDouble(paramparts.get(paramparts.size()-1))));
					}
					desiredComponents.add(jp);
				}
				
				

				else if (command.equals("slider"))
				{
					if (!layoutspecified) setLayoutAutomatic(true);
					JPanel jp;					
				
					jp=new JPanel();
					
					jp.setLayout(new BoxLayout(jp,BoxLayout.X_AXIS));
					makeBorder(jp);

					jp.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

					boolean legend=paramparts.size()==3 || paramparts.size()==7;

					JSlider slider=new JSlider(JSlider.HORIZONTAL);
					
					slider.setMinimum(0);
					slider.setValue(0);
					slider.setMaximum(1000);
					slider.setPreferredSize(new Dimension(200, 20));
					slider.setMinimumSize(new Dimension(200, 20));
					slider.setMaximumSize(new Dimension(200, 20));
					slider.setOpaque(false);
					slider.addChangeListener(new SliderListener(paramparts.get(0), slider, lastUpdateTime));
					slider.setFocusTraversalKeysEnabled(true);

					
					
					if (legend)
					{
						JLabel label = new JLabel("<html>"+getStyle()+"<body>"+stripQuotation(paramparts.get(1))+HTML_FOOTER);
						label.setFont(FONT);
						label.setBackground(Color.white);
						label.setAlignmentX(Component.LEFT_ALIGNMENT);
						label.setBorder(new LineBorder(Color.white, 5));
						jp.add(label);
					}
					jp.add(slider);
					if (legend)
					{
						JLabel label = new JLabel("<html>"+getStyle()+"<body>"+stripQuotation(paramparts.get(2))+HTML_FOOTER);
						label.setFont(FONT);
						label.setBackground(Color.white);
						label.setAlignmentX(Component.LEFT_ALIGNMENT);
						label.setBorder(new LineBorder(Color.white, 5));
						jp.add(label);
					}
					jp.add(new Box.Filler(new Dimension(5, 5), new Dimension(Short.MAX_VALUE, 20), new Dimension(Short.MAX_VALUE, 20)));
					
					
					jp.setAlignmentX(Component.LEFT_ALIGNMENT);
					jp.setMaximumSize(new Dimension(getWidth(),(int)(FONT.getSize()*2*2)));
					jp.setMinimumSize(new Dimension(0,(int)(FONT.getSize()*2)));
					jp.setBackground(Color.white);
					jp.setOpaque(true);
					jp.invalidate();
					jp.validate();
					jp.doLayout();


					componentCache.put(linenum, jp);
				

					if(!automaticlayout)
					{
						manualLayoutCache.put(jp,new Rectangle(
								(int)Utils.toDouble(paramparts.get(paramparts.size()-4)), 
								(int)Utils.toDouble(paramparts.get(paramparts.size()-3)), 
								(int)Utils.toDouble(paramparts.get(paramparts.size()-2)), 
								(int)Utils.toDouble(paramparts.get(paramparts.size()-1))));
					}
					desiredComponents.add(jp);
				}


				else if (command.equals("button") )
				{
					if (!layoutspecified) setLayoutAutomatic(true);

					String var=paramparts.get(0);
					String label=paramparts.get(1);

					if (label.startsWith("\""))
						label=label.substring(label.indexOf("\"")+1, label.lastIndexOf("\""));

					JButton jb;

					if (componentCache.containsKey(linenum) && componentCache.get(linenum)  instanceof JButton && !desiredComponents.contains(componentCache.get(linenum)))
					{
						jb=(JButton)componentCache.get(linenum);
						jb.setText(label);
						for (ActionListener al:jb.getActionListeners())
							jb.removeActionListener(al);
					}
					else
					{
						jb=new JButton();
						jb.setFont(FONT);
						jb.setMinimumSize(new Dimension(FONT.getSize()*10,FONT.getSize()+8));
						jb.setMaximumSize(new Dimension(FONT.getSize()*10,FONT.getSize()+8));
						jb.setPreferredSize(new Dimension(FONT.getSize()*10,FONT.getSize()+8));
						jb.setAlignmentX(Component.LEFT_ALIGNMENT);
						componentCache.put(linenum, jb);
					}

					jb.setFocusTraversalKeysEnabled(true);
					jb.setText("<html>"+getStyle()+"<body>"+label+HTML_FOOTER);

					ActionListener[] actionListeners = jb.getActionListeners();
					for (ActionListener al:actionListeners)
						jb.removeActionListener(al);

					jb.addActionListener(new ABListener(var,lastUpdateTime));
					enableFocusTraversal(jb);

					if(!automaticlayout&&paramparts.size()>=6)
					{
						manualLayoutCache.put(jb,new Rectangle(
								(int)Utils.toDouble(paramparts.get(2)), 
								(int)Utils.toDouble(paramparts.get(3)), 
								(int)Utils.toDouble(paramparts.get(4)), 
								(int)Utils.toDouble(paramparts.get(5))));
					}

					desiredComponents.add(jb);
				}
			
				else if (command.equals("checkbox"))
				{
					if (!layoutspecified) setLayoutAutomatic(true);
					JPanel jp;
					JCheckBox jab;

					if (componentCache.containsKey(linenum) && componentCache.get(linenum) instanceof JPanel
							&& ((JPanel)componentCache.get(linenum)).getComponent(0) instanceof JCheckBox
							&& !desiredComponents.contains(componentCache.get(linenum)))
					{
						jp=(JPanel)componentCache.get(linenum);
						jab=(JCheckBox)jp.getComponent(0);
					}
					else
					{
						jp=new JPanel();
						jab=new JCheckBox();
						jab.setFont(FONT);
						jab.setMinimumSize(new Dimension(FONT.getSize()*10,FONT.getSize()+8));
						jab.setMaximumSize(new Dimension(FONT.getSize()*10,FONT.getSize()+8));
						jab.setAlignmentX(Component.LEFT_ALIGNMENT);
						jab.setOpaque(false);
						makeBorder(jp);
						jp.add(jab);
						jp.setAlignmentX(Component.LEFT_ALIGNMENT);
						jp.setMaximumSize(new Dimension(1000,(int)(FONT.getSize()+12)));
						jp.setMinimumSize(new Dimension(0,(int)(FONT.getSize()+12)));
						jp.setBackground(Color.white);
						jp.setLayout(new BoxLayout(jp,BoxLayout.X_AXIS));
						jp.setOpaque(false);
						componentCache.put(linenum, jp);
					}
					jab.setFocusTraversalKeysEnabled(true);
					enableFocusTraversal(jab);

					String var=paramparts.get(0);
					String label=paramparts.get(1);

					String varname=paramparts.firstElement().substring(1,paramparts.firstElement().length()-1);
					if (defaults.containsKey(varname)&& !isDisplayed(jp))
					{
						Object o=defaults.get(varname);
						if (Utils.toDouble((String)o)!=0)
						{
							jab.setSelected(true);
						}
					}

					
					if (label.startsWith("\""))
						label=label.substring(label.indexOf("\"")+1, label.lastIndexOf("\""));

					jab.setText("<html>"+getStyle()+"<body>"+label+HTML_FOOTER);


					ActionListener[] actionListeners = jab.getActionListeners();
					for (ActionListener al:actionListeners)
						jab.removeActionListener(al);

					jab.addActionListener(new CBListener(var,jab,lastUpdateTime));

					if(!automaticlayout&&paramparts.size()>=6)
					{
						manualLayoutCache.put(jp,new Rectangle(
								(int)Utils.toDouble(paramparts.get(2)), 
								(int)Utils.toDouble(paramparts.get(3)), 
								(int)Utils.toDouble(paramparts.get(4)), 
								(int)Utils.toDouble(paramparts.get(5))));
					}

					desiredComponents.add(jp);
				}

				else if (command.equals("style"))
				{
					String content=param;
					if (param.startsWith("\""))
						content=param.substring(param.indexOf("\"")+1, param.lastIndexOf("\""));
					customstyle=content;
				}
				else if (command.equals("setdefault"))
				{
					String key=paramparts.get(0);
					if (paramparts.size()>1)
					{
						String content=paramparts.get(1);
						if (paramparts.get(1).startsWith("\""))
							content=paramparts.get(1).substring(paramparts.get(1).indexOf("\"")+1, paramparts.get(1).lastIndexOf("\""));
						defaults.put(key, content.replaceAll("\\\\n", "\n"));
					}
					else if (defaults.containsKey(key))
						defaults.remove(key);
				}
				else if (command.equals("manualLayout"))
				{
					setLayoutAutomatic(false);
				}
				else if (command.equals("recordKeys"))
				{
					recordkeys=true;
				}
				else if (command.equals("locale"))
				{
					//String locale=stripQuotation(paramparts.get(0));
/*					final Object[] o={"locale",locale};
					clientApplet.cc.send(ServerCommand.SUBMIT_VALUE, o);	*/
				}
				else if (command.equals("enableMouseTracking"))
				{
					setMouseTracking(true);
				}
				else if (command.equals("disableMouseTracking"))
				{
					setMouseTracking(false);
				}
				else if (command.equals("eyetrackerInitialise"))
				{
					if (eyetrackerInterface!=null)
						eyetrackerInterface.destroy();
					eyetrackerInterface=new EyetrackerInterface();
                    
                    // do some simple function overloading to ease handling for user 
                    if (paramparts.size() == 0)
                    	info("not enough arguments for initialisation given");
                    // if there is only one parameter it should be only the type
                    else if (paramparts.size() == 1)
                    {
						eyetrackerInterface.setTrackerType(stripQuotation(paramparts.get(0)));
                        eyetrackerInterface.initialise("",0,0);
                    }
                    // otherwise its another eye tracker with more options
                    else if (paramparts.size() == 4) 
                    {
						eyetrackerInterface.setTrackerType(stripQuotation(paramparts.get(0)));
                    	eyetrackerInterface.initialise(stripQuotation(paramparts.get(1)), 
							Integer.parseInt(stripQuotation(paramparts.get(2))), 
							Integer.parseInt(stripQuotation(paramparts.get(3))));
                        
                    }
				}
				else if (command.equals("eyetrackerStart"))
				{
                    // do some simple function overloading to ease handling for user 
                    if (paramparts.size()==0)
                        eyetrackerInterface.start(0, "");
                    else if (paramparts.size() == 1)
                    	eyetrackerInterface.start(0,
    							stripQuotation(paramparts.get(0)));
                    else if (paramparts.size() == 2)
                        eyetrackerInterface.start(Integer.parseInt(stripQuotation(paramparts.get(0))),
							stripQuotation(paramparts.get(1)));
				}
				else if (command.equals("eyetrackerStop"))
				{
					eyetrackerInterface.stop();
				}
				else if (command.equals("eyetrackerTrigger"))
				{
					if (paramparts.size() == 0)
						info("no trigger given");
					else 
						eyetrackerInterface.trigger(stripQuotation(paramparts.get(0)));
				}				
				else if (command.equals("eyetrackerCalibrate"))
				{
                    info("Deprecated. We won't use calibration inside BoXS in further versions");
				}

			}
			catch(Exception e)
			{
				String s="<html>Exception:<br>"+e.getMessage()+"<br>";
				for (StackTraceElement ste:e.getStackTrace())
					s+=ste.toString()+"<br>";
				clientApplet.updateMessage(s, false);
				e.printStackTrace();

			}
		}

		setFocusCycleRoot(true);

		// 1) remove components from mainPanel which are no longer desired
		LinkedList<Component> deleteComponents=new LinkedList<Component>();
		for (Component c:getComponents())
			if (!desiredComponents.contains(c))
				deleteComponents.add(c);
		for (Component c:deleteComponents)
		{
			remove(c);
			majorchange=true;
		}

		// add missing components
		int prevCompPos=-1;
		for (Component c:desiredComponents)
		{
			boolean exists=false;

			for (int i=0; i<getComponents().length; i++)
			{
				if (getComponents()[i]==c)
				{
					exists=true;
					prevCompPos=i;
				}
			}


			if (!exists)
			{
				add(c,prevCompPos+1);
				majorchange=true;
				prevCompPos++;
			}
			
			if(!automaticlayout && manualLayoutCache.containsKey(c))
				c.setBounds(manualLayoutCache.get(c));
		}

		
		if (recordkeys)
		{
			JTextField capturefield=new JTextField("");
			capturefield.setMaximumSize(new Dimension(1,1));
			capturefield.setMaximumSize(new Dimension(1,1));
			capturefield.addKeyListener(new KeyAdapter()
				{
					public void keyPressed(KeyEvent e)
					{
						if (clientApplet!=null && clientApplet.cc!=null && e.getKeyCode()!=0)
				    	{
							Object[] o={"_lastkey",new Double(e.getKeyCode()),new Long(new Date().getTime())};
							clientApplet.cc.send(ServerCommand.SUBMIT_VALUE, o);
					    }
					}
				});

			add(capturefield);
			capturefield.setFocusable(true);
			capturefield.requestFocus();
		}

		if (majorchange)
			serverSideValid=true;

		checkFulfilled();
		setEnabled(true);
		if (changed || majorchange)
		{
			validate();
			doLayout();
			invalidate();
			repaint();
			if (firstcomp!=null)
			{
				firstcomp.requestFocusInWindow();
				firstcomp.requestFocus();
			}
		}
		
		if (clientApplet.cc!=null && !_update.equals("") && !_update.equals("finish"))
		{
			Object[] o={"_clientdisplaytime"+linenum,new Double(new Date().getTime()),new Long(new Date().getTime())};
			clientApplet.cc.send(ServerCommand.SUBMIT_VALUE, o);
			Object[] o2={"_clientwidth",new Double(getWidth()),new Long(new Date().getTime())};
			clientApplet.cc.send(ServerCommand.SUBMIT_VALUE, o2);
			Object[] o3={"_clientheight",new Double(getHeight()),new Long(new Date().getTime())};
			clientApplet.cc.send(ServerCommand.SUBMIT_VALUE, o3);
		}
	}


	private String stripQuotation(String val)
	{
		return (val).substring(1,(val).length()-1);
	}

	public void makeBorder(JComponent c)
	{
		c.setBorder(new LineBorder(Color.white,FONT.getSize()/4));
	}


	private void disableenter(JTextArea jtf) {
		AbstractAction tabAction = new AbstractAction() {  
			private static final long serialVersionUID = 1L;
		    public void actionPerformed(ActionEvent e) {  
		    }  
		};  
		KeyStroke tabKey = KeyStroke.getKeyStroke("ENTER");  
		jtf.getInputMap().put(tabKey, tabAction);
		tabKey = KeyStroke.getKeyStroke("RETURN");  
		jtf.getInputMap().put(tabKey, tabAction);
	}
	
	private boolean isDisplayed(Component jp) {
		for (Component c:getComponents())
			if (c==jp) return true;
		return false;
	}



	private void enableFocusTraversal(final Component jtf) {
		if (firstcomp==null) firstcomp=jtf;
		
		jtf.setFocusable(true);
		jtf.setFocusTraversalKeysEnabled(true);
		
		Set<KeyStroke> newForwardKeys = new HashSet<KeyStroke>();
		newForwardKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
		Set<KeyStroke> newBackwardKeys = new HashSet<KeyStroke>();
		newBackwardKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK));
		jtf.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
		    newForwardKeys);
		jtf.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
			    newBackwardKeys);
	}

	
	public synchronized void checkFulfilled()
	{
		boolean clientSideValid=true;
		for (Object b:inputFulfilled)
		{
			if (b instanceof InputTextFieldHandler && !((InputTextFieldHandler)b).valid)
			{
				clientSideValid=false;
			}
			else if (b instanceof ButtonGroup)
			{
				if (((ButtonGroup) b).getSelection()==null) clientSideValid=false;
			}
		}
		
		for (JButton wb:waitButtons)
			wb.setEnabled(clientSideValid && serverSideValid && !waitButtonPressed);
	}

	private void setLayoutAutomatic(boolean b) {
		layoutspecified=true;
		if (automaticlayout==b) return;
		automaticlayout=b;
		setLayout(automaticlayout?new BoxLayout(this,BoxLayout.Y_AXIS):null);
	}
	
	
	private synchronized void setMouseTracking(boolean b) {
		
		if (mousetrackingthread!=null)
			try
			{
				mousetrackingthread.interrupt();
				mousetrackingthread.join();
			} catch (InterruptedException e)
			{
				info("there is a problem with mouse tracking");
			}
		
		if (b)
		{
			mousetrackingstarttime=new Date().getTime();
			mousetrackingthread=new Thread(){
				public void run() {
					try
					{
						while (true)
						{
							final Object[] o={"_mousepos",
									MouseInfo.getPointerInfo().getLocation().x+"."+MouseInfo.getPointerInfo().getLocation().y,
									new Long(new Date().getTime() - mousetrackingstarttime)};
							clientApplet.cc.send(ServerCommand.SUBMIT_VALUE, o);

							Thread.sleep(250);
						}
					}
					catch (InterruptedException e)
					{
						info("there is a problem with mouse tracking");
					}
					info("finished");
				};
			};
			mousetrackingthread.start();
		}
		
	}
	
	
	

	public synchronized void setServerSideValid(ServerSideValidInfo ssvi) {
		serverSideValid=ssvi.ssvi==null;
		errormessage=ssvi.ssvi;
		for (JButton b:waitButtons)
			b.setText(errormessage!=null?errormessage:waitButtonsOkayText.get(b));

		checkFulfilled();
	}

	
	
	public String getStyle()
	{
		// workaround for Java-Bug
		@SuppressWarnings("unused")
		ParserDelegator workaround = new ParserDelegator();
		// http://forums.oracle.com/forums/thread.jspa?messageID=7432921
		
		String style="";
		style+="<style>\n";
		
		if (customstyle!=null && !customstyle.equals(""))
		{
			style+=customstyle;
		}
		else
		{
			style+="body{ padding: 0px; font-size: "+Math.round(FONT.getSize()*0.8)+"px; }\n";
			style+="h1{font-size: 130%; margin-top: 0px; margin-bottom: 3px; font-weight: normal; }\n";
			style+="h2{font-size: 115%; margin-top: 0px; margin-bottom: 3px; font-weight: normal; }\n";
			style+="table{background-color: #eeeeee; border:solid; border-width:1px; border-color: #000000; margin:5px; margin-left:10px;}\n";
			style+="td,th{padding:5px;text-align: center; }\n";
			style+="th{background-color: #dddddd; }\n"; 
		}
		
		style+="</style>\n";
		return style;
	}


	public class CBListener implements ActionListener
	{
		String var;
		JCheckBox val;
		Date starttime;

		public CBListener(String _var, JCheckBox _val, Date _startTime)
		{
			var=_var;
			val=_val;
			starttime=_startTime;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			checkFulfilled();
			final Object[] o={var,val.isSelected()?new Double(1):new Double(0),new Long(new Date().getTime() - starttime.getTime())};
			clientApplet.cc.send(ServerCommand.SUBMIT_VALUE, o);	
		}

	}

	

	class InputTextFieldHandler implements DocumentListener
	{
		String varname;
		@SuppressWarnings("rawtypes")
		Class type;
		JTextArea jtf;
		Date starttime;

		@SuppressWarnings("rawtypes")
		InputTextFieldHandler(JTextArea _jtf,  String _varname, Class _type, Date _startTime)
		{
			varname=_varname;
			type=_type;
			jtf=_jtf;
			starttime=_startTime;
		}

		@Override
		public void changedUpdate(DocumentEvent arg0)
		{
			upd();
		}

		@Override
		public void insertUpdate(DocumentEvent arg0)
		{
			upd();
		}

		@Override
		public void removeUpdate(DocumentEvent arg0)
		{
			upd();
		}


		boolean valid=false;

		void upd()
		{
			try
			{

				Object[] o={varname,type==Double.class?
						new Double(jtf.getText().replaceAll(",", ".")):jtf.getText(),
						new Long(new Date().getTime() - starttime.getTime())};
				serverSideValid=false;
				jtf.setBackground(Color.white);
				clientApplet.cc.send(ServerCommand.SUBMIT_VALUE, o);
				valid=true;
			}
			catch(Exception e)
			{
				jtf.setBackground(new Color(255,220,220));
				valid=false;
			}
			checkFulfilled();
		}
		
	}



	public class RBListener implements ActionListener
	{
		String var;
		Object val;
		Date starttime;

		public RBListener(String _var, Object _val, Date _startTime)
		{
			var=_var;
			val=_val;
			starttime=_startTime;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			checkFulfilled();
			final Object[] o={var,val,new Long(new Date().getTime() - starttime.getTime())};
			clientApplet.cc.send(ServerCommand.SUBMIT_VALUE, o);	
		}

	}

	

	public class SliderListener implements ChangeListener
	{
		String var;
		JSlider sld;
		Date starttime;

		public SliderListener(String _var, JSlider _sld, Date _startTime)
		{
			var=_var;
			sld=_sld;
			starttime=_startTime;
		}

		@Override
		public void stateChanged(ChangeEvent e)
		{
			checkFulfilled();
			final Object[] o={var,((double)sld.getValue())/1000d,
					new Long(new Date().getTime() - starttime.getTime())};
			clientApplet.cc.send(ServerCommand.SUBMIT_VALUE, o);	
			
		}

	}





	public class ABListener implements ActionListener
	{
		String var;
		Object val;
		Date starttime;

		public ABListener(String _var, Date _starttime)
		{
			var=_var;
			val=new Double(1);
			starttime=_starttime;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			checkFulfilled();
			final Object[] o={var,val,new Long(new Date().getTime() - starttime.getTime())};

			clientApplet.cc.send(ServerCommand.SUBMIT_VALUE, o);
		}
	}

	
	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		setEnabledRecursive(this, enabled);
	}


	private void setEnabledRecursive(Container c, boolean val)
	{
		for (Component c2:c.getComponents())
		{
			c2.setEnabled(val);
			if (c2 instanceof Container)
				setEnabledRecursive((Container)c2, val);
		}
	}
}
