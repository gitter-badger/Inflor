/*
 * ------------------------------------------------------------------------ Copyright 2016 by Aaron
 * Hart Email: Aaron.Hart@gmail.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License, Version 3, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, see <http://www.gnu.org/licenses>.
 * ---------------------------------------------------------------------
 *
 * Created on December 14, 2016 by Aaron Hart
 */
package main.java.inflor.core.data;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.protobuf.InvalidProtocolBufferException;

import main.java.inflor.core.proto.FCSFrameProto.Message;
import main.java.inflor.core.proto.FCSFrameProto.Message.Dimension;
import main.java.inflor.core.proto.FCSFrameProto.Message.Keyword;
import main.java.inflor.core.proto.FCSFrameProto.Message.Transform;
import main.java.inflor.core.proto.FCSFrameProto.Message.Transform.Builder;
import main.java.inflor.core.transforms.AbstractTransform;
import main.java.inflor.core.transforms.BoundDisplayTransform;
import main.java.inflor.core.transforms.LogicleTransform;
import main.java.inflor.core.transforms.LogrithmicTransform;
import main.java.inflor.core.transforms.TransformType;
import main.java.inflor.core.utils.FCSUtilities;


// don't use the default serializer, there is a protobuf spec.
@SuppressWarnings("serial")
public class FCSFrame extends DomainObject implements Comparable<String> {

  private static final String LOAD_FAILURE = "Failed to de-serialize FCS Frame";

  private TreeSet<FCSDimension> columnData;
  private Map<String, String> keywords;
  private String displayName;
  private String compReference;
  private Integer rowCount = -1;
  private ArrayList<Subset> subsets = new ArrayList<>();

  /**
   * Store keywords and numeric columns in a persistable object.
   * 
   * @param inKeywords some annotation to get started with. Must be a valid FCS header but may be
   *        added to later.
   */
  public FCSFrame(Map<String, String> keywords, int rowCount) {
    this(null, keywords, rowCount);
  }

  public FCSFrame(String priorUUID, Map<String, String> keywords, int rowCount) {
    super(priorUUID);
    this.keywords = keywords;
    columnData = new TreeSet<>();
    this.rowCount = rowCount;
    displayName = FCSUtilities.chooseDisplayName(this);
  }

  // minimal constructor, use with .load()
  public FCSFrame() {
    super(null);
  }

  private static BitSet extractMaskFromSubsetMessage(Message.Subset subsetMessage) {
    BitSet mask = new BitSet(subsetMessage.getMaskCount());
    for (int j = 0; j < mask.size(); j++) {
      if (subsetMessage.getMask(j)) {
        mask.set(j);
      }
    }
    return mask;
  }

  public static FCSFrame load(byte[] bytes) throws InvalidProtocolBufferException {
    final Message loadedMessage = Message.parseFrom(bytes);

    // Load the keywords
    final HashMap<String, String> keywords = loadKeywords(loadedMessage);

    // Load the Dimensions
    final int rowCount = loadedMessage.getEventCount();
    final FCSFrame fcsFrame = new FCSFrame(loadedMessage.getId(), keywords, rowCount);
    final int dimCount = loadedMessage.getDimensionCount();
    final String[] dimen = new String[dimCount];
    for (int j = 0; j < dimCount; j++) {
      Dimension dim = loadedMessage.getDimension(j);
      String priorUUID = dim.getId();
      dimen[j] = priorUUID;
      final FCSDimension currentDimension = new FCSDimension(priorUUID, fcsFrame.getRowCount(),
          dim.getIndex(), dim.getPnn(), dim.getPns(), dim.getPneF1(), dim.getPneF2(), dim.getPnr());
      for (int i = 0; i < currentDimension.getSize(); i++) {
        currentDimension.getData()[i] = dim.getData(i);
      }
      AbstractTransform preferredTransform = readTransform(dim);
      currentDimension.setPreferredTransform(preferredTransform);
      fcsFrame.addDimension(currentDimension);
    }

    // load the subsets
    if (loadedMessage.getSubsetCount() > 0) {
      for (int i = 0; i < loadedMessage.getSubsetCount(); i++) {
        Message.Subset subsetMessage = loadedMessage.getSubset(i);
        Subset subset = loadSubset(subsetMessage);
        fcsFrame.addSubset(subset);
      }
    }
    fcsFrame.setDisplayName(FCSUtilities.chooseDisplayName(fcsFrame));
    return fcsFrame;
  }

