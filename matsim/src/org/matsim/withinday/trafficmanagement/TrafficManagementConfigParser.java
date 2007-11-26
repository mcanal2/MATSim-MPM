/* *********************************************************************** *
 * project: org.matsim.*
 * TrafficManagementConfigParser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.withinday.trafficmanagement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.events.Events;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Route;
import org.matsim.utils.io.MatsimXmlParser;
import org.matsim.withinday.trafficmanagement.feedbackcontroler.BangBangControler;
import org.matsim.withinday.trafficmanagement.feedbackcontroler.FeedbackControler;
import org.matsim.withinday.trafficmanagement.feedbackcontroler.NoControl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import playground.arvid_daniel.coopers.fromArvid.ControlInputImpl1;
import playground.arvid_daniel.coopers.fromArvid.ControlInputImplDistribution;
import playground.arvid_daniel.coopers.fromArvid.ControlInputImplSB;
import playground.arvid_daniel.coopers.fromArvid.ControlInputImplSBNoise;


public class TrafficManagementConfigParser extends MatsimXmlParser {

	private final static Logger log = Logger.getLogger(TrafficManagementConfigParser.class);
	
	private final static String TRAFFICMANAGEMENT = "trafficmanagement";

	private final static String GUIDANCEDEVICE = "guidanceDevice";

	private final static String CONTROLINPUTCLASS = "controlInputClass";

	private final static String MESSAGEHOLDTIME = "messageHoldTime";

	private final static String CONTROLEVENTS = "controlEvents";

	private final static String NOMINALSPLITTING = "nominalSplitting";

	private final static String DEADZONESYSOUT = "deadZoneSystemOutput";

	private final static String DEADZONESYSIN = "deadZoneSystemInput";

	private final static String SIGNLINK = "signLink";

	private final static String DIRECTIONLINKS = "directionLink";

	private final static String COMPLIANCE = "compliance";

	private final static String BENEFITCONTROL = "benefitControl";

	private final static String MAINROUTE = "mainRoute";
	
	private final static String ALTERNATIVEROUTE = "alternativeRoute";
	
	private final static String NODE = "node";
	
	private final static String ID = "id";
	
	private final static String FEEDBACKCONTROLER = "feedBackControler";

	private static final String BANGBANGCONTROLER = "BangBangControler";

//	private static final String CONSTANTCONTROLER = "ConstantControler";
	
	private static final String NOCONTROL = "NoControl";
	
//	private static final String PCONTROLER = "PControler";
	
//	private static final String PIDCONTROLER = "PIDControler";
	
	private static final String CONTROLINPUTSB = "ControlInputImplSB";
	
	private static final String CONTROLINPUT1 = "ControlInputImpl1";
	
	private static final String CONTROLINPUTSBNOISE = "ControlInputImplSBNoise";
	
	private static final String CONTROLINPUTDISTRIBUTION = "ControlInputImplDistribution";
	
	private final static String CONTROLINPUT = "controlInput";
	
	private final static String ACCIDENT = "accident";
	
	private final static String LINKID = "linkId";
	
	private final static String CAPACITYREDUCTIONFACTOR ="capacityReductionFactor";
	
	private final static String STARTTIME = "startTime";
	
	private final static String ENDTIME = "endTime";

	private TrafficManagement trafficManagement;

	private VDSSign vdsSign;

	private ArrayList<Node> currentRouteNodes;

	private NetworkLayer network;

	private String configPath;
	
	private ControlInput controlInput;

	private Accident accident;

	private Events events;
	

	public TrafficManagementConfigParser(final NetworkLayer network,
			final String trafficManagementConfig, final Events events) {
		this.network = network;
		this.configPath = trafficManagementConfig;
		this.events = events;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (name.equalsIgnoreCase(TRAFFICMANAGEMENT)) {
			this.trafficManagement = new TrafficManagement();
		}
		else if (name.equalsIgnoreCase(GUIDANCEDEVICE)) {
			this.vdsSign = new VDSSign();
		}
		else if (name.equalsIgnoreCase(MAINROUTE) || name.equalsIgnoreCase(ALTERNATIVEROUTE)) {
			this.currentRouteNodes = new ArrayList<Node>();
		}
		else if (name.equalsIgnoreCase(ACCIDENT)) {
			this.accident = new Accident();
		}
		else if (name.equalsIgnoreCase(NODE)) {
			String id = atts.getValue(ID);
			Node node = this.network.getNode(id);
			if (node != null) {
				this.currentRouteNodes.add(node);
			}
			else {
				throw new RuntimeException("The Node with the id: " + id
						+ " could not be found in the network");
			}
		}
	}
	@Override
	public void endTag(final String name, String content, final Stack<String> context) {
		content = content.trim();
		if (name.equalsIgnoreCase(MAINROUTE)) {
			Route route = new Route();
			route.setRoute(this.currentRouteNodes);
			this.controlInput.setMainRoute(route);
		}
		else if (name.equalsIgnoreCase(ALTERNATIVEROUTE)) {
			Route route = new Route();
			route.setRoute(this.currentRouteNodes);
			this.controlInput.setAlternativeRoute(route);
		}
		else if (name.equalsIgnoreCase(CONTROLINPUTCLASS)) {
			this.controlInput = createControlInput(content);
			this.vdsSign.setControlInput(this.controlInput);
		}
		else if (name.equalsIgnoreCase(FEEDBACKCONTROLER)) {
			FeedbackControler controler = createControlTheoryControler(content);
			this.vdsSign.setControler(controler);
		}
		else if (name.equalsIgnoreCase(MESSAGEHOLDTIME)) {
			this.vdsSign.setMessageHoldTime(Integer.valueOf(content));
		}
		else if (name.equalsIgnoreCase(CONTROLEVENTS)) {
			this.vdsSign.setControlEvents(Integer.valueOf(content));
		}
		else if (name.equalsIgnoreCase(NOMINALSPLITTING)) {
			this.vdsSign.setNominalSplitting(Double.parseDouble(content));
		}
		else if (name.equalsIgnoreCase(DEADZONESYSIN)) {
			this.vdsSign.setDeadZoneSystemInput(Double.parseDouble(content));
		}
		else if (name.equalsIgnoreCase(DEADZONESYSOUT)) {
			this.vdsSign.setDeadZoneSystemOutput(Double.parseDouble(content));
		}
		else if (name.equalsIgnoreCase(SIGNLINK)) {
			Link l = this.network.getLink(content);
			if (l != null) {
				this.vdsSign.setSignLink(l);				
			}
			else {
				throw new IllegalArgumentException("the sign link of the sign could not be found in the network!");
			}
		}
		else if (name.equalsIgnoreCase(DIRECTIONLINKS)) {
			Link l = this.network.getLink(content);
			if (l != null) {
				this.vdsSign.setDirectionLink(l);				
			}
			else {
				throw new IllegalArgumentException("the direction link of the sign could not be found in the network!");
			}
		}
		else if (name.equalsIgnoreCase(COMPLIANCE)) {
			this.vdsSign.setCompliance(Double.parseDouble(content));
		}
		else if (name.equalsIgnoreCase(BENEFITCONTROL)) {
			this.vdsSign.setBenefitControl(Boolean.parseBoolean(content));
		}
		else if (name.equalsIgnoreCase(GUIDANCEDEVICE)) {
			this.vdsSign.init();
			this.trafficManagement.addVDSSign(this.vdsSign);
		}
		else if (name.equalsIgnoreCase(CONTROLINPUT)) {
			this.controlInput.init();
		}
		else if (name.equalsIgnoreCase(LINKID)) {
			this.accident.setLinkId(content);
		}
		else if (name.equalsIgnoreCase(CAPACITYREDUCTIONFACTOR)) {
			this.accident.setCapacityReductionFactor(Double.parseDouble(content));
		}
		else if (name.equalsIgnoreCase(STARTTIME)) {
			this.accident.setStartTime(content);
		}
		else if (name.equalsIgnoreCase(ENDTIME)) {
			this.accident.setEndTime(content);
		}
		else if (name.equalsIgnoreCase(ACCIDENT)) {
			this.trafficManagement.addAccident(this.accident);
			this.accident = null;
		}
	}

	private FeedbackControler createControlTheoryControler(final String content) {
		if (content.compareTo(BANGBANGCONTROLER) == 0) {
			return new BangBangControler();
		}
		else if (content.compareTo(NOCONTROL) == 0) {
			return new NoControl();
		}
		throw new IllegalArgumentException("ControlTheoryControler of xml is not known in the TrafficManagementConfiguration.");
	}

	private ControlInput createControlInput(final String content) {
		if (content.trim().compareTo(CONTROLINPUTSB) == 0) {
			ControlInputImplSB controlInput = new ControlInputImplSB();
			controlInput.setAccidents(this.trafficManagement.getAccidents());
			this.events.addHandler(controlInput);
			return controlInput;
		}
		else if (content.trim().compareTo(CONTROLINPUT1) == 0) {
			ControlInputImpl1 controlInput = new ControlInputImpl1();
			this.events.addHandler(controlInput);
			return controlInput;
		}
		else if (content.trim().compareTo(CONTROLINPUTDISTRIBUTION) == 0) {
			ControlInputImplDistribution controlInput = new ControlInputImplDistribution();
			controlInput.setAccidents(this.trafficManagement.getAccidents());
			this.events.addHandler(controlInput);
			return controlInput;
		}
		else if (content.trim().compareTo(CONTROLINPUTSBNOISE) == 0) {
			ControlInputImplSBNoise controlInput = new ControlInputImplSBNoise();
			controlInput.setAccidents(this.trafficManagement.getAccidents());
			this.events.addHandler(controlInput);
			return controlInput;
		}
		
		throw new IllegalArgumentException("The ControlInput of xml is not known in the TrafficManagementConfiguration.");
	}

	public TrafficManagement initTrafficManagement() {
		try {
			super.parse(this.configPath);

			return this.trafficManagement;
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
