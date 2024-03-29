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

import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.DefaultPacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.segmentrouting.policy.api.DropPolicy;
import org.onosproject.segmentrouting.policy.api.PolicyId;
import org.onosproject.segmentrouting.policy.api.PolicyService;
import org.onosproject.segmentrouting.policy.api.TrafficMatch;
import org.onosproject.segmentrouting.policy.api.TrafficMatchId;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalTime;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true,
        service = {ConQuestService.class}
)
public class ConQuest implements ConQuestService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationId appId;
    private static final int DEFAULT_PRIORITY = 10;
    private int blockDuration = Constants.DEFAULT_BLOCK_DURATION_MS; // milliseconds

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PolicyService policyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PiPipeconfService pipeconfService;

    private final List<ConQuestReport> receivedReports = new ArrayList<>();
    private final Timer unblockingTimer = new HashedWheelTimer();
    private final CustomPacketProcessor processor = new CustomPacketProcessor();
    private final Set<ConQuestReport> blockedFlows = new HashSet<>();
    private final Set<Ip4Prefix> whitelist = new HashSet<>();

    private PolicyId blockingPolicyId;

    private final Map<DeviceId, Boolean> cachedConquestDeviceLookups = new HashMap<>();


    private String getHexString(byte[] byteBuffer) {
        StringBuilder sb = new StringBuilder();
        for (byte b : byteBuffer) {
            sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }


    @Activate
    protected void activate() {
        // currently no properties to register
        //cfgService.registerProperties(getClass());
        appId = coreService.registerApplication(Constants.APP_NAME,
                () -> log.info("Periscope down."));

        // Register the packet processor.
        packetService.addProcessor(processor, PacketProcessor.director(1));

        // Set up clone sessions on all available devices
        addAllCloneSessions();

        blockingPolicyId = policyService.addOrUpdatePolicy(new DropPolicy());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        // we didnt register anything
        //cfgService.unregisterProperties(getClass(), false);

        // Deregister the packet processor.
        packetService.removeProcessor(processor);
        unblockingTimer.stop();
        // Remove clone sessions and flow rules from all available devices
        cleanUp();

        policyService.removePolicy(blockingPolicyId);

        log.info("Stopped");
    }


    @Modified
    public void modified(ComponentContext context) {
        log.info("Reconfigured");
    }

    private boolean isConquestDevice(DeviceId deviceId) {
        Boolean answer = cachedConquestDeviceLookups.getOrDefault(deviceId, null);
        if (answer != null) return answer;

        var optPipeconf = pipeconfService.getPipeconf(deviceId);
        if (optPipeconf.isEmpty()) {
            answer = false;
        } else {
            PiPipeconf pipeconf = optPipeconf.get();
            answer = pipeconf.id().id().contains("conquest");
        }
        cachedConquestDeviceLookups.put(deviceId, answer);
        return answer;
    }

    @Override
    public void blockFlow(ConQuestReport report) {
        if (report.protocol != Constants.PROTO_UDP && report.protocol != Constants.PROTO_TCP) {
            log.info("Not blocking non-TCP/UDP flow that has IP protocol {}", report.protocolInt());
            return;
        }

        if (blockDuration == 0) {
            log.info("Blocking duration is set to 0, not blocking flow");
            return;
        }

        for (Ip4Prefix prefix : whitelist) {
            if (prefix.contains(report.srcAddr())) {
                log.info("Source address in report matches whitelisted prefix {}. Not blocking", prefix);
                return;
            } else if (prefix.contains(report.dstAddr())) {
                log.info("Destination address in report matches whitelisted prefix {}. Not blocking", prefix);
                return;
            }
        }

        String blockDurationString = "~forever~";
        if (blockDuration > 0) {
            blockDurationString = String.format("for %dms", blockDuration);
        }

        var trafficSelectorBuilder = DefaultTrafficSelector.builder()
                .matchIPProtocol(report.protocol)
                .matchIPSrc(IpPrefix.valueOf(report.srcIp, 32))
                .matchIPDst(IpPrefix.valueOf(report.dstIp, 32));

        TpPort srcPort = TpPort.tpPort(report.srcPortInt());
        TpPort dstPort = TpPort.tpPort(report.dstPortInt());

        if (report.protocol == Constants.PROTO_TCP) {
            trafficSelectorBuilder
                    .matchTcpSrc(srcPort)
                    .matchTcpDst(dstPort);
        } else if (report.protocol == Constants.PROTO_UDP) {
            trafficSelectorBuilder
                    .matchUdpSrc(srcPort)
                    .matchUdpDst(dstPort);
        }
        TrafficSelector trafficSelector = trafficSelectorBuilder.build();

        log.info("Blocking {} {} in response to report.",
                trafficSelector, blockDurationString);

        TrafficMatchId trafficMatchId = policyService.addOrUpdateTrafficMatch(
                new TrafficMatch(trafficSelector, blockingPolicyId));

        blockedFlows.add(report);
        if (blockDuration < 0)
            return;
        unblockingTimer.newTimeout(new UnblockTimerTask(report, trafficMatchId), blockDuration, TimeUnit.MILLISECONDS);
    }


    @Override
    public void setBlockDuration(int blockDuration) {
        this.blockDuration = blockDuration;
    }


    @Override
    public Collection<String> getCurrentlyBlockedFlows() {
        return blockedFlows.stream()
                .map(ConQuestReport::toString)
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public void whitelistPrefix(Ip4Prefix prefix) {
        whitelist.add(prefix);
    }

    @Override
    public void clearWhitelist() {
        whitelist.clear();
    }

    @Override
    public Collection<Ip4Prefix> readWhitelist() {
        return List.copyOf(whitelist);
    }

    private void cleanUp() {
        // Clean up all groups installed by this app
        log.info("Cleaning up groups and table entries that may be hanging around.");
        removeAllCloneSessions();
        //groupService.removeListener();
        // Clean up all table entries installed by this app
        flowRuleService.removeFlowRulesById(appId);
    }


    private void addCloneSessions(DeviceId deviceId) {
        log.info("Adding clone session {} to device {}", Constants.MIRROR_SESSION_ID, deviceId);
        final GroupDescription cloneGroup = ConQuestUtils.buildCloneGroup(
                appId,
                deviceId,
                Constants.MIRROR_SESSION_ID,
                // Ports where to clone the packet.
                // Just controller in this case.
                Collections.singleton(PortNumber.CONTROLLER));
        groupService.addGroup(cloneGroup);
    }


    private void addAllCloneSessions() {
        for (Device device : deviceService.getAvailableDevices()) {
            if (isConquestDevice(device.id())) {
                addCloneSessions(device.id());
            }
        }
        log.info("Added all clone sessions.");
    }

    private void removeCloneSessions(DeviceId deviceId) {
        for (Group group : groupService.getGroups(deviceId, appId)) {
            groupService.removeGroup(deviceId, group.appCookie(), appId);
        }
    }

    private void removeAllCloneSessions() {
        for (Device device : deviceService.getAvailableDevices()) {
            if (isConquestDevice(device.id())) {
                removeCloneSessions(device.id());
            }
        }
    }

    @Override
    public void removeReportTriggers(DeviceId deviceId) {
        int count = 0;
        for (FlowEntry installedEntry : flowRuleService.getFlowEntriesById(appId)) {
            if (installedEntry.deviceId().equals(deviceId)
                    && installedEntry.table().equals(Constants.REPORT_TRIGGER_TABLE)) {
                flowRuleService.removeFlowRules(installedEntry);
                count++;
            }
        }
        log.info("Removed {} flow rules from device {}", count, deviceId);
    }

    @Override
    public void removeAllReportTriggers() {
        int count = 0;
        for (FlowEntry installedEntry : flowRuleService.getFlowEntriesById(appId)) {
            if (installedEntry.table().equals(Constants.REPORT_TRIGGER_TABLE)) {
                flowRuleService.removeFlowRules(installedEntry);
                count++;
            }
        }
        log.info("Removed {} flow rules from the network", count);

    }

    @Override
    public List<ConQuestReport> getReceivedReports() {
        return List.copyOf(receivedReports);
    }

    @Override
    public void clearReceivedReports() {
        receivedReports.clear();
    }

    private Set<FlowRule> buildReportTriggerRules(DeviceId deviceId, int minQueueDelay, int minFlowSizeInQueue) {
        Set<FlowRule> rules = new HashSet<>();
        for (int ecnVal : new int[]{0, 1, 2, 3}) {
            var matchBuilder = PiCriterion.builder().matchExact(Constants.ECN_BITS, ecnVal);
            // Only include the range match keys if they are non-trivial
            if (minFlowSizeInQueue != 0) {
                matchBuilder
                        .matchRange(Constants.FLOW_SIZE_IN_QUEUE, minFlowSizeInQueue, Constants.FLOW_SIZE_RANGE_MAX);
            }
            if (minQueueDelay != 0) {
                matchBuilder
                        .matchRange(Constants.QUEUE_DELAY, minQueueDelay, Constants.QUEUE_DELAY_RANGE_MAX);
            }
            PiCriterion match = matchBuilder.build();

            PiAction action = PiAction.builder()
                    .withId(Constants.TRIGGER_REPORT)
                    .build();

            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(deviceId).fromApp(appId).makePermanent()
                    .forTable(Constants.REPORT_TRIGGER_TABLE)
                    .withSelector(DefaultTrafficSelector.builder().matchPi(match).build())
                    .withTreatment(DefaultTrafficTreatment.builder().piTableAction(action).build())
                    .withPriority(DEFAULT_PRIORITY)
                    .build();
            rules.add(rule);
        }
        return rules;
    }

    @Override
    public void addReportTrigger(DeviceId deviceId, int minQueueDelay, int minFlowSizeInQueue) {
        for (FlowRule rule : buildReportTriggerRules(deviceId, minQueueDelay, minFlowSizeInQueue)) {
            log.info("Installing trigger rule {}", rule);
            flowRuleService.applyFlowRules(rule);
        }
        log.info("Added report trigger flow rules for device {}", deviceId);
    }

    @Override
    public void addReportTriggerEverywhere(int minQueueDelay, int minFlowSizeInQueue) {
        for (Device device : deviceService.getAvailableDevices()) {
            if (isConquestDevice(device.id())) {
                addReportTrigger(device.id(), minQueueDelay, minFlowSizeInQueue);
            }
        }
    }


    @Override
    public void removeAllEntries() {
        log.info("Clearing table entries installed by this app.");
        flowRuleService.removeFlowRulesById(appId);
    }


    private class CustomPacketProcessor implements PacketProcessor {

        private final Logger log = LoggerFactory.getLogger(getClass());

        @Override
        public void process(PacketContext context) {
            Ethernet packet = context.inPacket().parsed();
            DeviceId sourceDevice = context.inPacket().receivedFrom().deviceId();


            if (packet.getEtherType() == Constants.CONQUEST_ETHERTYPE) {

                ByteBuffer pktBuf = context.inPacket().unparsed();
                String strBuf = getHexString(pktBuf.array());
                log.info("Received packet-in that is for us: {}", strBuf);

                byte[] bstream = packet.getPayload().serialize();

                ByteBuffer bb = ByteBuffer.wrap(bstream);

                Ip4Address srcIp = Ip4Address.valueOf(bb.getInt());
                Ip4Address dstIp = Ip4Address.valueOf(bb.getInt());
                short srcPort = bb.getShort();
                short dstPort = bb.getShort();
                byte protocol = bb.get();
                ImmutableByteSequence queueSize = ImmutableByteSequence.copyFrom(bb.getInt());

                LocalTime timeReceived = LocalTime.now();

                ConQuestReport report = new ConQuestReport(srcIp, dstIp, srcPort, dstPort, protocol, queueSize,
                        LocalTime.now());

                receivedReports.add(report);
                log.info("Received ConQuest report from {}: {}", sourceDevice, report);
                blockFlow(report);
            //} else if (log.isDebugEnabled()) {
            } else  {
                log.info("Received packet-in that wasn't for us. Do nothing.");
                ByteBuffer pktBuf = context.inPacket().unparsed();
                String strBuf = getHexString(pktBuf.array());
                log.info("Received packet-in not for us from {}: {}", sourceDevice, strBuf);
                log.info("EtherType was: {}", packet.getEtherType());
                log.debug(strBuf);
            }
        }
    }


    private final class UnblockTimerTask implements TimerTask {
        ConQuestReport blockedFlowReport;
        TrafficMatchId blockedTrafficMatchId;

        UnblockTimerTask(ConQuestReport blockedFlowReport, TrafficMatchId blockedTrafficMatchId) {
            this.blockedFlowReport = blockedFlowReport;
            this.blockedTrafficMatchId = blockedTrafficMatchId;
        }

        @Override
        public void run(Timeout timeout) {
            log.info("Unblocking {}", blockedFlowReport);
            policyService.removeTrafficMatch(blockedTrafficMatchId);
            blockedFlows.remove(this.blockedFlowReport);
        }
    }
}
