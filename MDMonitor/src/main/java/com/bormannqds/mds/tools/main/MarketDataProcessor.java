package com.bormannqds.mds.tools.main;

import com.bormannqds.mds.lib.configuration.ConfigurationResource;
import com.bormannqds.mds.lib.gateway.ApplicationContext;
import com.bormannqds.mds.lib.protobufmessages.MarketData;
import com.bormannqds.mds.lib.protocoladaptor.Connection;
import com.bormannqds.mds.lib.protocoladaptor.TranslatingBridge;
import com.bormannqds.mds.tools.monitoringtool.MonitoringTool;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.*;

/**
 * S1:
 *  START:
 *      setup bridge
 *      reply READY -> REP
 *      tau
 *
 *  send ADDRESS <base ticker> -> TB control port
 *      receive <REPLY_SYNC_SEQ>|<data port address>
 *          map: base ticker -> data port address
 *          if data port address is not seen before:
 *              create SUB socket and add to list:
 *              connect: SUB socket -> data port address
 *              subscribeAll: ALL (later only Active Tickers but TB already filters)
 *      receive UNRECOGNISED => mark, log and show missing symbol for <base ticker>
 *      receive UNSUPPORTED => mark, log and show unsupported product type for <base ticker>
 *      receive NO CONFIG => mark, log and show missing MC or roll schedule config for <base ticker>
 *  if more on interest list:
 *      restart ADDRESS
 *  else
 *      tau
 *
 *  send START -> TB control port
 *      receive READY (or not :-)
 *      tau
 *
 * S2:
 *  Poll loop {CTRL socket, SUB sockets}:
 *      Ctrl msg:
 *          PAUSE:
 *              if (suspended)
 *                  restart S2
 *              send PAUSE -> TB control port
 *              receive DONE on TB control port
 *              reply DONE -> REP
 *              restart S2
 *          RESUME:
 *              if (resumed)
 *                  restart S2
 *              send RESUME -> TB control port
 *              receive READY on TB control port
 *              reply READY -> REP
 *              restart S2
 *          STOP:
 *              send STOP -> TB control port
 *              receive DONE on TB control port
 *              reply DONE -> REP
 *              break poll loop
 *  Trade msg: transform to Protobuf with Trade payload
 *	send Protobuf -> PUB socket
 *  ToB Quote msg: transform to Protobuf with ToB Quote payload
 *	send Protobuf -> PUB socket
 *
 *  HALT:
 *
 * Created by bormanng on 10/07/15.
 */
public class MarketDataProcessor implements Runnable {
    public static final String START_CMD = TranslatingBridge.START_CMD;
    public static final String SUSPEND_CMD = TranslatingBridge.SUSPEND_CMD;
    public static final String RESUME_CMD = TranslatingBridge.RESUME_CMD;
    public static final String STOP_CMD = TranslatingBridge.STOP_CMD;
    public static final String DONE_REPLY = TranslatingBridge.DONE_REPLY; // to SUSPEND_CMD, RESUME_CMD, STOP_CMD
    public static final String READY_REPLY = TranslatingBridge.READY_REPLY; // to START_CMD

    public MarketDataProcessor(final MonitoringTool monitoringTool) {
        this.monitoringTool = monitoringTool;
    }

    public Connection getCtrlConnection() {
        return ctrlConn;
    }

    public void start(final String name) {
        if (me == null) { // avoid accidental thread rabbits
            me = new Thread(this, name);
            me.start();
            try {
                Thread.sleep(105); // allow the thread to settle in before allowing clients to connect
            }
            catch (InterruptedException ie) {
                LOGGER.warn("Settle-in sleep interrupted...", ie);
            }
        }
    }

    public void stop() {
        Thread moriturus = me;
        me = null;
        if (moriturus.isAlive()) {
            try {
                Thread.sleep(5); // allow the thread to wind down before cutting clients off
            }
            catch (InterruptedException ie) {
                LOGGER.warn("Wind-down sleep interrupted...", ie);
            }
            moriturus.interrupt();
        }
    }

