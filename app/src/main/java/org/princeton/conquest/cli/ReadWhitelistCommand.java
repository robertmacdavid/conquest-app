package org.princeton.conquest.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.Ip4Prefix;
import org.onosproject.cli.AbstractShellCommand;
import org.princeton.conquest.ConQuestService;

import java.util.Collection;

@Service
@Command(scope = "conquest", name = "read-whitelist",
        description = "Read the whitelist of IP prefixes exempt from ConQuest Enforcement.")
public class ReadWhitelistCommand extends AbstractShellCommand {

    @Override
    protected void doExecute() {
        ConQuestService app = get(ConQuestService.class);
        for (Ip4Prefix prefix : app.readWhitelist()) {
            print(prefix.toString());
        }
    }
}
