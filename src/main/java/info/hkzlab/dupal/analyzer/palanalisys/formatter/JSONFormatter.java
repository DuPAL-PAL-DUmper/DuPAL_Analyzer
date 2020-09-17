package info.hkzlab.dupal.analyzer.palanalisys.formatter;

import org.json.JSONArray;
import org.json.JSONObject;

import info.hkzlab.dupal.analyzer.devices.PALSpecs;
import info.hkzlab.dupal.analyzer.palanalisys.graph.*;
import info.hkzlab.dupal.analyzer.palanalisys.simple.SimpleState;

public class JSONFormatter {
    public static final int JSON_FORMAT_REVISION = 1;

    private JSONFormatter() {};
   
    public static String formatJSON(PALSpecs pSpecs, SimpleState[] states) {
        JSONObject rootObject = new JSONObject();
        
        // Fill in the header and insert it into the root object
        rootObject.put("header", buildHeader(pSpecs));
        JSONArray ssArray = new JSONArray();

        for(SimpleState ss : states) {
            ssArray.put(buildObjectFromSimpleState(ss));
        }

        rootObject.put("states", ssArray);

        return rootObject.toString(4);
    }

    public static String formatJSON(PALSpecs pSpecs, int ioAsOutMask, OutState[] states) {
        JSONObject rootObject = new JSONObject();
        JSONArray osLinks = new JSONArray();
        JSONArray regLinks = new JSONArray();

        // Fill in the header and insert it into the root object
        rootObject.put("header", buildHeader(pSpecs, ioAsOutMask));

        for(OutState os : states) {
            OutLink[] oLinks = os.getOutLinks();
            RegLink[] rLinks = os.getRegLinks();

            for(OutLink ol : oLinks) osLinks.put(buildObjectFromOutLink(ol));
            for(RegLink rl : rLinks) osLinks.put(buildObjectFromRegLink(rl));
        }

        rootObject.put("oLinks", osLinks);
        rootObject.put("rLinks", regLinks);

        return rootObject.toString(4);
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

        ssObject.put("inputs", ss.input);
        ssObject.put("outputs", ss.output);
        ssObject.put("hiz", ss.hiz);

        return ssObject;
    }

    private static JSONObject buildObjectFromPINs(OutStatePins pins) {
        JSONObject pinsObject = new JSONObject();

        pinsObject.put("outputs", pins.out);
        pinsObject.put("hiz", pins.hiz);

        return pinsObject;
    }

    private static JSONObject buildObjectFromOutLink(OutLink ol) {
        JSONObject linkObject = new JSONObject();

        linkObject.put("inputs", ol.getLinkInputs());
        linkObject.put("source", buildObjectFromPINs(ol.src.pins));
        linkObject.put("destination", buildObjectFromPINs(ol.dest.pins));

        return linkObject;
    }

    private static JSONObject buildObjectFromRegLink(RegLink rl) {
        JSONObject linkObject = new JSONObject();

        linkObject.put("inputs", rl.getLinkInputs());
        linkObject.put("source", buildObjectFromPINs(rl.src.pins));
        linkObject.put("middle", buildObjectFromPINs(rl.middle.pins));
        linkObject.put("destination", buildObjectFromPINs(rl.dest.pins));

        return linkObject;
    }
}