    // TODO Replace with ctrl connection to main thread
    public void join() {
        try {
            me.join();
        } catch (InterruptedException ie) {
            LOGGER.warn("join() interrupted...", ie);
        }
    }

    /**
     * C. Poll loop SUB sockets for Active Tickers (TB auto-filters!)
     *     Protobuf msg: do things with it such as displaying...
     */
    public void run() {
        // let's be ready on the server connection before the thundering horde arrives
        final ZMQ.Socket ctrlSocket = Connection.createSocket(ZMQ.REP); // Control connection
        ctrlSocket.bind(ctrlConn.getAddress());

        setUpBridge();
        if (processControlMsgsUntilStartCmd(ctrlSocket)) { // we were interrupted, time to leave
            final ZMQ.Socket tbCtrlSocket = Connection.createSocket(ZMQ.REQ);
            tbCtrlSocket.connect(bridge.getCtrlConnection().getAddress()); // connect to the bridge control port

            fsmState = MdpFsmState.MAKING_ADDRESS_REQS;
            if (processMsgsUntilStopCmd(ctrlSocket, tbCtrlSocket)) {
                ctrlSocket.close();
                tbCtrlSocket.close();
            }
        }
        takeDownBridge();
        fsmState = MdpFsmState.HALT;
    }

    private enum MdpFsmState {
        START, MAKING_ADDRESS_REQS, RECEIVING_CMD_OR_MD, SUSPENDED, STOPPED, HALT
    }

    /**
     * Set up the bridge.
     */
    private void setUpBridge() {
        bridge.start("Bridge");
    }

    /**
     * Take down the bridge.
     */
    private void takeDownBridge() {
        bridge.stop();
    }

    private boolean processControlMsgsUntilStartCmd(final ZMQ.Socket ctrlSocket) {
        final Thread thisThread = Thread.currentThread();

        // S1
        // --
        while (me == thisThread && !Thread.currentThread().isInterrupted()) {
            final ZMsg cmdMsg = ZMsg.recvMsg(ctrlSocket);
            if (cmdMsg == null) {
                LOGGER.warn("Interrupted at ZMsg.recvMsg()...");
                break;
            }
            final String cmd = cmdMsg.popString();
            LOGGER.debug("cmd: "+cmd);
            if (cmd.equals(START_CMD)) {
                final ZMsg replyMsg = new ZMsg();
                replyMsg.add(READY_REPLY);
                replyMsg.send(ctrlSocket);
                cmdMsg.destroy();
                return true;
            }
            else {
                LOGGER.warn(cmd + ": unsupported command received in S1.");
            }
            cmdMsg.destroy();
        }

        return false; // we were interrupted!
    }

    /**
     * Implements client-side of the bridge control protocol:
     *
     * @return list of relevant data connections
     */
    private List<Connection> discoverDataConnections(final ZMQ.Socket tbCtrlSocket) {
        final ConfigurationResource configResource = ApplicationContext.getInstance().getConfigurationResource(); // short-hand
        final Set<String> dataAddresses = new HashSet<>();
        final ArrayList<Connection> dataConnections = new ArrayList<>();

        ZMsg reqMsg;
        ZMsg responseMsg;
        String response;
        for (final Map.Entry<String, List<String>> pgBtkrsEntry: configResource.getInterestList().entrySet()) {
            LOGGER.info("Registering interest for market data from product group " + pgBtkrsEntry.getKey());
            for (final String baseTicker : pgBtkrsEntry.getValue()) {
                LOGGER.info("Generating subscriptions for " + baseTicker);
                reqMsg = new ZMsg();
                reqMsg.add(TranslatingBridge.ADDRESS_REQ_CMD);
                reqMsg.add(baseTicker);
                reqMsg.send(tbCtrlSocket);

                responseMsg = ZMsg.recvMsg(tbCtrlSocket);
                if (responseMsg == null) {
                    LOGGER.warn("Interrupted at ZMsg.recvMsg()...");
                    return null;
                }
                response = responseMsg.popString();
                switch(response) {
                    case TranslatingBridge.NO_CONFIG_REPLY:
                    case TranslatingBridge.UNRECOGNISED_REPLY:
                    case TranslatingBridge.UNSUPPORTED_REPLY:
                        LOGGER.error(baseTicker + ": bridge said " + response);
                        break;
                    case TranslatingBridge.REPLY_SYNC_SEQ:
                        final String dataAddress = responseMsg.popString();
                        if (!dataAddresses.contains(dataAddress)) {
                            dataAddresses.add(dataAddress);
                            dataConnections.add(new Connection(dataAddress));
                            groupDataAddressMap.add(pgBtkrsEntry.getKey());
                        }
                        break;
                    default:
                        LOGGER.error("Bridge is mumbling: " + response);
                }
                responseMsg.destroy();
            }
        }
        return dataConnections;
    }

