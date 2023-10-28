package me.vadim.test;

import me.vadim.test.collage.Gui;

import javax.annotation.RegEx;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author vadim
 */
public class SwingUtil {

	@RegEx
	public static final String ILLEGAL_FILENAME_REGEX = "[^A-Za-z0-9._ -]+";

	@RegEx
	public static final String LEGAL_FILENAME_REGEX = "[A-Za-z0-9._ -]+";

	public static JComponent[] createJFileChooser(JFrame parent, boolean textArea, AtomicReference<File> fileReference, String chooserTitle, String buttonText, @MagicConst int fileSelectionMode) {
		return createJFileChooser(parent, textArea, fileReference, chooserTitle, buttonText, fileSelectionMode, (f) -> { });
	}

	/**
	 * @return [ScrollPane (text area), Button]
	 */
	public static JComponent[] createJFileChooser(JFrame parent, boolean textArea, AtomicReference<File> fileReference, String chooserTitle, String buttonText, @MagicConst int fileSelectionMode, Consumer<File> accept) {
		JFileChooser chooser = new JFileChooser(fileReference.get());
		chooser.setFileSelectionMode(fileSelectionMode);
		chooser.setDialogTitle(chooserTitle);
		chooser.setSelectedFile(fileReference.get());

		Optional<JTextArea>   jTextArea;
		Optional<JScrollPane> jScrollPane;
		if (textArea) {
			JTextArea   in     = new JTextArea(fileReference.get().getAbsolutePath());
			JScrollPane scroll = new JScrollPane(in, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			jTextArea   = Optional.of(in);
			jScrollPane = Optional.of(scroll);
			enableTabbing(in);
			scroll.setPreferredSize(in.getPreferredSize());
			scroll.setSize(in.getPreferredSize());
			scroll.setAutoscrolls(true);
			in.addKeyListener(new KeyListener() {
				@Override
				public void keyTyped(KeyEvent e) {

				}

				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						File f = new File(in.getText());
						fileReference.set(f);

						if (f.isDirectory()) chooser.setCurrentDirectory(f);
						if (f.isFile()) chooser.setSelectedFile(f);

						accept.accept(f);
						e.consume();
					}
				}

				@Override
				public void keyReleased(KeyEvent e) {

				}
			});
		} else {
			jTextArea   = Optional.empty();
			jScrollPane = Optional.empty();
		}

		JButton button = new JButton(buttonText);
		button.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				chooser.updateUI();

				File f;
				if (textArea) {
					f = new File(jTextArea.orElseThrow().getText());
					fileReference.set(f);
				} else {
					f = fileReference.get();
				}

				if (f.isDirectory()) chooser.setCurrentDirectory(f);
				if (f.isFile()) chooser.setSelectedFile(f);

				if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
					f = chooser.getSelectedFile();
					fileReference.set(f);
					if (textArea) jTextArea.orElseThrow().setText(f.getAbsolutePath());
					accept.accept(f);
				}
			}
		});

		return new JComponent[] { jScrollPane.orElse(null), button };
	}

	public static BufferedImage cacheImage(String name) {
		BufferedImage temp;

		try {
			temp = ImageIO.read(Gui.class.getResourceAsStream('/' + name));
		} catch (Exception e) {
			temp = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
		}

		return temp;
	}

	public static void lookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			for (Window window : JFrame.getWindows()) {
				SwingUtilities.updateComponentTreeUI(window);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void lookAndFeel(Window window) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			SwingUtilities.updateComponentTreeUI(window);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void enableTabbing(JComponent component) {
		AbstractAction transferFocus = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				((Component) e.getSource()).transferFocus();
			}
		};
		component.getInputMap().put(KeyStroke.getKeyStroke("TAB"), "transferFocus");
		component.getActionMap().put("transferFocus", transferFocus);
	}

	public static void delay(int delayms, Runnable run) {
		Timer t = new Timer(delayms, null);
		ActionListener a = (x) -> {
			t.stop();
			run.run();
		};
		t.addActionListener(a);
		t.start();
	}

}
