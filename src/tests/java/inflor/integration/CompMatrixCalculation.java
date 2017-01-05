package tests.java.inflor.integration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import main.java.inflor.core.compensation.MatrixCalculator;
import main.java.inflor.core.data.FCSFrame;
import main.java.inflor.core.fcs.FCSFileReader;

  public class CompMatrixCalculation {
    ArrayList<FCSFrame> dataSet = new ArrayList<>();

    public static void main(String[] args) throws Exception {
      String path = CompensationTestDatasets.OMIP16.getPath();
      final File folder = new File(path);
      
      File[] files = folder.listFiles();
      
      List<FCSFrame> streamedFiles = Arrays
          .asList(files)
          .stream()
          .map(File::getAbsolutePath)
          .filter(FCSFileReader::isValidFCS)
          .map(FCSFileReader::read)
          .collect(Collectors.toList());
      
      MatrixCalculator mCalc = new MatrixCalculator(streamedFiles);      
      mCalc.removeCompDimension("Blue Vid-A");
      Optional<FCSFrame> apcFrame = streamedFiles.stream().filter(f -> f.getPrefferedName().equals("BEADS_APC_G08.fcs")).findAny();
      if (apcFrame.isPresent()){//it is
        mCalc.overrideMapping("APC-A",apcFrame.get().getPrefferedName());
      }
      
      double[][] mtx = mCalc.calculate();
      
    }
  }