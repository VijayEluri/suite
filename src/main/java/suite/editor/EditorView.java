package suite.editor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import suite.editor.Layout.Node;
import suite.editor.Layout.Orientation;

public class EditorView {

	private static final int windowWidth = 1280;
	private static final int windowHeight = 768;

	private Font font = new Font("Akkurat-Mono", Font.PLAIN, 12);

	private EditorController controller;

	private Node layout;

	private JFrame frame;

	private JLabel rightLabel;
	private JLabel topLabel;

	private JTextField leftTextField;

	private JScrollPane scrollPane;
	private JTextArea bottomTextArea;

	private JEditorPane editor;

	public JFrame run() {
		JTextField leftTextField = this.leftTextField = applyDefaults(new JTextField(32));

		DefaultListModel<String> listModel = new DefaultListModel<>();
		listModel.addElement("Jane Doe");
		listModel.addElement("John Smith");
		listModel.addElement("Kathy Green");

		JList<String> leftList = applyDefaults(new JList<>(listModel));

		JLabel rightLabel = this.rightLabel = applyDefaults(new JLabel("Right"));
		rightLabel.setVisible(false);

		JLabel topLabel = this.topLabel = applyDefaults(new JLabel("Top"));
		topLabel.setVisible(false);

		JTextArea bottomTextArea = this.bottomTextArea = applyDefaults(new JTextArea("Bottom"));
		bottomTextArea.setEditable(false);
		bottomTextArea.setRows(12);
		bottomTextArea.setVisible(false);

		JScrollPane scrollPane = this.scrollPane = new JScrollPane(bottomTextArea //
				, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED //
				, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		JEditorPane editor = this.editor = applyDefaults(new JEditorPane());

		Component box = Box.createRigidArea(new Dimension(8, 8));

		JButton okButton = applyDefaults(new JButton("OK"));
		okButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				controller.run(EditorView.this);
			}
		});

		JFrame frame = this.frame = new JFrame(getClass().getSimpleName());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setJMenuBar(createMenuBar());
		frame.setSize(new Dimension(windowWidth, windowHeight));
		frame.setVisible(true);

		int u = 64, u2 = u * 2;

		layout = Layout.layout(Orientation.HORIZONTAL //
				, Layout.layout(Orientation.VERTICAL //
						, Layout.fh(leftTextField, u, 16) //
						, Layout.c(leftList, u, u) //
				) //
				, Layout.layout(Orientation.VERTICAL //
						, Layout.fh(topLabel, u2, 64) //
						, Layout.c(editor, u2, u2) //
						, Layout.fh(box, u2, 8) //
						, Layout.fwh(okButton, 48, 16) //
						, Layout.layout(Orientation.VERTICAL //
								, Layout.c(scrollPane, u2, u) //
						) //
				) //
				, Layout.c(rightLabel, u, u) //
				);

		repaint();

		editor.requestFocus();

		return frame;
	}

	public void repaint() {
		new LayoutCalculator().arrange(frame.getContentPane(), layout);
		frame.repaint();
	}

	private JMenuBar createMenuBar() {
		final EditorView view = this;

		JMenuItem openMenuItem = applyDefaults(new JMenuItem("Open...", KeyEvent.VK_O));
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));

		JMenuItem saveMenuItem = applyDefaults(new JMenuItem("Save", KeyEvent.VK_S));
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));

		JMenuItem exitMenuItem = applyDefaults(new JMenuItem("Exit", KeyEvent.VK_X));
		exitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.quit(EditorView.this);
			}
		});

		JMenuItem leftMenuItem = applyDefaults(new JMenuItem("Left", KeyEvent.VK_L));
		leftMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
		leftMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.left(view);
			}
		});

		JMenuItem rightMenuItem = applyDefaults(new JMenuItem("Right", KeyEvent.VK_R));
		rightMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
		rightMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.right(view);
			}
		});

		JMenuItem topMenuItem = applyDefaults(new JMenuItem("Top", KeyEvent.VK_T));
		topMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.ALT_MASK));
		topMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.top(view);
			}
		});

		JMenuItem bottomMenuItem = applyDefaults(new JMenuItem("Bottom", KeyEvent.VK_B));
		bottomMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.ALT_MASK));
		bottomMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.bottom(view);
			}
		});

		JMenuItem runMenuItem = applyDefaults(new JMenuItem("Run", KeyEvent.VK_R));
		runMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.run(view);
			}
		});

		JMenu fileMenu = createMenu("File", KeyEvent.VK_F, openMenuItem, saveMenuItem, exitMenuItem);
		JMenu editMenu = createMenu("Edit", KeyEvent.VK_E);
		JMenu viewMenu = createMenu("View", KeyEvent.VK_V, leftMenuItem, rightMenuItem, topMenuItem, bottomMenuItem);
		JMenu projectMenu = createMenu("Project", KeyEvent.VK_P, runMenuItem);

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(viewMenu);
		menuBar.add(projectMenu);

		return menuBar;
	}

	private JMenu createMenu(String title, int keyEvent, JMenuItem... menuItems) {
		JMenu menu = applyDefaults(new JMenu(title));
		menu.setMnemonic(keyEvent);

		for (Component component : menuItems)
			menu.add(component);

		return menu;
	}

	private <T extends JComponent> T applyDefaults(T t) {
		t.setFont(font);
		t.setAlignmentX(Component.CENTER_ALIGNMENT);
		return t;
	}

	public void setController(EditorController controller) {
		this.controller = controller;
	}

	public JComponent getBottomToolbar() {
		return scrollPane;
	}

	public JComponent getLeftToolbar() {
		return leftTextField;
	}

	public JComponent getRightToolbar() {
		return rightLabel;
	}

	public JLabel getTopToolbar() {
		return topLabel;
	}

	public JTextField getLeftTextField() {
		return leftTextField;
	}

	public JTextArea getBottomTextArea() {
		return bottomTextArea;
	}

	public JEditorPane getEditor() {
		return editor;
	}

}
