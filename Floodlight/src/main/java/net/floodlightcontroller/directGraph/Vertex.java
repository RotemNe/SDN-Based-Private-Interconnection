package net.floodlightcontroller.directGraph;

import java.util.ArrayList;
import java.util.List;


/**
 * A named graph vertex with optional data.
 */

public class Vertex implements Comparable<Vertex> 
{
	private List<Edge> incomingEdges;

	private List<Edge> outgoingEdges;
	
	private List<Edge> allEdges;

	private String name;

	private boolean mark;

	private int markState;
	
	private int distance;


	/**
	 * Create a Vertex with name n and given data
	 * 
	 * @param n -
	 *          name of vertex
	 * @param data -
	 *          data associated with vertex
	 */
	public Vertex(String n)
	{
		incomingEdges = new ArrayList<Edge>();
		outgoingEdges = new ArrayList<Edge>();
		allEdges = new ArrayList<Edge>();
		name = n;
		mark = false;
		distance = Integer.MAX_VALUE;

	}

	/**
	 * @return the possibly null name of the vertex
	 */
	public String getName() {
		return name;
	}

	public int getDistance()
	{
		return distance;
	}
	
	public void setDistance(int dist)
	{
		distance = dist;
	}

	/**
	 * Add an edge to the vertex. If edge.from is this vertex, its an outgoing
	 * edge. If edge.to is this vertex, its an incoming edge. If neither from or
	 * to is this vertex, the edge is not added.
	 * 
	 * @param e -
	 *          the edge to add
	 * @return true if the edge was added, false otherwise
	 */
	public boolean addEdge(Edge e) {
		if (e.getFrom() == this)
			outgoingEdges.add(e);
		else if (e.getTo() == this)
			incomingEdges.add(e);
		else
			return false;
		allEdges.add(e);
		return true;
	}

	/**
	 * Add an outgoing edge ending at to.
	 * 
	 * @param to -
	 *          the destination vertex
	 * @param cost
	 *          the edge cost
	 */
	public void addOutgoingEdge(Vertex to, int cost) {
		Edge out = new Edge(this, to, cost,(short)0);
		outgoingEdges.add(out);
	}

	/**
	 * Add an incoming edge starting at from
	 * 
	 * @param from -
	 *          the starting vertex
	 * @param cost
	 *          the edge cost
	 */
	public void addIncomingEdge(Vertex from, int cost) {
		Edge out = new Edge(this, from, cost,(short)0);
		incomingEdges.add(out);
	}

	/**
	 * Check the vertex for either an incoming or outgoing edge mathcing e.
	 * 
	 * @param e
	 *          the edge to check
	 * @return true it has an edge
	 */
	public boolean hasEdge(Edge e) {
		if (e.getFrom() == this)
			return incomingEdges.contains(e);
		else if (e.getTo() == this)
			return outgoingEdges.contains(e);
		else
			return false;
	}

	/**
	 * Remove an edge from this vertex
	 * 
	 * @param e -
	 *          the edge to remove
	 * @return true if the edge was removed, false if the edge was not connected
	 *         to this vertex
	 */
	public boolean remove(Edge e) {
		if (e.getFrom() == this)
			outgoingEdges.remove(e);
		else if (e.getTo() == this)
			incomingEdges.remove(e);
		else
			return false;
		allEdges.remove(e);
		return true;
	}

	/**
	 * 
	 * @return the count of incoming edges
	 */
	public int getIncomingEdgeCount() {
		return incomingEdges.size();
	}

	/**
	 * Get the ith incoming edge
	 * 
	 * @param i
	 *          the index into incoming edges
	 * @return ith incoming edge
	 */
	public Edge getIncomingEdge(int i) {
		return incomingEdges.get(i);
	}
	


	/**
	 * Get the incoming edges
	 * 
	 * @return incoming edge list
	 */
	public List<Edge> getIncomingEdges() {
		return this.incomingEdges;
	}

	/**
	 * 
	 * @return the count of incoming edges
	 */
	public int getOutgoingEdgeCount() {
		return outgoingEdges.size();
	}


