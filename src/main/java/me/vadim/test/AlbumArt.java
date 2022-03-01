package me.vadim.test;

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

/**
 * @author vadim
 */
public class AlbumArt {


	static final String[] rev = {
			"content/revisions//java 1/pics",
			"content/revisions//java 2/pics",
			"content/revisions//py 3/pics",
			};

	static BufferedImage[] cache() throws IOException {
		File[] f = new File(rev[2]).listFiles(pathname -> pathname.toString().endsWith(".png") || pathname.toString().endsWith(".jfif"));
		if (f == null || f.length == 0) return new BufferedImage[0];
		BufferedImage[] img = new BufferedImage[f.length];

		for (int i = 0; i < f.length; i++)
			 img[i] = ImageIO.read(f[i]);

		return img;
	}

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
		return s;
	}

	public static String collage(double width, double height, BufferedImage[] images, int scale, boolean randomOffsets) throws IOException {
		BufferedImage generated = rotRev4(width, height, images, scale, randomOffsets);

		File out = new File("content/render/scale-" + (scale == -1 ? "auto" : scale) + "-[" + images.hashCode() + "].png");
		if (!out.getParentFile().exists()) out.getParentFile().mkdirs();

		ImageIO.write(generated, "png", out);

		return out.getAbsolutePath();
	}

	public static BufferedImage rotRev4(double w, double h, BufferedImage[] images, int scale, boolean rand) throws IOException {
		//find diagonal upon which the canvas aligned
		double d        = Math.max(w, h) * Math.sqrt(2);//diagonal of encapsulating square
		int    diagonal = (int) Math.round(d);

		//pick the largest angle (it won't work the with the smaller one)
		double theta = Math.max(Math.asin(h / d), Math.asin(w / d));

		if (scale == -1) {
			System.out.println("SCALE AUTO");
			scale = autoscale(w, h, d, theta, images.length);
		} else {
			scale = diagonal / scale;
		}

		BufferedImage   art = new BufferedImage((int) w, (int) h, BufferedImage.TYPE_INT_RGB);
		Graphics2D      g2d = ((Graphics2D) art.getGraphics());
		AffineTransform transform;

		g2d.setStroke(new BasicStroke(3f));

		int   i    = 0;
		float last = 0;
		for (double x = 0; x < d / scale; x++) {//iterate over (unscaled) X from a little outside {-(d/scale)/2} until the diagonal {d/scale}
			int r = rand ? (int) Math.round(Math.random() * scale) : 0;
			for (double y = last -= 1; y < (d / scale) + last; y++) {//iterate over (unscaled) Y, staring further outside each time in order to fill the rect
				transform = g2d.getTransform();

				int xc = (int) Math.round(x * scale),
						yc = (int) Math.round(y * scale);

				double deltaX = x * Math.cos(theta) - y * Math.sin(theta),
						deltaY = x * Math.sin(theta) + y * Math.cos(theta);

				deltaX *= scale;
				deltaY *= scale;

				//only proceed if we're in bounds
				boolean inB = inBounds(deltaX, deltaY, w, h) ||
							  inBounds(deltaX - scale, deltaY + scale, w, h) ||
							  inBounds(deltaX - scale, deltaY, w, h) ||
							  inBounds(deltaX, deltaY + scale, w, h);

				if (inB) {
					g2d.setColor(Color.WHITE);
					//rotate around origin
					g2d.rotate(theta);

					//draw image on rotated coordinate
					if (i < images.length) g2d.drawImage(images[i++], xc, yc + r, scale, scale, null);
				}

				g2d.setTransform(transform);
			}
		}

		return art;
	}

	static boolean inBounds(double x, double y, double w, double h) { return !(x > w || y > h || x < 0 || y < 0); }

	public static void tryScale() throws IOException {
		//cache images to create multiple renders quickly
		System.out.println("Loading images...");
		BufferedImage[] img = cache();
		System.out.println("Cached " + img.length + " images.");
		System.out.println("Press ENTER to continue.");

		//loop to try different scales conveniently
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String         line;
		int            mode   = -1;
		double         w      = -1, h = -1;//1920,1080;1080,2340
		boolean        random = false;
		while (!(line = reader.readLine()).equals("exit")) {
			System.out.println("\nType 'exit' to quit.");
			line = line.strip().toUpperCase();

			int in = -1;
			try {
				in = Integer.parseInt(line);
			} catch (NumberFormatException x) {
				if (mode == 2) {
					if (line.equals("Y")) in = 1;
					if (line.equals("N")) in = 0;
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
					System.out.print("Use random offsets? (EXPERIMENTAL: you'll have to crop the image) Y/N ");
					mode++;
				}
				case 2 -> {
					random = in == 1;
					System.out.println("Entering render mode. Repeat the following step until you are satisfied with the result.");
					System.out.print("Enter a scale (-1 for AUTO): ");
					mode++;
				}
				case 3 -> {
					//shuffle the images for unique results
					List<BufferedImage> temp = Arrays.asList(img);
					Collections.shuffle(temp);
					BufferedImage[] images = temp.toArray(BufferedImage[]::new);

					System.out.println("Saved to " + collage(w, h, images, in, random));
					System.out.print("Enter a scale (-1 for AUTO): ");
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		tryScale();
	}

}

