package io.landysh.inflor.java.knime.nodes.createGates.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.TitledBorder;

import org.jfree.chart.JFreeChart;

import io.landysh.inflor.java.core.dataStructures.ColumnStore;
import io.landysh.inflor.java.core.dataStructures.FCSDimension;
import io.landysh.inflor.java.core.plots.AbstractFCChart;
import io.landysh.inflor.java.core.plots.ChartSpec;
import io.landysh.inflor.java.core.plots.FCSChartPanel;
import io.landysh.inflor.java.core.plots.PlotTypes;
import io.landysh.inflor.java.core.plots.PlotUtils;
import io.landysh.inflor.java.core.plots.gateui.GateCreationToolBar;
import io.landysh.inflor.java.core.subsets.AbstractSubset;
import io.landysh.inflor.java.core.subsets.RootSubset;
import io.landysh.inflor.java.core.transforms.AbstractTransform;
import io.landysh.inflor.java.core.transforms.TransformType;
import io.landysh.inflor.java.core.utils.FCSUtils;
import io.landysh.inflor.java.knime.nodes.createGates.CreateGatesNodeDialog;
import sun.awt.windows.WEmbeddedFrame;

public class ChartEditorDialog extends JDialog {

	/**
	 * The modal dialog from which new chart definitions will be created 
	 * and existing charts may be edited
	 */

	private static final long serialVersionUID = 3249082301592821578L;
	// private static final Frame parent;
	protected JPanel previewPanel;
	protected JPanel settingsPanel;
	protected JPanel contentPanel;

	ChartSpec spec;

	private JButton m_okButton = null;
	private JButton m_cancelButton = null;
	public boolean isOK = false;
	private JComboBox<AbstractSubset> parentSelectorBox;
	private JComboBox<PlotTypes> plotTypeSelectorBox;
	private JPanel domainAxisGroup;
	private JComboBox<FCSDimension> domainParameterBox;
	private JPanel rangeAxisGroup;
	private JComboBox<FCSDimension> rangeDimBox;
	private JProgressBar progressBar;
	private CreateGatesNodeDialog parentDialog;
	private AbstractFCChart previewPlot;
	private FCSChartPanel chartPanel;
	private JComboBox<TransformType> domainTransformBox;
	private JComboBox<TransformType> rangeTransformBox;
	private GateCreationToolBar gatingToolBar;


	public ChartEditorDialog(Frame topFrame, CreateGatesNodeDialog parent) {
		
		/**
		 * Use this constructor to create a new chart. 
		 * 
		 * @param topFrame the frame in which this dialog resides.  Required to make dialog modal
		 * @param parent the parent dialog which stores the data model.
		 * @param id The UUID of the domain object. typically found in the settingsModel.
		 */
		// Initialize
		super(topFrame);
		parentDialog = parent;
		spec = new ChartSpec();
		spec.getPlotType();
		String first = parent.getSelectedSample().getData().navigableKeySet().first();
		String next = parent.getSelectedSample().getData().navigableKeySet().ceiling(first);

		spec.setDomainAxisName(parent.getSelectedSample().getVector(first).toString());
		spec.setRangeAxisName(parent.getSelectedSample().getVector(next).toString());
		setModal(true);

		// populate the dialog
		setTitle("Add a new plot.");
		final JPanel content = createContentPanel();
		getContentPane().add(content);
		pack();
		setLocationRelativeTo(getParent());
	}

	public ChartEditorDialog(WEmbeddedFrame topFrame, CreateGatesNodeDialog parent, String id) {
		/**
		 * Use this constructor to edit an existing chart. 
		 * 
		 * @param topFrame the frame in which this dialog resides.  Required to make dialog modal
		 * @param parent the parent dialog which stores the data model.
		 * @param id The UUID of the domain object. typically found in the settingsModel.
		 */
		super(topFrame);
		parentDialog = parent;
		spec = parentDialog.m_Settings.getChartSpec(id);
		setModal(true);

		// populate the dialog
		setTitle("Editing: " + spec.getDisplayName());
		final JPanel content = createContentPanel();
		parentSelectorBox.setSelectedIndex(0);
		plotTypeSelectorBox.setSelectedItem(spec.getPlotType());
		domainParameterBox.setSelectedItem(spec.getDomainAxisName());
		ColumnStore subsetData = ((AbstractSubset)parentSelectorBox.getSelectedItem()).getData();
		FCSDimension domainDimension = FCSUtils.findCompatibleDimension(subsetData, spec.getDomainAxisName());
		domainTransformBox.setSelectedItem(domainDimension.getPreferredTransform());
		FCSDimension rangeDimension = FCSUtils.findCompatibleDimension(subsetData, spec.getDomainAxisName());
		rangeDimBox.setSelectedItem(spec.getRangeAxisName());
		rangeTransformBox.setSelectedItem(rangeDimension.getPreferredTransform());

		getContentPane().add(content);
		pack();
		setLocationRelativeTo(getParent());
	}

