package me.vadim.test.collage;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static me.vadim.test.SwingUtil.*;
import static me.vadim.test.collage.AlbumArt.*;

/**
 * @author vadim
 */
public class Gui {

	private final        JFrame        frame;
	private static final BufferedImage villain = cacheImage("icon3.png");
	private static final BufferedImage whoops  = cacheImage("ohno.png");

	static String fmt(String[] fmt, int value)    { return fmt(fmt, String.valueOf(value)); }

	static String fmt(String[] fmt, String value) { return fmt[1].replace(fmt[0], value); }

	JTextArea[]           texts              = new JTextArea[2];//[w,h]
	String[]              scaleFmt           = {"$", "Scale ($)"};
	AtomicInteger         scale              = new AtomicInteger(0);
	JLabel                preview            = new JLabel();
	PreviewListener       previewListener    = new PreviewListener(this);
	//todo: fix starting dir
	AtomicReference<File> folder             = new AtomicReference<>(new File(new File("content/revisions/py 3/pics").getAbsolutePath()));//in
	AtomicReference<File> file               = new AtomicReference<>(new File("content/s.png"));//out
	AtomicInteger         random             = new AtomicInteger(NO_RANDOM);
	String[]              imgCtFmt           = {"$", "$ images"};
	JLabel                pictureCount       = new JLabel(fmt(imgCtFmt, 0), SwingConstants.CENTER);
	JSlider[]             translationSliders = new JSlider[3];

