package net.floodlightcontroller.privateinterconnection;


import net.floodlightcontroller.directGraph.*;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.topology.ITopologyService;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.openflow.protocol.Wildcards;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionStripVirtualLan;
import org.openflow.protocol.action.OFActionType;
import org.openflow.protocol.action.OFActionVirtualLanIdentifier;



import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;


public class PrivateInterConnectionMng implements IFloodlightModule, IOFMessageListener, ILinkDiscoveryListener,IOFSwitchListener,ICallBack
{
	protected IFloodlightProviderService floodlightProvider;
	protected Set<Long> macAddresses;
	protected static Logger logger;
	protected ILinkDiscoveryService linkDiscover;
	protected IDeviceService deviceManager;
	protected ITopologyService topology;

	private DirectGraph m_graph = new DirectGraph();
	private Long m_switchSrc = null;
	private Long m_switchDst = null;
	private final int Capacity = 1;
	private String m_sourceMacAddr;
	private String m_destMacAddr;
	private short m_switchDstPortToHostTarget;
	private short m_switchSrcPortToHostSource;
	private String m_srcIpFromAdmin;
	private String m_destIpFromAdmin;
	private static Map<String, String> vlanToPrivateIPv4 = new HashMap<String, String>();


	private enum RULE_TYPE 
	{
	    SOURCE_IPV4,
	    SOURCE_ARP,
	    SOURCE_ADD_VLAN,
	    SOURCE_ARP_ADD_VLAN,
	    DEST_IPV4,
	    DEST_ARP,
	    DROP_VLAN,
	    DROP_VLAN_ARP,
	    ADD_VLAN,
	    ADD_VLAN_ARP  
	}
	/**
	 * Function builds graph based on topology in 2 phases:
	 * 	1. addVerticesToGraph() - Building skeleton graph that contain only vertices - each vertex is a switch
	 *  2. addLinkesToGraph()   - Add links to graph based on switches links from topology.
	 * 
	 **/
	private void buildGraph()
	{
		addVerticesToGraph();
		addLinkesToGraph();
	}
	
	//Function add vertices to graph, each vertex is a switch in topology.
	private void addVerticesToGraph()
	{
		String vertexName;

		Map<Long, IOFSwitch> switches = this.floodlightProvider.getSwitches();
		for(IOFSwitch s :switches.values())
		{
			vertexName =Long.toString(s.getId());
			m_graph.addVertex(vertexName);			 			 
		}
		System.out.println(m_graph);
	}

	/**
	 * Function add links to graph based on switches links from topology.
	 * (-) For each switch we run through his links and adding them to graph.
	 * (-) Each link in graph is a edge combined from :
	 * 	   1. from 		 - switchId represent source side of the link.
	 * 	   2. to	 	 - switchId represent dest side of the link.
	 * 	   3. sourcePort - represent the source port of the link. 
	 * 	   4. Capacity   - represent the capacity of the link.
	 */
	private void addLinkesToGraph()
	{
		String from,to;
		short sourcePort,destPort;
		Map<Long, Set<Link>> mapswitch= linkDiscover.getSwitchLinks();

		for(Long switchId:mapswitch.keySet())
		{
			for(Link l: mapswitch.get(switchId))
			{
				from = Long.toString(l.getSrc());
				to = Long.toString(l.getDst());
				sourcePort = l.getSrcPort();
				destPort = l.getDstPort();
				m_graph.addEdge(from, to, Capacity ,sourcePort,destPort);
			} 
		}
		System.out.println(m_graph);
	}