	/**
	 * Get the ith outgoing edge
	 * 
	 * @param i
	 *          the index into outgoing edges
	 * @return ith outgoing edge
	 */
	public Edge getOutgoingEdge(int i) {
		return outgoingEdges.get(i);
	}
	

	/**
	 * Get the outgoing edges
	 * 
	 * @return outgoing edge list
	 */
	public List<Edge> getOutgoingEdges() {
		return this.outgoingEdges;
	}

	
	public List<Edge> getAllEdges()
	{
		return this.allEdges;
	}
     /*
	 * Search the outgoing edges looking for an edge whose's edge.to == dest.
	 * 
	 * @param dest
	 *          the destination
	 * @return the outgoing edge going to dest if one exists, null otherwise.
	 */
	public Edge findEdge(Vertex dest) {
		for (Edge e : outgoingEdges) {
			if (e.getTo() == dest)
				return e;
		}
		return null;
	}

	/**
	 * Search the outgoing edges for a match to e.
	 * 
	 * @param e -
	 *          the edge to check
	 * @return e if its a member of the outgoing edges, null otherwise.
	 */
	public Edge findEdge(Edge e) {
		if (outgoingEdges.contains(e))
			return e;
		else
			return null;
	}

	/**
	 * What is the cost from this vertext to the dest vertex.
	 * 
	 * @param dest -
	 *          the destination vertex.
	 * @return Return Integer.MAX_VALUE if we have no edge to dest, 0 if dest is
	 *         this vertex, the cost of the outgoing edge otherwise.
	 */
	public int capacity(Vertex dest) {
		if (dest == this)
			return 0;

		Edge e = findEdge(dest);
		int cost = Integer.MAX_VALUE;
		if (e != null)
			cost = e.getCapacity();
		return cost;
	}

	/**
	 * Is there an outgoing edge ending at dest.
	 * 
	 * @param dest -
	 *          the vertex to check
	 * @return true if there is an outgoing edge ending at vertex, false
	 *         otherwise.
	 */
	public boolean hasEdge(Vertex dest) {
		return (findEdge(dest) != null);
	}

	/**
	 * Has this vertex been marked during a visit
	 * 
	 * @return true is visit has been called
	 */
	public boolean visited() {
		return mark;
	}

	/**
	 * Set the vertex mark flag.
	 * 
	 */
	public void mark() {
		mark = true;
	}

	/**
	 * Set the mark state to state.
	 * 
	 * @param state
	 *          the state
	 */
	public void setMarkState(int state) {
		markState = state;
	}

	/**
	 * Get the mark state value.
	 * 
	 * @return the mark state
	 */
	public int getMarkState() {
		return markState;
	}

	/**
	 * Visit the vertex and set the mark flag to true.
	 * 
	 */
	public void visit() {
		mark();
	}

	/**
	 * Clear the visited mark flag.
	 * 
	 */
	public void clearMark() {
		mark = false;
	}

	/**
	 * @return a string form of the vertex with in and out edges.
	 */
	public String toString() 
	{
		StringBuffer tmp = new StringBuffer();
		tmp.append("Vertex " + name +": ");
		tmp.append("InEdges {");
		for (int i = 0; i < incomingEdges.size(); i++) {
			Edge e = incomingEdges.get(i);
			if (i > 0)
				tmp.append(',');
			tmp.append(e);
			/*
			tmp.append("[from = "+ e.getFrom().name);
			tmp.append(", capacity = " +e.getCapacity());
			tmp.append(']');*/
		}
		tmp.append("}, OutEdges {");
		for (int i = 0; i < outgoingEdges.size(); i++) {
			Edge e = outgoingEdges.get(i);
			if (i > 0)
				tmp.append(',');
			tmp.append(e);
			/*
			tmp.append("[To = "+ e.getTo().name);
			tmp.append(", capacity = " +e.getCapacity());
			tmp.append(']');
			*/
		}
		tmp.append('}');
		return tmp.toString();
	}

	@Override
	public int compareTo(Vertex otherVertex) 
	{		
		if(name == otherVertex.getName())
		{
			return 1;
		}
		return 0;
	}
	public boolean equals(String o) 
	{
		if(name.equals(o))
			return true;
		return false;
	}
	
}
