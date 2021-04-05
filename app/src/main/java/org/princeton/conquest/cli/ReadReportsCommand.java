package org.princeton.conquest.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.Ip4Address;
import org.onosproject.cli.AbstractShellCommand;
import org.princeton.conquest.ConQuestReport;
import org.princeton.conquest.ConQuestService;

import java.util.Collection;
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
    }
}


    @Override
    protected void doExecute() {
        ConQuestService app = get(ConQuestService.class);

        Collection<ConQuestReport> reports = app.getReceivedReports();

        Map<Pair<>>

        for (ConQuestReport report : reports) {

        int count = 0;
        for (ConQuestReport report : app.getReceivedReports()) {
            count += 1;
            print("%d) %s", count, report.toString());
        }
        print("%d reports found", count);
    }
}
