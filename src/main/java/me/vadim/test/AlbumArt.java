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

	public static void sample(int size) throws IOException {
		File i = new File("sample.png");
		File o = new File("sample_" + size + ".png");

		BufferedImage image  = ImageIO.read(i);
		BufferedImage target = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		Graphics2D    g2d    = (Graphics2D) target.getGraphics();

		g2d.drawImage(image, 0, 0, size, size, null);

		ImageIO.write(target, "png", o);
	}

	public static void combine(BufferedImage[] images, BufferedImage sample) throws IOException {
		int wh    = 5000;
		int scale = wh / 16;

		BufferedImage art = new BufferedImage(wh, wh, BufferedImage.TYPE_INT_RGB);
		Graphics2D    g2d = ((Graphics2D) art.getGraphics());

		int x = 0, y = 0;
		for (BufferedImage img : images) {
			AffineTransform transform = g2d.getTransform();

			g2d.drawImage(img, x * scale, y * scale, scale, scale, null, null);


			if (x++ * scale > wh) {
				y++;
				x = 0;
			}

			g2d.setTransform(transform);
		}

		File out = new File("content/render/scale-" + scale + ".png");
		if (!out.exists()) out.getParentFile().mkdirs();

		ImageIO.write(art, "png", out);
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
//		System.out.println("s: " + s + ", expect:" + expect + ", result:" + result);
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

	public static BufferedImage rotRev3(BufferedImage[] images, int scale) throws IOException {
//		double w = 1920, h = 1080;
		double w = 1080, h = 2340;

		//find diagonal upon which the canvas aligned
		double d        = Math.max(w, h) * Math.sqrt(2);//diagonal of encapsulating square
		int    diagonal = (int) Math.round(d);

		scale = diagonal / scale;

		double theta = Math.asin(h / d);

//		BufferedImage   art = new BufferedImage((int) w, (int) h, BufferedImage.TYPE_INT_RGB);
		BufferedImage   art = new BufferedImage((int) w * 3, (int) h * 3, BufferedImage.TYPE_INT_RGB);
		Graphics2D      g2d = ((Graphics2D) art.getGraphics());
		AffineTransform transform;
		g2d.translate(200, 200);

		g2d.setStroke(new BasicStroke(3f));

		int   i    = 0;
		float last = -1;
		// -(d / scale) / 2
//		System.out.println(Math.toDegrees(theta));
		System.out.println(Math.toDegrees(Math.asin(h / d)));
		System.out.println(Math.toDegrees(Math.asin(w / d)));
		for (double x = 0; x < d / scale; x++) {//iterate over (unscaled) X from a little outside {-(d/scale)/2} until the diagonal {d/scale}
			for (double y = last -= 1; y < (d / scale) + last + 1; y++) {//iterate over (unscaled) Y, staring further outside each time in order to fill the rect
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
//					if(i < images.length) g2d.drawImage(images[i++], xc, yc, scale, scale, null);
					g2d.drawRect(xc, yc, scale, scale);
				}

				g2d.setTransform(transform);
			}
		}
		g2d.setStroke(new BasicStroke(10));

		g2d.setColor(Color.BLUE);
		g2d.drawRect(0, 0, (int) w, (int) h);

		return art;
	}

	public static BufferedImage rotRev2(BufferedImage[] images, int scale) throws IOException {

		int t = 2000;


//		double w = 1920, h = 1080;
		double w = 1080, h = 2340;

		//find diagonal upon which the canvas aligned
//		double d        = Math.sqrt((w * w) + (h * h));
		double d        = Math.max(w, h) * Math.sqrt(2);//diagonal of encapsulating square
		int    diagonal = (int) Math.round(d);

		scale = diagonal / scale;

		double theta = Math.asin(h / d);

		BufferedImage art = new BufferedImage((int) w * 3, (int) h * 3, BufferedImage.TYPE_INT_RGB);
//		BufferedImage art = new BufferedImage((int) w, (int) h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = ((Graphics2D) art.getGraphics());

		g2d.setStroke(new BasicStroke(3f));
//		g2d.rotate(theta);

		AffineTransform transform = g2d.getTransform();
		g2d.setTransform(transform);

		int     i    = 0;
		float   last = 0;
		boolean outtaBounds;
		for (double x = -(d / scale) / 2; x < d / scale; x++) {
			for (double y = last -= .75; y < d / scale; y++) {
				transform = g2d.getTransform();

				double deltaX = x * Math.cos(theta) - y * Math.sin(theta),
						deltaY = x * Math.sin(theta) + y * Math.cos(theta);

				deltaX *= scale;
				deltaY *= scale;

				//only proceed if this coordinate (or the adjacent coordinates) are in bounds
				if (inBounds(deltaX, deltaY, w, h) ||
					inBounds(deltaX + scale, deltaY + scale, w, h) ||
					inBounds(deltaX - scale, deltaY - scale, w, h)) {

					g2d.setColor(Color.GREEN);
					g2d.drawOval((int) deltaX, (int) deltaY, scale, scale);

					g2d.setColor(Color.YELLOW);
					g2d.translate(t, t);
					g2d.drawOval((int) deltaX, (int) deltaY, scale, scale);
					outtaBounds = false;
				} else {
					g2d.setColor(Color.RED);
					g2d.drawOval((int) deltaX, (int) deltaY, scale, scale);

					g2d.setColor(Color.MAGENTA);
					g2d.translate(t, t);
					g2d.drawOval((int) deltaX, (int) deltaY, scale, scale);
					outtaBounds = true;
				}

				g2d.setTransform(transform);
				if (outtaBounds) continue;
				transform = g2d.getTransform();

				yeet:
				{
					if (false) break yeet;

					g2d.setColor(Color.WHITE);

//					g2d.translate(t, t);

//					if(i < images.length) g2d.drawImage(images[i++], (int) x * scale, (int) y * scale, scale, scale, null);


					int
							xc = (int) (x * scale),
							yc = (int) (y * scale);


					int
							xt = xc + (scale / 2),
							yt = yc + (scale / 2);

//					g2d.translate(xt, yt);
					g2d.rotate(theta);
//					g2d.translate(-xt, -yt);
					g2d.drawRect(xc, yc, scale, scale);

//					g2d.rotate(theta, x*scale + (scale / 2f), y*scale + (scale / 2f));
//					g2d.rotate(theta, x + (w/2f), y + (h/2f));
//					g2d.rotate(theta, x * scale, y * scale);
//					g2d.rotate(theta, deltaX, deltaY);
//					g2d.drawRect((int) deltaX, (int) deltaY, scale, scale);

//					g2d.setColor(Color.PINK);
//					g2d.drawRect((int) x * scale , (int) y * scale, scale, scale);
				}

				g2d.setTransform(transform);
			}
		}
		g2d.setStroke(new BasicStroke(10));

		g2d.setColor(Color.BLUE);
		g2d.drawRect(0, 0, (int) w, (int) h);

		g2d.translate(t, t);
		g2d.drawRect(0, 0, (int) w, (int) h);

		return art;
	}

	static boolean inBounds(double x, double y, double w, double h) { return !(x > w || y > h || x < 0 || y < 0); }

	public static BufferedImage rotRev1(BufferedImage[] images, int scale) throws IOException {
		//output resolution

		//apple mobile = 1080x2340
		double w = 1080, h = 2340;
		w *= 3;
		h *= 3;
//		double w = 1920, h = 1080;

		//find diagonal upon which the canvas aligned
		double d        = Math.sqrt((w * w) + (h * h));
		int    diagonal = (int) Math.round(d);

		//find the angle of rotation
//		double theta = Math.asin(w/d);
		double theta = Math.asin(h / d);

		//scale argument is a multiplier, so convert it to pixels (based on resolution) now
		scale = diagonal / scale;

		//create the canvas
//		BufferedImage art = new BufferedImage((int) w, (int) h, BufferedImage.TYPE_INT_RGB);
		BufferedImage art = new BufferedImage((int) w * 3, (int) h * 3, BufferedImage.TYPE_INT_RGB);
		Graphics2D    g2d = ((Graphics2D) art.getGraphics());

		int r = 1;
		int x = 0, y = 0;
//		int x = -3000, y = -3000;
//		g2d.rotate(Math.toRadians(45), -diagonal/2, diagonal/2);
		g2d.rotate(theta, -diagonal / 2, diagonal / 2);
//		g2d.rotate(Math.toRadians(45), wh, wh);
		int rand = (int) (Math.random() * scale);
		for (BufferedImage img : images) {
			AffineTransform transform = g2d.getTransform();

//			double x   = Math.random() * wh;
//			double y   = Math.random() * wh;
//			double rot = Math.random() * 360;
//			g2d.rotate(Math.toRadians(rot), x, y);
//			g2d.rotate(Math.toRadians(r++), x, y);
			g2d.translate(-4000, -9000);
			g2d.drawImage(img, (int) x + rand, (int) y, scale, scale, null);

//			g2d.drawImage(img, x, y, scale, scale, null);

//			int deltaScale = (3 * (scale / 4));
			//++ < 1/2
			//-- > 1/2
			int deltaScale = scale;
			if ((x += deltaScale) > diagonal * 2) {

				y += deltaScale;
				x    = 0;
				rand = (int) (Math.random() * scale);
			}

			g2d.setTransform(transform);
		}

		//perform a rotation transformation on the canvas before drawing
		//these are kind of magic values; some work better with certain aspect ratios
//		g2d.rotate(theta, -w/2, -h/2);
//		g2d.rotate(theta, -diagonal/2d, -diagonal/2d);
//		g2d.rotate(theta, diagonal/2d, diagonal/2d);
//		g2d.rotate(theta, 0 + w/2f, 0 + h/2f);
//		g2d.rotate(theta, -1500, 5000);
		//mind = dead

//		int expect = 20;//how many pictures across
//		int i      = 0;//image index

//		int mid = ((-diagonal / 2) + (diagonal / 2)) / 2;
//		System.out.println(mid);
//
//		double yeet = 0;
//		for (int y = (int) (-h / 2); y < h / 2; y += scale) {
//			int random = (int) (Math.random() * scale);
//			int random = 0;
//
//			for (int x = Math.round(-expect / 2f); x < Math.round(expect / 2f); x++) {
//			for (int x = 0; x < expect; x++) {
//				if (i >= images.length) break;
//				g2d.drawImage(images[i++], (int) (x * scale) + (diagonal / 2) - (random), (int) y, scale, scale, null);
//			}
//
//			int inc = 3;
//			if (y < mid) {
//				expect -= inc;
//				yeet -= 1;
//			} else if (y > mid) {
//				expect += inc;
//			}
//
//		}
//
		return art;
	}

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

