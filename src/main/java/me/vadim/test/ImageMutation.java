package me.vadim.test;

import java.awt.*;

/**
 * @author vadim
 */
public interface ImageMutation {

	int zoomAmount();

	Point translationDelta();

	Color background();

	static void aspectRatio(Dimension original, Dimension desired){
		if (desired.height == -1 && desired.width > 0) {//solve for h
			desired.height = (int) Math.round((original.getHeight() * desired.getWidth()) / original.getWidth());
		} else if (desired.width == -1 && desired.height > 0) {//solve for w
			desired.width = (int) Math.round((original.getWidth() * desired.getHeight()) / original.getHeight());
		}
	}

}
