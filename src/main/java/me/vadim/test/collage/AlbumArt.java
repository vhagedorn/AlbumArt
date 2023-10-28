package me.vadim.test.collage;

import me.vadim.test.MagicConst;
import me.vadim.test.collage.impl.ImageMut;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author vadim
 */
public class AlbumArt {

	// constants & utils

	public static final int NO_RANDOM = 0;
	public static final int X_AXIS = 1;
	public static final int Y_AXIS = 2;

	public static boolean inBounds(double x, double y, double w, double h) { return !(x > w || y > h || x < 0 || y < 0); }

	//eg:
	//15->15
	//	00001111->00001111
	//39->63
	//	00100111->00111111
	public static int calcScaleMask(int scale) {
		for (int i = 32; i >= 0; i--) {
			if ((scale & (1 << (i - 1))) >= 1) {
				int mask = 0;
				for (int j = 0; j < i; j++) mask |= 1 << j;
				return mask;
			}
		}
		return 0;
	}

	// entry point

	public static void main(String[] args) throws Exception {
		if (args.length >= 1 && args[0].equals("nogui")) {
			System.out.println("\tStarting in headless mode.");

			String wd = ".";
			if (args.length < 2)
				System.out.println("Did not specify a target directory, using CWD.");
			else
				wd = args[1];

			tryScale(wd);
		} else {
			System.out.println("\trun with parameter `nogui` to enter headless mode.");
			new Gui();
		}
	}

	// primitive command line application
	public static void tryScale(String workingDirectory) throws IOException {
		// cache images to create multiple renders quickly
		File wd = new File(workingDirectory);
		if (!wd.isDirectory()) {
			System.out.println("Working directory " + wd.getPath() + " does not exist!");
			return;
		}
		System.out.println("Loading images from '" + wd.getPath() + "'...");
		BufferedImage[] img = cache(wd);
		if (img.length == 0) {
			System.out.println("No images in target folder. Please make sure that there are images in '" + wd.getPath() + "' and you are in the right directory.");
			return;
		}
		System.out.println("Cached " + img.length + " images.");
		System.out.println("Press ENTER to continue.");

		// loop to try different scales conveniently
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String         line;

		double w = -1, h = -1; // 1920,1080; 1080,2340

		int mode   = -1;
		int random = NO_RANDOM;

		while (!(line = reader.readLine()).equals("exit")) {
			System.out.println("\nType 'exit' to quit.");
			line = line.strip().toUpperCase();

			int in = -1;
			try {
				in = Integer.parseInt(line);
			} catch (NumberFormatException x) {
				if (mode == 2) {
					in = NO_RANDOM;
					if (line.equals("X")) in = X_AXIS;
					if (line.equals("Y")) in = Y_AXIS;
				} else if (mode >= 0) {
					System.out.println("Please enter a number.");
					continue;
				}
			}

			if (mode >= 0 && mode < 3 && in < 0) {
				System.out.println("Enter a valid number!");
				continue;
			}

			switch (mode) {
				case -1 -> {
					System.out.print("Enter target resolution WIDTH: ");
					mode++;
				}
				case 0 -> {
					w = in;
					System.out.print("Enter target resolution HEIGHT: ");
					mode++;
				}
				case 1 -> {
					h = in;
					System.out.print("Use random offsets? (EXPERIMENTAL: you'll have to crop the image) X/Y/no ");
					mode++;
				}
				case 2 -> {
					random = in;
					System.out.println("Entering render mode. Repeat the following step until you are satisfied with the result.");
					System.out.print("Enter a scale (-1 for AUTO): ");
					mode++;
				}
				case 3 -> { // main image generation loop
					int scale = in;
					// shuffle the images for unique results
					List<BufferedImage> temp = Arrays.asList(img);
					Collections.shuffle(temp);
					BufferedImage[] images = temp.toArray(BufferedImage[]::new);

					BufferedImage generated = rpaste(w, h, images, scale, random, new ImageMut(Color.BLACK), System.nanoTime());

					File out = new File("scale-" + (scale == -1 ? "auto" : scale) + "-[" + images.hashCode() + "].png");
					if (!out.getParentFile().exists()) out.getParentFile().mkdirs();

					ImageIO.write(generated, "png", out);

					System.out.println("Saved to " + out.getAbsolutePath());
					System.out.print("Enter a scale (-1 for AUTO): ");
				}
			}
		}
	}

	private static BufferedImage[] cache(File dir) throws IOException {
		File[] f = dir.listFiles(pathname -> pathname.toString().endsWith(".png") || pathname.toString().endsWith(".jfif"));
		if (f == null || f.length == 0) return new BufferedImage[0];
		BufferedImage[] img = new BufferedImage[f.length];

		for (int i = 0; i < f.length; i++)
			 img[i] = ImageIO.read(f[i]);

		return img;
	}

	// main rendering logic