  private static HashMap<String, String> loadKeywords(final Message loadedMessage) {
    final HashMap<String, String> keywords = new HashMap<>();
    for (int i = 0; i < loadedMessage.getKeywordCount(); i++) {
      final Keyword keyword = loadedMessage.getKeyword(i);
      final String key = keyword.getKey();
      final String value = keyword.getValue();
      keywords.put(key, value);
    }
    return keywords;
  }

  private static Subset loadSubset(Message.Subset subsetMessage) {
    BitSet mask = extractMaskFromSubsetMessage(subsetMessage);

    int dimensionCount = subsetMessage.getDimensionsCount();
    String[] dimensions = null;
    if (dimensionCount > 0) {
      dimensions = new String[dimensionCount];
      for (int j = 0; j < dimensionCount; j++)
        dimensions[j] = subsetMessage.getDimensions(j);
    }

    int descriptorCount = subsetMessage.getDoubleValueCount();
    Double[] descriptors = null;
    if (descriptorCount > 0) {
      descriptors = new Double[descriptorCount];
      for (int j = 0; j < descriptorCount; j++)
        descriptors[j] = subsetMessage.getDoubleValue(j);
    }


    return new Subset(subsetMessage.getName(), mask, subsetMessage.getParentID(),
        subsetMessage.getId(), subsetMessage.getSubsetType(), dimensions, descriptors);
  }

  public static FCSFrame load(FileInputStream input) throws IOException {
    final byte[] buffer = new byte[input.available()];
    int bytesRead = input.read(buffer);
    if (bytesRead == buffer.length) {
      return FCSFrame.load(buffer);
    } else {
      throw new IOException(LOAD_FAILURE);
    }
  }

  private static AbstractTransform readTransform(Dimension dim) {
    if (dim.hasPreferredTransform()) {
      Transform tBuffer = dim.getPreferredTransform();
      String transType = tBuffer.getTransform().name();
      if (transType.equals(TransformType.LOGICLE.toString())) {
        double t = tBuffer.getLogicleT();
        double w = tBuffer.getLogicleW();
        double m = tBuffer.getLogicleM();
        double a = tBuffer.getLogicleA();
        return new LogicleTransform(t, w, m, a);
      } else if (transType.equals(TransformType.LOGARITHMIC.toString())) {
        return new LogrithmicTransform(tBuffer.getLogMin(), tBuffer.getLogMax());

      } else if (transType.equals(TransformType.BOUNDARY.toString())) {
        return new BoundDisplayTransform(tBuffer.getBoundMin(), tBuffer.getBoundMax());
      } else {
        throw new IllegalArgumentException("Unsupported transform type.");
      }
    }
    return null;
  }

  public void addDimension(FCSDimension newDim) {
    if (rowCount == newDim.getSize()) {
      columnData.add(newDim);
    } else {
      throw new IllegalStateException(
          "New dimension does not match frame size: " + rowCount.toString());
    }
  }

  public void addSubset(Subset subset) {
    this.subsets.add(subset);
  }

