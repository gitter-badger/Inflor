package io.landysh.inflor.java.core;

import static org.ejml.ops.CommonOps.invert;
import static org.ejml.ops.CommonOps.mult;

import java.util.Hashtable;
import java.util.NoSuchElementException;

import org.ejml.data.DenseMatrix64F;


public class AnnotatedVectorStore {
	
	//file details
	public String 						UUID;
	public Hashtable<String, String> 	annotation;
	public String[]			 			collumnNames;
	public Hashtable<String, Double[]> 	columnStore;
	
	// Compensation details
	public String[]						compParameters;
	public Integer[]					compParameterMap;
	public double[][]					FCSpillMatrix;
	public DenseMatrix64F				compMatrix;
	
	//fcs properties
	public Integer 						parameterCount;
	public Integer						eventCount;

	/** Store keywords and numeric columns in a persistable object.  
	 * @param FCSHeader some annotation to get started with. Must be a valid FCS header but may be added to later.
	 */
	public AnnotatedVectorStore(Hashtable<String, String> FCSHeader) throws Exception {
		annotation = FCSHeader;
		parameterCount = getKeywordValueInteger("$PAR");
		collumnNames = parseParameterList(annotation);
		eventCount = getKeywordValueInteger("$TOT");
		if (annotation.containsKey("$SPILL")||annotation.containsKey("SPILL")){
			parseSpillover(annotation);
			compMatrix = new DenseMatrix64F(FCSpillMatrix);
			invert(compMatrix);
		}
		
		if(!validateHeader()){
			Exception e = new Exception("Invalid FCS Header.");
			e.printStackTrace();
			throw e;
		}
	}

	private String[] parseParameterList(Hashtable<String, String> keywords2) {
		String[] plist = new String[parameterCount];
		for (int i=1;i<=parameterCount;i++){
			String keyword = ("$P"+ i + "N");
			plist[i-1] = getKeywordValueString(keyword);
		}
		return plist;
	}

	private Boolean validateHeader() {
		Boolean validHeader = false;
		// Check all required keywords later...
		if (getKeywordValueString("FCSVersion").contains("FCS") && getKeywordValueInteger("$TOT")!=null) {
			validHeader = true;
		} else {
			System.out.println("Invalid header: missing required information.");
		}

	return validHeader;
	}
	
	public Integer getKeywordValueInteger(String keyword) {
		//This method will try to return an FCS header keyword and if it isn't found will return -1!
		Integer value = -1; 
		if(annotation.containsKey(keyword)){
			String valueString = annotation.get(keyword).trim();
			value = Integer.parseInt(valueString);
		} else {
			System.out.println( keyword + " not found, -1 returned at your peril.");
		}
		return value;
	}

	public String getKeywordValueString(String keyword) {
		//This method will try to return an FCS header keyword and if it isn't found will return an empty string.
		String value = "";
		try{
			value = annotation.get(keyword).trim();
		}catch (NoSuchElementException e){
			System.out.println( keyword + " not found, <empy string> returned at your peril.");
		}
		return value;
	}
	
	public Hashtable<String, String> getHeader() {
		return annotation;
	}

	public void  setData(Hashtable<String, Double[]> allData) {
		 columnStore = allData;
	}

	public String[] getCannonColumnNames() {
		String[] columnNames = new String[parameterCount];
		for (int i=0;i<parameterCount;i++){
			String PnNValue = annotation.get("$P" + (i+1) + "N");
				columnNames[i] = (PnNValue).trim();
		}
		return columnNames;
	}
	
	public String[] getDisplayColumnNames() {
		String[] columnNames = new String[parameterCount];
		for (int i=0;i<parameterCount;i++){
			String PnNValue = annotation.get("$P" + (i+1) + "N");
			String PnSValue = annotation.get("$P" + (i+1) + "S");
			if(!PnNValue.equals(PnSValue) && PnSValue!=null ){
				columnNames[i] = (PnNValue + "  " + PnSValue).trim();
			} else {
				columnNames[i] = PnNValue.trim();
			}		}
		return columnNames;
	}
	public Hashtable<String,Double[]> getColumnData() {
		return columnStore;
	}

	public int findIndexByName(String s) throws Exception{
		Integer index = null;
		for (int i=0; i<collumnNames.length;i++){
			if (collumnNames[i].equals(s)){
				index = i;
			} 
		}
		if (index!= null){
			return index;
		} else {
			throw new Exception("Parameter index not found.");
		}
	}
	
	public String findStainName (String parameterName){
		String stainName = "";
		try {
			int index = findIndexByName(parameterName);
			stainName = getKeywordValueString("$P" + (1+index)+"S");
		} catch (Exception e) {
			System.out.print("No stain name found for: " + parameterName);
		}
		
		return stainName;
	}
	
	//Unit test: Completed but not running.
	private void parseSpillover(Hashtable<String, String> keywords) 
			throws Exception {
		String spill = null;
		
		//Check for spillover keywords
		if(keywords.containsKey("$SPILLOVER")){
			spill = keywords.get("SPILLOVER");
		} else if (keywords.containsKey("SPILL")){
			spill = keywords.get("SPILL");
		} else {
			throw new Exception("No spillover keyword found.");
		}
		
		// Magic string parsing from FCS Spec PDF
		String[] s = spill.split(",");
		int p = Integer.parseInt(s[0].trim());
		if (p >= 2){
			double[] spills = new double[p*p];
			int k=0;
			for (int i=p+1; i<spills.length + p + 1;i++){
				spills[k] = Double.parseDouble(s[i]);
				k++;
			}
			double[][] matrix = new double[p][p];
			String[] compPars = new String[p];
			Integer[] pMap = new Integer[p];
			for(int i=0;i<compPars.length;i++){
				compPars[i] = s[i+1];
				pMap[i] = findIndexByName(compPars[i]);
				double[] row = new double[p];
				for (int j=0;j<p;j++){
					int index = 1 + p + i*p+j;
					row[j] = Double.parseDouble(s[index]);	
				}
				matrix[i] = row;
			}
		compParameterMap = pMap;
		compParameters = compPars;
		FCSpillMatrix = matrix;
		}else {
			throw new Exception("Spillover Keyword - " + spill + " - appears to be invalid.");
		}
	}
	public double[] doCompRow(double[] FCSRow) throws Exception {
		double[] compedRow = null;
		if (compParameters!= null){
			compedRow = new double[compParameters.length];
			double[] unCompedRow = new double[compParameters.length];
			for (int i=0;i<compParameters.length;i++ ){				
				int index = compParameterMap[i];
				unCompedRow[i] = FCSRow[index];
				}
			DenseMatrix64F unCompedVector = new DenseMatrix64F(new double[][] {unCompedRow});
			DenseMatrix64F c = new DenseMatrix64F(new double[][] {unCompedRow});  
			mult(unCompedVector,compMatrix,c);
			compedRow = c.data;
			}
		if (compedRow!=null){
			return compedRow;
		} else {
			Exception e = new Exception("Comped array is null, this should not happen.");
			e.printStackTrace();
			throw e;
		}
	}
}