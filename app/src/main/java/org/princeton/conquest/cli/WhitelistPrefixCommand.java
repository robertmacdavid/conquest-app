package org.princeton.conquest.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.cli.AbstractShellCommand;
import org.princeton.conquest.ConQuestReport;
import org.princeton.conquest.ConQuestService;

import java.time.LocalTime;

@Service
@Command(scope = "conquest", name = "whitelist",
        description = "Whitelist the given IP prefix from ConQuest enforcement.")
public class WhitelistPrefixCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "ipv4-prefix",
            description = "IPv4 prefix to whitelist",
            required = true)
    String ipv4Prefix = null;

    @Override
    protected void doExecute() {
        ConQuestService app = get(ConQuestService.class);

        Ip4Prefix prefix = Ip4Prefix.valueOf(ipv4Prefix);

        print("Whitelisting %s", prefix.toString());
        app.whitelistPrefix(prefix);
    }
}
