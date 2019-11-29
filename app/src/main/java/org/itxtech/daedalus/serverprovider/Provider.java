package org.itxtech.daedalus.serverprovider;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import org.itxtech.daedalus.Daedalus;
import org.itxtech.daedalus.util.Logger;
import org.itxtech.daedalus.util.RuleResolver;
import org.minidns.dnsmessage.DnsMessage;
import org.minidns.record.A;
import org.minidns.record.AAAA;
import org.minidns.record.Record;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;

/**
 * Daedalus Project
 *
 * @author iTX Technologies
 * @link https://itxtech.org
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
public abstract class Provider {
    protected Channel channel;
    protected long queryTimes = 0;

    public long getQueryTimes() {
        return queryTimes;
    }

    public Provider(Channel channel) {
        this.channel = channel;
    }

    public abstract void query(DnsMessage message, InetSocketAddress receiver) throws Exception;

    protected void sendResponse(ByteBuf buf, InetSocketAddress receiver) throws IOException {
        DnsMessage message = getDnsMessage(buf);
        if (Daedalus.getPrefs().getBoolean("settings_debug_output", false)) {
            Logger.debug("DnsResponse: " + message.toString());
        }
        channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(message.toArray()), receiver));
    }

    public static DnsMessage getDnsMessage(ByteBuf buf) throws IOException {
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        return new DnsMessage(req);
    }

    public DnsMessage resolve(DnsMessage message) {
        queryTimes++;
        String dnsQueryName = message.getQuestion().name.toString();

        try {
            String response = RuleResolver.resolve(dnsQueryName, message.getQuestion().type);
            if (response != null && message.getQuestion().type == Record.TYPE.A) {
                Logger.info("Provider: Resolved " + dnsQueryName + "  Local resolver response: " + response);
                DnsMessage.Builder builder = message.asBuilder()
                        .setQrFlag(true)
                        .addAnswer(new Record<>(dnsQueryName, Record.TYPE.A, 1, 64,
                                new A(Inet4Address.getByName(response).getAddress())));
                return builder.build();
            } else if (response != null && message.getQuestion().type == Record.TYPE.AAAA) {
                Logger.info("Provider: Resolved " + dnsQueryName + "  Local resolver response: " + response);
                DnsMessage.Builder builder = message.asBuilder()
                        .setQrFlag(true)
                        .addAnswer(new Record<>(dnsQueryName, Record.TYPE.AAAA, 1, 64,
                                new AAAA(Inet6Address.getByName(response).getAddress())));
                return builder.build();
            }
        } catch (Exception e) {
            Logger.logException(e);
        }
        return null;
    }
}
