package com.fisbein.joan.gui;

import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.mail.MessagingException;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;

import com.fisbein.joan.model.ImapCopier;
import com.fisbein.joan.model.ImapCopyAplicationEvent;
import com.fisbein.joan.model.ImapCopyEvent;
import com.fisbein.joan.model.ImapCopyFolderEvent;
import com.fisbein.joan.model.ImapCopyListenerInterface;
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
public class ImapCopyGui extends javax.swing.JFrame implements ImapCopyListenerInterface {
	private static final long serialVersionUID = 3773683855479203142L;

	private final static Logger log = Logger.getLogger(ImapCopyGui.class);

	private JButton btnStop;

	private JLabel labelLog;

	private JButton btnCopy;

	private JButton btnEditImapSource;

	private JButton btnEditImapTarget;

	private JTextField textImapTarget;

	private JTextField textImapSource;

	private JLabel labelImapTarget;

	private JLabel labelImapSource;

	private Thread copyThread;

	/**
	 * Auto-generated main method to display this JFrame
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ImapCopyGui inst = new ImapCopyGui();
				inst.setLocationRelativeTo(null);
				inst.setVisible(true);
			}
		});
	}

	public ImapCopyGui() {
		super();
		initGUI();
	}

	private void initGUI() {
		try {
			FormLayout thisLayout = new FormLayout("max(p;5dlu), 83dlu, 78dlu, max(p;5dlu)",
					"max(p;5dlu), max(p;5dlu), max(p;5dlu), max(p;5dlu), max(p;15dlu)");
			getContentPane().setLayout(thisLayout);
			this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			this.setTitle("Imap Copy");
			this.setPreferredSize(new java.awt.Dimension(474, 128));
			this.setResizable(false);
			{
				labelImapSource = new JLabel();
				getContentPane().add(labelImapSource, new CellConstraints("1, 1, 1, 1, default, default"));
				labelImapSource.setText("Source Account:");
			}
			{
				labelImapTarget = new JLabel();
				getContentPane().add(labelImapTarget, new CellConstraints("1, 2, 1, 1, default, default"));
				labelImapTarget.setText("Target Account:");
			}
			{
				textImapSource = new JTextField();
				getContentPane().add(textImapSource, new CellConstraints("2, 1, 2, 1, default, default"));
			}
			{
				textImapTarget = new JTextField();
				getContentPane().add(textImapTarget, new CellConstraints("2, 2, 2, 1, default, default"));
			}
			{
				btnEditImapTarget = new JButton();
				getContentPane().add(btnEditImapTarget, new CellConstraints("4, 2, 1, 1, default, default"));
				btnEditImapTarget.setText("...");
				btnEditImapTarget.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						btnEditImapTargetActionPerformed(evt);
					}
				});
			}
			{
				btnEditImapSource = new JButton();
				getContentPane().add(btnEditImapSource, new CellConstraints("4, 1, 1, 1, default, default"));
				btnEditImapSource.setText("...");
				btnEditImapSource.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						btnEditImapSourceActionPerformed(evt);
					}
				});
			}
			{
				btnCopy = new JButton();
				FlowLayout btnCopyLayout = new FlowLayout();
				btnCopy.setLayout(btnCopyLayout);
				getContentPane().add(btnCopy, new CellConstraints("2, 4, 1, 1, default, default"));
				btnCopy.setText("Copy");
				btnCopy.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						btnCopyActionPerformed(evt);
					}
				});
			}
			{
				labelLog = new JLabel();
				getContentPane().add(labelLog, new CellConstraints("2, 3, 3, 1, default, default"));
				labelLog.setText("log");
			}
			{
				btnStop = new JButton();
				FlowLayout btnStopLayout = new FlowLayout();
				btnStop.setLayout(btnStopLayout);
				getContentPane().add(btnStop, new CellConstraints("3, 4, 1, 1, default, default"));
				btnStop.setText("Stop");
				btnStop.setEnabled(false);
				btnStop.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						btnStopActionPerformed(evt);
					}
				});
			}
			this.setSize(474, 128);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void btnCopyActionPerformed(ActionEvent evt) {
		log.debug("btnCopy.actionPerformed, event=" + evt);

		ImapCopier ic = new ImapCopier();
		ic.addImapCopyListener(this);
		try {
			ic.openSourceConnection(textImapSource.getText());
			ic.openTargetConnection(textImapTarget.getText());
			btnCopy.setEnabled(false);
			btnStop.setEnabled(true);
			copyThread = new Thread(ic);
			copyThread.start();
		} catch (MessagingException e) {
			log.error(e.getMessage(), e);
		}
	}

	private void btnEditImapSourceActionPerformed(ActionEvent evt) {
		log.debug("btnEditImapSource.actionPerformed, event=" + evt);
		imapUrlCreatorDialog dialog = new imapUrlCreatorDialog(this);
		Rectangle bounds = dialog.getBounds();
		bounds.x = this.getBounds().x;
		bounds.y = this.getBounds().y;
		dialog.setBounds(bounds);

		dialog.setURL(textImapSource.getText());
		dialog.setModal(true);
		dialog.setVisible(true);
	}

	private void btnEditImapTargetActionPerformed(ActionEvent evt) {
		log.debug("btnEditImapTarget.actionPerformed, event=" + evt);
		imapUrlCreatorDialog dialog = new imapUrlCreatorDialog(this);
		Rectangle bounds = dialog.getBounds();
		bounds.x = this.getBounds().x;
		bounds.y = this.getBounds().y;
		dialog.setBounds(bounds);

		dialog.setURL(textImapTarget.getText());
		dialog.setModal(true);
		dialog.setVisible(true);
	}

	public void eventNotification(ImapCopyEvent event) {
		if (event instanceof ImapCopyFolderEvent) {
			labelLog.setText("Copying Folder:" + ((ImapCopyFolderEvent) event).getFolderName());
			this.repaint();
		} else if (event instanceof ImapCopyAplicationEvent) {
			if (((ImapCopyAplicationEvent) event).getType() == ImapCopyAplicationEvent.END) {
				btnCopy.setEnabled(true);
				btnStop.setEnabled(false);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void btnStopActionPerformed(ActionEvent evt) {
		log.debug("btnStop.actionPerformed, event=" + evt);
		this.copyThread.stop();
		btnCopy.setEnabled(true);
		btnStop.setEnabled(false);
	}
}
