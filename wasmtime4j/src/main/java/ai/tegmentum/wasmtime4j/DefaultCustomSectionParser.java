package ai.tegmentum.wasmtime4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Default implementation of CustomSectionParser.
 *
 * <p>This implementation provides parsing support for standard WebAssembly custom sections
 * including "name", "producers", and "target_features" sections, as well as generic parsing
 * for arbitrary custom sections.
 *
 * @since 1.0.0
 */
final class DefaultCustomSectionParser implements CustomSectionParser {

  private static final Set<CustomSectionType> SUPPORTED_TYPES = Set.of(
      CustomSectionType.NAME,
      CustomSectionType.PRODUCERS,
      CustomSectionType.TARGET_FEATURES,
      CustomSectionType.UNKNOWN
  );

  @Override
  public Optional<CustomSection> parseCustomSection(final String name, final byte[] data) {
    if (name == null) {
      throw new IllegalArgumentException("Section name cannot be null");
    }
    if (data == null) {
      throw new IllegalArgumentException("Section data cannot be null");
    }

    try {
      final CustomSectionType type = CustomSectionType.fromName(name);
      return Optional.of(new CustomSection(name, data, type));
    } catch (final Exception e) {
      // Log the error and return empty
      return Optional.empty();
    }
  }

  @Override
  public Optional<NameSection> parseNameSection(final byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Section data cannot be null");
    }

