package suite.sample;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.table.AbstractTableModel;

/**
 * A boring demonstration of various Swing components.
 */
public class SwingTable {

	public static void main(String args[]) {
		JLabel label = new JLabel("Hello World~~~");

		JTextPane editor = new JTextPane();
		editor.setFont(new Font("Monospac821 BT", Font.PLAIN, 10));

		JTable table = new JTable(new AbstractTableModel() {
			private static final long serialVersionUID = -1;

			@Override
			public int getColumnCount() {
				return 3;
			}

			@Override
			public int getRowCount() {
				return 3;
			}

			@Override
			public Object getValueAt(int row, int col) {
				return row * col;
			}

			@Override
			public boolean isCellEditable(int row, int col) {
				return true;
			}
		});

		JButton button = new JButton("Click Me!");
		button.setMnemonic(KeyEvent.VK_C); // Alt-C as hot key
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				System.out.println("GOT " + event);
			}
		});

		// Flow layout allows the components to be their preferred size
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(label);
		panel.add(editor);
		panel.add(table);
		panel.add(button);

		JFrame frame = new JFrame();
		frame.setContentPane(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// frame.setLocation(200, 200);
		frame.pack(); // Pack it up for display
		frame.setVisible(true);
	}

}