  private Builder buildTransform(FCSDimension fcsdim) {
    // Set the preferred transformation.
    Message.Transform.Builder tBuilder = Message.Transform.newBuilder();
    AbstractTransform transform = fcsdim.getPreferredTransform();
    if (transform instanceof LogicleTransform) {
      LogicleTransform logicle = (LogicleTransform) transform;
      tBuilder.setTransform(Message.Transform.Type.LOGICLE);
      tBuilder.setLogicleT(logicle.getT());
      tBuilder.setLogicleW(logicle.getW());
      tBuilder.setLogicleM(logicle.getM());
      tBuilder.setLogicleA(logicle.getA());
    } else if (transform instanceof LogrithmicTransform) {
      LogrithmicTransform logTransform = (LogrithmicTransform) transform;
      tBuilder.setTransform(Message.Transform.Type.LOG);
      tBuilder.setLogMin(logTransform.getMinRawValue());
      tBuilder.setLogMax(logTransform.getMaxRawValue());
    } else if (transform instanceof BoundDisplayTransform) {
      tBuilder.setTransform(Message.Transform.Type.BOUNDARY);
      BoundDisplayTransform boundaryTransform = (BoundDisplayTransform) transform;
      tBuilder.setBoundMin(boundaryTransform.getMinTranformedValue());
      tBuilder.setBoundMax(boundaryTransform.getMaxTransformedValue());
    }
    tBuilder.setId(transform.getID());
    return tBuilder;
  }

  @Override
  public int compareTo(String arg0) {
    return this.getDisplayName().compareTo(arg0);
  }

  public FCSFrame deepCopy() {
    byte[] bytes = this.saveAsBytes();
    try {
      return FCSFrame.load(bytes);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
      return null;
    }
  }

  public String getCompRef() {
    return this.compReference;
  }

  public TreeSet<FCSDimension> getData() {
    return columnData;
  }

  public int getDimensionCount() {
    return getDimensionNames().size();
  }

