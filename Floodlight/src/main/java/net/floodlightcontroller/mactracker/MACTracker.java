package net.floodlightcontroller.mactracker;


import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.topology.ITopologyService;
import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDeviceService;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.Set;
import java.util.Collection;
import java.util.List;
import java.util.Map;



public class MACTracker implements IFloodlightModule, IOFMessageListener, ILinkDiscoveryListener,IOFSwitchListener
{
	protected IFloodlightProviderService floodlightProvider;
	protected Set<Long> macAddresses;
	protected static Logger logger;
	protected ILinkDiscoveryService linkDiscover;
	protected IDeviceService deviceManager;
	protected ITopologyService topology;


	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) 
	{
		Ethernet eth =IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

		Long sourceMACHash = Ethernet.toLong(eth.getSourceMACAddress());

		if (!macAddresses.contains(sourceMACHash)) {
			macAddresses.add(sourceMACHash);
			logger.info("MAC Address: {} seen on switch: {}",
					HexString.toHexString(sourceMACHash),
					sw.getId());
		}

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
		logger = LoggerFactory.getLogger(MACTracker.class);

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
		return MACTracker.class.getSimpleName();
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
