package org.princeton.conquest.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.princeton.conquest.ConQuestService;

/**
 * ConQuest command to read all currently blocked flows
 */
@Service
@Command(scope = "conquest", name = "read-blocked-flows",
        description = "Read flows blocked by ConQuest")
public class ReadBlockedFlowsCommand extends AbstractShellCommand {
    @Override
    protected void doExecute() {
        ConQuestService app = get(ConQuestService.class);
        for (String s : app.getCurrentlyBlockedFlows()) {
            print(s);
        }
    }
}
