package info.hkzlab.dupal.analyzer.palanalisys.formatter;

import org.json.JSONArray;
import org.json.JSONObject;

import info.hkzlab.dupal.analyzer.devices.PALSpecs;
import info.hkzlab.dupal.analyzer.palanalisys.graph.*;
import info.hkzlab.dupal.analyzer.palanalisys.simple.SimpleState;

public class JSONFormatter {
    public static final int JSON_FORMAT_REVISION = 2;

    private JSONFormatter() {};
   
    public static JSONObject formatJSON(PALSpecs pSpecs, SimpleState[] states) {
        JSONObject rootObject = new JSONObject();
        
        // Fill in the header and insert it into the root object
        rootObject.put("header", buildHeader(pSpecs));
        JSONArray ssArray = new JSONArray();

        for(SimpleState ss : states) {
            ssArray.put(buildObjectFromSimpleState(ss));
        }

        rootObject.put("states", ssArray);

        return rootObject;
    }

    public static JSONObject formatJSON(PALSpecs pSpecs, int ioAsOutMask, OutState[] states) {
        JSONObject rootObject = new JSONObject();
        JSONArray osLinks = new JSONArray();
        JSONArray regLinks = new JSONArray();
        JSONArray osStates = new JSONArray();

        // Fill in the header and insert it into the root object
        rootObject.put("header", buildHeader(pSpecs, ioAsOutMask));

        for(OutState os : states) {
            OutLink[] oLinks = os.getOutLinks();
            RegLink[] rLinks = os.getRegLinks();

            for(OutLink ol : oLinks) osLinks.put(buildObjectFromOutLink(ol));
            for(RegLink rl : rLinks) regLinks.put(buildObjectFromRegLink(rl));
            osStates.put(buildObjectFromPINs(os.pins));
        }

        rootObject.put("states", osStates);
        rootObject.put("oLinks", osLinks);
        rootObject.put("rLinks", regLinks);

        return rootObject;
    }

    private static JSONObject buildHeader(PALSpecs pSpecs) {
        return buildHeader(pSpecs, 0);
    }

    private static JSONObject buildHeader(PALSpecs pSpecs, int ioAsOutMask) {
        JSONObject header = new JSONObject();
        JSONObject palDetails = new JSONObject();

        palDetails.put("type", pSpecs.toString());
        if(pSpecs.getPinCount_IO() > 0) palDetails.put("IOsAsOUT", ioAsOutMask);
        
        header.put("revision", JSON_FORMAT_REVISION);
        header.put("analyzerProgram", "DuPAL Analyzer");
        header.put("analyzerVersion", JSONFormatter.class.getPackage().getImplementationVersion());
        header.put("PAL", palDetails);

        return header;
    }

    private static JSONObject buildObjectFromSimpleState(SimpleState ss) {
        JSONObject ssObject = new JSONObject();

        ssObject.put("in", ss.input);
        ssObject.put("out", ss.output);
        ssObject.put("hiz", ss.hiz);

        return ssObject;
    }

    private static JSONObject buildObjectFromPINs(OutStatePins pins) {
        JSONObject pinsObject = new JSONObject();

        pinsObject.put("out", pins.out);
        pinsObject.put("hiz", pins.hiz);
        pinsObject.put("hash", pins.hashCode());

        return pinsObject;
    }

    private static JSONObject buildObjectFromOutLink(OutLink ol) {
        JSONObject linkObject = new JSONObject();

        linkObject.put("inputs", ol.getLinkInputs());
        linkObject.put("src", ol.src.pins.hashCode());
        linkObject.put("dst", ol.dest.pins.hashCode());

        return linkObject;
    }

    private static JSONObject buildObjectFromRegLink(RegLink rl) {
        JSONObject linkObject = new JSONObject();

        linkObject.put("inputs", rl.getLinkInputs());
        linkObject.put("src", rl.src.pins.hashCode());
        linkObject.put("mid", rl.middle.pins.hashCode());
        linkObject.put("dst", rl.dest.pins.hashCode());

        return linkObject;
    }
}
