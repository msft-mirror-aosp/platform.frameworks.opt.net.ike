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

import static com.android.ike.ikev2.message.IkePayload.PayloadType;

import android.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;

/**
 * IkeHeader represents an IKE message header. It contains all header attributes and provide methods
 * for encoding and decoding it.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.1">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2).
 */
public final class IkeHeader {
    public static final int IKE_HEADER_LENGTH = 28;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        EXCHANGE_TYPE_IKE_INIT_SA,
        EXCHANGE_TYPE_IKE_AUTH,
        EXCHANGE_TYPE_CREATE_CHILD_SA,
        EXCHANGE_TYPE_INFORMATIONAL
    })
    public @interface ExchangeType {}

    public static final int EXCHANGE_TYPE_IKE_INIT_SA = 34;
    public static final int EXCHANGE_TYPE_IKE_AUTH = 35;
    public static final int EXCHANGE_TYPE_CREATE_CHILD_SA = 36;
    public static final int EXCHANGE_TYPE_INFORMATIONAL = 37;

    public final long ikeInitiatorSpi;
    public final long ikeResponderSpi;
    @PayloadType public final int nextPayloadType;
    public final byte majorVersion;
    public final byte minorVersion;
    @ExchangeType public final int exchangeType;
    public final boolean isResponse;
    public final boolean fromIkeInitiator;
    public final int messageId;
    public final int messageLength;

    /**
     * Construct an instance of IkeHeader. It is only called in the process of building outbound
     * message.
     *
     * @param iSpi the SPI of IKE initiator
     * @param rSpi the SPI of IKE responder
     * @param nextPType the first payload's type
     * @param eType the type of IKE exchange being used
     * @param isResp indicates if this message is a response or a request
     * @param fromInit indictaes if this message is sent from the IKE initiator or the IKE responder
     * @param msgId the message identifier
     * @param length the length of the total message in octets
     */
    public IkeHeader(
            long iSpi,
            long rSpi,
            @PayloadType int nextPType,
            @ExchangeType int eType,
            boolean isResp,
            boolean fromInit,
            int msgId,
            int length) {
        ikeInitiatorSpi = iSpi;
        ikeResponderSpi = rSpi;
        nextPayloadType = nextPType;
        exchangeType = eType;
        isResponse = isResp;
        fromIkeInitiator = fromInit;
        messageId = msgId;
        messageLength = length;

        // Major version of IKE protocol in use; it must be set to 2 when building an IKEv2 message.
        majorVersion = 2;
        // Minor version of IKE protocol in use; it must be set to 0 when building an IKEv2 message.
        minorVersion = 0;
    }

    /**
     * Decode IKE header from a byte array and construct an IkeHeader instance.
     *
     * @param packet the raw byte array of the whole IKE message
     */
    public IkeHeader(byte[] packet) {
        if (packet.length <= IKE_HEADER_LENGTH) {
            throw new IllegalArgumentException("Malformed message");
        }

        ByteBuffer buffer = ByteBuffer.wrap(packet);

        ikeInitiatorSpi = buffer.getLong();
        ikeResponderSpi = buffer.getLong();
        nextPayloadType = Byte.toUnsignedInt(buffer.get());

        byte versionByte = buffer.get();
        majorVersion = (byte) ((versionByte >> 4) & 0x0F);
        minorVersion = (byte) (versionByte & 0x0F);

        exchangeType = Byte.toUnsignedInt(buffer.get());

        byte flagsByte = buffer.get();
        isResponse = ((flagsByte & 0x20) != 0);
        fromIkeInitiator = ((flagsByte & 0x08) != 0);

        messageId = buffer.getInt();
        messageLength = buffer.getInt();
    }
}
