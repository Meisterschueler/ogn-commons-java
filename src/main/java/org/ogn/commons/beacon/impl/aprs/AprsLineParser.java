/**
 * Copyright (c) 2014 OGN, All Rights Reserved.
 */

package org.ogn.commons.beacon.impl.aprs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ogn.commons.beacon.OgnBeacon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AprsLineParser {

    public static final String APRS_SENTENCE_PATTERN = "(.+)>.+:/\\d+h(\\d{4}\\.\\d{2})(N|S)(.)(\\d{5}\\.\\d{2})(E|W)(.)((\\d{3})/(\\d{3}))?/A=(\\d{6}).*";

    // OGN APRS servers reply to the client or send periodic heart-bit where first character is #
    // e.g:
    // # aprsc 2.0.14-g28c5a6a
    // # logresp PCBE13-1 unverified, server GLIDERN2
    private static final String APRS_SRV_MSG_FIRST_CHARACTER = "#";
    private static final String RF_TOKEN = "RF:";
    private static final String CPU_TOKEN = "CPU:";

    private Pattern p = Pattern.compile(APRS_SENTENCE_PATTERN);

    private static Logger LOG = LoggerFactory.getLogger(AprsLineParser.class);

    private static AprsLineParser theInstance;

    public static AprsLineParser get() {
        if (null == theInstance) {
            theInstance = new AprsLineParser();
        }
        return theInstance;
    }

    public OgnBeacon parse(String aprsLine) {
        return parse(aprsLine, true, true);
    }

    public OgnBeacon parse(String aprsLine, boolean processAircraftBeacons, boolean processReceiverBeacons) {
        LOG.trace(aprsLine);
        OgnBeacon result = null;

        Matcher m1 = p.matcher(aprsLine); // Try to match

        if (m1.matches()) {
            if (!aprsLine.startsWith(APRS_SRV_MSG_FIRST_CHARACTER)) {
                if (!aprsLine.contains(RF_TOKEN) && !(aprsLine.contains(CPU_TOKEN))) {
                    if (processAircraftBeacons) {
                        // match aircraft beacons
                        LOG.debug("Aircraft beacon: {}", aprsLine);
                        result = new AprsAircraftBeacon(aprsLine);
                    }
                } else {
                    if (processReceiverBeacons) {
                        // match receiver beacons
                        LOG.debug("Receiver beacon: {}", aprsLine);
                        result = new AprsReceiverBeacon(aprsLine);
                    }
                }
            }

        }

        return result;
    }
}