	private JButton createCancelButton() {
		m_cancelButton = new JButton();
		m_cancelButton.setText("Cancel");
		m_cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				setVisible(false);
			}
		});
		return m_cancelButton;
	}

	private JPanel createContentPanel() {
		// Create the panel
		progressBar = new JProgressBar();
		final Component plotOptionsPanel = createPlotOptionsPanel();
		
		contentPanel = new JPanel(new GridBagLayout());
		previewPanel = createPreviewPanel();
		gatingToolBar = new GateCreationToolBar(chartPanel);
		m_okButton = createOkButton();
		m_cancelButton = createCancelButton();
		final JPanel buttonPanel = new JPanel(new FlowLayout());
		buttonPanel.add(m_okButton);
		buttonPanel.add(m_cancelButton);

		
		//GridLayout
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		
		//Gating toolbar
		gbc.gridx = 0;
		gbc.gridy = 0;
		contentPanel.add(gatingToolBar, gbc);
		//Preview Panel
		gbc.gridx = 0;
		gbc.gridy = 1;
		contentPanel.add(previewPanel, gbc);
		//Plot Options
		gbc.gridy = 2;
		contentPanel.add(plotOptionsPanel, gbc);
		gbc.gridy = 3;
		contentPanel.add(createHorizontalAxisGroup(), gbc);
		gbc.gridy = 4;
		contentPanel.add(createVerticalAxisGroup(), gbc);
		//Button Panel
		gbc.anchor = GridBagConstraints.SOUTHEAST;
		gbc.gridy = 5;
		//ProgressBar
		gbc.gridy = 6;
		progressBar = new JProgressBar();
		progressBar.setVisible(false);
		contentPanel.add(progressBar, gbc);
		contentPanel.add(buttonPanel, gbc);
		contentPanel.setPreferredSize(new Dimension(300, 450));
		
		return contentPanel;
	}

	private FCSChartPanel createPreviewPanel() {
		ColumnStore chartDataSource = (ColumnStore) parentDialog.getSelectedSample();
		String domainName = spec.getDomainAxisName();
		FCSDimension domainDimension = FCSUtils.findCompatibleDimension(chartDataSource, domainName);
		FCSDimension rangeDimension= FCSUtils.findCompatibleDimension(chartDataSource, spec.getRangeAxisName());		
		previewPlot = PlotUtils.createPlot(spec);
		JFreeChart chart = previewPlot.createChart(domainDimension, rangeDimension);
		chartPanel = new FCSChartPanel(chart, chartDataSource);
		chartPanel.setPreferredSize(new Dimension(280,250));
		return chartPanel;
	}


	private Component createHorizontalAxisGroup() {
		domainAxisGroup = new JPanel();
		domainAxisGroup.setLayout(new FlowLayout());
		domainAxisGroup
				.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Horizontal Axis"));
		
		domainParameterBox = new JComboBox<FCSDimension>();
		
		parentDialog.getSelectedSample()
					.getData()
					.values()
					.forEach((dimension)->domainParameterBox.addItem(dimension));
		
		domainParameterBox.setSelectedIndex(1);
		domainParameterBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				spec.setDomainAxisName((String) domainParameterBox.getModel().getSelectedItem().toString());
				updatePreviewPlot();
			}
		});
		
		//Transform selector
		final TransformType[] domainTransforms = TransformType.values();
		domainTransformBox = new JComboBox<TransformType>(domainTransforms);
		domainTransformBox.setSelectedItem(spec.getDomainTransform());
		domainTransformBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				TransformType selectedType = (TransformType) domainTransformBox.getSelectedItem();
				AbstractTransform newTransform = ((FCSDimension)domainParameterBox.getSelectedItem()).getTransform(selectedType);
				spec.setDomainTransform(newTransform);
				updatePreviewPlot();
			}
		});
		
		domainAxisGroup.add(domainParameterBox);
		domainAxisGroup.add(domainTransformBox);

		return domainAxisGroup;
	}

	/**
	 * This method initializes okButton.
	 *
	 * @return javax.swing.JButton
	 */
	private JButton createOkButton() {
		m_okButton = new JButton();
		m_okButton.setText("Ok");
		m_okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				updatePreviewPlot();
				isOK = true;
				setVisible(false);
			}
		});
		return m_okButton;
	}

	private JComboBox<AbstractSubset> createParentSelector() {
		parentSelectorBox = new JComboBox<AbstractSubset>();
		parentSelectorBox.addItem(new RootSubset(parentDialog.getSelectedSample()));
		parentDialog.m_Settings.getSubSets().forEach(subset -> parentSelectorBox.addItem(subset));
		parentSelectorBox.setSelectedIndex(0);
		parentSelectorBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				String parentID = ((AbstractSubset)parentSelectorBox.getSelectedItem()).ID;
				spec.setParent(parentID);
				updatePreviewPlot();
			}
		});
		return parentSelectorBox;
	}

	protected void updatePreviewPlot() {
		progressBar.setVisible(true);
		progressBar.setStringPainted(true);
		progressBar.setString("Initializing");
		progressBar.getModel().setValue(1);
		ColumnStore data = (ColumnStore) parentDialog.getSelectedSample();
		FCSDimension domainDimension = FCSUtils.findCompatibleDimension(data, spec.getDomainAxisName());
		FCSDimension rangeDimension = FCSUtils.findCompatibleDimension(data, spec.getRangeAxisName());
		UpdatePlotWorker worker = new UpdatePlotWorker(progressBar, chartPanel, spec, domainDimension, rangeDimension);
		worker.execute();
	}

	private JPanel createPlotOptionsPanel() {
		final JPanel panel = new JPanel(new FlowLayout());
		panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "General Options"));
		parentSelectorBox = createParentSelector();
		panel.add(parentSelectorBox);
		plotTypeSelectorBox = createPlotTypeSelector();
		panel.add(plotTypeSelectorBox);
		return panel;
	}

	private JComboBox<PlotTypes> createPlotTypeSelector() {
		plotTypeSelectorBox = new JComboBox<PlotTypes>(PlotTypes.values());
		plotTypeSelectorBox.setSelectedItem(spec.getPlotType());
		plotTypeSelectorBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				PlotTypes newValue = (PlotTypes) plotTypeSelectorBox.getModel().getSelectedItem();
				spec.clone();
				spec.setPlotType(newValue);
				updatePreviewPlot();
			}
		});
		return plotTypeSelectorBox;
	}

	private Component createVerticalAxisGroup() {
		rangeAxisGroup = new JPanel();
		rangeAxisGroup.setLayout(new FlowLayout());
		TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Vertical Axis");
		rangeAxisGroup.setBorder(border);
	 	
		rangeDimBox = new JComboBox<FCSDimension>();
		
		parentDialog.getSelectedSample()
					.getData()
					.values()
					.forEach((dimension)->rangeDimBox.addItem(dimension));

		rangeDimBox.setSelectedIndex(0);
		rangeDimBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				spec.setRangeAxisName((String) rangeDimBox.getModel().getSelectedItem().toString());
				updatePreviewPlot();
			}
		});
		
		//Transform selector
		final TransformType[] domainTransforms = TransformType.values();
		rangeTransformBox = new JComboBox<TransformType>(domainTransforms);
		TransformType selectedTransform = ((FCSDimension) rangeDimBox.getSelectedItem()).getPreferredTransform().type;
		rangeTransformBox.setSelectedItem(selectedTransform);
		rangeTransformBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				TransformType selectedType = (TransformType) rangeTransformBox.getSelectedItem();
				AbstractTransform newTransform = ((FCSDimension) rangeDimBox.getSelectedItem()).getTransform(selectedType);
				spec.setRangeTransform(newTransform);
				updatePreviewPlot();
			}
		});
		//Add the components
		rangeAxisGroup.add(rangeDimBox);
		rangeAxisGroup.add(rangeTransformBox);
		
		return rangeAxisGroup;
	}

	public ChartSpec getChartSpec() {
		return spec;
	}
}
