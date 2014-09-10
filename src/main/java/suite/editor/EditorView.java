package suite.editor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

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
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import suite.editor.LayoutCalculator.Orientation;

public class EditorView {

	private int windowWidth = 1280;
	private int windowHeight = 768;

	private Font font;
	private Font narrowFont;

	private EditorController controller;

	private JEditorPane editor;
	private JFrame frame;
	private LayoutCalculator lay;
	private LayoutCalculator.Node layout;
	private JList<String> searchList;
	private JTextField searchTextField;
	private JTextArea messageTextArea;
	private DefaultListModel<String> listModel;
	private JLabel rightLabel;
	private JScrollPane messageScrollPane;
	private JTextField filenameTextField;

	private boolean isModified = false;

	public EditorView() {
		String os = System.getenv("OS");
		String monoFont;
		String sansFont;

		if (os != null && os.startsWith("Windows")) {
			System.setProperty("awt.useSystemAAFontSettings", "off");
			System.setProperty("swing.aatext", "false");
			monoFont = "Courier";
			sansFont = "Arial";
		} else {
			monoFont = "Akkurat-Mono";
			sansFont = "Sans";
		}

		font = new Font(monoFont, Font.PLAIN, 12);
		narrowFont = new Font(sansFont, Font.PLAIN, 12);
	}

