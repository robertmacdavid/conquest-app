package org.princeton.conquest.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.princeton.conquest.ConQuestService;

/**
 * ConQuest command to set the block duration for problematic flows
 */
@Service
@Command(scope = "conquest", name = "set-block-duration",
        description = "Set block duration for problematic flows detected by ConQuest")
public class SetBlockDurationCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "block-duration",
            description = "Block duration in milliseconds. 0 disables blocking, -1 makes it permanent",
            required = true)
    int blockDuration = 0;

    @Override
    protected void doExecute() {
        ConQuestService app = get(ConQuestService.class);

        app.setBlockDuration(blockDuration);
    }
}
