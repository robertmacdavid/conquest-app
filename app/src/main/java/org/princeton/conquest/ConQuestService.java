/*
 * Copyright 2020-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.princeton.conquest;

import org.onlab.packet.Ip4Prefix;
import org.onosproject.net.DeviceId;

import java.util.Collection;
import java.util.List;

public interface ConQuestService {

    /**
     * Remove all table entries installed by this app.
     */
    void removeAllEntries();

    /**
     * Set the duration in milliseconds for which problematic flows will be blocked.
     * If a block duration of 0 is provided, then blocking is disabled. If -1, blocking
     * is permanent.
     *
     * @param blockDuration flow block duration in milliseconds. 0 disables blocking, -1 makes it permanent
     */
    void setBlockDuration(int blockDuration);

    /**
     * Temporarily block a flow by injecting a conquest report into the app. Visible for testing.
     *
     * @param report the report to inject into the app
     */
    void blockFlow(ConQuestReport report);

    /**
     * Get a list of descriptions of currently blocked flow 5-tuples.
     *
     * @return a list of 5-tuple strings
     */
    Collection<String> getCurrentlyBlockedFlows();

    /**
     * Install table entries in the dataplane to produce control plane reports when queues exceed a target delay and
     * some flow is occupying too much of the queue.
     *
     * @param minQueueDelay      The queue delay needed for a report to be generated
     * @param minFlowSizeInQueue How many queue bytes a single flow should occupy for a report to be generated
     */
    void addReportTriggerEverywhere(int minQueueDelay, int minFlowSizeInQueue);

    /**
     * Install table entries in the dataplane to produce control plane reports when queues exceed a target depth
     *
     * @param deviceId           The network device where we should add the report trigger
     * @param minQueueDelay      The queue delay needed for a report to be generated
     * @param minFlowSizeInQueue How many queue bytes a single flow should occupy for a report to be generated
     */
    void addReportTrigger(DeviceId deviceId, int minQueueDelay, int minFlowSizeInQueue);

    /**
     * Remove report triggers from the target device.
     *
     * @param deviceId The device from which report triggers should be removed
     */
    void removeReportTriggers(DeviceId deviceId);

    /**
     * Remove report triggers from all devices on the network.
     */
    void removeAllReportTriggers();

    /**
     * Get all ConQuest reports received by the app.
     *
     * @return A collection of received ConQuest reports.
     */
    List<ConQuestReport> getReceivedReports();

    /**
     * Clear the ConQuest reports received by the app.
     */
    void clearReceivedReports();

    /**
     * Whitelist an IPv4 prefix from ConQuest enforcement.
     *
     * @param prefix the prefix to whitelist
     */
    void whitelistPrefix(Ip4Prefix prefix);

    /**
     * Clear the whitelist of IPv4 prefixes exempt from ConQuest enforcement.
     */
    void clearWhitelist();

    /**
     * Read the current list of IPv4 prefixes whitelisted from ConQuest enforcement.
     *
     * @return the current whitelist
     */
    Collection<Ip4Prefix> readWhitelist();
}