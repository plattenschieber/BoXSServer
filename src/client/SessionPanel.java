package client;

import java.awt.BorderLayout;

import javax.swing.JPanel;

public class SessionPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	public ExperimentSessionData esd;
	public VariableMonitor vm=new VariableMonitor();
	ClientConnection cc;

	public SessionPanel(ExperimentSessionData _esd, ClientConnection _cc)
	{
		cc=_cc;
		updateData(_esd);
		setLayout(new BorderLayout());
		add(vm,BorderLayout.CENTER);


	}


	public void updateData(ExperimentSessionData _esd)
	{
		esd=_esd;
		if (esd!=null)
			vm.updateData(esd.varspace);

	}
}
