package me.vadim.test;

import java.awt.*;
import java.awt.event.*;

/**
 * @author vadim
 */
public class PreviewListener implements MouseListener, MouseMotionListener, MouseWheelListener, ImageMutation {

	private final Gui gui;

	public PreviewListener(Gui gui) {
		this.gui = gui;
	}

	@Override
	public int zoomAmount() { return zoomW; }

	@Override
	public Point translationDelta() { return new Point(translateX, translateY); }

	private Color bg = Color.BLACK;
	public ImageMutation withBackground(Color color){
		bg = color;
		return this;
	}

	@Override
	public Color background() { return bg; }

	boolean dragging;
	int startX, startY;
	public int translateX, translateY;
	@Override
	public void mousePressed(MouseEvent e) {
		dragging = true;
		startX = e.getX();
		startY = e.getY();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		dragging = false;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		int mx = e.getX();
		int my = e.getY();

		int dx = mx - startX;
		int dy = my - startY;

		translateX += dx;
		translateY += dy;

		gui.translationSliders[0].setValue(translateX);
		gui.translationSliders[1].setValue(translateY);

		startX = e.getX();
		startY = e.getY();

		gui.render(false);
	}

	public int zoomW;
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		zoomW -= e.getUnitsToScroll();
		zoomW = Math.max(0, zoomW);
		gui.translationSliders[2].setValue(zoomW);
		gui.render(false);
	}

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	@Override
	public void mouseMoved(MouseEvent e) {

	}
}
