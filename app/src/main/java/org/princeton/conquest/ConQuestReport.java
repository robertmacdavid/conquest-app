package org.princeton.conquest;

import org.onlab.packet.Ip4Address;
import org.onlab.util.ImmutableByteSequence;

import java.text.DecimalFormat;
import java.time.LocalTime;

public class ConQuestReport {

    Ip4Address srcIp;
    Ip4Address dstIp;
    short srcPort;
    short dstPort;
    byte protocol;
    ImmutableByteSequence queueSize;
    LocalTime reportTime;

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
     * @param reportTime   time at which the report was received
     */
    public ConQuestReport(Ip4Address srcIpAddress, Ip4Address dstIpAddress,
                          short srcPort, short dstPort,
                          byte protocol, ImmutableByteSequence queueSize,
                          LocalTime reportTime) {
        this.srcIp = srcIpAddress;
        this.dstIp = dstIpAddress;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.protocol = protocol;
        this.queueSize = queueSize;
        this.reportTime = reportTime;
    }

    public Ip4Address srcAddr() {
        return this.srcIp;
    }

    public Ip4Address dstAddr() {
        return this.dstIp;
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

    public LocalTime getReportTime() {
        return reportTime;
    }

    public String queueSizeString() {
        long size;
        try {
            size = queueSize.fit(64).asReadOnlyBuffer().getLong();
        } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
            // This catch is impossible, just adding it to satisfy the IDE
            return "";
        }

        String hrSize = null;
    
        double b = size;
        double k = size / 1024.0;
        double m = size / (1024.0 * 1024.0);

        DecimalFormat dec = new DecimalFormat("0.00");

        if (m > 1) {
            hrSize = dec.format(m).concat(" MB");
        } else if (k > 1) {
            hrSize = dec.format(k).concat(" KB");
        } else {
            hrSize = dec.format(b).concat(" Bytes");
        }
        return hrSize;
    }

    public String protocolString() {
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
        return protocol;
    }

    public String toString() {
        return String.format("(%s, %s:%d->%s:%d, Size:%s, Received:%s)", protocolString(),
                this.srcIp.toString(), this.srcPortInt(),
                this.dstIp.toString(), this.dstPortInt(),
                this.queueSizeString(), this.reportTime.toString());
    }
}
