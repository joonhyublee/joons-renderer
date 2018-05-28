package org.sunflow.system;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;

/**
 * This code was edited or generated using CloudGarden's Jigloo SWT/Swing GUI
 * Builder, which is free for non-commercial use. If Jigloo is being used
 * commercially (ie, by a corporation, company or business for any purpose
 * whatever) then you should purchase a license for each developer using Jigloo.
 * Please visit www.cloudgarden.com for details. Use of Jigloo implies
 * acceptance of these licensing terms. A COMMERCIAL LICENSE HAS NOT BEEN
 * PURCHASED FOR THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED LEGALLY FOR
 * ANY CORPORATE OR COMMERCIAL PURPOSE.
 */
@SuppressWarnings("serial")
public class RenderGlobalsPanel extends JTabbedPane {

    private JPanel generalPanel;
    private JComboBox maxSamplingComboxBox;
    private JPanel samplingPanel;
    private JComboBox minSamplingComboBox;
    private JLabel jLabel6;
    private JLabel jLabel5;
    private JRadioButton defaultRendererRadioButton;
    private JRadioButton bucketRendererRadioButton;
    private JPanel bucketRendererPanel;
    private JLabel jLabel2;
    private JPanel rendererPanel;
    private JTextField threadTextField;
    private JCheckBox threadCheckBox;
    private JLabel jLabel3;
    private JPanel threadsPanel;
    private JLabel jLabel1;
    private JPanel resolutionPanel;
    private JTextField resolutionYTextField;
    private JTextField resolutionXTextField;
    private JCheckBox resolutionCheckBox;

    /**
     * This method initializes this
     */
    private void initialize() {
    }

    /**
     * Auto-generated main method to display this JPanel inside a new JFrame.
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.getContentPane().add(new RenderGlobalsPanel());
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public RenderGlobalsPanel() {
        super();
        initialize();
        initGUI();
    }

    private void initGUI() {
        try {
            setPreferredSize(new Dimension(400, 300));
            {
                generalPanel = new JPanel();
                FlowLayout generalPanelLayout = new FlowLayout();
                generalPanelLayout.setAlignment(FlowLayout.LEFT);
                generalPanel.setLayout(generalPanelLayout);
                this.addTab("General", null, generalPanel, null);
                {
                    resolutionPanel = new JPanel();
                    generalPanel.add(resolutionPanel);
                    FlowLayout resolutionPanelLayout = new FlowLayout();
                    resolutionPanel.setLayout(resolutionPanelLayout);
                    resolutionPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED), "Resolution", TitledBorder.LEADING, TitledBorder.TOP));
                    {
                        resolutionCheckBox = new JCheckBox();
                        resolutionPanel.add(resolutionCheckBox);
                        resolutionCheckBox.setText("Override");
                    }
                    {
                        jLabel1 = new JLabel();
                        resolutionPanel.add(jLabel1);
                        jLabel1.setText("Image Width:");
                    }
                    {
                        resolutionXTextField = new JTextField();
                        resolutionPanel.add(resolutionXTextField);
                        resolutionXTextField.setText("640");
                        resolutionXTextField.setPreferredSize(new java.awt.Dimension(50, 20));
                    }
                    {
                        jLabel2 = new JLabel();
                        resolutionPanel.add(jLabel2);
                        jLabel2.setText("Image Height:");
                    }
                    {
                        resolutionYTextField = new JTextField();
                        resolutionPanel.add(resolutionYTextField);
                        resolutionYTextField.setText("480");
                        resolutionYTextField.setPreferredSize(new java.awt.Dimension(50, 20));
                    }
                }
                {
                    threadsPanel = new JPanel();
                    generalPanel.add(threadsPanel);
                    threadsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED), "Threads", TitledBorder.LEADING, TitledBorder.TOP));
                    {
                        threadCheckBox = new JCheckBox();
                        threadsPanel.add(threadCheckBox);
                        threadCheckBox.setText("Use All Processors");
                    }
                    {
                        jLabel3 = new JLabel();
                        threadsPanel.add(jLabel3);
                        jLabel3.setText("Threads:");
                    }
                    {
                        threadTextField = new JTextField();
                        threadsPanel.add(threadTextField);
                        threadTextField.setText("1");
                        threadTextField.setPreferredSize(new java.awt.Dimension(50, 20));
                    }
                }
            }
            {
                rendererPanel = new JPanel();
                FlowLayout rendererPanelLayout = new FlowLayout();
                rendererPanelLayout.setAlignment(FlowLayout.LEFT);
                rendererPanel.setLayout(rendererPanelLayout);
                this.addTab("Renderer", null, rendererPanel, null);
                {
                    defaultRendererRadioButton = new JRadioButton();
                    rendererPanel.add(defaultRendererRadioButton);
                    defaultRendererRadioButton.setText("Default Renderer");
                }
                {
                    bucketRendererPanel = new JPanel();
                    BoxLayout bucketRendererPanelLayout = new BoxLayout(bucketRendererPanel, javax.swing.BoxLayout.Y_AXIS);
                    bucketRendererPanel.setLayout(bucketRendererPanelLayout);
                    rendererPanel.add(bucketRendererPanel);
                    bucketRendererPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED), "Bucket Renderer", TitledBorder.LEADING, TitledBorder.TOP));
                    {
                        bucketRendererRadioButton = new JRadioButton();
                        bucketRendererPanel.add(bucketRendererRadioButton);
                        bucketRendererRadioButton.setText("Enable");
                    }
                    {
                        samplingPanel = new JPanel();
                        GridLayout samplingPanelLayout = new GridLayout(2, 2);
                        samplingPanelLayout.setColumns(2);
                        samplingPanelLayout.setHgap(5);
                        samplingPanelLayout.setVgap(5);
                        samplingPanelLayout.setRows(2);
                        samplingPanel.setLayout(samplingPanelLayout);
                        bucketRendererPanel.add(samplingPanel);
                        {
                            jLabel5 = new JLabel();
                            samplingPanel.add(jLabel5);
                            jLabel5.setText("Min:");
                        }
                        {
                            ComboBoxModel minSamplingComboBoxModel = new DefaultComboBoxModel(new String[]{
                                "Item One", "Item Two"});
                            minSamplingComboBox = new JComboBox();
                            samplingPanel.add(minSamplingComboBox);
                            minSamplingComboBox.setModel(minSamplingComboBoxModel);
                        }
                        {
                            jLabel6 = new JLabel();
                            samplingPanel.add(jLabel6);
                            jLabel6.setText("Max:");
                        }
                        {
                            ComboBoxModel maxSamplingComboxBoxModel = new DefaultComboBoxModel(new String[]{
                                "Item One", "Item Two"});
                            maxSamplingComboxBox = new JComboBox();
                            samplingPanel.add(maxSamplingComboxBox);
                            maxSamplingComboxBox.setModel(maxSamplingComboxBoxModel);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.getLogger(RenderGlobalsPanel.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}