package org.princeton.conquest;

import org.onlab.packet.Ip4Address;
import org.onlab.util.ImmutableByteSequence;
import static com.google.common.base.Preconditions.checkArgument;

public class ConQuestReport {

    Ip4Address srcIp;
    Ip4Address dstIp;
    short srcPort;
    short dstPort;
    byte protocol;
    ImmutableByteSequence queueSize;

    /**
     * Constructs ConQuest Report data
     */
    public ConQuestReport() {
        this.srcIp = Ip4Address.ZERO;
        this.dstIp = Ip4Address.ZERO;
        this.srcPort = 0;
        this.dstPort = 0;
        this.protocol = 0;
        this.queueSize = ImmutableByteSequence.ofZeros(4);
    }

    /**
     * Constructs ConQuest Report data with specific values.
     *
     * @param srcIpAddress source IP address of the reported flow
     * @param dstIpAddress destination IP address of the reported flow
     * @param srcPort      source L4 port of the reported flow
     * @param dstPort      destination L4 port of the reported flow
     * @param protocol     L4 protocol of the reported flow
     * @param queueSize    queue occupancy of the reported flow
     */
    public ConQuestReport(Ip4Address srcIpAddress, Ip4Address dstIpAddress,
                          short srcPort, short dstPort,
                          byte protocol, ImmutableByteSequence queueSize) {
        this.srcIp = srcIpAddress;
        this.dstIp = dstIpAddress;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.protocol = protocol;
        this.queueSize = queueSize;
    }

    public int srcPortInt() {
        return this.srcPort & 0xffff;
    }

    public int dstPortInt() {
        return this.dstPort & 0xffff;
    }

    public int protocolInt() {
        return this.protocol & 0xff;
    }

    public String toString() {
        String protocol;
        switch (this.protocol) {
            case Constants.PROTO_ICMP:
                protocol = "ICMP";
                break;
            case Constants.PROTO_TCP:
                protocol = "TCP";
                break;
            case Constants.PROTO_UDP:
                protocol = "UDP";
                break;
            default:
                protocol = String.format("PROTO:%d", this.protocolInt());
                break;
        }
        return String.format("(%s, %s:%d->%s:%d, Size:%s)", protocol,
                this.srcIp.toString(), this.srcPortInt(),
                this.dstIp.toString(), this.dstPortInt(),
                this.queueSize);
    }
}
