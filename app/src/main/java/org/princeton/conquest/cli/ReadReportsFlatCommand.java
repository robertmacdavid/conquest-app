package org.princeton.conquest.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.princeton.conquest.ConQuestReport;
import org.princeton.conquest.ConQuestService;

import java.util.ArrayList;
import java.util.List;

/**
 * ConQuest read reports command.
 */
@Service
@Command(scope = "conquest", name = "read-reports-flat",
        description = "Grab all received ConQuest reports")
public class ReadReportsFlatCommand extends AbstractShellCommand {
    @Override
    protected void doExecute() {
        ConQuestService app = get(ConQuestService.class);

        int count = 0;
        for (ConQuestReport report : app.getReceivedReports()) {
            count += 1;
            print("%d) %s", count, report.toString());
        }
        print("%d reports found", count);
    }
}
