package net.floodlightcontroller.directGraph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A directed graph data structure.
 */

public class DirectGraph 
{
	private ConcurrentHashMap <String, Vertex> m_vertices;
	private Vertex m_sourceVertex;
	private Vertex m_destVertex;
	private final int Capacity = 1;
	private final String ExpendSuffix_1 = ".1";
	private final String ExpendSuffix_2 = ".2";
	
	// Vector<Edge> of edges in the graph
	private List<Edge> m_edges;


	/**
	 * Construct a new graph without any vertices or edges
	 */
	public DirectGraph() 
	{
		m_vertices = new ConcurrentHashMap <String, Vertex>();
		m_edges = new ArrayList<Edge>();
		m_sourceVertex = null;
		m_destVertex = null;
	}

	/**
	 * Are there any vertices in the graph
	 * 
	 * @return true if there are no vertices in the graph
	 */
	public boolean isEmpty() 
	{
		return m_vertices.size() == 0;
	}

	/**
	 * Add a vertex to the graph
	 * 
	 * @param v
	 *            the Vertex to add
	 * @return true if the vertex was added
 	 *	       false if it was already in the graph.
	 */
	public void addVertex(Vertex v)
	{
		if (!m_vertices.containsValue(v)) 
		{
			m_vertices.put(v.getName(),v);
		}
	}

	/**
	 * Create Vertex with input name & Add a vertex to the graph
	 * 
	 * @param vertexName
	 *            the vertexName to add
	 *            
	 * @return true if the vertex was added
	 *  	   false if it was already in the graph.
	 */
	public Vertex addVertex(String vertexName) 
	{
		if (!m_vertices.containsKey(vertexName))
		{
			Vertex v = new Vertex(vertexName);
			m_vertices.put(vertexName,v);
		}
		return m_vertices.get(vertexName);
	}

	/**
	 * Get the vertex count.
	 * 
	 * @return the number of vertices in the graph.
	 */
	public int size() 
	{
		return m_vertices.size();
	}

	/**
	 * Get the source vertex
	 * @return the source vertex if one is set
 	 *         null if no vertex has been set as source        
	 */
	public Vertex getSourceVertex()
	{
		return m_sourceVertex;
	}
	
	/**
	 * Get the dest vertex
	 * @return the dest vertex if one is set
	 * 		   null if no vertex has been set as dest        
	 */
	public Vertex getDestVertex()
	{
		return m_destVertex;
	}
	
	/**
	 * Get the source vertex by input source vertex name
	 * 
	 * @param sourceName
	 *            the name of the source vertex that need to returns.
	 *            
	 * @return source vertex if vertex adapt to input name exist
	 *  	   null the input vertex name not exist.
	 */
	public Vertex getSourceVertex(String sourceName)
	{
		if(m_vertices.containsKey(sourceName))
		{
			return m_vertices.get(sourceName);
		}
		return null;
	}
	
	/**
	 * Get the dest vertex by input dest vertex name
	 * 
	 * @param destName
	 *            the name of the dest vertex that need to returns.
	 *            
	 * @return dest vertex if vertex adapt to input name exist
	 *  	   null the input vertex name not exist.
	 */
	public Vertex getDestVertex(String destName)
	{
		if(m_vertices.containsKey(destName))
		{
			return m_vertices.get(destName);
		}
		return null;
	}

	
	/**
	 * Get the graph vertices
	 * 
	 * @return the graph vertices
	 */
	public ConcurrentHashMap <String,Vertex> getVerticies() 
	{
		return this.m_vertices;
	}

	/**
	 * Add input Edge to graph
	 * @param edge
	 * 			The edge to add.
	 * @return true if edge added.
	 * 		   false if from Vertex already has this Edge  & edge didn't added.
	 */
	public boolean addEdge(Edge edge) 
	{
		Vertex from  = edge.getFrom();
		Vertex to = edge.getTo();

		if (from.findEdge(to) != null)
		{
			return false;
		}
		else 
		{
			from.addEdge(edge);
			to.addEdge(edge);
			m_edges.add(edge);
			edge.setUpdate();
			return true;
		}
	}

