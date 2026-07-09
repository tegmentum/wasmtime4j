/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasmtime4j.wit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.tegmentum.wasmtime4j.exception.ValidationException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the mixed-variant list-validation bug that blocked the Jena webfunction
 * plugin's {@code wf_tree_rows.wasm} round-trip.
 *
 * <p>When a WIT guest returns {@code list<binding>} where {@code binding.value} is a variant (e.g.
 * {@code iri | literal | bnode}) and sibling rows carry different runtime cases, each variant
 * instance is deserialized by {@link WitValueDeserializer#deserialize(int, byte[])} with a
 * cases-map that only reflects the observed case. Before the fix, {@link WitList#of} rejected the
 * second element because its independently-reconstructed variant type wasn't {@link Object#equals}
 * to the first's, even though they were structurally the same synthetic placeholder. The fix is to
 * compare element types with {@link WitType#isStructurallyCompatibleWith(WitType)} instead of
 * strict equality, treating two "deserialized_variant" placeholders as compatible when overlapping
 * cases have compatible payloads.
 */
@DisplayName("WitList mixed-variant regression")
final class WitListMixedVariantTest {

  private static final int VARIANT_DISCRIMINATOR = 12;
  private static final int RECORD_DISCRIMINATOR = 7;
  private static final int STRING_DISCRIMINATOR = 6;

  /**
   * Builds the wire format for a variant with a UTF-8 string payload. Format: {@code [name_len:
   * u32][name: UTF-8][has_payload: u8=1][payload_disc: i32=6] [payload_len: u32][payload: string]}.
   */
  private static byte[] variantWithStringPayload(final String caseName, final String payload) {
    final byte[] nameBytes = caseName.getBytes(StandardCharsets.UTF_8);
    final byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
    final int payloadDataLen = 4 + payloadBytes.length; // string wire = [len][bytes]
    final int total = 4 + nameBytes.length + 1 + 4 + 4 + payloadDataLen;
    final ByteBuffer buf = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
    buf.putInt(nameBytes.length);
    buf.put(nameBytes);
    buf.put((byte) 1);
    buf.putInt(STRING_DISCRIMINATOR);
    buf.putInt(payloadDataLen);
    buf.putInt(payloadBytes.length);
    buf.put(payloadBytes);
    return buf.array();
  }

  /**
   * Builds the wire format for a record with a single "value" field carrying an already- serialized
   * variant (the {@code binding} shape simplified for this regression). Format matches {@link
   * WitValueDeserializer} record parsing: {@code [field_count: u32][for each: name_len, name, disc,
   * data_len, data]}.
   */
  private static byte[] recordWrappingVariant(final byte[] variantData) {
    final byte[] fieldNameBytes = "value".getBytes(StandardCharsets.UTF_8);
    final int total = 4 + 4 + fieldNameBytes.length + 4 + 4 + variantData.length;
    final ByteBuffer buf = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
    buf.putInt(1); // field count
    buf.putInt(fieldNameBytes.length);
    buf.put(fieldNameBytes);
    buf.putInt(VARIANT_DISCRIMINATOR);
    buf.putInt(variantData.length);
    buf.put(variantData);
    return buf.array();
  }

  @Test
  @DisplayName("WitList.of accepts sibling variants deserialized with different observed cases")
  void mixedVariantListAcceptsBothCases() throws ValidationException {
    final WitValue iri =
        WitValueDeserializer.deserialize(
            VARIANT_DISCRIMINATOR, variantWithStringPayload("iri", "http://example.com/a"));
    final WitValue literal =
        WitValueDeserializer.deserialize(
            VARIANT_DISCRIMINATOR, variantWithStringPayload("literal", "hello"));

    assertNotNull(iri);
    assertNotNull(literal);

    // Before the fix this threw IllegalArgumentException from WitList.validate.
    final WitList list = assertDoesNotThrow(() -> WitList.of(iri, literal));
    assertEquals(2, list.size());
  }

  @Test
  @DisplayName("WitList.of accepts records whose variant field carries different observed cases")
  void mixedVariantRecordListMatchesBindingShape() throws ValidationException {
    final byte[] iriRow =
        recordWrappingVariant(variantWithStringPayload("iri", "http://example.com/a"));
    final byte[] literalRow = recordWrappingVariant(variantWithStringPayload("literal", "hello"));

    final WitValue rowIri = WitValueDeserializer.deserialize(RECORD_DISCRIMINATOR, iriRow);
    final WitValue rowLiteral = WitValueDeserializer.deserialize(RECORD_DISCRIMINATOR, literalRow);

    // Wrap both orderings to prove symmetry of isStructurallyCompatibleWith — first-element
    // inference must not depend on which row appears first.
    final WitList forward = assertDoesNotThrow(() -> WitList.of(Arrays.asList(rowIri, rowLiteral)));
    final WitList reverse = assertDoesNotThrow(() -> WitList.of(Arrays.asList(rowLiteral, rowIri)));

    assertEquals(2, forward.size());
    assertEquals(2, reverse.size());
  }
}
