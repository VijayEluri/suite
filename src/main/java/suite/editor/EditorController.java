package suite.editor;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.DefaultListModel;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import suite.Suite;
import suite.node.Node;
import suite.node.io.Formatter;
import suite.node.pp.PrettyPrinter;
import suite.os.FileUtil;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;
import suite.util.To;
import suite.util.Util;

public class EditorController {

	private Thread runThread;
	private EditorModel model;
	private EditorView view;

	public EditorController() {
	}

	public void _init(EditorModel model, EditorView view, EditorController controller) {
		this.model = model;
		this.view = view;

		model.getModifiedChanged().register(b -> view.repaint());
	}

	public void bottom() {
		view.toggleBottom();
	}

	public void close() {
		confirmSave(view.getFrame()::dispose);
	}

	public void copy(boolean isAppend) {
		ClipboardUtil clipboardUtil = new ClipboardUtil();
		String selectedText = view.getEditor().getSelectedText();
		if (selectedText != null)
			clipboardUtil.setClipboardText((isAppend ? clipboardUtil.getClipboardText() : "") + selectedText);
	}

	public void downToSearchList() {
		JList<String> leftList = view.getLeftList();
		DefaultListModel<String> listModel = view.getListModel();

		leftList.requestFocusInWindow();

		if (!listModel.isEmpty())
			leftList.setSelectedValue(listModel.get(0), true);
	}

	public void evaluate() {
		run(text -> {
			String result;
			try {
				Node node = Suite.evaluateFun(text, true);
				result = Formatter.dump(node);
			} catch (Exception ex) {
				result = To.string(ex);
			}
			return result;
		});
	}

	public void evaluateType() {
		run(text -> {
			String result;
			try {
				Node node = Suite.evaluateFunType(text);
				result = Formatter.dump(node);
			} catch (Exception ex) {
				result = To.string(ex);
			}
			return result;
		});
	}

	public void format() {
		JEditorPane editor = view.getEditor();
		Node node = Suite.parse(editor.getText());
		editor.setText(new PrettyPrinter().prettyPrint(node));
	}

	public void funFilter() {
		boolean isDo = false;
		JFrame frame = view.getFrame();
		JEditorPane editor = view.getEditor();

		String fun = JOptionPane.showInputDialog(frame //
				, "Enter " + (isDo ? "do " : "") + "function:", "Functional Filter", JOptionPane.PLAIN_MESSAGE);

		editor.setText(Suite.evaluateFilterFun(fun, editor.getText(), false, false));
	}

	public void left() {
		view.toggleLeft();
	}

	public void newFile() {
		confirmSave(() -> {
			JEditorPane editor = view.getEditor();
			editor.setText("");
			editor.requestFocusInWindow();

			model.changeFilename("pad");
			model.changeModified(false);
		});
	}

	public void newWindow() {
		EditorModel model = new EditorModel();
		EditorController controller = new EditorController();
		EditorView view1 = new EditorView();

		view1._init(model, view1, controller);
		controller._init(model, view1, controller);

		view1.run(controller, Editor.class.getSimpleName());
	}

	public void open() {
		confirmSave(() -> {
			File dir = new File(model.getFilename()).getParentFile();
			JFileChooser fileChooser = dir != null ? new JFileChooser(dir) : new JFileChooser();
			if (fileChooser.showOpenDialog(view.getFrame()) == JFileChooser.APPROVE_OPTION)
				load(fileChooser.getSelectedFile().getPath());
		});
	}

	public void paste() {
		JEditorPane editor = view.getEditor();
		String orig = editor.getText();
		String pasteText = new ClipboardUtil().getClipboardText();

		if (pasteText != null) {
			int s = editor.getSelectionStart();
			int e = editor.getSelectionEnd();
			editor.setText(orig.substring(0, s) + pasteText + orig.substring(e, orig.length()));
			editor.setCaretPosition(s + pasteText.length());
		}
	}

	public void save() {
		try (OutputStream os = FileUtil.out(model.getFilename())) {
			os.write(view.getEditor().getText().getBytes(FileUtil.charset));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		model.changeModified(false);
	}

	public void right() {
		view.toggleRight();
	}

	public void searchFor() {
		view.focusSearchTextField();
	}

	public void searchFiles() {
		DefaultListModel<String> listModel = view.getListModel();
		listModel.clear();

		String text = model.getSearchText();

		if (!text.isEmpty()) {
			Streamlet<String> files = FileUtil.findPaths(Paths.get(".")) //
					.map(Path::toString) //
					.filter(filename -> filename.contains(text));

			for (String filename : files)
				listModel.addElement(filename);
		}
	}

	public void selectList() {
		load(view.getLeftList().getSelectedValue());
	}

	public void top() {
		view.toggleTop();
	}

	public void unixFilter() {
		JFrame frame = view.getFrame();
		JEditorPane editor = view.getEditor();

		String command = JOptionPane.showInputDialog(frame //
				, "Enter command:", "Unix Filter", JOptionPane.PLAIN_MESSAGE);

		try {
			Process process = Runtime.getRuntime().exec(command);

			try (OutputStream pos = process.getOutputStream(); Writer writer = new OutputStreamWriter(pos, FileUtil.charset)) {
				writer.write(editor.getText());
			}

			process.waitFor();

			editor.setText(To.string(process.getInputStream()));
		} catch (IOException | InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void load(String filename) {
		try {
			model.changeFilename(filename);

			String text = FileUtil.read(filename);

			JEditorPane editor = view.getEditor();
			editor.setText(text);
			editor.setCaretPosition(0);
			editor.requestFocusInWindow();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		model.changeModified(false);
	}

	private void confirmSave(Runnable action) {
		JFrame frame = view.getFrame();
		if (model.getIsModified())
			switch (JOptionPane.showConfirmDialog(frame //
					, "Would you like to save your changes?", "Close" //
					, JOptionPane.YES_NO_CANCEL_OPTION)) {
			case JOptionPane.YES_OPTION:
				save();
			case JOptionPane.NO_OPTION:
				action.run();
				break;
			default:
			}
		else
			action.run();
	}

	private void run(Fun<String, String> fun) {
		JEditorPane editor = view.getEditor();
		String selectedText = editor.getSelectedText();
		String text = selectedText != null ? selectedText : editor.getText();

		if (runThread == null || !runThread.isAlive())
			(runThread = Util.startThread(() -> {
				JTextArea bottomTextArea = view.getMessageTextArea();
				bottomTextArea.setEnabled(false);
				bottomTextArea.setText("RUNNING...");

				String result = fun.apply(text);

				bottomTextArea.setText(result);
				bottomTextArea.setEnabled(true);
				bottomTextArea.setVisible(true);

				view.refresh();
				view.getEditor().requestFocusInWindow();
			})).start();
		else
			JOptionPane.showMessageDialog(view.getFrame(), "Previous evaluation in progress");
	}

}
