package tests.java.inflor.ui;

import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIDefaults;
import javax.swing.UIManager;

import org.jfree.ui.ApplicationFrame;

import main.java.inflor.core.data.FCSFrame;
import main.java.inflor.core.fcs.FCSFileReader;
import main.java.inflor.core.gates.Hierarchical;
import main.java.inflor.core.gates.RangeGate;
import main.java.inflor.core.plots.ChartSpec;
import main.java.inflor.core.plots.PlotTypes;
import main.java.inflor.core.ui.CellLineageTree;

@SuppressWarnings("serial")
public class LineageViewFrame extends ApplicationFrame {
  private static final String TREE_LEFT_CHILD_INDENT = "Tree.leftChildIndent";
  private static final String DIM3 = "FSC-A";
  private static final String DIM2 = "SSC-A";
  private static final String DIM1 = "SSC-W";

  public LineageViewFrame(String title) throws IOException {
    super(title);

    List<Hierarchical> testSpecs = new ArrayList<>();

    String logiclePath = "src/io/landysh/inflor/tests/extData/logicle-example.fcs";
    final FCSFileReader reader = new FCSFileReader(logiclePath);
    reader.readData();
    final FCSFrame dataStore = reader.getFCSFrame();

    ChartSpec ly = new ChartSpec();
    ly.setPlotType(PlotTypes.DENSITY);
    ly.setDomainAxisName(DIM1);
    ly.setRangeAxisName(DIM2);

    ChartSpec ly2 = new ChartSpec();
    ly2.setPlotType(PlotTypes.DENSITY);
    ly2.setDomainAxisName(DIM3);
    ly2.setRangeAxisName(DIM2);


    ChartSpec ly3 = new ChartSpec();
    ly3.setPlotType(PlotTypes.DENSITY);
    ly3.setDomainAxisName(DIM3);
    ly3.setRangeAxisName(DIM2);
    ly3.setParentID(ly.getID());
    
    RangeGate g1 = new RangeGate("LY", new String[]{DIM3, DIM2}, new double[]{25000, 100000}, new double[]{36000,200000}, "gate");
        
    g1.setParentID(dataStore.getID());
    ly.setParentID("gate");

    testSpecs.add(ly);
    testSpecs.add(ly2);
    testSpecs.add(ly3);
    testSpecs.add(g1);


    UIDefaults defaults = UIManager.getDefaults();
    Integer oldValue = (int) defaults.get(TREE_LEFT_CHILD_INDENT);
    defaults.put(TREE_LEFT_CHILD_INDENT, 110);

    CellLineageTree testPanel = new CellLineageTree(dataStore,testSpecs);
    testPanel.setRootVisible(true);

    this.setDefaultCloseOperation(EXIT_ON_CLOSE);
    this.getContentPane().add(testPanel);
    defaults.put(TREE_LEFT_CHILD_INDENT, oldValue);
  }

  public static void main(String[] args) throws Exception {
    LineageViewFrame test = new LineageViewFrame("ContourPlotTest");
    test.pack();
    test.setSize(new Dimension(400, 700));
    test.setVisible(true);
  }
}
// EOF