    try {
      final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
      final NameSection.Builder builder = NameSection.builder();

      while (buffer.hasRemaining()) {
        final int subsectionType = readUnsignedByte(buffer);
        final int subsectionSize = readULEB128(buffer);

        if (subsectionSize < 0 || subsectionSize > buffer.remaining()) {
          break; // Invalid subsection
        }

        final byte[] subsectionData = new byte[subsectionSize];
        buffer.get(subsectionData);
        final ByteBuffer subsectionBuffer = ByteBuffer.wrap(subsectionData).order(ByteOrder.LITTLE_ENDIAN);

        switch (subsectionType) {
          case 0: // Module name
            builder.setModuleName(readString(subsectionBuffer));
            break;
          case 1: // Function names
            builder.setFunctionNames(readNameMap(subsectionBuffer));
            break;
          case 2: // Local names
            builder.setLocalNames(readLocalNameMap(subsectionBuffer));
            break;
          case 3: // Type names
            builder.setTypeNames(readNameMap(subsectionBuffer));
            break;
          case 4: // Table names
            builder.setTableNames(readNameMap(subsectionBuffer));
            break;
          case 5: // Memory names
            builder.setMemoryNames(readNameMap(subsectionBuffer));
            break;
          case 6: // Global names
            builder.setGlobalNames(readNameMap(subsectionBuffer));
            break;
          case 7: // Element segment names
            builder.setElementSegmentNames(readNameMap(subsectionBuffer));
            break;
          case 8: // Data segment names
            builder.setDataSegmentNames(readNameMap(subsectionBuffer));
            break;
          case 9: // Tag names
            builder.setTagNames(readNameMap(subsectionBuffer));
            break;
          default:
            // Unknown subsection type, skip
            break;
        }
      }

      return Optional.of(builder.build());
    } catch (final Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<ProducersSection> parseProducersSection(final byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Section data cannot be null");
    }

    try {
      final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
      final ProducersSection.Builder builder = ProducersSection.builder();

      final int fieldCount = readULEB128(buffer);
      for (int i = 0; i < fieldCount && buffer.hasRemaining(); i++) {
        final String fieldName = readString(buffer);
        final List<ProducersSection.ProducerEntry> entries = readProducerEntries(buffer);

        switch (fieldName) {
          case "language":
            builder.setLanguages(entries);
            break;
          case "processed-by":
            builder.setProcessedBy(entries);
            break;
          case "sdk":
            builder.setSdk(entries);
            break;
          default:
            // Unknown field, skip
            break;
        }
      }

      return Optional.of(builder.build());
    } catch (final Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<TargetFeaturesSection> parseTargetFeaturesSection(final byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Section data cannot be null");
    }

    try {
      final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
      final TargetFeaturesSection.Builder builder = TargetFeaturesSection.builder();

      final int featureCount = readULEB128(buffer);
      for (int i = 0; i < featureCount && buffer.hasRemaining(); i++) {
        final int prefix = readUnsignedByte(buffer);
        final String featureName = readString(buffer);

        final TargetFeaturesSection.FeatureStatus status;
        switch (prefix) {
          case 0x2B: // '+'
            status = TargetFeaturesSection.FeatureStatus.REQUIRED;
            break;
          case 0x3D: // '='
            status = TargetFeaturesSection.FeatureStatus.USED;
            break;
          case 0x2D: // '-'
            status = TargetFeaturesSection.FeatureStatus.DISABLED;
            break;
          default:
            continue; // Skip unknown status
        }

        builder.addFeature(new TargetFeaturesSection.FeatureEntry(featureName, status));
      }

      return Optional.of(builder.build());
    } catch (final Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean supports(final String sectionName) {
    if (sectionName == null) {
      throw new IllegalArgumentException("Section name cannot be null");
    }

    final CustomSectionType type = CustomSectionType.fromName(sectionName);
    return SUPPORTED_TYPES.contains(type);
  }

  @Override
  public Set<CustomSectionType> getSupportedTypes() {
    return SUPPORTED_TYPES;
  }

  @Override
  public CustomSectionValidationResult validateSection(final String name, final byte[] data) {
    if (name == null) {
      throw new IllegalArgumentException("Section name cannot be null");
    }
    if (data == null) {
      throw new IllegalArgumentException("Section data cannot be null");
    }

    final CustomSectionValidationResult.Builder builder = CustomSectionValidationResult.builder();

    // Basic size validation
    if (data.length == 0) {
      builder.addWarning(name, "Section is empty");
    }

    // Try to parse the section based on its type
    final CustomSectionType type = CustomSectionType.fromName(name);
    switch (type) {
      case NAME:
        if (!parseNameSection(data).isPresent()) {
          builder.addError(name, "Failed to parse name section");
        }
        break;
      case PRODUCERS:
        if (!parseProducersSection(data).isPresent()) {
          builder.addError(name, "Failed to parse producers section");
        }
        break;
      case TARGET_FEATURES:
        if (!parseTargetFeaturesSection(data).isPresent()) {
          builder.addError(name, "Failed to parse target features section");
        }
        break;
      case UNKNOWN:
        // For unknown sections, just do basic validation
        validateUnknownSection(name, data, builder);
        break;
      default:
        builder.addWarning(name, "Section type not yet supported for validation");
        break;
    }

    return builder.build();
  }

  @Override
  public Optional<CustomSection> createCustomSection(final String name,
                                                     final CustomSectionType type,
                                                     final Object structuredData) {
    if (name == null) {
      throw new IllegalArgumentException("Section name cannot be null");
    }
    if (type == null) {
      throw new IllegalArgumentException("Section type cannot be null");
    }
    if (structuredData == null) {
      throw new IllegalArgumentException("Structured data cannot be null");
    }

    try {
      byte[] data = null;

      switch (type) {
        case NAME:
          if (structuredData instanceof NameSection) {
            data = serializeNameSection((NameSection) structuredData).orElse(null);
          }
          break;
        case PRODUCERS:
          if (structuredData instanceof ProducersSection) {
            data = serializeProducersSection((ProducersSection) structuredData).orElse(null);
          }
          break;
        case TARGET_FEATURES:
          if (structuredData instanceof TargetFeaturesSection) {
            data = serializeTargetFeaturesSection((TargetFeaturesSection) structuredData).orElse(null);
          }
          break;
        default:
          return Optional.empty();
      }

      if (data != null) {
        return Optional.of(new CustomSection(name, data, type));
      }
    } catch (final Exception e) {
      // Serialization failed
    }

    return Optional.empty();
  }

  @Override
  public Optional<byte[]> serializeNameSection(final NameSection nameSection) {
    if (nameSection == null) {
      throw new IllegalArgumentException("Name section cannot be null");
    }

    try {
      final List<byte[]> subsections = new ArrayList<>();

      // Module name subsection (type 0)
      if (nameSection.getModuleName().isPresent()) {
        subsections.add(createSubsection(0, writeString(nameSection.getModuleName().get())));
      }

      // Function names subsection (type 1)
      if (!nameSection.getFunctionNames().isEmpty()) {
        subsections.add(createSubsection(1, writeNameMap(nameSection.getFunctionNames())));
      }

      // Local names subsection (type 2)
      if (!nameSection.getAllLocalNames().isEmpty()) {
        subsections.add(createSubsection(2, writeLocalNameMap(nameSection.getAllLocalNames())));
      }

      // Type names subsection (type 3)
      if (!nameSection.getTypeNames().isEmpty()) {
        subsections.add(createSubsection(3, writeNameMap(nameSection.getTypeNames())));
      }

      // Table names subsection (type 4)
      if (!nameSection.getTableNames().isEmpty()) {
        subsections.add(createSubsection(4, writeNameMap(nameSection.getTableNames())));
      }

      // Memory names subsection (type 5)
      if (!nameSection.getMemoryNames().isEmpty()) {
        subsections.add(createSubsection(5, writeNameMap(nameSection.getMemoryNames())));
      }

      // Global names subsection (type 6)
      if (!nameSection.getGlobalNames().isEmpty()) {
        subsections.add(createSubsection(6, writeNameMap(nameSection.getGlobalNames())));
      }

      // Element segment names subsection (type 7)
      if (!nameSection.getElementSegmentNames().isEmpty()) {
        subsections.add(createSubsection(7, writeNameMap(nameSection.getElementSegmentNames())));
      }

      // Data segment names subsection (type 8)
      if (!nameSection.getDataSegmentNames().isEmpty()) {
        subsections.add(createSubsection(8, writeNameMap(nameSection.getDataSegmentNames())));
      }

      // Tag names subsection (type 9)
      if (!nameSection.getTagNames().isEmpty()) {
        subsections.add(createSubsection(9, writeNameMap(nameSection.getTagNames())));
      }

      return Optional.of(combineByteArrays(subsections));
    } catch (final Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<byte[]> serializeProducersSection(final ProducersSection producersSection) {
    if (producersSection == null) {
      throw new IllegalArgumentException("Producers section cannot be null");
    }

    try {
      final List<byte[]> fields = new ArrayList<>();

      if (!producersSection.getLanguages().isEmpty()) {
        fields.add(writeProducerField("language", producersSection.getLanguages()));
      }

      if (!producersSection.getProcessedBy().isEmpty()) {
        fields.add(writeProducerField("processed-by", producersSection.getProcessedBy()));
      }

      if (!producersSection.getSdk().isEmpty()) {
        fields.add(writeProducerField("sdk", producersSection.getSdk()));
      }

      final byte[] fieldsData = combineByteArrays(fields);
      final byte[] fieldCount = writeULEB128(fields.size());

      return Optional.of(combineByteArrays(List.of(fieldCount, fieldsData)));
    } catch (final Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<byte[]> serializeTargetFeaturesSection(final TargetFeaturesSection targetFeaturesSection) {
    if (targetFeaturesSection == null) {
      throw new IllegalArgumentException("Target features section cannot be null");
    }

    try {
      final List<byte[]> features = new ArrayList<>();

      for (final TargetFeaturesSection.FeatureEntry feature : targetFeaturesSection.getFeatures()) {
        final byte prefix;
        switch (feature.getStatus()) {
          case REQUIRED:
            prefix = 0x2B; // '+'
            break;
          case USED:
            prefix = 0x3D; // '='
            break;
          case DISABLED:
            prefix = 0x2D; // '-'
            break;
          default:
            continue; // Skip unknown status
        }

        final byte[] featureData = combineByteArrays(List.of(
            new byte[]{prefix},
            writeString(feature.getName())
        ));
        features.add(featureData);
      }

      final byte[] featuresData = combineByteArrays(features);
      final byte[] featureCount = writeULEB128(features.size());

      return Optional.of(combineByteArrays(List.of(featureCount, featuresData)));
    } catch (final Exception e) {
      return Optional.empty();
    }
  }

  // Helper methods for reading binary data

  private int readUnsignedByte(final ByteBuffer buffer) {
    return buffer.get() & 0xFF;
  }

  private int readULEB128(final ByteBuffer buffer) {
    int result = 0;
    int shift = 0;
    byte b;

    do {
      b = buffer.get();
      result |= (b & 0x7F) << shift;
      shift += 7;
    } while ((b & 0x80) != 0);

    return result;
  }

  private String readString(final ByteBuffer buffer) {
    final int length = readULEB128(buffer);
    final byte[] bytes = new byte[length];
    buffer.get(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private Map<Integer, String> readNameMap(final ByteBuffer buffer) {
    final Map<Integer, String> nameMap = new HashMap<>();
    final int count = readULEB128(buffer);

    for (int i = 0; i < count && buffer.hasRemaining(); i++) {
      final int index = readULEB128(buffer);
      final String name = readString(buffer);
      nameMap.put(index, name);
    }

    return nameMap;
  }

  private Map<Integer, Map<Integer, String>> readLocalNameMap(final ByteBuffer buffer) {
    final Map<Integer, Map<Integer, String>> localNameMap = new HashMap<>();
    final int functionCount = readULEB128(buffer);

    for (int i = 0; i < functionCount && buffer.hasRemaining(); i++) {
      final int functionIndex = readULEB128(buffer);
      final Map<Integer, String> locals = readNameMap(buffer);
      localNameMap.put(functionIndex, locals);
    }

    return localNameMap;
  }

  private List<ProducersSection.ProducerEntry> readProducerEntries(final ByteBuffer buffer) {
    final List<ProducersSection.ProducerEntry> entries = new ArrayList<>();
    final int entryCount = readULEB128(buffer);

    for (int i = 0; i < entryCount && buffer.hasRemaining(); i++) {
      final String name = readString(buffer);
      final String version = readString(buffer);
      entries.add(new ProducersSection.ProducerEntry(name, version));
    }

    return entries;
  }

  // Helper methods for writing binary data

  private byte[] writeULEB128(final int value) {
    final List<Byte> bytes = new ArrayList<>();
    int remaining = value;

    do {
      byte b = (byte) (remaining & 0x7F);
      remaining >>>= 7;
      if (remaining != 0) {
        b |= 0x80;
      }
      bytes.add(b);
    } while (remaining != 0);

    final byte[] result = new byte[bytes.size()];
    for (int i = 0; i < bytes.size(); i++) {
      result[i] = bytes.get(i);
    }
    return result;
  }

  private byte[] writeString(final String str) {
    final byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
    final byte[] lengthBytes = writeULEB128(strBytes.length);
    return combineByteArrays(List.of(lengthBytes, strBytes));
  }

  private byte[] writeNameMap(final Map<Integer, String> nameMap) {
    final List<byte[]> entries = new ArrayList<>();

    for (final Map.Entry<Integer, String> entry : nameMap.entrySet()) {
      entries.add(combineByteArrays(List.of(
          writeULEB128(entry.getKey()),
          writeString(entry.getValue())
      )));
    }

    final byte[] entriesData = combineByteArrays(entries);
    final byte[] count = writeULEB128(nameMap.size());

    return combineByteArrays(List.of(count, entriesData));
  }

  private byte[] writeLocalNameMap(final Map<Integer, Map<Integer, String>> localNameMap) {
    final List<byte[]> functions = new ArrayList<>();

    for (final Map.Entry<Integer, Map<Integer, String>> entry : localNameMap.entrySet()) {
      functions.add(combineByteArrays(List.of(
          writeULEB128(entry.getKey()),
          writeNameMap(entry.getValue())
      )));
    }

    final byte[] functionsData = combineByteArrays(functions);
    final byte[] count = writeULEB128(localNameMap.size());

    return combineByteArrays(List.of(count, functionsData));
  }

  private byte[] writeProducerField(final String fieldName, final List<ProducersSection.ProducerEntry> entries) {
    final List<byte[]> entryData = new ArrayList<>();

    for (final ProducersSection.ProducerEntry entry : entries) {
      entryData.add(combineByteArrays(List.of(
          writeString(entry.getName()),
          writeString(entry.getVersion() != null ? entry.getVersion() : "")
      )));
    }

    final byte[] entriesData = combineByteArrays(entryData);
    final byte[] entryCount = writeULEB128(entries.size());

    return combineByteArrays(List.of(
        writeString(fieldName),
        entryCount,
        entriesData
    ));
  }

  private byte[] createSubsection(final int type, final byte[] data) {
    final byte[] typeBytes = new byte[]{(byte) type};
    final byte[] sizeBytes = writeULEB128(data.length);
    return combineByteArrays(List.of(typeBytes, sizeBytes, data));
  }

  private byte[] combineByteArrays(final List<byte[]> arrays) {
    final int totalLength = arrays.stream().mapToInt(arr -> arr.length).sum();
    final byte[] result = new byte[totalLength];
    int offset = 0;

    for (final byte[] array : arrays) {
      System.arraycopy(array, 0, result, offset, array.length);
      offset += array.length;
    }

    return result;
  }

  private void validateUnknownSection(final String name, final byte[] data,
                                     final CustomSectionValidationResult.Builder builder) {
    // Basic validation for unknown sections
    if (data.length > CustomSectionSecurity.MAX_CUSTOM_SECTION_SIZE) {
      builder.addError(name, "Section size exceeds maximum allowed size");
    }

    if (CustomSectionSecurity.containsSuspiciousPatterns(data)) {
      builder.addWarning(name, "Section contains suspicious patterns");
    }
  }
}