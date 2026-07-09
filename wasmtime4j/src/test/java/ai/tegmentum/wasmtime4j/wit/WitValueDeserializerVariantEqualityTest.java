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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import ai.tegmentum.wasmtime4j.exception.ValidationException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Root-cause regression tests for {@link WitValueDeserializer#deserialize(int, byte[])} on variant
 * values. Two variants of the same nominal WIT type (e.g. the {@code value} variant with cases
 * {@code iri | literal | bnode}) used to deserialize to distinct {@link WitType} instances because
 * each embedded only the observed case in its cases map. The fix collapses every deserialized
 * variant onto a shared synthetic {@link WitType} singleton so cross-instance {@code equals()}
 * holds regardless of which runtime case appeared on the wire.
 */
@DisplayName("Deserialized variant type equality")
final class WitValueDeserializerVariantEqualityTest {

  private static final int VARIANT_DISCRIMINATOR = 12;
  private static final int STRING_DISCRIMINATOR = 6;

  private static byte[] variantWithStringPayload(final String caseName, final String payload) {
    final byte[] nameBytes = caseName.getBytes(StandardCharsets.UTF_8);
    final byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
    final int payloadDataLen = 4 + payloadBytes.length;
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

  @Test
  @DisplayName("Two deserialized variants with different observed cases share the same WitType")
  void deserializedVariantsShareCanonicalType() throws ValidationException {
    final WitValue iri =
        WitValueDeserializer.deserialize(
            VARIANT_DISCRIMINATOR, variantWithStringPayload("iri", "http://example.com/a"));
    final WitValue literal =
        WitValueDeserializer.deserialize(
            VARIANT_DISCRIMINATOR, variantWithStringPayload("literal", "hello"));

    // Both nominal-.equals() (root-cause fix) AND same identity (singleton is deliberate).
    assertEquals(iri.getType(), literal.getType());
    assertSame(iri.getType(), literal.getType());

    // The observed case name still flows through unchanged — no information loss.
    assertEquals("iri", ((WitVariant) iri).getCaseName());
    assertEquals("literal", ((WitVariant) literal).getCaseName());
  }
}