	/**
	 * Insert a directed, weighted Edge into the graph.
	 * 
	 * @param from
	 *            - the Edge starting vertex
	 * @param to
	 *            - the Edge ending vertex
	 * @param capacity
	 *            - the Edge capacity
	 * @param sourcePort
	 * 			  - the source vertex port on the edge.
	 * 
	 * @return true if the Edge was added, false if from already has this Edge
	 * @throws IllegalArgumentException
	 *             if from/to are not vertices in the graph
	 */
	public boolean addEdge(Vertex from, Vertex to, int capacity,short sourcePort) throws IllegalArgumentException 
	{
		if (m_vertices.containsValue(from) == false)
		{
			throw new IllegalArgumentException("from is not in graph");
		}
		if (m_vertices.containsValue(to) == false) {
			throw new IllegalArgumentException("to is not in graph");
		}
		Edge e = new Edge(from, to, capacity,sourcePort);
		return addEdge(e);
	}

	/**
	 * Insert a directed, weighted Edge into the graph.
	 * 
	 * @param fromVertexName
	 *            - the Edge starting vertex name
	 * @param to
	 *            - the Edge ending vertex name
	 * @param capacity
	 *            - the Edge capacity
	 * @param sourcePort
	 * 			  - the source vertex port on the edge.
	 * 
	 * @return true if the Edge was added, false if from already has this Edge
	 * @throws IllegalArgumentException
	 *             if from/to are not vertices in the graph
	 */
	public boolean addEdge(String fromVertexName, String toVertexName, int capacity,short sourcePort)
	{
		Vertex from;
		Vertex to;
		if (!m_vertices.containsKey(fromVertexName)) 
		{
			addVertex(fromVertexName);
		}
		if (!m_vertices.containsKey(toVertexName)) 
		{
			addVertex(toVertexName);
		}
		from = m_vertices.get(fromVertexName);
		to = m_vertices.get(toVertexName);

		Edge e = new Edge(from, to, capacity,sourcePort);
		return addEdge(e);
	}

	/**
	 * Get the graph edges
	 * 
	 * @return the graph edges
	 */
	public List<Edge> getEdges()
	{
		return m_edges;
	}

	/**
	 * Remove a vertex from the graph
	 * 
	 * @param v
	 *            the Vertex to remove
	 * @return true if the Vertex was removed
	 */
	public boolean removeVertex(Vertex v) 
	{
		String vertexKey = v.getName();		
		if (!m_vertices.containsKey(vertexKey))
		{
			return false;
		}

		m_vertices.remove(vertexKey);

		// Remove the edges associated with v
		for (int n = 0; n < v.getOutgoingEdgeCount(); n++)
		{
			Edge e = v.getOutgoingEdge(n);
			v.remove(e);
			Vertex to = e.getTo();
			to.remove(e);
			m_edges.remove(e);
		}
		for (int n = 0; n < v.getIncomingEdgeCount(); n++) {
			Edge e = v.getIncomingEdge(n);
			v.remove(e);
			Vertex predecessor = e.getFrom();
			predecessor.remove(e);
		}
		return true;
	}

	/**
	 * Remove an Edge from the graph
	 * 
	 * @param from
	 *            - the Edge starting vertex
	 * @param to
	 *            - the Edge ending vertex
	 * @return true if the Edge exists, false otherwise
	 */
	public boolean removeEdge(Vertex from, Vertex to) {
		Edge e = from.findEdge(to);
		if (e == null)
		{
			return false;
		}
		else 
		{
			from.remove(e);
			to.remove(e);
			m_edges.remove(e);
			return true;
		}
	}


	/**
	 * Clear the update state of all edges in the graph by calling clearUpdate() on all edges
	 * 
	 * @see Edge#clearUpdate()
	 */
	private void clearUpdates()
	{
		for(Edge e :m_edges)
		{
			e.clearUpdate();
		}
	}


