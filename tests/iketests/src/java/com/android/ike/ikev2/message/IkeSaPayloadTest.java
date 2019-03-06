/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ike.ikev2.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.util.Pair;

import com.android.ike.ikev2.SaProposal;
import com.android.ike.ikev2.exceptions.IkeException;
import com.android.ike.ikev2.exceptions.InvalidSyntaxException;
import com.android.ike.ikev2.message.IkeSaPayload.Attribute;
import com.android.ike.ikev2.message.IkeSaPayload.AttributeDecoder;
import com.android.ike.ikev2.message.IkeSaPayload.DhGroupTransform;
import com.android.ike.ikev2.message.IkeSaPayload.EncryptionTransform;
import com.android.ike.ikev2.message.IkeSaPayload.EsnTransform;
import com.android.ike.ikev2.message.IkeSaPayload.IntegrityTransform;
import com.android.ike.ikev2.message.IkeSaPayload.KeyLengthAttribute;
import com.android.ike.ikev2.message.IkeSaPayload.PrfTransform;
import com.android.ike.ikev2.message.IkeSaPayload.Proposal;
import com.android.ike.ikev2.message.IkeSaPayload.Transform;
import com.android.ike.ikev2.message.IkeSaPayload.TransformDecoder;
import com.android.ike.ikev2.message.IkeSaPayload.UnrecognizedAttribute;
import com.android.ike.ikev2.message.IkeSaPayload.UnrecognizedTransform;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public final class IkeSaPayloadTest {
    private static final String PROPOSAL_RAW_PACKET =
            "0000002c010100040300000c0100000c800e0080030000080300000203000008040"
                    + "000020000000802000002";

    private static final String TWO_PROPOSAL_RAW_PACKET =
            "020000dc010100190300000c0100000c800e00800300000c0100000c800e00c0030"
                    + "0000c0100000c800e01000300000801000003030000080300000c0300"
                    + "00080300000d030000080300000e03000008030000020300000803000"
                    + "005030000080200000503000008020000060300000802000007030000"
                    + "080200000403000008020000020300000804000013030000080400001"
                    + "40300000804000015030000080400001c030000080400001d03000008"
                    + "0400001e030000080400001f030000080400000f03000008040000100"
                    + "300000804000012000000080400000e000001000201001a0300000c01"
                    + "000014800e00800300000c01000014800e00c00300000c01000014800"
                    + "e01000300000c0100001c800e01000300000c01000013800e00800300"
                    + "000c01000013800e00c00300000c01000013800e01000300000c01000"
                    + "012800e00800300000c01000012800e00c00300000c01000012800e01"
                    + "000300000802000005030000080200000603000008020000070300000"
                    + "802000004030000080200000203000008040000130300000804000014"
                    + "0300000804000015030000080400001c030000080400001d030000080"
                    + "400001e030000080400001f030000080400000f030000080400001003"
                    + "00000804000012000000080400000e";
    private static final String ENCR_TRANSFORM_RAW_PACKET = "0300000c0100000c800e0080";
    private static final String PRF_TRANSFORM_RAW_PACKET = "0000000802000002";
    private static final String INTEG_TRANSFORM_RAW_PACKET = "0300000803000002";
    private static final String DH_GROUP_TRANSFORM_RAW_PACKET = "0300000804000002";
    private static final String ESN_TRANSFORM_RAW_PACKET = "0000000805000000";

    private static final int TRANSFORM_TYPE_OFFSET = 4;
    private static final int TRANSFORM_ID_OFFSET = 7;

    private static final String ATTRIBUTE_RAW_PACKET = "800e0080";

    private static final int PROPOSAL_NUMBER = 1;

    @IkePayload.ProtocolId
    private static final int PROPOSAL_PROTOCOL_ID = IkePayload.PROTOCOL_ID_IKE;

    private static final byte PROPOSAL_SPI_SIZE = 0;
    private static final byte PROPOSAL_SPI = 0;

    // Constants for multiple proposals test
    private static final byte[] PROPOSAL_NUMBER_LIST = {1, 2};

    private static final int KEY_LEN = 128;

    private AttributeDecoder mMockedAttributeDecoder;
    private KeyLengthAttribute mAttributeKeyLength128;
    private List<Attribute> mAttributeListWithKeyLength128;

    @Before
    public void setUp() throws Exception {
        mMockedAttributeDecoder = mock(AttributeDecoder.class);
        mAttributeKeyLength128 = new KeyLengthAttribute(SaProposal.KEY_LEN_AES_128);
        mAttributeListWithKeyLength128 = new LinkedList<>();
        mAttributeListWithKeyLength128.add(mAttributeKeyLength128);
    }

    @Test
    public void testDecodeAttribute() throws Exception {
        byte[] inputPacket = TestUtils.hexStringToByteArray(ATTRIBUTE_RAW_PACKET);
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputPacket);

        Pair<Attribute, Integer> pair = Attribute.readFrom(inputBuffer);
        Attribute attribute = pair.first;

        assertTrue(attribute instanceof KeyLengthAttribute);
        assertEquals(Attribute.ATTRIBUTE_TYPE_KEY_LENGTH, attribute.type);
        assertEquals(KEY_LEN, ((KeyLengthAttribute) attribute).keyLength);
    }

    @Test
    public void testDecodeEncryptionTransform() throws Exception {
        byte[] inputPacket = TestUtils.hexStringToByteArray(ENCR_TRANSFORM_RAW_PACKET);
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputPacket);

        when(mMockedAttributeDecoder.decodeAttributes(anyInt(), any()))
                .thenReturn(mAttributeListWithKeyLength128);
        Transform.sAttributeDecoder = mMockedAttributeDecoder;

        Transform transform = Transform.readFrom(inputBuffer);
        assertTrue(transform instanceof EncryptionTransform);
        assertEquals(Transform.TRANSFORM_TYPE_ENCR, transform.type);
        assertEquals(SaProposal.ENCRYPTION_ALGORITHM_AES_CBC, transform.id);
        assertTrue(transform.isSupported);
    }

    @Test
    public void testDecodeEncryptionTransformWithInvalidKeyLength() throws Exception {
        byte[] inputPacket = TestUtils.hexStringToByteArray(ENCR_TRANSFORM_RAW_PACKET);
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputPacket);

        List<Attribute> attributeList = new LinkedList<>();
        Attribute keyLengAttr = new KeyLengthAttribute(SaProposal.KEY_LEN_AES_128 + 1);
        attributeList.add(keyLengAttr);

        when(mMockedAttributeDecoder.decodeAttributes(anyInt(), any())).thenReturn(attributeList);
        Transform.sAttributeDecoder = mMockedAttributeDecoder;

        try {
            Transform.readFrom(inputBuffer);
            fail("Expected InvalidSyntaxException for invalid key length.");
        } catch (InvalidSyntaxException expected) {
        }
    }

    @Test
    public void testConstructEncryptionTransformWithUnsupportedId() throws Exception {
        try {
            new EncryptionTransform(-1);
            fail("Expected IllegalArgumentException for unsupported Transform ID");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testConstructEncryptionTransformWithInvalidKeyLength() throws Exception {
        try {
            new EncryptionTransform(SaProposal.ENCRYPTION_ALGORITHM_3DES, 129);
            fail("Expected IllegalArgumentException for invalid key length.");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testDecodePrfTransform() throws Exception {
        byte[] inputPacket = TestUtils.hexStringToByteArray(PRF_TRANSFORM_RAW_PACKET);
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputPacket);

        when(mMockedAttributeDecoder.decodeAttributes(anyInt(), any()))
                .thenReturn(new LinkedList<Attribute>());
        Transform.sAttributeDecoder = mMockedAttributeDecoder;

        Transform transform = Transform.readFrom(inputBuffer);
        assertTrue(transform instanceof PrfTransform);
        assertEquals(Transform.TRANSFORM_TYPE_PRF, transform.type);
        assertEquals(SaProposal.PSEUDORANDOM_FUNCTION_HMAC_SHA1, transform.id);
        assertTrue(transform.isSupported);
    }

    @Test
    public void testConstructPrfTransformWithUnsupportedId() throws Exception {
        try {
            new PrfTransform(-1);
            fail("Expected IllegalArgumentException for unsupported Transform ID");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testDecodeIntegrityTransform() throws Exception {
        byte[] inputPacket = TestUtils.hexStringToByteArray(INTEG_TRANSFORM_RAW_PACKET);
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputPacket);

        when(mMockedAttributeDecoder.decodeAttributes(anyInt(), any()))
                .thenReturn(new LinkedList<Attribute>());
        Transform.sAttributeDecoder = mMockedAttributeDecoder;

        Transform transform = Transform.readFrom(inputBuffer);
        assertTrue(transform instanceof IntegrityTransform);
        assertEquals(Transform.TRANSFORM_TYPE_INTEG, transform.type);
        assertEquals(SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA1_96, transform.id);
        assertTrue(transform.isSupported);
    }

    @Test
    public void testDecodeIntegrityTransformWithUnrecognizedAttribute() throws Exception {
        byte[] inputPacket = TestUtils.hexStringToByteArray(INTEG_TRANSFORM_RAW_PACKET);
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputPacket);

        when(mMockedAttributeDecoder.decodeAttributes(anyInt(), any()))
                .thenReturn(mAttributeListWithKeyLength128);
        Transform.sAttributeDecoder = mMockedAttributeDecoder;

        Transform transform = Transform.readFrom(inputBuffer);
        assertTrue(transform instanceof IntegrityTransform);
        assertEquals(Transform.TRANSFORM_TYPE_INTEG, transform.type);
        assertEquals(SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA1_96, transform.id);
        assertFalse(transform.isSupported);
    }

    @Test
    public void testConstructIntegrityTransformWithUnsupportedId() throws Exception {
        try {
            new IntegrityTransform(-1);
            fail("Expected IllegalArgumentException for unsupported Transform ID");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testDecodeDhGroupTransform() throws Exception {
        byte[] inputPacket = TestUtils.hexStringToByteArray(DH_GROUP_TRANSFORM_RAW_PACKET);
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputPacket);

        when(mMockedAttributeDecoder.decodeAttributes(anyInt(), any()))
                .thenReturn(new LinkedList<Attribute>());
        Transform.sAttributeDecoder = mMockedAttributeDecoder;

        Transform transform = Transform.readFrom(inputBuffer);
        assertTrue(transform instanceof DhGroupTransform);
        assertEquals(Transform.TRANSFORM_TYPE_DH, transform.type);
        assertEquals(SaProposal.DH_GROUP_1024_BIT_MODP, transform.id);
        assertTrue(transform.isSupported);
    }

    @Test
    public void testConstructDhGroupTransformWithUnsupportedId() throws Exception {
        try {
            new DhGroupTransform(-1);
            fail("Expected IllegalArgumentException for unsupported Transform ID");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testDecodeEsnTransform() throws Exception {
        byte[] inputPacket = TestUtils.hexStringToByteArray(ESN_TRANSFORM_RAW_PACKET);
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputPacket);

        when(mMockedAttributeDecoder.decodeAttributes(anyInt(), any()))
                .thenReturn(new LinkedList<Attribute>());
        Transform.sAttributeDecoder = mMockedAttributeDecoder;

        Transform transform = Transform.readFrom(inputBuffer);
        assertTrue(transform instanceof EsnTransform);
        assertEquals(Transform.TRANSFORM_TYPE_ESN, transform.type);
        assertEquals(EsnTransform.ESN_POLICY_NO_EXTENDED, transform.id);
        assertTrue(transform.isSupported);
    }

    @Test
    public void testDecodeEsnTransformWithUnsupportedId() throws Exception {
        byte[] inputPacket = TestUtils.hexStringToByteArray(ESN_TRANSFORM_RAW_PACKET);
        inputPacket[TRANSFORM_ID_OFFSET] = -1;
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputPacket);

        when(mMockedAttributeDecoder.decodeAttributes(anyInt(), any()))
                .thenReturn(new LinkedList<Attribute>());
        Transform.sAttributeDecoder = mMockedAttributeDecoder;

        Transform transform = Transform.readFrom(inputBuffer);
        assertTrue(transform instanceof EsnTransform);
        assertEquals(Transform.TRANSFORM_TYPE_ESN, transform.type);
        assertFalse(transform.isSupported);
    }

    @Test
    public void testDecodeEsnTransformWithUnrecognizedAttribute() throws Exception {
        byte[] inputPacket = TestUtils.hexStringToByteArray(ESN_TRANSFORM_RAW_PACKET);
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputPacket);

        when(mMockedAttributeDecoder.decodeAttributes(anyInt(), any()))
                .thenReturn(mAttributeListWithKeyLength128);
        Transform.sAttributeDecoder = mMockedAttributeDecoder;

        Transform transform = Transform.readFrom(inputBuffer);
        assertTrue(transform instanceof EsnTransform);
        assertEquals(Transform.TRANSFORM_TYPE_ESN, transform.type);
        assertEquals(EsnTransform.ESN_POLICY_NO_EXTENDED, transform.id);
        assertFalse(transform.isSupported);
    }

    @Test
    public void testDecodeUnrecognizedTransform() throws Exception {
        byte[] inputPacket = TestUtils.hexStringToByteArray(ENCR_TRANSFORM_RAW_PACKET);
        inputPacket[TRANSFORM_TYPE_OFFSET] = 6;
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputPacket);

        when(mMockedAttributeDecoder.decodeAttributes(anyInt(), any()))
                .thenReturn(mAttributeListWithKeyLength128);
        Transform.sAttributeDecoder = mMockedAttributeDecoder;

        Transform transform = Transform.readFrom(inputBuffer);

        assertTrue(transform instanceof UnrecognizedTransform);
    }

    @Test
    public void testDecodeTransformWithRepeatedAttribute() throws Exception {
        byte[] inputPacket = TestUtils.hexStringToByteArray(ENCR_TRANSFORM_RAW_PACKET);
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputPacket);

        List<Attribute> attributeList = new LinkedList<>();
        attributeList.add(mAttributeKeyLength128);
        attributeList.add(mAttributeKeyLength128);

        when(mMockedAttributeDecoder.decodeAttributes(anyInt(), any())).thenReturn(attributeList);
        Transform.sAttributeDecoder = mMockedAttributeDecoder;

        try {
            Transform.readFrom(inputBuffer);
            fail("Expected InvalidSyntaxException for repeated Attribute Type Key Length.");
        } catch (InvalidSyntaxException expected) {
        }
    }

    @Test
    public void testDecodeTransformWithUnrecognizedTransformId() throws Exception {
        byte[] inputPacket = TestUtils.hexStringToByteArray(ENCR_TRANSFORM_RAW_PACKET);
        inputPacket[TRANSFORM_ID_OFFSET] = 1;
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputPacket);

        when(mMockedAttributeDecoder.decodeAttributes(anyInt(), any()))
                .thenReturn(mAttributeListWithKeyLength128);
        Transform.sAttributeDecoder = mMockedAttributeDecoder;

        Transform transform = Transform.readFrom(inputBuffer);

        assertFalse(transform.isSupported);
    }

    @Test
    public void testDecodeTransformWithUnrecogniedAttributeType() throws Exception {
        byte[] inputPacket = TestUtils.hexStringToByteArray(ENCR_TRANSFORM_RAW_PACKET);
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputPacket);

        List<Attribute> attributeList = new LinkedList<>();
        attributeList.add(mAttributeKeyLength128);
        Attribute attributeUnrecognized = new UnrecognizedAttribute(1, new byte[0]);
        attributeList.add(attributeUnrecognized);

        when(mMockedAttributeDecoder.decodeAttributes(anyInt(), any())).thenReturn(attributeList);
        Transform.sAttributeDecoder = mMockedAttributeDecoder;

        Transform transform = Transform.readFrom(inputBuffer);

        assertFalse(transform.isSupported);
    }

    @Test
    public void testTransformEquals() throws Exception {
        EncryptionTransform mEncrAesGcm8Key128TransformLeft =
                new EncryptionTransform(
                        SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_8, SaProposal.KEY_LEN_AES_128);
        EncryptionTransform mEncrAesGcm8Key128TransformRight =
                new EncryptionTransform(
                        SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_8, SaProposal.KEY_LEN_AES_128);

        assertEquals(mEncrAesGcm8Key128TransformLeft, mEncrAesGcm8Key128TransformRight);

        EncryptionTransform mEncrAesGcm8Key192TransformLeft =
                new EncryptionTransform(
                        SaProposal.ENCRYPTION_ALGORITHM_AES_GCM_8, SaProposal.KEY_LEN_AES_192);

        assertNotEquals(mEncrAesGcm8Key128TransformLeft, mEncrAesGcm8Key192TransformLeft);

        IntegrityTransform mIntegHmacSha1TransformLeft =
                new IntegrityTransform(SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA1_96);
        IntegrityTransform mIntegHmacSha1TransformRight =
                new IntegrityTransform(SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA1_96);

        assertNotEquals(mEncrAesGcm8Key128TransformLeft, mIntegHmacSha1TransformLeft);
        assertEquals(mIntegHmacSha1TransformLeft, mIntegHmacSha1TransformRight);
    }

    @Test
    public void testDecodeSingleProposal() throws Exception {
        byte[] inputPacket = TestUtils.hexStringToByteArray(PROPOSAL_RAW_PACKET);
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputPacket);
        TransformDecoder mockedDecoder = mock(TransformDecoder.class);
        when(mockedDecoder.decodeTransforms(anyInt(), any())).thenReturn(new Transform[0]);
        Proposal.sTransformDecoder = mockedDecoder;

        Proposal proposal = Proposal.readFrom(inputBuffer);

        assertEquals(PROPOSAL_NUMBER, proposal.number);
        assertEquals(PROPOSAL_PROTOCOL_ID, proposal.protocolId);
        assertEquals(PROPOSAL_SPI_SIZE, proposal.spiSize);
        assertEquals(PROPOSAL_SPI, proposal.spi);
        assertEquals(0, proposal.transformArray.length);
    }

    @Test
    public void testDecodeMultipleProposal() throws Exception {
        byte[] inputPacket = TestUtils.hexStringToByteArray(TWO_PROPOSAL_RAW_PACKET);
        Proposal.sTransformDecoder =
                new TransformDecoder() {
                    @Override
                    public Transform[] decodeTransforms(int count, ByteBuffer inputBuffer)
                            throws IkeException {
                        for (int i = 0; i < count; i++) {
                            // Read length field and move position
                            inputBuffer.getShort();
                            int length = Short.toUnsignedInt(inputBuffer.getShort());
                            byte[] temp = new byte[length - 4];
                            inputBuffer.get(temp);
                        }
                        return new Transform[0];
                    }
                };

        IkeSaPayload payload = new IkeSaPayload(false, inputPacket);

        assertEquals(PROPOSAL_NUMBER_LIST.length, payload.proposalList.size());
        for (int i = 0; i < payload.proposalList.size(); i++) {
            Proposal proposal = payload.proposalList.get(i);
            assertEquals(PROPOSAL_NUMBER_LIST[i], proposal.number);
            assertEquals(IkePayload.PROTOCOL_ID_IKE, proposal.protocolId);
            assertEquals(0, proposal.spiSize);
        }
    }
}
