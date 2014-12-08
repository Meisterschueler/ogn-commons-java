package org.ogn.commons.flarm;

import static org.ogn.commons.utils.StringUtils.hex2ascii;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.ogn.commons.beacon.AircraftDescriptor;
import org.ogn.commons.beacon.impl.AircraftDescriptorImpl;
import org.ogn.commons.utils.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles FlarmNet db. Flarmnet db is loaded from remote service and kept in a cache. This class is
 * thread-safe!
 * 
 * @author Seb, wbuczak
 */
public class FlarmNet {

    private static final int FLARMNET_LINE_LENGTH = 86;

    private static Logger LOG = LoggerFactory.getLogger(FlarmNet.class);

    private static final String DEFAULT_FLARMNET_FILE_URL = "http://flarmnet.org/files/data.fln";

    private ConcurrentMap<String, AircraftDescriptor> flarmNetCache = new ConcurrentHashMap<>();

    private String flarmnetFileUrl;

    public FlarmNet() {
        this(DEFAULT_FLARMNET_FILE_URL);
    }

    public FlarmNet(String flarmnetFileUrl) {
        if (flarmnetFileUrl == null) {
            this.flarmnetFileUrl = DEFAULT_FLARMNET_FILE_URL;
        } else {
            this.flarmnetFileUrl = flarmnetFileUrl;
        }
    }

    public synchronized void reload() {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        BufferedReader br = null;
        try {

            URL url = new URL(flarmnetFileUrl);
      
            Streams.copy(url.openStream(), bos);

            br = new BufferedReader(new StringReader(bos.toString()));
            String line;
            while ((line = br.readLine()) != null) {
                String decodedLine = new String(hex2ascii(line));
                LOG.trace(decodedLine);

                if (decodedLine.length() == FLARMNET_LINE_LENGTH) {

                    String id = decodedLine.substring(0, 6).trim();
                    String owner = decodedLine.substring(6, 26).trim();
                    String home = decodedLine.substring(27, 48).trim();
                    String model = decodedLine.substring(48, 69).trim();
                    String regNumber = decodedLine.substring(69, 76).trim();
                    String cn = decodedLine.substring(76, 79).trim();
                    String freq = decodedLine.substring(79, 86).trim();

                    AircraftDescriptor desc = new AircraftDescriptorImpl(regNumber, cn, owner, home, model, freq);

                    if (flarmNetCache.replace(id, desc) == null) {
                        flarmNetCache.put(id, desc);
                    }
                }
            }// while
        } catch (Exception e) {
            LOG.error("Exception exception caught", e);
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (Exception e) {
                // nothing can be done, apart from logging
                LOG.warn("Exception exception caught", e);
            }
        }
    }

    public AircraftDescriptor getDescriptor(String id) {
        return flarmNetCache.get(id);
    }
}