	public String toString()
	{
		StringBuffer tmp = new StringBuffer("Graph: \n");

		for (Vertex v : m_vertices.values())
		{
			tmp.append(v);
			tmp.append("\n");
		}
		return tmp.toString();
	}


	/**
	 * Method to check if there is an augmenting path present in the residual Graph Gf using BFS
	 * 
	 * @param G
	 * @param s
	 * @param t
	 * @return
	 */
	private boolean isAugmentingPathPresent(Vertex source, Vertex target)
	{
		Queue<Vertex> queue;
		int distanceVertexFrom;
		if (source == target)
		{
			return false;
		}
		for (Vertex v : m_vertices.values())
		{
			v.setDistance(Integer.MAX_VALUE);
		}

		queue = new LinkedList<Vertex>();
		queue.add(source);
		source.setDistance(0);

		while (!queue.isEmpty())
		{
			Vertex u = queue.poll();
			if (u == target) 
			{
				break;
			}

			for(Edge edge : u.getAllEdges())
			{
				Vertex v = edge.other(u);
				if ((v.getDistance() == Integer.MAX_VALUE) && 
						((edge.residualCapacityTo(v)) > 0)) 
				{
					distanceVertexFrom = u.getDistance();
					v.setDistance(distanceVertexFrom + 1);
					queue.add(v);
				}
			}
		}
		return target.getDistance() >= 0;
	}

	
	public Vertex getVertex(String vertexName) 
	{
		return m_vertices.get(vertexName);
	}

	/**
	 * DFS implementation to find the layered graph Gl
	 * 
	 * @param s
	 * @param t
	 * @param maxValue
	 * @return
	 */
	private int DinicDFS(Vertex source, Vertex target, int maxValue) {
		int ret = 0;
		int add = 0;
		int minVal;
		if (maxValue == 0) 
		{
			return 0;
		}
		if (source == target)
		{
			return maxValue;
		}

		for(Edge edge : source.getAllEdges())
		{
			if ((edge.other(source).getDistance() == source.getDistance() + 1)&& 
					(edge.residualCapacityTo(edge.other(source)) > 0))	
			{
				minVal = Math.min(edge.residualCapacityTo(edge.other(source)),maxValue);	
				add = DinicDFS(edge.other(source), target, minVal);		
				ret += add;
				edge.addResidualFlowTo(edge.other(source), add);	
				if (edge.residualCapacityTo(edge.other(source)) == 0)
				{
					break;
				}

			}
		}
		return ret;
	}

	/**
	 * Execute the Dinics method
	 * 
	 * @param G
	 * @param s
	 * @param t
	 */
	public int dinicsMaxFlow(Vertex source, Vertex target) 
	{
		m_sourceVertex = source;
		m_destVertex = target;
		int flow = 0;
		String sourceVertexName = source.getName();
		String targerVertexName = target.getName();
		
		expandGraph(sourceVertexName, targerVertexName);

		// while there exists an augmenting path, use it
		while (isAugmentingPathPresent(source, target))
		{
			int df = DinicDFS(source, target, Integer.MAX_VALUE);
			if (df == 0) 
			{
				break;
			}
			flow += df;
			System.out.println(flow);
		}
		
		reduceGraph(sourceVertexName, targerVertexName);
		return flow;
	}