    private List<ZMQ.Socket> connectDataPorts(final List<Connection> dataConnections) {
        final List<ZMQ.Socket> dataSockets = new ArrayList<>();

        for (final Connection dataConnection: dataConnections) {
            ZMQ.Socket dataSocket = Connection.createSocket(ZMQ.SUB);
            dataSockets.add(dataSocket);
            dataSocket.connect(dataConnection.getAddress());
        }

        return dataSockets;
    }

    /**
     * control bridge
     */
    private static boolean controlBridge(final String cmd, final String expect, final ZMQ.Socket tbCtrlSocket) {
        final ZMsg reqMsg = new ZMsg();
        reqMsg.add(cmd);
        reqMsg.send(tbCtrlSocket);
        final ZMsg tbResponseMsg = ZMsg.recvMsg(tbCtrlSocket);
        if (tbResponseMsg == null) {
            LOGGER.warn("Interrupted at ZMsg.recvMsg()...");
            return false;
        }
        final String tbResponse = tbResponseMsg.popString();
        if (!tbResponse.equals(expect)) {
            LOGGER.warn(tbResponse + ": bridge is mumbling");
            return false; // don't trust a mumbling bridge
        }
        tbResponseMsg.destroy();
        LOGGER.debug("response: " + tbResponse);
        return true;
    }

    /**
     * control bridge and signal readiness to client
     */
    private static boolean controlBridgeAndReply(final String cmd, final String expect, final ZMQ.Socket tbCtrlSocket, final String reply, final ZMQ.Socket ctrlSocket) {
        if (controlBridge(cmd, expect, tbCtrlSocket)) {
            final ZMsg replyMsg = new ZMsg();
            replyMsg.add(reply);
            replyMsg.send(ctrlSocket);
            return true;
        }
        return false;
    }

