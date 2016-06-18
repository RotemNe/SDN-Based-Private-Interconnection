package net.floodlightcontroller.directGraph;


/**
 * A directed, weighted edge in a graph
 */
public class Edge
{
	private Vertex m_from;	
	private Vertex m_to;
	private int m_capacity;
	private int m_flow;
	private short m_sourcePort;
	private short m_destPort;
	private boolean m_isUpdate;
	private boolean m_isRuleInstalled;

	/**
	 * Create a zero cost edge between from and to
	 * @param from
	 *          the starting vertex
	 * @param to
	 *          the ending vertex
	 */
	public Edge(Vertex from, Vertex to,short sourcePort,short destPort) 
	{
		this(from, to, 0,sourcePort,destPort);
	}

	/**
	 * Create an edge between from and to with the given cost.
	 * 
	 * @param from
	 *          the starting vertex
	 * @param to
	 *          the ending vertex
	 * @param cost
	 *          the cost of the edge
	 */
	public Edge(Vertex from, Vertex to, int capacity,short sourcePort, short destPort) 
	{
		m_from = from;
		m_to = to;
		m_capacity = capacity;
		m_isUpdate = false;
		m_flow =0;
		m_sourcePort = sourcePort;
		m_destPort = destPort;
		m_isRuleInstalled = false;
	}

	/**
	 * Get the ending vertex
	 * 
	 * @return ending vertex
	 */
	public Vertex getTo()
	{
		return m_to;
	}

	/**
	 * Get the starting vertex
	 * 
	 * @return starting vertex
	 */
	public Vertex getFrom() 
	{
		return m_from;
	}

	/**
	 * Get the cost of the edge
	 * 
	 * @return cost of the edge
	 */
	public int getCapacity() 
	{
		return m_capacity;
	}

	public int getFlow()
	{
		return m_flow;
	}
	
	public short getSourcePort()
	{
		return m_sourcePort;
	}
	
	public short getDestPort()
	{
		return m_destPort;
	}
	
	public boolean getIsRuleInstalled()
	{
		return m_isRuleInstalled;
	}

	/**
	 * Set the update flag of the edge
	 * 
	 */
	public void setUpdate() 
	{
		m_isUpdate = true;
	}
	
	public void setCapacity(int c)
	{
		m_capacity = c;
	}
	
	public void setIsRuleInstalled()
	{
		m_isRuleInstalled = true;
	}
	
	public void clearIsRuleInstalled()
	{
		m_isRuleInstalled = false;
	}

	/**
	 * Clear the update mark flag
	 */
	public void clearUpdate() 
	{
		m_isUpdate = false;
	}

	/**
	 * Get the edge update flag
	 * 
	 * @return edge update flag
	 */
	public boolean isUpdate() 
	{
		return m_isUpdate;
	}

	/**
	 * String rep of edge
	 * 
	 * @return string rep with from/to vertex names and cost
	 */
	public String toString() 
	{
		StringBuffer tmp = new StringBuffer("Edge[from: ");
		tmp.append(m_from.getName());
		tmp.append(",to: ");
		tmp.append(m_to.getName());
		tmp.append(", capacity: ");
		tmp.append(m_capacity);
		tmp.append(", flow: " + getFlow());
		tmp.append(", Source Port: " + getSourcePort());
		tmp.append("]");
		return tmp.toString();
	}

	public Vertex other(Vertex vertex) 
	{
		if (vertex == m_from)
		{
			return m_to;
		}
		else if (vertex == m_to)
		{
			return m_from;
		}
		else
		{
			throw new RuntimeException("Illegal endpoint");
		}
	}


	public void setFlow(int flow) 
	{
		m_flow = flow;
	}

	public int residualCapacityTo(Vertex vertex) 
	{
		if (vertex == m_from)
		{
			return m_flow;
		}
		else if (vertex == m_to)
		{
			return m_capacity - m_flow;
		}
		else
		{
			throw new RuntimeException("Illegal endpoint");
		}
	}

	public void addResidualFlowTo(Vertex vertex, int delta) 
	{
		if (vertex == m_from)
		{
			m_flow -= delta;
		}
		else if (vertex == m_to)
		{
			m_flow += delta;
		}
		else
		{
			throw new RuntimeException("Illegal endpoint");
		}
	}
	
	public void updateFrom(Vertex fromUpd)
	{
		m_from = fromUpd;
	}
	
	public void updateTo(Vertex toUpd)
	{
		m_to = toUpd;
	}
	
	public void updateFromTo(Vertex fromUpd, Vertex toUpd)
	{
		m_from = fromUpd;
		m_to = toUpd;
	}

}