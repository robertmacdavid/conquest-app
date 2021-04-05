package org.princeton.conquest.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.Ip4Address;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.DeviceIdCompleter;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.princeton.conquest.ConQuestReport;
import org.princeton.conquest.ConQuestService;

import java.time.LocalTime;

@Service
@Command(scope = "conquest", name = "block-flow",
        description = "Test the ConQuest's ability to block a flow.")
public class BlockFlowCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "ipv4-src",
            description = "Source IP of the flow to block",
            required = true)
    String ipv4Src = null;

    @Argument(index = 1, name = "ipv4-dst",
            description = "Destination IP of the flow to block",
            required = true)
    String ipv4Dst = null;

    @Argument(index = 2, name = "l4-sport",
            description = "L4 source port of the flow to block",
            required = true)
    short l4Sport = 0;

    @Argument(index = 3, name = "l4-dport",
            description = "L4 dest port of the flow to block",
            required = true)
    short l4Dport = 0;

    @Argument(index = 4, name = "protocol",
            description = "IP protocol of the flow to block",
            required = true)
    byte protocol = 0;


    @Override
    protected void doExecute() {
        ConQuestService app = get(ConQuestService.class);

        Ip4Address srcAddr = Ip4Address.valueOf(ipv4Src);
        Ip4Address dstAddr = Ip4Address.valueOf(ipv4Dst);

        ConQuestReport report = new ConQuestReport(srcAddr, dstAddr, l4Sport, l4Dport, protocol,
                ImmutableByteSequence.copyFrom(0), LocalTime.now());

        print("Blocking flow for report %s", report.toString());
        app.blockFlow(report);
    }
}
