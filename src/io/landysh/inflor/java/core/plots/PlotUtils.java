package io.landysh.inflor.java.core.plots;

import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.data.Range;

import io.landysh.inflor.java.core.transforms.AbstractDisplayTransform;
import io.landysh.inflor.java.core.transforms.BoundDisplayTransform;
import io.landysh.inflor.java.core.transforms.LogicleTransform;
import io.landysh.inflor.java.core.transforms.LogrithmicDisplayTransform;
import io.landysh.inflor.java.core.transforms.TransformType;
import io.landysh.inflor.java.knime.nodes.createGates.ui.ScatterPlot;

public class PlotUtils {

	public static ValueAxis createAxis(String key, AbstractDisplayTransform transform) {
		TransformType type = transform.getType();
		
		if (type.equals(TransformType.Linear)){
			NumberAxis axis = new NumberAxis(key);
			axis.setRange(new Range(0,262144));
			axis.setTickMarkInsideLength(0);
			axis.setTickMarkOutsideLength(2);
			return axis;
			
		} else if (type.equals(TransformType.Bounded)){
			NumberAxis axis = new NumberAxis(key);
			BoundDisplayTransform bdt = (BoundDisplayTransform) transform;
			axis.setRange(new Range(bdt.getMinValue(),bdt.getMaxValue()));
			axis.setTickMarkInsideLength(0);
			axis.setTickMarkOutsideLength(2);
			return axis;
		
		} else if (type.equals(TransformType.Logrithmic)){
			LogarithmicAxis axis = new LogarithmicAxis(key);
			LogrithmicDisplayTransform logTransform = (LogrithmicDisplayTransform) transform;
			axis.setRange(new Range(logTransform.getMin(), logTransform.getMax()));
			axis.setTickMarkInsideLength(0);
			axis.setTickMarkOutsideLength(2);
			axis.setExpTickLabelsFlag(true);
			return axis;
			
		} else if (type.equals(TransformType.Logicle)){
			LogicleTransform logicleTransform = (LogicleTransform) transform;
			NumberAxis axis = new LogicleAxis(key, logicleTransform);
			return axis;
		
		} else {
			throw new RuntimeException("Transformation type not supported. Yet.");
		}
	}

	public static AbstractFCSPlot createPlot(PlotSpec plotSpec) {
		PlotTypes type = plotSpec.getPlotType();
		AbstractFCSPlot newPlot = null;		
		if (type.equals(PlotTypes.Contour)){
			newPlot = new ContourPlot(plotSpec);
		} else if (type.equals(PlotTypes.Scatter)){
			newPlot = new ScatterPlot(plotSpec);
		} else {
			newPlot = new FakePlot(null, null);
		}
		return newPlot;
	}
}