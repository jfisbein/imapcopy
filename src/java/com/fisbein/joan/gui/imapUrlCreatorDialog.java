package com.fisbein.joan.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.mail.URLName;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * This code was edited or generated using CloudGarden's Jigloo SWT/Swing GUI Builder, which is free
 * for non-commercial use. If Jigloo is being used commercially (ie, by a corporation, company or
 * business for any purpose whatever) then you should purchase a license for each developer using
 * Jigloo. Please visit www.cloudgarden.com for details. Use of Jigloo implies acceptance of these
 * licensing terms. A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR THIS MACHINE, SO JIGLOO OR THIS
 * CODE CANNOT BE USED LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
 */
public class imapUrlCreatorDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -3030207701909920747L;

	private final static Logger log = Logger.getLogger(imapUrlCreatorDialog.class);

	private JLabel jLabel1;

	private JLabel jLabel2;

	private JLabel jLabel4;

	private JButton btnOk;

	private JRadioButton radioSSLNo;

	private JRadioButton radioSSLYes;

	private ButtonGroup buttonGroupSSL;

	private JTextField textServer;

	private JTextField textPassword;

	private JTextField textUsername;

	private JLabel jLabel3;

	private URLName URL;

	/**
	 * Auto-generated main method to display this JDialog
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame();
				imapUrlCreatorDialog inst = new imapUrlCreatorDialog(frame);
				inst.setVisible(true);
			}
		});
	}

	public imapUrlCreatorDialog(JFrame frame) {
		super(frame);
		initGUI();
	}

	private void initGUI() {
		try {
			FormLayout thisLayout = new FormLayout("max(p;5dlu), 26dlu, 96dlu",
					"max(p;5dlu), max(p;5dlu), max(p;5dlu), max(p;5dlu), max(p;15dlu)");
			getContentPane().setLayout(thisLayout);
			this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			this.setResizable(false);
			{
				jLabel1 = new JLabel();
				getContentPane().add(jLabel1, new CellConstraints("1, 1, 1, 1, default, default"));
				jLabel1.setText("Use SSL:");
			}
			{
				jLabel2 = new JLabel();
				getContentPane().add(jLabel2, new CellConstraints("1, 2, 1, 1, default, default"));
				jLabel2.setText("Username:");
			}
			{
				jLabel3 = new JLabel();
				getContentPane().add(jLabel3, new CellConstraints("1, 3, 1, 1, default, default"));
				jLabel3.setText("Password:");
			}
			{
				jLabel4 = new JLabel();
				getContentPane().add(jLabel4, new CellConstraints("1, 4, 1, 1, default, default"));
				jLabel4.setText("Server:");
			}
			{
				textUsername = new JTextField();
				getContentPane().add(textUsername, new CellConstraints("2, 2, 2, 1, default, default"));
			}
			{
				textPassword = new JTextField();
				getContentPane().add(textPassword, new CellConstraints("2, 3, 2, 1, default, default"));
			}
			{
				textServer = new JTextField();
				getContentPane().add(textServer, new CellConstraints("2, 4, 2, 1, default, default"));
			}
			{
				radioSSLYes = new JRadioButton();
				getContentPane().add(radioSSLYes, new CellConstraints("2, 1, 1, 1, default, default"));
				radioSSLYes.setText("Yes");
				getButtonGroupSSL().add(radioSSLYes);
			}
			{
				radioSSLNo = new JRadioButton();
				getContentPane().add(radioSSLNo, new CellConstraints("3, 1, 1, 1, default, default"));
				radioSSLNo.setText("No");
				getButtonGroupSSL().add(radioSSLNo);
			}
			{
				btnOk = new JButton();
				getContentPane().add(btnOk, new CellConstraints("2, 5, 2, 1, default, default"));
				btnOk.setText("Ok");
				btnOk.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						btnOkActionPerformed(evt);
					}
				});
			}
			this.setSize(340, 150);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ButtonGroup getButtonGroupSSL() {
		if (buttonGroupSSL == null) {
			buttonGroupSSL = new ButtonGroup();
		}
		return buttonGroupSSL;
	}

	private void btnOkActionPerformed(ActionEvent evt) {
		log.debug("btnOk.actionPerformed, event=" + evt);
		String protocol;
		if (radioSSLYes.isSelected()) {
			protocol = "imaps";
		} else {
			protocol = "imap";
		}

		URL = new URLName(protocol, textServer.getText().trim(), -1, null, textUsername.getText().trim(), textPassword
				.getText().trim());
		this.dispose();
	}

	public String getURL() {
		return URL.toString();
	}

	public void setURL(String url) {
		try {
			URL = new URLName(url);
			textUsername.setText(URL.getUsername());
			textPassword.setText(URL.getPassword());
			textServer.setText(URL.getHost());
			if (URL.getProtocol().equals("imaps")) {
				radioSSLYes.setSelected(true);
			} else {
				radioSSLNo.setSelected(true);
			}
		} catch (Exception e) {
			URL = null;
		}
	}
}