	/**
	 * Function get called after members SrcIp & DestIp set by AdminServer.(The hosts IP's)
	 * Function initialize the following members:
	 * 	(-)switchSrc - switch that source host connected to.
	 * 	(-)sourceMacAddr - source host MAC address.
	 * 	(-)switchDst - switch that destination host connected to.
	 * 	(-)destMacAddr - destination host MAC address.
	 * 	(-)switchDstPortToHostTarget - Port from destination switch to destination host.
	 *  
	 * Run through all known devices:
	 * (1) Get IPv4 addresses associated with the device.
	 * (2) In case device has associated IPv4: means controller learned at least 1 switch connected to this device.
	 * 	(2.1) Get the first IPv4 address associated with the device (assocIpv4Addr)
	 * 	(2.2) Case assocIpv4Addr = srcIP -> we found the source host and extract the data needed.
	 *  (2.3) Case assocIpv4Addr = DestIP -> we found the destination host and extract the data needed.
	 **/
	private boolean initDataOnHosts(String srcIP, String destIP)	
	{
		System.out.println("######### In initDataOnHosts #############");
		SwitchPort[] SwitchPort;
		boolean isHostSrcFound = false;
		boolean isHostDestFound = false;
		
		//Get all devices known by the controller.(device = host)
		Collection<? extends IDevice> alldevices = deviceManager.getAllDevices();

		for (IDevice device : alldevices)
		{
			Integer [] deviceAssociatedIPv4Addr = device.getIPv4Addresses();

			if(deviceAssociatedIPv4Addr.length>0)
			{
				String assocIpv4Addr = IPv4.fromIPv4Address(deviceAssociatedIPv4Addr[0]);

				if(assocIpv4Addr.equals(srcIP))
				{
					//Get attachment points associated with the device
					SwitchPort = device.getAttachmentPoints();
					if(SwitchPort.length>0)
					{
						m_switchSrc =SwitchPort[0].getSwitchDPID();
						m_sourceMacAddr = device.getMACAddressString();
						m_switchSrcPortToHostSource =( short)SwitchPort[0].getPort();
						isHostSrcFound = true;
						System.out.println("######### Update src Host "+"switchId"+m_switchSrc.toString()+ "#############");
					}	
				}
				else if(assocIpv4Addr .equals(destIP))
				{
					//Get attachment points associated with the device
					SwitchPort =device.getAttachmentPoints();
					if(SwitchPort.length>0)
					{
						m_switchDst =SwitchPort[0].getSwitchDPID();
						m_destMacAddr = device.getMACAddressString();
						m_switchDstPortToHostTarget =(short)SwitchPort[0].getPort();
						isHostDestFound = true;
						System.out.println("######### Update dest Host "+"switchId"+m_switchDst.toString()+ "#############");
					}	
				}				
			}		
		}
		if(isHostSrcFound && isHostDestFound)
		{
			return true;
		}
		return false;	
	}


	/**
	 * Function install rules on switched that there is flow through them.
	 * 1. Run through outgoing edges of sourceVertex (represent the source switch):
	 * 2. Case edge flow > 0 :
	 *    - Assign private IPv4 address to current vlan id and maintain in m_vladToIp map.
	 * 	  - Install rules for source host switch :
	 * 			from source switch install rule type :SOURCE_ADD_VLAN,SOURCE_ARP_ADD_VLAN
	 *          to source switch, install rule type : DEST_IPV4,DEST_ARP
	 *    - invoke installRuleToHostSrc() for install rules TO host source from source switch.
	 * 	  - Update that rule installed for above edge by setIsRuleInstalled() 
	 * 	  - Invoke installRulesOnFlowDFS() - install rules with same vlan on the path that include above edge.
	 * 										 The path is discovered by DFS from the dest vertex to target.
	 * 	  - Increment vlan count for next flow paths from source vertex that may discovered next iteration.
	 * 
	 * -> Finally we install rules on all switches that there is flow through them from source switch to destination host.
	 * -> The installation includes rules from the dest switch to dest host.
	 * -> On each path from source switch to dest host we install rule with differ vlan.
	 */
	private void installRulesOnFlow()
	{			
		Vertex sourceVertex = m_graph.getSourceVertex();
		short vlanNum = 1;
		for(Edge sourceOutEdge :sourceVertex.getOutgoingEdges())
		{
			if(sourceOutEdge.getFlow()>0)
			{
				attachIpAddressToVlan(vlanNum);
				installRule(sourceVertex.getName(), sourceOutEdge.getSourcePort(), vlanNum,RULE_TYPE.SOURCE_ADD_VLAN);
				installRule(sourceOutEdge.getTo().getName(), sourceOutEdge.getDestPort(), vlanNum,RULE_TYPE.DEST_IPV4);
				installRule(sourceVertex.getName(), sourceOutEdge.getSourcePort(), vlanNum,RULE_TYPE.SOURCE_ARP_ADD_VLAN);
				installRule(sourceOutEdge.getTo().getName(), sourceOutEdge.getDestPort(), vlanNum,RULE_TYPE.DEST_ARP);
				installRuleToHostSrc(sourceVertex.getName(),vlanNum, sourceOutEdge.getSourcePort());
				sourceOutEdge.setIsRuleInstalled();
				installRulesOnFlowDFS(sourceOutEdge.getTo(),vlanNum);
				vlanNum++;
			}
		}	
	}
	