    private boolean processMsgsUntilStopCmd(final ZMQ.Socket ctrlSocket, final ZMQ.Socket tbCtrlSocket) {
        final Thread thisThread = Thread.currentThread();

        final List<Connection> dataConnections = discoverDataConnections(tbCtrlSocket);
        if (dataConnections != null) {
            final List<ZMQ.Socket> dataSockets = connectDataPorts(dataConnections);

            if (controlBridge(TranslatingBridge.START_CMD, TranslatingBridge.READY_REPLY, tbCtrlSocket)) {
                // S2
                // --
                int maxPollNdx = dataSockets.size() + 1;
                final ZMQ.Poller inPoller = new ZMQ.Poller(maxPollNdx);
                inPoller.register(ctrlSocket);
                dataSockets.forEach(inPoller::register);

                dataSockets.forEach(ds -> ds.subscribe(ZMQ.SUBSCRIPTION_ALL));
                fsmState = MdpFsmState.RECEIVING_CMD_OR_MD;
                LOGGER.info("Processing started...");
                while (thisThread == me && !Thread.currentThread().isInterrupted()) {
                    int nrRaised = inPoller.poll();
                    if (nrRaised == 0) { // spurious wake-up, interrupted???
                        LOGGER.warn("Spurious wake-up: interrupted at Poller.poll()???");
                        continue; // not sure we were interrupted so retest poll loop condition
                    }
                    int remNrRaised = nrRaised;
                    if (remNrRaised > 0 && inPoller.pollin(0)) { // Control calling
                        --remNrRaised;
                        final ZMsg cmdMsg = ZMsg.recvMsg(ctrlSocket);
                        if (cmdMsg == null) {
                            LOGGER.warn("Interrupted at ZMsg.recvMsg()...");
                            break;
                        }
                        final String cmd = cmdMsg.popString();
                        LOGGER.debug("cmd: "+cmd);
                        switch (cmd) {
                            case SUSPEND_CMD:
                                if (fsmState == MdpFsmState.RECEIVING_CMD_OR_MD) {
                                    dataSockets.forEach(ds -> ds.unsubscribe(ZMQ.SUBSCRIPTION_ALL));
                                    if (!controlBridgeAndReply(TranslatingBridge.SUSPEND_CMD, TranslatingBridge.DONE_REPLY, tbCtrlSocket, DONE_REPLY, ctrlSocket))
                                        return false;
                                    fsmState = MdpFsmState.SUSPENDED;
                                }
                                break;
                            case RESUME_CMD:
                                if (fsmState == MdpFsmState.SUSPENDED) {
                                    if (!controlBridgeAndReply(TranslatingBridge.RESUME_CMD, TranslatingBridge.READY_REPLY, tbCtrlSocket, READY_REPLY, ctrlSocket))
                                        return false;
                                    dataSockets.forEach(ds -> ds.subscribe(ZMQ.SUBSCRIPTION_ALL));
                                    fsmState = MdpFsmState.RECEIVING_CMD_OR_MD;
                                }
                                break;
                            case STOP_CMD:
                                cmdMsg.destroy();
                                dataSockets.forEach(ds -> ds.unsubscribe(ZMQ.SUBSCRIPTION_ALL));
                                dataSockets.forEach(ZMQ.Socket::close);
                                fsmState = MdpFsmState.STOPPED;
                                return controlBridgeAndReply(TranslatingBridge.STOP_CMD, TranslatingBridge.DONE_REPLY, tbCtrlSocket, DONE_REPLY, ctrlSocket);
                            default:
                                LOGGER.warn(cmd + ": unsupported command received in S2.");
                                break;
                        }
                        cmdMsg.destroy();
                    }
                    int si = 0;
                    for (int i = 1; remNrRaised > 0 && i < maxPollNdx; ++i, ++si) { // exhaust at least nrRaised but no more
                        if (inPoller.pollin(i)) { // Pump i calling; if we got paused, the pollin() should reset the poller
                            --remNrRaised;
                            if (fsmState == MdpFsmState.RECEIVING_CMD_OR_MD) {
                                assert dataSockets.get(si).equals(inPoller.getSocket(i)); // Huh, not much we can do beyond here!
                                final ZMsg mdMsg = ZMsg.recvMsg(inPoller.getSocket(i)); // get socket from poller=>saves an i - 1 :-P
                                if (mdMsg == null) {
                                    LOGGER.warn("Interrupted at ZMsg.recvMsg()...");
                                    break;
                                }
                                final String symbol = mdMsg.popString();
                                try {
                                    MarketData.MarketDataMessage msgPayload = MarketData.MarketDataMessage.parseFrom(mdMsg.pop().getData());
                                    monitoringTool.scheduleMarketDataModelUpdate(groupDataAddressMap.get(si), msgPayload);
                                }
                                catch (InvalidProtocolBufferException ipbe) {
                                    LOGGER.warn(symbol + ": msg payload not a recognised protobuf...", ipbe);
                                }
                                mdMsg.destroy();
                            }
                            // else: silently drop msg whilst SUSPENDED
                        }
                    }
                } // falls through if poll()/recvMsg() was interrupted
            }
        }
        return false;
    }

    private static final Logger LOGGER = LogManager.getLogger(MarketDataProcessor.class);
    private static final String CTRL_ADDRESS = "inproc://CtrlMDP";

    private final Connection ctrlConn = new Connection(CTRL_ADDRESS);
    private final TranslatingBridge bridge = new TranslatingBridge();
    private final ArrayList<String> groupDataAddressMap = new ArrayList<>(); // implicit ndx-based dataAddress->group map
    private final MonitoringTool monitoringTool;

    private MdpFsmState fsmState = MdpFsmState.START;
    private volatile Thread me;
}