	public void reduceGraph(String source, String target)
	{
		String from,to,origTo,origFrom;
		Vertex fromVer,toVer;
		short sourcePort;
		Edge edge;
		clearUpdates();

		List<Edge> edgesToAdd = new ArrayList<Edge>();
		List<Edge> edgesToRemove = new ArrayList<Edge>();

		for(Edge e : m_edges)
		{
			fromVer = e.getFrom() ;
			toVer = e.getTo();
			from = e.getFrom().getName();
			to = e.getTo().getName();
			sourcePort = e.getSourcePort();

			if(needUpdate(from, to,source,target) && (!e.isUpdate()))
			{
				if(from == source || from == target)				
				{	
					origTo = to.substring(0,to.indexOf('.'));
					edge = new Edge(fromVer, getVertex(origTo), Capacity, sourcePort);
					edge.setFlow(e.getFlow());
					edgesToAdd.add(edge);

				}
				else if(to == source || to == target)
				{
					origFrom = from.substring(0,from.indexOf('.'));
					edge = new Edge(getVertex(origFrom), toVer, Capacity, sourcePort);
					edge.setFlow(e.getFlow());
					edgesToAdd.add(edge);
				}
				else
				{
					origTo = to.substring(0,to.indexOf('.'));
					origFrom = from.substring(0,from.indexOf('.'));
					if(!(origTo.equals(origFrom)))
					{
						edge = new Edge(getVertex(origFrom),getVertex(origTo),Capacity,sourcePort);
						edge.setFlow(e.getFlow());
						edgesToAdd.add(edge);
					}

				}
				edgesToRemove.add(e);
			}
		}
		
		addEdgeFromList(edgesToAdd);
		removeEdgeFromList(edgesToRemove);
		reduceVertices();
	}

	private void reduceVertices()
	{
		List<Vertex> vertexToRemove = new ArrayList<Vertex>();

		for(Vertex v: m_vertices.values())
		{
			if(v.getName().contains("."))
			{
				vertexToRemove.add(v);
			}
		}		
		for(Vertex v :vertexToRemove)
		{
			removeVertex(v);
		}

	}

	private void addEdgeFromList(List<Edge> edgesToAdd)
	{
		for(Edge e :edgesToAdd)
		{
			addEdge(e);
		}
		edgesToAdd.clear();
	}

	private void removeEdgeFromList(List<Edge> edgesToRemove)
	{
		for(Edge e :edgesToRemove)
		{
			removeEdge(e.getFrom(),e.getTo());
		}
		edgesToRemove.clear();	
	}

	private void expandGraph(String source, String target)
	{
		String from,to;
		Vertex fromVer,toVer;
		short sourcePort;
		clearUpdates();
		expandVertices(source,target);

		List<Edge> edgesToAdd = new ArrayList<Edge>();
		List<Edge> edgesToRemove = new ArrayList<Edge>();

		for(Edge e : m_edges)
		{
			fromVer = e.getFrom() ;
			toVer= e.getTo();
			from = e.getFrom().getName();
			to = e.getTo().getName();
			sourcePort = e.getSourcePort();

			if(needUpdate(from, to,source,target) && (!e.isUpdate()))
			{
				if(from == source || from == target)
				{	
					edgesToAdd.add(new Edge(fromVer,getVertex( to + ExpendSuffix_1), Capacity, sourcePort));
	
				}
				else if(to == source || to == target)
				{
					edgesToAdd.add(new Edge(getVertex(from + ExpendSuffix_2), toVer, Capacity, sourcePort));
				}
				else
				{
					edgesToAdd.add(new Edge(getVertex(from + ExpendSuffix_2), getVertex(to + ExpendSuffix_1), Capacity, sourcePort));
				}

				edgesToRemove.add(e);
			}
		}

		addEdgeFromList(edgesToAdd);
		removeEdgeFromList(edgesToRemove);	
	}

	private boolean needUpdate(String fromName,String toName,String source, String target)
	{
		if( (fromName != source && toName != target)||
				(fromName != target && toName != source) )
		{
			return true;
		}
		return false;
	}

	private void expandVertices(String source, String target)
	{
		String vertexName;
		Iterator<Entry<String, Vertex>> it = m_vertices.entrySet().iterator();
		while (it.hasNext()) 
		{
			Map.Entry<String, Vertex> entry =it.next();
			vertexName = (String) entry.getKey();
			if(vertexName != source && vertexName != target && !vertexName.contains("."))
			{
				addVertex( vertexName + ExpendSuffix_1);
				addVertex( vertexName + ExpendSuffix_2);

				addEdge( vertexName + ExpendSuffix_1, vertexName + ExpendSuffix_2, Capacity, (short)0);
			}
		}
	}
}