	public static Map<String, String> getVlanToPrivateIPv4Map()
	{
		return vlanToPrivateIPv4;
	}
	
	public static void clearData()
	{
		vlanToPrivateIPv4.clear();		
	}

	private void installRulesOnFlowDFS(Vertex vertex,short vlanNum)
	{
		Queue<Vertex> queue = new LinkedList<Vertex>();
		queue.add(vertex);
		Vertex vertexToInstall;
		Vertex destVertex = m_graph.getDestVertex();

		
		while(!queue.isEmpty())
		{
			vertexToInstall = queue.poll();
			for(Edge e : vertexToInstall.getOutgoingEdges())
			{
				if(vertexToInstall == destVertex)
				{
					installRuleToHostDst(destVertex.getName(),vlanNum);
					return;
				}
				if(e.getFlow()>0 && !e.getIsRuleInstalled())
				{
					installRule(vertexToInstall.getName(),e.getSourcePort(),vlanNum, RULE_TYPE.SOURCE_IPV4);
					installRule(e.getTo().getName(),e.getDestPort(),vlanNum, RULE_TYPE.DEST_IPV4);
					installRule(vertexToInstall.getName(),e.getSourcePort(),vlanNum, RULE_TYPE.SOURCE_ARP);
					installRule(e.getTo().getName(),e.getDestPort(),vlanNum, RULE_TYPE.DEST_ARP);
					e.setIsRuleInstalled();
					queue.add(e.getTo());
				}
			}
		}
	}

	//Install rules from destination host switch to destination host
	private void installRuleToHostDst(String destVertex,short vlanNum)
	{
		installRule(destVertex, m_switchDstPortToHostTarget, vlanNum,RULE_TYPE.DROP_VLAN);
		installRule(destVertex, m_switchDstPortToHostTarget, vlanNum,RULE_TYPE.DROP_VLAN_ARP);
	}
	//Install rules from source host switch to source host
	private void installRuleToHostSrc(String destVertex,short vlanNum, short inPort)
	{
		installRule(destVertex, m_switchSrcPortToHostSource, vlanNum,RULE_TYPE.DEST_IPV4 );
		installRule(destVertex, m_switchSrcPortToHostSource, vlanNum,RULE_TYPE.DEST_ARP);
	}
	
