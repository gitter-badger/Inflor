package io.landysh.inflor.java.core.gatingML.gates.polygonGate;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

public class PolygonGateTest {

	@Test
	public void testEvaluate() throws Exception {
		// Setup
		final String id = "1";

		final String d1Name = "d1";
		final Double[] d1Values = { 0., 0., 1., 1. };

		final String d2Name = "d2";
		final Double[] d2Values = { 0., 1., 1., 0. };

		final ConcurrentHashMap<String, double[]> testData = new ConcurrentHashMap<String, double[]>();
		testData.put(d1Name, new double[] { 0.5 });
		testData.put(d2Name, new double[] { 0.5 });

		// Test
		final PolygonGate testGate = new PolygonGate(id);

		testGate.setDimensions(d1Name, d2Name);
		testGate.setPoints(new ArrayList<Double>(Arrays.asList(d1Values)),
				new ArrayList<Double>(Arrays.asList(d2Values)));

		testGate.validate();

		final boolean[] result = testGate.evaluate(testData, 1);

		// Assert
		assertEquals("Evaluated", result[0], true);
		System.out.println("EventFrameTest::testEvaluate completed (succefully or otherwise)");
	}

	@Test
	public void testInitialization() throws Exception {
		// Setup
		final String id = "1";

		// Test
		final PolygonGate testGate = new PolygonGate(id);

		// Assert
		assertEquals("Initialized", id, testGate.getId());
		System.out.println("PolygonGateTest::testInitialization completed (succefully or otherwise)");
	}

	@Test
	public void testValidate() throws Exception {
		// Setup
		final String id = "1";

		final String d1Name = "d1";
		final Double[] d1Values = { 0., 0., 1., 1. };

		final String d2Name = "d2";
		final Double[] d2Values = { 0., 1., 0., 1. };

		final ArrayList<String> trueNames = new ArrayList<String>();
		trueNames.add(d1Name);
		trueNames.add(d2Name);

		// Test
		final PolygonGate testGate = new PolygonGate(id);

		testGate.setDimensions(d1Name, d2Name);
		testGate.setPoints(new ArrayList<Double>(Arrays.asList(d1Values)),
				new ArrayList<Double>(Arrays.asList(d2Values)));

		testGate.validate();

		final ArrayList<String> testNames = testGate.getDimensionNames();
		final Double[] testVertex = testGate.getVertex(0);

		// Assert
		assertEquals("Initialized", testNames, trueNames);
		assertEquals("Validated", testVertex, new Double[] { 0., 0. });
		System.out.println("EventFrameTest::testValidate completed (succefully or otherwise)");
	}
}