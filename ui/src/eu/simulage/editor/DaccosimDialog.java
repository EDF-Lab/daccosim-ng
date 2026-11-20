package eu.simulage.editor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class DaccosimDialog extends JDialog {

	private Runnable okRunnable = this::dispose;
	private Runnable cancelRunnable = this::dispose;
	private Runnable resetRunnable = null;
	private JButton reset;
	private JButton okButton;
	private JButton cancelButton;

	DaccosimDialog(Frame owner, String title) {
		super(owner);
		setTitle(title);
		setIconImage(new ImageIcon(Main.class.getResource("/images/logo_16.png")).getImage());
		setModal(true);
		setLayout(new BorderLayout());
	}

	protected DaccosimDialog(Dialog owner) {
		super(owner);
		setIconImage(new ImageIcon(Main.class.getResource("/images/logo_16.png")).getImage());
		setModal(true);
		setLayout(new BorderLayout());
	}

	public DaccosimDialog withButtons() {
		add(buttons(), BorderLayout.SOUTH);
		return this;
	}

	void withReset(Runnable resetRunnable) {
		reset.setVisible(true);
		this.resetRunnable = resetRunnable;
	}

	protected JRootPane createRootPane() {
		JRootPane rootPane = new JRootPane();
		rootPane.registerKeyboardAction((e) -> cancelRunnable.run(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		rootPane.registerKeyboardAction((e) -> okRunnable.run(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		return rootPane;
	}

	public void cancelRunnable(Runnable runnable) {
		this.cancelRunnable = runnable;
	}

	public DaccosimDialog okRunnable(Runnable runnable) {
		this.okRunnable = runnable;
		return this;
	}

	private JPanel buttons() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		reset = new JButton("Default");
		reset.setPreferredSize(new Dimension(80, 25));
		reset.addActionListener(e -> resetRunnable.run());
		reset.setVisible(false);
		okButton = new JButton("Ok");
		okButton.setPreferredSize(new Dimension(80, 25));
		okButton.addActionListener(e -> okRunnable.run());
		cancelButton = new JButton("Cancel");
		cancelButton.setPreferredSize(new Dimension(80, 25));
		cancelButton.addActionListener(e -> cancelRunnable.run());
		panel.add(reset);
		panel.add(okButton);
		panel.add(cancelButton);
		return panel;
	}

	public JButton okButton() {
		return okButton;
	}

	public JButton cancelButton() {
		return cancelButton;
	}

	public JButton resetButton() {
		return reset;
	}
}
