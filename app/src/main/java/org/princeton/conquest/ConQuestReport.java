package org.princeton.conquest;

import org.onlab.packet.Ip4Address;
import org.onlab.util.ImmutableByteSequence;
import static com.google.common.base.Preconditions.checkArgument;

public class ConQuestReport {

    Ip4Address srcIp;
    Ip4Address dstIp;
    ImmutableByteSequence srcPort;
    ImmutableByteSequence dstPort;
    ImmutableByteSequence protocol;
    ImmutableByteSequence queueSize;

    /**
     * Constructs ConQuest Report data
     */
    public ConQuestReport() {
        this.srcIp = Ip4Address.ZERO;
        this.dstIp = Ip4Address.ZERO;
        this.srcPort = ImmutableByteSequence.ofZeros(16);
        this.dstPort = ImmutableByteSequence.ofZeros(16);
        this.protocol = ImmutableByteSequence.ofZeros(8);
        this.queueSize = ImmutableByteSequence.ofZeros(32);
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
                          ImmutableByteSequence srcPort, ImmutableByteSequence dstPort,
                          ImmutableByteSequence protocol, ImmutableByteSequence queueSize) {
        this.srcIp = srcIpAddress;
        this.dstIp = dstIpAddress;
        checkArgument(srcPort.size() == 2, "L4 port must be 2 bytes");
        checkArgument(dstPort.size() == 2, "L4 port must be 2 bytes");
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        checkArgument(protocol.size() == 1, "IP protocol must be 1 byte");
        this.protocol = protocol;
        checkArgument(queueSize.size() == 4, "Queue occupancy must be 4 bytes");
        this.queueSize = queueSize;
    }

    public String toString() {
        String protocol;
        switch (this.protocol.asReadOnlyBuffer().get(0)) {
            case 1:
                protocol = "ICMP";
                break;
            case 6:
                protocol = "TCP";
                break;
            case 17:
                protocol = "UDP";
                break;
            default:
                protocol = "UNKNOWN";
                break;
        }
        return String.format("(%s, %s:%s->%s:%s, %s)", protocol,
                this.srcIp.toString(), this.srcPort,
                this.dstIp.toString(), this.dstPort, this.queueSize);
    }
}
