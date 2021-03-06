package tests.java.inflor.integration;

import java.util.ArrayList;
import java.util.logging.Logger;

import main.java.inflor.core.data.FCSFrame;
import main.java.inflor.core.fcs.FCSFileReader;
import main.java.inflor.core.gates.RangeGate;
import main.java.inflor.core.logging.LogFactory;

public class RangeGateCalculation {
  static final int FILE_COUNT = 1;
  ArrayList<FCSFrame> dataSet = new ArrayList<>();

  public static void main(String[] args) throws Exception {
    Logger logger = LogFactory.createLogger(RangeGateCalculation.class.getName());
    
    String bigPath = "src/io/landysh/inflor/tests/extData/20mbFCS3.fcs";
    
    RangeGate rangeGate = new RangeGate("Foo", new String[] {"FSC-A", "SSC-A"},
        new double[] {40, 000, 60, 000}, new double[] {5000, 10000});

    FCSFrame data = FCSFileReader.read(bigPath);
    long start = System.currentTimeMillis();
    for (int i = 0; i < FILE_COUNT; i++) {
      rangeGate.evaluate(data);
    }
    long end = System.currentTimeMillis();
    logger.fine("Millis: " + (end - start));
  }
}
