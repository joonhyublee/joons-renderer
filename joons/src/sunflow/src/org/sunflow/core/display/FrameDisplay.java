package org.sunflow.core.display;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Display;
import org.sunflow.image.Color;
import org.sunflow.system.ImagePanel;

public class FrameDisplay implements Display {

    private String filename;
    private RenderFrame frame;

    public FrameDisplay() {
        this(null);
    }

    public FrameDisplay(String filename) {
        this.filename = filename;
        frame = null;
    }

    @Override
    public void imageBegin(int w, int h, int bucketSize) {
        if (frame == null) {
            frame = new RenderFrame();
            frame.imagePanel.imageBegin(w, h, bucketSize);
            Dimension screenRes = Toolkit.getDefaultToolkit().getScreenSize();
            boolean needFit = false;
            if (w >= (screenRes.getWidth() - 200) || h >= (screenRes.getHeight() - 200)) {
                frame.imagePanel.setPreferredSize(new Dimension((int) screenRes.getWidth() - 200, (int) screenRes.getHeight() - 200));
                needFit = true;
            } else {
                frame.imagePanel.setPreferredSize(new Dimension(w, h));
            }
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            if (needFit) {
                frame.imagePanel.fit();
            }
        } else {
            frame.imagePanel.imageBegin(w, h, bucketSize);
        }
    }

    @Override
    public void imagePrepare(int x, int y, int w, int h, int id) {
        frame.imagePanel.imagePrepare(x, y, w, h, id);
    }

    @Override
    public void imageUpdate(int x, int y, int w, int h, Color[] data, float[] alpha) {
        frame.imagePanel.imageUpdate(x, y, w, h, data, alpha);
    }

    @Override
    public void imageFill(int x, int y, int w, int h, Color c, float alpha) {
        frame.imagePanel.imageFill(x, y, w, h, c, alpha);
    }

    @Override
    public void imageEnd() {
        frame.imagePanel.imageEnd();
        if (filename != null) {
            frame.imagePanel.save(filename);
        }
    }

    @SuppressWarnings("serial")
    private static class RenderFrame extends JFrame {

        ImagePanel imagePanel;

        RenderFrame() {
            super("Sunflow v" + SunflowAPI.VERSION);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        System.exit(0);
                    }
                }
            });
            imagePanel = new ImagePanel();
            setContentPane(imagePanel);
            pack();
        }
    }
}