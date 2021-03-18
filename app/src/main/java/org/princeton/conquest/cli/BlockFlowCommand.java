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

@Service
@Command(scope = "conquest", name = "block-flow",
        description = "Test the ConQuest's ability to block a flow.")
public class BlockFlowCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "uri",
            description = "Device ID at which to block the flow",
            required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String uri = null;

    @Argument(index = 1, name = "ipv4-src",
            description = "Source IP of the flow to block",
            required = true)
    String ipv4Src = null;

    @Argument(index = 2, name = "ipv4-dst",
            description = "Destination IP of the flow to block",
            required = true)
    String ipv4Dst = null;

    @Argument(index = 3, name = "l4-sport",
            description = "L4 source port of the flow to block",
            required = true)
    short l4Sport = 0;

    @Argument(index = 4, name = "l4-dport",
            description = "L4 dest port of the flow to block",
            required = true)
    short l4Dport = 0;

    @Argument(index = 5, name = "protocol",
            description = "IP protocol of the flow to block",
            required = true)
    byte protocol = 0;


    @Override
    protected void doExecute() {
        ConQuestService app = get(ConQuestService.class);

        DeviceService deviceService = get(DeviceService.class);
        Device device = deviceService.getDevice(DeviceId.deviceId(uri));
        if (device == null) {
            print("Device \"%s\" is not found", uri);
            return;
        }

        Ip4Address srcAddr = Ip4Address.valueOf(ipv4Src);
        Ip4Address dstAddr = Ip4Address.valueOf(ipv4Dst);
        ImmutableByteSequence l4Sport = ImmutableByteSequence.copyFrom(this.l4Sport);
        ImmutableByteSequence l4Dport = ImmutableByteSequence.copyFrom(this.l4Dport);
        ImmutableByteSequence protocol = ImmutableByteSequence.copyFrom(this.protocol);

        ConQuestReport report = new ConQuestReport(srcAddr, dstAddr, l4Sport, l4Dport, protocol,
                ImmutableByteSequence.copyFrom(0));

        print("Blocking flow for report %s", report.toString());
        app.blockFlow(device.id(), report);
    }
}