  public ArrayList<String> getDimensionNames() {
    return columnData.stream().map(FCSDimension::getShortName)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  public double[] getRow(int index, boolean transformData) {
    final double[] row = new double[getDimensionCount()];
    int i = 0;
    if (transformData) {
      for (String shortName : getDimensionNames()) {
        FCSDimension dim = getDimension(shortName);
        double rawValue = dim.getData()[index];
        row[i] = dim.getPreferredTransform().transform(rawValue);
        i++;
      }
    } else {
      for (String shortName : getDimensionNames()) {
        FCSDimension dim = getDimension(shortName);
        row[i] = dim.getData()[index];
        i++;
      }
    }
    return row;
  }

  public String getDisplayName() {
    String name = getID();
    if (this.displayName != null) {
      name = this.displayName;
    }
    return name;
  }

  public FCSDimension getDimension(String shortName) {
    Optional<FCSDimension> matchingDimension = columnData.stream()
        .filter(dimension -> shortName.equals(dimension.getShortName())).findAny();
    if (matchingDimension.isPresent()) {
      return matchingDimension.get();
    } else {
      return null;
    }
  }

  public Map<String, String> getKeywords() {
    return keywords;
  }

  public String getKeywordValue(String keyword) {
    if (keywords.containsKey(keyword)){
      return keywords.get(keyword).trim();
    } else {
      return null;
    }
  }

  public int getRowCount() {
    return rowCount;
  }

  public Map<String, Integer> getSubsetRow(int index) {
    Map<String, Integer> map = new HashMap<>();
    for (Subset s : subsets) {
      boolean value = s.getMembers().get(index);
      if (value) {
        map.put(s.getLabel(), 1);
      } else {
        map.put(s.getLabel(), 0);
      }
    }
    return map;
  }

  public List<Subset> getSubsets() {
    return subsets;
  }

  public boolean hasDimension(String shortName) {
    Iterator<FCSDimension> iter = columnData.iterator();
    while (iter.hasNext()) {
      if (iter.next().getShortName().equals(shortName)) {
        return true;
      }
    }
    return false;
  }

  public boolean hasSubset(String subsetNameScatter) {
    for (Subset s : subsets) {
      if (s.getLabel().equals(subsetNameScatter)) {
        return true;
      }
    }
    return false;
  }

  public byte[] saveAsBytes() {
    final Message buffer = createMessage();
    return buffer.toByteArray();
  }
  
  public String saveAsString() {
    final Message buffer = createMessage();
    return buffer.toString();
  }

  private Message createMessage() {
    // create the builder
    final Message.Builder messageBuilder = Message.newBuilder();
    messageBuilder.setId(this.getID());
    messageBuilder.setEventCount(this.rowCount);

    // add the dimension names.
    for (final String name : getDimensionNames()) {
      messageBuilder.addDimNames(name);
    }

    // add the keywords.
    for (Entry<String, String> s : keywords.entrySet()) {
      final String key = s.getKey();
      final String value = s.getValue();
      final Message.Keyword.Builder keyBuilder = Message.Keyword.newBuilder();
      keyBuilder.setKey(key);
      keyBuilder.setValue(value);
      final Message.Keyword newKeyword = keyBuilder.build();
      messageBuilder.addKeyword(newKeyword);
    }
    // add the FCS Dimensions.
    for (FCSDimension dim : columnData) {
      Message.Dimension.Builder dimBuilder = Message.Dimension.newBuilder();
      dimBuilder.setIndex(dim.getIndex());
      dimBuilder.setPnn(dim.getShortName());
      dimBuilder.setPns(dim.getStainName());
      dimBuilder.setPneF1(dim.getPNEF1());
      dimBuilder.setPneF2(dim.getPNEF2());
      dimBuilder.setPnr(dim.getRange());
      dimBuilder.setId(dim.getID());

      // Add the numeric data
      final double[] rawArray = dim.getData();
      for (final double value : rawArray) {
        dimBuilder.addData(value);
      }
      Builder tBuilder = buildTransform(dim);
      dimBuilder.setPreferredTransform(tBuilder.build());

      final Message.Dimension fcsdim = dimBuilder.build();
      messageBuilder.addDimension(fcsdim);
    }

    // Add subsets
    if (subsets != null) {
      subsets.forEach(subset -> saveSubset(messageBuilder, subset));
    }
    return messageBuilder.build();
  }

  private void saveSubset(final Message.Builder messageBuilder, Subset currentSubset) {
    Message.Subset.Builder sBuilder = Message.Subset.newBuilder();
    sBuilder.setId(currentSubset.getID());
    sBuilder.setParentID(currentSubset.getParentID());
    sBuilder.setName(currentSubset.getLabel());
    sBuilder.setSubsetType(currentSubset.getType());
    String overrideID = currentSubset.getOverrideID();
    if (overrideID!=null){
      sBuilder.setOverrideID(currentSubset.getOverrideID());
    }

    String[] dimensions = currentSubset.getDimensions();
    if (dimensions != null)
      sBuilder.addAllDimensions(Arrays.asList(dimensions));

    Double[] descriptors = currentSubset.getDescriptors();
    if (descriptors != null)
      sBuilder.addAllDoubleValue(Arrays.asList(descriptors));

    BitSet mask = currentSubset.getMembers();
    for (int i = 0; i < mask.size(); i++)
      sBuilder.addMask(mask.get(i));
    Message.Subset subset = sBuilder.build();
    messageBuilder.addSubset(subset);
  }

  public int save(FileOutputStream out) throws IOException {
    final byte[] message = this.saveAsBytes();
    out.write(message);
    out.flush();
    return message.length;
  }

  public void setCompRef(String id) {
    this.compReference = id;
  }

  public void setData(TreeSet<FCSDimension> sortedSet) {
    columnData = sortedSet;
  }

  public void setDisplayName(String newDisplayName) {
    displayName = newDisplayName;
  }

  @Override
  public String toString() {
    return getDisplayName();
  }

  public FCSFrame getFilteredFrame(String referenceSubset) {
    Optional<Subset> targetSubset = subsets
      .stream()
      .filter(sub->sub.getLabel().equals(referenceSubset))
      .findAny();
    
    if (targetSubset.isPresent()){
      Subset currentSubset = targetSubset.get();
      List<Subset> ancestors = currentSubset.findAncestors(getSubsets());
      BitSet mask = currentSubset.evaluate(ancestors);
      return FCSUtilities.filterFrame(mask, this);
    } else {
      return null;
    }
  }

  public List<String> getSubsetNames() {
    return subsets.stream().map(Subset::getLabel).collect(Collectors.toList());
  }
}
