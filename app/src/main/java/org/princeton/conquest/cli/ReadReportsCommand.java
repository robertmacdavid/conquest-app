package org.princeton.conquest.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.Ip4Address;
import org.onosproject.cli.AbstractShellCommand;
import org.princeton.conquest.ConQuestReport;
import org.princeton.conquest.ConQuestService;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ConQuest read reports command.
 */
@Service
@Command(scope = "conquest", name = "read-reports",
        description = "Grab all received ConQuest reports")
public class ReadReportsCommand extends AbstractShellCommand {
    private static class IpPair {
        Ip4Address src;
        Ip4Address dst;

        IpPair(Ip4Address src, Ip4Address dst) {
            this.src = src;
            this.dst = dst;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IpPair that = (IpPair) o;
            return src.equals(that.src) && dst.equals(that.dst);
        }

        @Override
        public int hashCode() {
            return Objects.hash(src, dst);
        }
    }

    private static class PortPair {
        int src;
        int dst;
        String proto;

        PortPair(int src, int dst, String proto) {
            this.src = src;
            this.dst = dst;
            this.proto = proto;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PortPair portPair = (PortPair) o;
            return src == portPair.src && dst == portPair.dst && proto.equals(portPair.proto);
        }

        @Override
        public int hashCode() {
            return Objects.hash(src, dst, proto);
        }
    }


    @Override
    protected void doExecute() {
        ConQuestService app = get(ConQuestService.class);

        Collection<ConQuestReport> reports = app.getReceivedReports();

        Map<IpPair, Map<PortPair, List<ConQuestReport>>> groupedReports = new HashMap<>();

        for (ConQuestReport report : reports) {
            IpPair ipPair = new IpPair(report.srcAddr(), report.dstAddr());
            PortPair portPair = new PortPair(report.srcPortInt(), report.dstPortInt(), report.protocolString());
            var portPairMap = groupedReports.compute(ipPair, (key, val) -> {
                if (val == null) {
                    val = new HashMap<>();
                }
                return val;
            });
            portPairMap.compute(portPair, (key, val) -> {
                if (val == null) {
                    val = new ArrayList<>();
                }
                val.add(report);
                return val;
            });
        }

        int numFlows = 0;
        for (var outerEntry : groupedReports.entrySet()) {
            IpPair ipPair = outerEntry.getKey();
            var portPairMap = outerEntry.getValue();
            print("SrcIp %s, DstIp %s, %d Distinct 5-tuples",
                    ipPair.src.toString(), ipPair.dst.toString(), portPairMap.size());
            numFlows += portPairMap.size();
            for (var innerEntry : portPairMap.entrySet()) {
                PortPair portPair = innerEntry.getKey();
                List<ConQuestReport> reportGroup = innerEntry.getValue();
                reportGroup.sort((report1, report2) -> report1.getReportTime().compareTo(report2.getReportTime()));
                LocalTime latestReceivedTime = LocalTime.MIN;
                for (ConQuestReport report : reportGroup) {
                    if (report.getReportTime().isAfter(latestReceivedTime)) {
                        latestReceivedTime = report.getReportTime();
                    }
                }
                print("--Proto %s, SrcPort %d, DstPort %d, %d reports",
                        portPair.proto, portPair.src, portPair.dst, reportGroup.size());
                for (ConQuestReport report : reportGroup) {
                    print("----%s queue size at time %s", report.queueSizeString(), report.getReportTime().toString());
                }
            }
        }
        print("%d total reports received from %d flows", reports.size(), numFlows);
    }
}