	public Gui() {
		frame = new JFrame("Album Art Collage");

		//pane
		Container pane = frame.getContentPane();

		pane.setLayout(new GridBagLayout());

		GridBagConstraints _pane = new GridBagConstraints();
		_pane.anchor = GridBagConstraints.FIRST_LINE_START;

		JPanel left = new JPanel(new GridBagLayout());
		{
			GridBagConstraints _left = new GridBagConstraints();

			_left.anchor = GridBagConstraints.FIRST_LINE_START;
			_left.fill   = GridBagConstraints.HORIZONTAL;

			JPanel options = new JPanel(new GridBagLayout());
			{
				GridBagConstraints _options = new GridBagConstraints();
				_options.anchor = GridBagConstraints.FIRST_LINE_START;
				_options.fill   = GridBagConstraints.HORIZONTAL;

				options.setBorder(new TitledBorder("Options"));

				JPanel resolution = new JPanel(new GridBagLayout());
				{
					GridBagConstraints _resolution = new GridBagConstraints();
					_resolution.anchor = GridBagConstraints.FIRST_LINE_START;
					_resolution.fill   = GridBagConstraints.BOTH;

					resolution.setBorder(new TitledBorder("Resolution"));

					for (int i = 0; i < 2; i++) {
						JTextArea text = texts[i] = new JTextArea(1, 4);

						text.setText(i == 0 ? "1920" : "1080");

						JScrollPane scroll = new JScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
						scroll.setAutoscrolls(true);

						text.addKeyListener(new KeyListener() {
							@Override
							public void keyTyped(KeyEvent e) {
								//enforce numbers only
								if (!Character.isDigit(e.getKeyChar()) && !(e.getKeyCode() == KeyEvent.VK_BACK_SPACE)) e.consume();
							}

							@Override
							public void keyPressed(KeyEvent e) {
								//disable paste
								if ((e.isControlDown() && e.getKeyCode() == KeyEvent.VK_V) || e.getKeyCode() == KeyEvent.VK_ENTER) e.consume();
							}

							@Override
							public void keyReleased(KeyEvent e) { }
						});

						enableTabbing(text);
					}

					_resolution.gridx = 0;
					_resolution.gridy = 0;
					resolution.add(texts[0], _resolution);
					_resolution.gridx++;
					resolution.add(new JLabel("x"), _resolution);
					_resolution.gridx++;
					resolution.add(texts[1], _resolution);
				}
				_options.gridx = 0;
				_options.gridy = 0;
				options.add(resolution, _options);

				JPanel misc = new JPanel(new GridBagLayout());
				{
					GridBagConstraints _misc = new GridBagConstraints();
					_misc.anchor = GridBagConstraints.FIRST_LINE_START;
					_misc.fill   = GridBagConstraints.BOTH;

					//2nd row
					JPanel       randomAxis = new JPanel(new GridBagLayout());
					ButtonGroup  group      = new ButtonGroup();
					final String x_axis     = "X";
					final String y_axis     = "Y";
					{
						GridBagConstraints _randomAxis = new GridBagConstraints();
						_randomAxis.fill   = GridBagConstraints.HORIZONTAL;
						_randomAxis.anchor = GridBagConstraints.FIRST_LINE_START;

						randomAxis.setBorder(new TitledBorder("Axis"));

						ActionListener a = (event) -> {
							switch (event.getActionCommand()) {
								case x_axis -> random.set(X_AXIS);
								case y_axis -> random.set(Y_AXIS);
								default -> random.set(NO_RANDOM);
							}
							reSeed();
							render(false);
						};

						_randomAxis.gridx = 0;
						_randomAxis.gridy = 0;
						for (int i = X_AXIS; i <= Y_AXIS; i++) {//x & y random axis radio buttons
							JRadioButton axis = new JRadioButton(i == X_AXIS ? x_axis : y_axis);
							axis.setMnemonic(i == X_AXIS ? KeyEvent.VK_X : KeyEvent.VK_Y);
							axis.setActionCommand(i == X_AXIS ? x_axis : y_axis);
							axis.addActionListener(a);
							axis.setEnabled(random.get() != NO_RANDOM);

							group.add(axis);
							group.setSelected(axis.getModel(), i == X_AXIS);

							randomAxis.add(axis, _randomAxis);
							_randomAxis.gridx++;
						}
					}
					_misc.gridx = 0;
					_misc.gridy = 1;
					misc.add(randomAxis, _misc);

					_misc.gridx++;
					misc.add(pictureCount, _misc);

					//1st row
					JCheckBox randomize = new JCheckBox("Apply random offsets");
					randomize.addActionListener(new AbstractAction() {
						@Override
						public void actionPerformed(ActionEvent e) {
							boolean r = randomize.isSelected();
							group.getElements().asIterator().forEachRemaining((it) -> it.setEnabled(r));
							if (r)
								switch (group.getSelection().getActionCommand()) {
									case x_axis -> random.set(X_AXIS);
									case y_axis -> random.set(Y_AXIS);
									default -> random.set(NO_RANDOM);
								}
							else random.set(NO_RANDOM);
							reSeed();
							render(false);
						}
					});
					_misc.gridx = 0;
					_misc.gridy = 0;
					misc.add(randomize, _misc);

					JButton shuffle = new JButton("Shuffle images");
					shuffle.addActionListener(new AbstractAction() {
						@Override
						public void actionPerformed(ActionEvent e) {
							List<BufferedImage> temp = Arrays.asList(cache);
							Collections.shuffle(temp);
							cache = temp.toArray(BufferedImage[]::new);
							render(false);
						}
					});
					_misc.gridx++;
					misc.add(shuffle, _misc);

					JButton go = new JButton("Go!");//re-render
					go.addActionListener(new AbstractAction() {
						@Override
						public void actionPerformed(ActionEvent e) {
							render(true);
						}
					});
					_misc.gridx++;
					misc.add(go, _misc);
				}
				_options.gridx++;
				options.add(misc, _options);
			}
			_left.gridx = 0;
			_left.gridy = 0;
			left.add(options, _left);

			JPanel io = new JPanel(new GridBagLayout());
			io.setBorder(new TitledBorder("I/O"));
			{
				GridBagConstraints _io = new GridBagConstraints();

				JPanel input = new JPanel(new GridBagLayout());
				input.setBorder(new TitledBorder("Album art directory"));
				{
					GridBagConstraints _input = new GridBagConstraints();
					_input.fill   = GridBagConstraints.BOTH;
					_input.anchor = GridBagConstraints.FIRST_LINE_START;

					JComponent[] chooser =  createJFileChooser(frame, true, folder, "Choose album art folder", "Choose Folder", JFileChooser.DIRECTORIES_ONLY, this::cacheFolder);

					_input.gridx = 0;
					_input.gridy = 0;
					input.add(chooser[0], _input);

					_input.gridx++;
					input.add(chooser[1], _input);
				}
				_io.gridx = 0;
				_io.gridy = 0;
				io.add(input, _io);

				JPanel output = new JPanel(new GridBagLayout());
				{
					GridBagConstraints _output = new GridBagConstraints();
					_output.anchor = GridBagConstraints.NORTH;
					_output.fill   = GridBagConstraints.HORIZONTAL;

					JComponent[] chooser = createJFileChooser(frame, false, file, "Choose output file", "Save Render", JFileChooser.FILES_ONLY, (f) -> {
						if (renderedImage == null)
							JOptionPane.showMessageDialog(frame, "Error: No render to save.", "Error!", JOptionPane.ERROR_MESSAGE);
						else {
							Dimension r = resolution();
							JOptionPane.showMessageDialog(frame, "Rendering at resolution " + r.width + "x" + r.height + ".", "Rendering", JOptionPane.INFORMATION_MESSAGE);
							saveRender();
							JOptionPane.showMessageDialog(frame, "Saved the render to: " + f.getAbsolutePath(), "Success!", JOptionPane.INFORMATION_MESSAGE);
						}
					});

					_output.gridx = 0;
					_output.gridy = 0;
					output.add(chooser[1], _output);
				}
				_io.gridy++;
				io.add(output, _io);
			}
			_left.gridy++;
			left.add(io, _left);

			JPanel transform = new JPanel(new GridBagLayout());
			{
				transform.setBorder(new TitledBorder("Transform Controls"));

				GridBagConstraints _transform = new GridBagConstraints();
				_transform.anchor = GridBagConstraints.ABOVE_BASELINE;
				_transform.fill   = GridBagConstraints.NONE;

				_transform.gridy = 0;
				_transform.gridx = 1;
				transform.add(new JLabel());
				_transform.gridx++;
				transform.add(new JLabel("Drag and scroll on the preview to change these."));

				ChangeListener c = (x) -> {
					if(previewListener.dragging) return;

					JSlider src = ((JSlider) x.getSource());
					switch (src.getName()) {
						case "X" -> previewListener.translateX = src.getValue();
						case "Y" -> previewListener.translateY = src.getValue();
						case "Zoom" -> previewListener.zoomW = src.getValue();
					}
					render(false);
				};

				final String[] str   = {"X", "Y", "Zoom"};
				int[][]        range = {{-1000, 1000}, {-1000, 1000}, {0, 500}};
				for (int i = 0; i < str.length; i++) {
					JSlider slider = translationSliders[i] = new JSlider(JSlider.HORIZONTAL);
					slider.setName(str[i]);
					slider.setMinimum(range[i][0]);
					slider.setMaximum(range[i][1]);
					slider.setMinorTickSpacing(range[i][1] / 100);
					slider.setMajorTickSpacing(range[i][1] / 10);
					slider.setSnapToTicks(true);
					slider.setValue(0);
					slider.addChangeListener(c);

					JButton reset = new JButton("Reset");
					reset.addActionListener((x) -> slider.setValue(0));

					_transform.gridx = 0;
					_transform.gridy++;
					transform.add(new JLabel(str[i]), _transform);
					_transform.gridx++;
					transform.add(slider, _transform);
					_transform.gridx++;
					transform.add(reset, _transform);
				}

				_transform.gridx = 1;
				_transform.gridy++;
				JButton reset = new JButton("Reset All");
				reset.addActionListener((x) -> {
					previewListener.zoomW      = 0;
					previewListener.translateX = 0;
					previewListener.translateY = 0;
					for (JSlider slider : translationSliders) slider.setValue(0);
				});
				transform.add(reset, _transform);
			}
			_left.gridy++;
			left.add(transform, _left);
		}
		_pane.gridx = 0;
		_pane.gridy = 0;
		pane.add(left, _pane);

		JPanel right = new JPanel(new GridBagLayout());
		{
			GridBagConstraints _right = new GridBagConstraints();

			JPanel top = new JPanel(new GridBagLayout());
			{
				GridBagConstraints _top = new GridBagConstraints();
				_top.anchor = GridBagConstraints.ABOVE_BASELINE;
				_top.fill   = GridBagConstraints.NONE;

				JLabel scaleLabel = new JLabel(fmt(scaleFmt, "Auto"));
				_top.gridx = 1;
				_top.gridy = 0;
				top.add(scaleLabel, _top);

				JLabel label = new JLabel("Auto");
				_top.gridx = 0;
				_top.gridy = 1;
				top.add(label, _top);

				label      = new JLabel("Manual");
				_top.gridx = 2;
				_top.gridy = 1;
				top.add(label, _top);

				JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 50, 0);
				slider.setMinorTickSpacing(1);
				slider.setMajorTickSpacing(5);
				slider.setSnapToTicks(true);
				slider.addChangeListener((e) -> {
					int value = slider.getValue();
					scale.set(value);
					scaleLabel.setText(fmt(scaleFmt, value == 0 ? "Auto" : String.valueOf(value)));
					render(false);
				});
				_top.gridx = 1;
				_top.gridy = 1;
				top.add(slider, _top);
			}
			_right.gridx = 0;
			_right.gridy = 0;
			right.add(top, _right);

			JPanel bottom = new JPanel(new GridBagLayout());
			bottom.setBorder(new LineBorder(Color.GRAY));
			{
				postImage(villain, previewListener.withBackground(preview.getBackground()));

				preview.addMouseListener(previewListener);
				preview.addMouseMotionListener(previewListener);
				preview.addMouseWheelListener(previewListener);

				GridBagConstraints _bottom = new GridBagConstraints();
				_bottom.anchor = GridBagConstraints.ABOVE_BASELINE;
				_bottom.fill   = GridBagConstraints.VERTICAL;
				_bottom.gridx  = 0;
				_bottom.gridy  = 0;
				bottom.add(preview, _bottom);
			}
			_right.gridy = 1;
			right.add(bottom, _right);
		}
		_pane.gridx++;
		pane.add(right, _pane);