	private void installRule(String switchId, short sourcePort,short vlanNum,RULE_TYPE ruleType)
	{
		OFFlowMod flow = new OFFlowMod(); 
		OFMatch match = new OFMatch();
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		OFActionOutput outputAction = new OFActionOutput();
		OFActionStripVirtualLan stripVlanAction = new OFActionStripVirtualLan();
		OFActionVirtualLanIdentifier addVlanIdAction = new OFActionVirtualLanIdentifier();
		
		//Get switch to install rule
		Map<Long, IOFSwitch> switches = this.floodlightProvider.getSwitches();
		Long s = Long.valueOf(switchId);
		IOFSwitch switchToInstall = switches.get(s);
		
		String privateSrcIp = vlanToPrivateIPv4.get(Short.toString(vlanNum));
		
        switch(ruleType)
        {
        case SOURCE_ADD_VLAN:
        	match.setNetworkSource(IPv4.toIPv4Address(privateSrcIp)); //source IPV4
        	match.setNetworkDestination(IPv4.toIPv4Address(m_destIpFromAdmin)); //destIPV4
        	match.setDataLayerType(Ethernet.TYPE_IPv4);
        	match.setWildcards(Wildcards.FULL
    				.matchOn(Flag.DL_TYPE)
    				.matchOn(Flag.NW_SRC).withNwSrcMask(32)
                    .matchOn(Flag.NW_DST).withNwDstMask(32));
        	
        	addVlanIdAction.setVirtualLanIdentifier(vlanNum);
        	actions.add(addVlanIdAction);
        	break;
        case SOURCE_ARP_ADD_VLAN:
        	match.setNetworkSource(IPv4.toIPv4Address(privateSrcIp));    //dest IPV4
        	match.setNetworkDestination(IPv4.toIPv4Address(m_destIpFromAdmin)); //src IPV4
        	match.setNetworkProtocol((byte)ARP.OP_REQUEST);
        	match.setDataLayerType(Ethernet.TYPE_ARP);
        	match.setWildcards(Wildcards.FULL
    				.matchOn(Flag.DL_TYPE)
        			.matchOn(Flag.NW_PROTO)
    				.matchOn(Flag.NW_SRC).withNwSrcMask(32)
                    .matchOn(Flag.NW_DST).withNwDstMask(32));
        	
        	addVlanIdAction.setVirtualLanIdentifier(vlanNum);
        	actions.add(addVlanIdAction);
        	break;
        case SOURCE_IPV4:
        	match.setNetworkSource(IPv4.toIPv4Address(privateSrcIp)); //source IPV4
        	match.setNetworkDestination(IPv4.toIPv4Address(m_destIpFromAdmin)); //destIPV4
        	match.setDataLayerVirtualLan(vlanNum);
        	match.setDataLayerType(Ethernet.TYPE_IPv4);
        	match.setWildcards(Wildcards.FULL
    				.matchOn(Flag.DL_VLAN)
    				.matchOn(Flag.DL_TYPE)
    				.matchOn(Flag.NW_SRC).withNwSrcMask(32)
                    .matchOn(Flag.NW_DST).withNwDstMask(32));
        	break;
        case SOURCE_ARP:   
        	match.setDataLayerVirtualLan(vlanNum);
        	match.setNetworkSource(IPv4.toIPv4Address(privateSrcIp));    //dest IPV4
        	match.setNetworkDestination(IPv4.toIPv4Address(m_destIpFromAdmin)); //src IPV4
        	match.setNetworkProtocol((byte)ARP.OP_REQUEST);
        	match.setDataLayerType(Ethernet.TYPE_ARP);
        	match.setWildcards(Wildcards.FULL
    				.matchOn(Flag.DL_TYPE)
        			.matchOn(Flag.NW_PROTO)
    				.matchOn(Flag.NW_SRC).withNwSrcMask(32)
                    .matchOn(Flag.NW_DST).withNwDstMask(32)
    				.matchOn(Flag.DL_VLAN));
      	
        	break;
        case DEST_IPV4:
        	match.setNetworkSource(IPv4.toIPv4Address(m_destIpFromAdmin));    //dest IPV4
        	match.setNetworkDestination(IPv4.toIPv4Address(privateSrcIp)); //src IPV4
        	//match.setDataLayerSource(m_destMacAddr);
        	//match.setDataLayerDestination(localSrcMac);//--->m_sourceMacAddr
    		match.setDataLayerType(Ethernet.TYPE_IPv4);
        	match.setWildcards(Wildcards.FULL
    				//.matchOn(Flag.DL_SRC)
    				//.matchOn(Flag.DL_DST));
        			 //.matchOn(Flag.NW_PROTO)
    				.matchOn(Flag.DL_TYPE)
    				.matchOn(Flag.NW_SRC).withNwSrcMask(32)
                    .matchOn(Flag.NW_DST).withNwDstMask(32));
        	break;
        case DEST_ARP:
        	match.setNetworkSource(IPv4.toIPv4Address(m_destIpFromAdmin));    //dest IPV4
        	match.setNetworkDestination(IPv4.toIPv4Address(privateSrcIp)); //src IPV4
        	match.setNetworkProtocol((byte)ARP.OP_REPLY);
        	//match.setDataLayerSource(m_destMacAddr);
        	//match.setDataLayerDestination(localSrcMac);//--->m_sourceMacAddr
    		//match.setDataLayerType(Ethernet.TYPE_RARP);
        	match.setDataLayerType(Ethernet.TYPE_ARP);
        	match.setWildcards(Wildcards.FULL
    				//.matchOn(Flag.DL_SRC)
    				//.matchOn(Flag.DL_DST));
    				.matchOn(Flag.DL_TYPE)
        			.matchOn(Flag.NW_PROTO)
    				.matchOn(Flag.NW_SRC).withNwSrcMask(32)
                    .matchOn(Flag.NW_DST).withNwDstMask(32));
            
        	break;
        case DROP_VLAN:
        	match.setNetworkSource(IPv4.toIPv4Address(privateSrcIp)); //source IPV4
        	match.setNetworkDestination(IPv4.toIPv4Address(m_destIpFromAdmin)); //destIPV4
        	match.setDataLayerVirtualLan(vlanNum);
        	//match.setDataLayerSource(localSrcMac);//--->m_sourceMacAddr
        	//matchIpv4.setDataLayerDestination(m_destMacAddr);
        	match.setDataLayerType(Ethernet.TYPE_IPv4);
        	match.setWildcards(Wildcards.FULL
    				//.matchOn(Flag.DL_SRC)
    				//.matchOn(Flag.DL_DST)
    				.matchOn(Flag.DL_VLAN)
    				.matchOn(Flag.DL_TYPE)
    				.matchOn(Flag.NW_SRC).withNwSrcMask(32)
                    .matchOn(Flag.NW_DST).withNwDstMask(32));            
            actions.add(stripVlanAction);
        	break;
        case DROP_VLAN_ARP:

        	match.setDataLayerVirtualLan(vlanNum);
        	match.setDataLayerSource(m_sourceMacAddr);
        	match.setDataLayerDestination("ff:ff:ff:ff:ff:ff");
        	match.setWildcards(Wildcards.FULL
    				.matchOn(Flag.DL_SRC)
    				.matchOn(Flag.DL_DST)
    				.matchOn(Flag.DL_VLAN));
            actions.add(stripVlanAction);
        	break;
        case ADD_VLAN:
        	match.setNetworkSource(IPv4.toIPv4Address(m_destIpFromAdmin));    //dest IPV4
        	match.setNetworkDestination(IPv4.toIPv4Address(privateSrcIp)); //src IPV4
    		match.setDataLayerType(Ethernet.TYPE_IPv4);
        	match.setWildcards(Wildcards.FULL
    				.matchOn(Flag.DL_TYPE)
        			 //.matchOn(Flag.NW_PROTO)
    				.matchOn(Flag.NW_SRC).withNwSrcMask(32)
                    .matchOn(Flag.NW_DST).withNwDstMask(32));
        	
        	addVlanIdAction.setVirtualLanIdentifier(vlanNum);
        	actions.add(addVlanIdAction);
        	break;
        case ADD_VLAN_ARP:
        	match.setNetworkSource(IPv4.toIPv4Address(m_destIpFromAdmin));    //dest IPV4
        	match.setNetworkDestination(IPv4.toIPv4Address(privateSrcIp)); //src IPV4
    		//match.setDataLayerType(Ethernet.TYPE_ARP);
        	match.setNetworkProtocol((byte)ARP.OP_REPLY);
        	match.setWildcards(Wildcards.FULL
    				//.matchOn(Flag.DL_TYPE)
        			 .matchOn(Flag.NW_PROTO)
    				.matchOn(Flag.NW_SRC).withNwSrcMask(32)
                    .matchOn(Flag.NW_DST).withNwDstMask(32));
        	
        	addVlanIdAction.setVirtualLanIdentifier(vlanNum);
        	actions.add(addVlanIdAction);
        	break;
        }
 	
		System.out.println("Flow Installed: " + match.toString());	

		//set  rule Actions
		outputAction.setType(OFActionType.OUTPUT); 
		outputAction.setPort(sourcePort); 
		//outputAction.setLength((short) OFActionOutput.MINIMUM_LENGTH);
		actions.add(outputAction);
		
		flow.setBufferId(-1);
		flow.setActions(actions);
		flow.setMatch(match);
		if(ruleType == RULE_TYPE.DROP_VLAN || ruleType == RULE_TYPE.DROP_VLAN_ARP)
		{
		  flow.setLengthU(OFFlowMod.MINIMUM_LENGTH + outputAction.getLengthU()+stripVlanAction.getLengthU()); 
		}
		else if (ruleType == RULE_TYPE.ADD_VLAN || ruleType == RULE_TYPE.ADD_VLAN_ARP || ruleType == RULE_TYPE.SOURCE_ADD_VLAN || ruleType == RULE_TYPE.SOURCE_ARP_ADD_VLAN)
		{
			  flow.setLengthU(OFFlowMod.MINIMUM_LENGTH + outputAction.getLengthU()+addVlanIdAction.getLengthU()); 
		}
		else
		{
			 flow.setLengthU(OFFlowMod.MINIMUM_LENGTH + outputAction.getLengthU()); 
		}
		System.out.println("Action Installed: " + outputAction.toString());	
		//Send the rule
		try 
		{
			switchToInstall.write(flow,null);
			switchToInstall.flush();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

    
    private void attachIpAddressToVlan(short vlanNum)
    {	
    	 System.out.println("#######################S T A R T ##############################");
        
         Random r = new Random();
         String privateIp = "10" + "." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256);          
         vlanToPrivateIPv4.put(Short.toString(vlanNum), privateIp);
         
         System.out.println(" Add To Map Key + " + Short.toString(vlanNum) + " Value :" + vlanToPrivateIPv4.get(Short.toString(vlanNum)));
    	 System.out.println("####################### E N D  ##############################");
    }
    
    

	public String privateInterConnectionHandler(String srcIP,String destIp)
	{
		int numEdgesInMinCutSet = 0;	// t value
		m_srcIpFromAdmin = srcIP;
		m_destIpFromAdmin = destIp;	
		m_graph = new DirectGraph();
	    m_switchSrc = null;
		m_switchDst = null;

		System.out.println("######### In privateInterConnectionHandler #############");

		boolean isInitSuccedded = initDataOnHosts(m_srcIpFromAdmin,m_destIpFromAdmin);
		if(isInitSuccedded)
		{		
		    buildGraph();
	
			/**
			 * Case we found the source & dest switches of hosts we execute:
			 *  1. dinicsMaxFlow() - Execute maxFlow on graph and get the numEdgesInMinCutSet result.
			 *  2. flowDFS() - install flow rules to switches that there is flow through them.
			 */
			if(m_switchSrc !=null && m_switchDst != null)
			{		
				numEdgesInMinCutSet = m_graph.dinicsMaxFlow(m_graph.getSourceVertex(m_switchSrc.toString()), m_graph.getDestVertex(m_switchDst.toString()));
				System.out.println("######### minCut ############# "+ numEdgesInMinCutSet);
				installRulesOnFlow();
				System.out.println("######### INVOKE FLOWS #############");
			}
		}
         
		return  String.valueOf(numEdgesInMinCutSet);	
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) 
	{
		return Command.CONTINUE;
	}
	
	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException 
	{
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		this.floodlightProvider.addOFSwitchListener(this);
		this.linkDiscover = context.getServiceImpl(ILinkDiscoveryService.class);
		this.linkDiscover.addListener(this);
		deviceManager = context.getServiceImpl(IDeviceService.class);
		macAddresses = new ConcurrentSkipListSet<Long>();
		logger = LoggerFactory.getLogger(PrivateInterConnectionMng.class);
		
		//Create Thread AdminServer & Invoke the thread
		Thread t = new Thread(new AdminServer(12345,this));
		t.start();
	}


	@Override
	public void startUp(FloodlightModuleContext context) 
	{
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() 
	{
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		return l;
	}

	@Override
	public String getName() {
		return PrivateInterConnectionMng.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() 
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls()
	{
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void addedSwitch(IOFSwitch sw) 
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void removedSwitch(IOFSwitch sw) 
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void switchPortChanged(Long switchId) 
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void linkDiscoveryUpdate(LDUpdate update)
	{

	}

	@Override
	public void linkDiscoveryUpdate(List<LDUpdate> updateList)
	{
		// TODO Auto-generated method stub

	}
}