	public JFrame run(String title) {
		JTextField searchTextField = this.searchTextField = applyDefaults(new JTextField(32));
		searchTextField.addActionListener(event -> controller.searchFiles(EditorView.this));
		searchTextField.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.VK_DOWN)
					controller.downToSearchList(EditorView.this);
			}
		});

		DefaultListModel<String> listModel = this.listModel = new DefaultListModel<>();
		listModel.addElement("<Empty>");

		JList<String> searchList = this.searchList = applyDefaults(new JList<>(listModel));
		searchList.setFont(narrowFont);
		searchList.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.VK_ENTER)
					controller.selectList(EditorView.this);
			}
		});
		searchList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() == 2)
					controller.selectList(EditorView.this);
			}
		});

		JLabel rightLabel = this.rightLabel = applyDefaults(new JLabel("Right"));
		rightLabel.setVisible(false);

		JTextField filenameTextField = this.filenameTextField = applyDefaults(new JTextField("pad"));
		filenameTextField.setVisible(false);

		JTextArea messageTextArea = this.messageTextArea = applyDefaults(new JTextArea("Bottom"));
		messageTextArea.setEditable(false);
		messageTextArea.setRows(12);
		messageTextArea.setVisible(false);

		JScrollPane messageScrollPane = this.messageScrollPane = createScrollPane(messageTextArea);

		JEditorPane editor = this.editor = applyDefaults(new JEditorPane());
		editor.getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent event) {
				changed();
			}

			public void insertUpdate(DocumentEvent event) {
				changed();
			}

			public void changedUpdate(DocumentEvent event) {
			}

			private void changed() {
				setModified(true);
			}
		});

		JScrollPane editorScrollPane = createScrollPane(editor);

		JButton okButton = applyDefaults(new JButton("OK"));
		okButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				controller.evaluate(EditorView.this);
			}
		});

		JFrame frame = this.frame = new JFrame(title);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setJMenuBar(createMenuBar());
		frame.setSize(new Dimension(windowWidth, windowHeight));
		frame.setVisible(true);
		frame.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent event) {
				refresh();
			}
		});

		int u = 64, u3 = u * 3;

		lay = new LayoutCalculator(frame.getContentPane());
		layout = lay.box(Orientation.HORIZONTAL //
				, lay.ex(u, lay.box(Orientation.VERTICAL //
						, lay.fx(24, lay.c(searchTextField)) //
						, lay.ex(u, lay.c(searchList)) //
						)) //
				, lay.ex(u3, lay.box(Orientation.VERTICAL //
						, lay.fx(24, lay.c(filenameTextField)) //
						, lay.ex(u3, lay.c(editorScrollPane)) //
						, lay.fx(8, lay.b()) //
						, lay.fx(24, lay.box(Orientation.HORIZONTAL //
								, lay.ex(u3, lay.b()) //
								, lay.fx(64, lay.c(okButton)) //
								, lay.ex(u3, lay.b()) //
								)) //
						, lay.ex(u3, lay.c(messageScrollPane)) //
						)) //
				, lay.ex(u, lay.c(rightLabel)) //
				);

		controller.newFile(this);
		refresh();

		return frame;
	}

	public void setModified(boolean isModified) {
		this.isModified = isModified;
		repaint();
	}

	public void refresh() {
		if (lay != null && layout != null)
			lay.arrange(layout);
		repaint();
	}

	private void repaint() {
		frame.setTitle((isModified ? "* " : "") + filenameTextField.getText().replace(File.separatorChar, '/'));
		frame.repaint();
	}

	private JMenuBar createMenuBar() {
		EditorView view = this;

		JMenuItem newMenuItem = applyDefaults(new JMenuItem("New...", KeyEvent.VK_N));
		newMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
		newMenuItem.addActionListener(event -> controller.newFile(view));

		JMenuItem openMenuItem = applyDefaults(new JMenuItem("Open...", KeyEvent.VK_O));
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		openMenuItem.addActionListener(event -> controller.open(view));

		JMenuItem saveMenuItem = applyDefaults(new JMenuItem("Save", KeyEvent.VK_S));
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		saveMenuItem.addActionListener(event -> controller.save(view));

		JMenuItem searchMenuItem = applyDefaults(new JMenuItem("Search"));
		searchMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		searchMenuItem.addActionListener(event -> controller.searchFor(view));

		JMenuItem exitMenuItem = applyDefaults(new JMenuItem("Exit", KeyEvent.VK_X));
		exitMenuItem.addActionListener(event -> controller.quit(EditorView.this));

		JMenuItem copyMenuItem = applyDefaults(new JMenuItem("Copy"));
		copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
		copyMenuItem.addActionListener(event -> controller.copy(view, false));

		JMenuItem copyAppendMenuItem = applyDefaults(new JMenuItem("Copy Append"));
		copyAppendMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		copyAppendMenuItem.addActionListener(event -> controller.copy(view, true));

		JMenuItem pasteMenuItem = applyDefaults(new JMenuItem("Paste"));
		pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
		pasteMenuItem.addActionListener(event -> controller.paste(view));

		JMenuItem formatMenuItem = applyDefaults(new JMenuItem("Format", KeyEvent.VK_F));
		formatMenuItem.addActionListener(event -> controller.format(EditorView.this));

		JMenuItem unixFilterMenuItem = applyDefaults(new JMenuItem("Unix Filter...", KeyEvent.VK_U));
		unixFilterMenuItem.addActionListener(event -> controller.unixFilter(EditorView.this));

		JMenuItem leftMenuItem = applyDefaults(new JMenuItem("Left", KeyEvent.VK_L));
		leftMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
		leftMenuItem.addActionListener(event -> controller.left(view));

		JMenuItem rightMenuItem = applyDefaults(new JMenuItem("Right", KeyEvent.VK_R));
		rightMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
		rightMenuItem.addActionListener(event -> controller.right(view));

		JMenuItem topMenuItem = applyDefaults(new JMenuItem("Top", KeyEvent.VK_T));
		topMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.ALT_MASK));
		topMenuItem.addActionListener(event -> controller.top(view));

		JMenuItem bottomMenuItem = applyDefaults(new JMenuItem("Bottom", KeyEvent.VK_B));
		bottomMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.ALT_MASK));
		bottomMenuItem.addActionListener(event -> controller.bottom(view));

		JMenuItem evalMenuItem = applyDefaults(new JMenuItem("Evaluate", KeyEvent.VK_E));
		evalMenuItem.addActionListener(event -> controller.evaluate(view));

		JMenuItem evalTypeMenuItem = applyDefaults(new JMenuItem("Evaluate Type", KeyEvent.VK_T));
		evalTypeMenuItem.addActionListener(event -> controller.evaluateType(view));

		JMenuItem newWindowMenuItem = applyDefaults(new JMenuItem("New Window", KeyEvent.VK_N));
		newWindowMenuItem.addActionListener(event -> controller.newWindow(view));

		JMenu fileMenu = createMenu("File", KeyEvent.VK_F //
				, newMenuItem, openMenuItem, saveMenuItem, searchMenuItem, exitMenuItem);

		JMenu editMenu = createMenu("Edit", KeyEvent.VK_E //
				, copyMenuItem, copyAppendMenuItem, pasteMenuItem, formatMenuItem, unixFilterMenuItem);

		JMenu viewMenu = createMenu("View", KeyEvent.VK_V //
				, leftMenuItem, rightMenuItem, topMenuItem, bottomMenuItem);

		JMenu projectMenu = createMenu("Project", KeyEvent.VK_P //
				, evalMenuItem, evalTypeMenuItem);

		JMenu windowMenu = createMenu("Window", KeyEvent.VK_W //
				, newWindowMenuItem, newWindowMenuItem);

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(viewMenu);
		menuBar.add(projectMenu);
		menuBar.add(windowMenu);

		return menuBar;
	}

	private JMenu createMenu(String title, int keyEvent, JMenuItem... menuItems) {
		JMenu menu = applyDefaults(new JMenu(title));
		menu.setMnemonic(keyEvent);

		for (Component component : menuItems)
			menu.add(component);

		return menu;
	}

	private JScrollPane createScrollPane(Component component) {
		return new JScrollPane(component //
				, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED //
				, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
		return messageScrollPane;
	}

	public JList<String> getLeftList() {
		return searchList;
	}

	public JComponent getLeftToolbar() {
		return searchTextField;
	}

	public DefaultListModel<String> getListModel() {
		return listModel;
	}

	public JTextArea getMessageTextArea() {
		return messageTextArea;
	}

	public JComponent getRightToolbar() {
		return rightLabel;
	}

	public JTextField getSearchTextField() {
		return searchTextField;
	}

	public JTextField getFilenameTextField() {
		return filenameTextField;
	}

	public JEditorPane getEditor() {
		return editor;
	}

	public JFrame getFrame() {
		return frame;
	}

}