		lookAndFeel();
//		frame.setPreferredSize();
		frame.setIconImage(villain);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		render(false);
	}

	int scale() { return scale.get(); }

	Dimension resolution() {
		//there are already sanitized
		int w = Integer.parseInt(texts[0].getText());
		int h = Integer.parseInt(texts[1].getText());

		return new Dimension(w, h);
	}

	Dimension scaled() {
		Dimension original = resolution();
		Dimension desired  = new Dimension(-1, 500);
		ImageMutation.aspectRatio(original, desired);

		desired.width  = Math.max(desired.width, 1);
		desired.height = Math.max(desired.height, 1);

		return desired;
	}

	static boolean set = false;

	void postImage(BufferedImage render, ImageMutation mut) {
		//scale down the actual to a reasonable size
		Dimension desired = scaled();
		if (!set) {
			preview.setSize(desired);
//			set = true;
		}
		preview.setPreferredSize(desired);
		frame.pack();

		BufferedImage size = new BufferedImage(desired.width, desired.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D    g2d  = size.createGraphics();
		g2d.setColor(mut.background());//only post-processing here is bg, everything else was done in rotRev4
		g2d.fillRect(0, 0, desired.width, desired.height);
		g2d.drawImage(render, 0, 0, desired.width, desired.height, null);

		preview.setIcon(new ImageIcon(size));//full size is saved to file, scaled in preview so that it doesn't run away
	}

	public void saveRender(){
		render(true);

		File f = file.get();
		if(!f.exists()) {
			try {
				f.getParentFile().mkdirs();
				f.createNewFile();
			}catch (IOException e){
				e.printStackTrace();
				return;
			}
		}

		String ext;
		try {
			String[] split = f.getName().split("\\.");
			ext = split[split.length - 1];
		} catch (Exception x) {
			ext = "png";
		}
		try {
			ImageIO.write(renderedImage, ext, f);
		} catch (IOException x) {
			x.printStackTrace();
		}
	}

	public void reSeed() {
		seed = System.nanoTime();
	}

	BufferedImage renderedImage;
	long          seed = System.nanoTime();

	public void render(boolean full) {
		Dimension resolution = full ? resolution() : scaled();//performance boost for ui
		int       scale      = scale();

		if (cache == null || cache.length == 0) {//first run (no folder selected)
			cacheFolder(folder.get());
		}

		ImageMutation imut = previewListener.withBackground(full ? Color.BLACK : Color.RED);
		if (full) {
			Dimension scaled = scaled();
			int       x      = previewListener.translateX * (resolution.width / scaled.width);
			int       y      = previewListener.translateY * (resolution.height / scaled.height);

			Dimension zoomDim = resolution.width < resolution.height ? new Dimension(previewListener.zoomW, -1 ) : new Dimension(-1, previewListener.zoomW);
			ImageMutation.aspectRatio(resolution, zoomDim);

			imut = new ImageMut(resolution.width < resolution.height ? zoomDim.height : zoomDim.width, new Point(x, y), Color.BLACK);
		}

		BufferedImage result = whoops;
		try {
			//minimum on scale slider is 0 but method takes -1
			if (cache.length == 0) throw new IllegalArgumentException("Folder has no images");
			renderedImage = result = AlbumArt.rotRev4(resolution.getWidth(), resolution.getHeight(), cache, scale == 0 ? -1 : scale, random.get(), imut, seed);
		} catch (Exception e) {
			e.printStackTrace();
			renderedImage = null;
		} finally {
			postImage(result, imut);
		}
	}

	BufferedImage[] cache = new BufferedImage[0];

	public void cacheFolder(File folder) {
		JDialog popup = new JDialog(frame, "Loading...");
		popup.setLayout(new GridBagLayout());
		GridBagConstraints g = new GridBagConstraints();
		g.gridx  = 0;
		g.gridy  = 0;
		g.anchor = GridBagConstraints.FIRST_LINE_START;
		g.fill   = GridBagConstraints.HORIZONTAL;

		JLabel label = new JLabel("Loading images...");
		popup.add(label, g);

		JProgressBar bar = new JProgressBar();
		bar.setStringPainted(true);
		g.gridy++;
		popup.add(bar, g);

		if (folder.isDirectory()) {
			File[] files = folder.listFiles();
			if (files != null) {
				bar.setMinimum(0);
				bar.setMaximum(files.length);
			}
		}

		lookAndFeel(popup);
		popup.setLocationRelativeTo(null);
		popup.pack();
		popup.setVisible(true);

		bar.setValue(0);

		new Thread(() -> {
			try {
				cache = readImages(folder, bar).values().toArray(BufferedImage[]::new);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();

		pictureCount.setText(fmt(imgCtFmt, cache.length));
		popup.setVisible(false);
		if (cache.length > 0)
			JOptionPane.showMessageDialog(frame, "Success! Ready to generate collages.", "Success!", JOptionPane.INFORMATION_MESSAGE);
		else
			JOptionPane.showMessageDialog(frame, "Failure! Make sure that you select a folder with pictures in it.", "Failure!", JOptionPane.ERROR_MESSAGE);
	}

	static Map<File, BufferedImage> readImages(File folder, JProgressBar update) {
		if (!folder.isDirectory()) return Collections.emptyMap();

		Map<File, BufferedImage> images = new HashMap<>();
		File[]              files  = folder.listFiles();
		if (files != null)
			for (File file : files) {
				BufferedImage image = null;
				try {
					image = ImageIO.read(file);
				} catch (IOException ignored) {}

				if (image != null) images.put(file, image);

				if (update != null) SwingUtilities.invokeLater(() -> update.setValue(update.getValue() + 1));
			}

		return images;
	}

}
