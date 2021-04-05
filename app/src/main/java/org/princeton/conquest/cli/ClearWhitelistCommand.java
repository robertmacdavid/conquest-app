package org.princeton.conquest.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.Ip4Prefix;
import org.onosproject.cli.AbstractShellCommand;
import org.princeton.conquest.ConQuestService;

@Service
@Command(scope = "conquest", name = "clear-whitelist",
        description = "Clear the whitelist of IP prefixes exempt from ConQuest Enforcement.")
public class ClearWhitelistCommand extends AbstractShellCommand {

    @Override
    protected void doExecute() {
        ConQuestService app = get(ConQuestService.class);
        print("Clearing whitelist");
        app.clearWhitelist();
    }
}
