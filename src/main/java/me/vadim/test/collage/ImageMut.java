package me.vadim.test.collage;

import java.awt.*;

/**
 * @author vadim
 */
public class ImageMut implements ImageMutation {

	private final int zoomAmount;
	private final Point translationDelta;
	private final Color background;

	public ImageMut(Color background) {
		this(0, new Point(), background);
	}

	public ImageMut(Point translationDelta, Color background) {
		this(0, translationDelta, background);
	}

	public ImageMut(int zoomAmount, Point translationDelta, Color background) {
		this.zoomAmount       = zoomAmount;
		this.translationDelta = translationDelta;
		this.background       = background;
	}

	@Override
	public int zoomAmount() { return zoomAmount; }

	@Override
	public Point translationDelta() { return new Point(translationDelta); }

	@Override
	public Color background() { return background; }
}