	public static int autoscale(double w, double h, double d, double theta, int expect) {
		int result;
		int s = 1;
		//Some of the ugliest code know to man. Enjoy reading through this!
		while (true) {//top-level, use area to guess the approximate scale
			result = (int) Math.round((w / s) * (h / s));
			if (result < expect) {//now we're close, but area didn't take the janky rotations
				int i = expect;
				while (i >= expect) {//now let's narrow it down with the actual looping code used in rotRev4
					i = 0;
					float last = 0;
					for (double x = 0; x < d / s; x++) {
						for (double y = last -= 1; y < (d / s) + last; y++) {
							double deltaX = x * Math.cos(theta) - y * Math.sin(theta),
									deltaY = x * Math.sin(theta) + y * Math.cos(theta);

							deltaX *= s;
							deltaY *= s;

							if (inBounds(deltaX, deltaY, w, h) ||
								inBounds(deltaX - s, deltaY + s, w, h) ||
								inBounds(deltaX - s, deltaY, w, h) ||
								inBounds(deltaX, deltaY + s, w, h)) i++;
						}
					}
					s++;
				}
				break;
			}
			s++;
		}
		return ++s;
	}

	public static BufferedImage rpaste(double w, double h, BufferedImage[] images, int scale, @MagicConst int random, ImageMutation imut, long seed) throws IOException {
		Dimension resolution = new Dimension((int) w, (int) h);

		// find diagonal upon which the canvas aligned
		double d        = Math.max(w, h) * Math.sqrt(2); // diagonal of encapsulating square
		int    diagonal = (int) Math.round(d);

		// pick the largest angle (it won't work the with the smaller one)
		double theta = Math.max(Math.asin(h / d), Math.asin(w / d));

		if (scale == -1) {
			scale = autoscale(w, h, d, theta, images.length);
		} else {
			scale = diagonal / scale;
		}

		BufferedImage   art = new BufferedImage((int) w, (int) h, BufferedImage.TYPE_INT_RGB);
		Graphics2D      g2d = ((Graphics2D) art.getGraphics());
		AffineTransform transform;

		g2d.translate(imut.translationDelta().x, imut.translationDelta().y);
		g2d.setColor(imut.background());
		g2d.fillRect(0, 0, resolution.width, resolution.height);

		g2d.setStroke(new BasicStroke(3f));

		int    i    = 0;
		float  last = 0;
		Random rng  = new Random(seed);
		for (double x = 0; x < d / scale; x++) { // iterate over (unscaled) X until the diagonal {d/scale}
			int r = (int) (random == Y_AXIS ? Math.round(rng.nextFloat() * scale) : 0); // y-random
//			System.out.println(last + "," + (d/scale));
			for (double y = last -= 1; y < (d / scale) + last; y++) { // iterate over (unscaled) Y, staring further outside each time in order to
//				System.out.println(y);
				// fill the rect
				r = (int) (random == X_AXIS ? Math.round(new Random(Math.round(y) ^ seed).nextFloat() * 10000f) : r);//x-random
				r &= calcScaleMask(scale);
				r %= scale;
				// seed the random for the current Y-value (since the loop goes the other way), and use that to get X-axis randomization
				// !not necessary if you want Y-axis randomization!
				// basically the random didn't have enough play in the higher-order bits, so:
				//	multiply (shift) it over to access the lower-order (decimal) bits, then
				//	mask the part that we need (the place-values included in the scale, hence `calcScaleMask`)

				transform = g2d.getTransform();

				int xc = (int) Math.round(x * scale),
						yc = (int) Math.round(y * scale);

				double deltaX = x * Math.cos(theta) - y * Math.sin(theta),
						deltaY = x * Math.sin(theta) + y * Math.cos(theta);

				deltaX *= scale;
				deltaY *= scale;

				// only proceed if we're in bounds
				boolean inB = inBounds(deltaX, deltaY, w, h) ||
							  inBounds(deltaX - scale, deltaY + scale, w, h) ||
							  inBounds(deltaX - scale, deltaY, w, h) ||
							  inBounds(deltaX, deltaY + scale, w, h);

				if (inB) {
					g2d.setColor(Color.WHITE);
					// rotate around origin
					g2d.rotate(theta);

					// draw image on rotated coordinate
					if (i < images.length) g2d.drawImage(images[i++], xc - (random == X_AXIS ? r : 0), yc - (random == Y_AXIS ? r : 0), scale, scale, null);
				}

				g2d.setTransform(transform);
			}
		}

		// post processing (zoom)
		Dimension zoomed = new Dimension(-1, (int) h + imut.zoomAmount());
		ImageMutation.aspectRatio(resolution, zoomed);

		BufferedImage post       = new BufferedImage(resolution.width, resolution.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D    processing = post.createGraphics();
		processing.setColor(imut.background());
		processing.fillRect(0, 0, resolution.width, resolution.height);
		processing.drawImage(art, 0, 0, zoomed.width, zoomed.height, null);

		return post;
	}

}

