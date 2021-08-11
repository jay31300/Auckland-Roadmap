import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;

/**
 * This is the main class for the mapping program. It extends the GUI abstract
 * class and implements all the methods necessary, as well as having a main
 * function.
 * 
 * @author tony
 */
public class Mapper extends GUI {

	public static final Color NODE_COLOUR = new Color(77, 113, 255);
	public static final Color SEGMENT_COLOUR = new Color(130, 130, 130);
	public static final Color HIGHLIGHT_COLOUR = new Color(255, 219, 77);

	// these two constants define the size of the node squares at different zoom
	// levels; the equation used is node size = NODE_INTERCEPT + NODE_GRADIENT *
	// log(scale)
	public static final int NODE_INTERCEPT = 1;
	public static final double NODE_GRADIENT = 0.8;

	// defines how much you move per button press, and is dependent on scale.
	public static final double MOVE_AMOUNT = 100;
	// defines how much you zoom in/out per button press, and the maximum and
	// minimum zoom levels.
	public static final double ZOOM_FACTOR = 1.3;
	public static final double MIN_ZOOM = 1, MAX_ZOOM = 200;

	// how far away from a node you can click before it isn't counted.
	public static final double MAX_CLICKED_DISTANCE = 0.15;

	// these two define the 'view' of the program, ie. where you're looking and
	// how zoomed in you are.
	private Location origin;
	private double scale;

	// our data structures.
	private Graph graph;
	private Trie trie;

	private static double bestDist = Double.MAX_VALUE;
	private static Node closest = null;
	private static Node closestV2 = null;

	public static Set<Node> vistedNode = new HashSet<Node>();

	public static PriorityQueue<Tuple> fringe = new PriorityQueue<Tuple>();
	public static ArrayList<Node> neighbours = new ArrayList<Node>();

	public static ArrayList <Segment> highlightedSegz = new ArrayList<Segment>();


	public static int numSubTrees;
	public static Set<Node> articulationP;

	@Override
	protected void redraw(Graphics g) {
		if (graph != null)
			graph.draw(g, getDrawingAreaDimension(), origin, scale);
	}

	@Override
	protected void onClick(MouseEvent e) {
		Location clicked = Location.newFromPoint(e.getPoint(), origin, scale);
		// find the closest node.
		bestDist = Double.MAX_VALUE;
//		closest = null;
//		closestV2 = null;


		for (Node node : graph.nodes.values()) {
			double distance = clicked.distance(node.location);
			if (distance < bestDist) {
				bestDist = distance;
				closest = node;
			}
		}


		// if it's close enough, highlight it and show some information.
		if (clicked.distance(closest.location) < MAX_CLICKED_DISTANCE) {
			graph.setHighlight(closest);
			getTextOutputArea().setText(closest.toString());

			if(closestV2 != null){ 
				graph.setHighlight2(closestV2); //can only highligt one at a time
				aStar(closest, closestV2);
				
			}

		}
		closestV2 = closest;
		

	}
	public void aStar(Node start, Node goal){
		boolean goalReached = false;
		for(Segment i : graph.segments){
			i.highlighted = false;
		}
		highlightedSegz.clear();

		vistedNode = new HashSet<Node>();
		//Makes new Tupoo with null partent cause its very starting node theres no costtohere, last pram gets cost to end
		Tuple intial = new Tuple(start, null, 0, start.location.distance(goal.location));
		fringe.add(intial);
		while (!fringe.isEmpty()){
			Tuple currentTuple = fringe.poll();
			Node currentNode = currentTuple.nodeCurrent;
			vistedNode.add(currentNode);
			if(currentNode == goal){
				goalReached = true;
				break;
			}
			neighbours = new ArrayList<Node>();
			for(Segment seg : currentNode.segments){
				if(seg.start == currentNode){
					neighbours.add(seg.end);
				}
				else{
					neighbours.add(seg.start);
				}
			}

			double costSoFar = currentTuple.costToHere;
			for(Node neigh : neighbours){
				if(!vistedNode.contains(neigh)){
					double length = 0;
					//Finds the weight of all segments
					for(Segment allSeg : graph.segments){
						if (allSeg.node1ID == currentNode.nodeID && allSeg.node2ID == neigh.nodeID) {
							length = length + allSeg.length;
						}
						if (allSeg.node1ID == neigh.nodeID && allSeg.node2ID == currentNode.nodeID) {
							length = length + allSeg.length;
						}
					}
					double costToNewNeigh = costSoFar + length;
					double coseToEnd = costToNewNeigh + neigh.location.distance(goal.location);

					neigh.previousNodes = currentNode;

					fringe.add(new Tuple(neigh, currentNode, costToNewNeigh, coseToEnd));
				}
			}
		}

		if(goalReached) {
			Node nodeBackTrak = goal;
			while (nodeBackTrak != start) {
				for (Segment segz : nodeBackTrak.segments) {

					if (segz.end == nodeBackTrak && segz.start == nodeBackTrak.previousNodes) {
						highlightedSegz.add(segz);
					}
					if (segz.start == nodeBackTrak && segz.end == nodeBackTrak.previousNodes) { //??
						highlightedSegz.add(segz);
					}
				}
				nodeBackTrak = nodeBackTrak.previousNodes;
			}
			getTextOutputArea().append("\nThis is your journey \n");

			ArrayList<String> noDuplicates = new ArrayList<>();
			double totalkm = 0.0;

			for (Segment i : highlightedSegz) {
				i.highlighted = true;

				if(!noDuplicates.contains(i.road.name)) {
					getTextOutputArea().append(i.road.name + " distance of: " + i.length + "\n");
				}
				noDuplicates.add(i.road.name);
				totalkm += i.length;
			}
			getTextOutputArea().append("Your total distance is " + totalkm + "km\n");

		}
	}

	public void findArticu() {
		ArrayList<Node> nodeNotVis = new ArrayList<>();
		for (Node n : graph.nodes.values()) {
			nodeNotVis.add(n);
		}

		Set<Node> articulationP = articSearch(nodeNotVis);

		for(Node n : articulationP){
			n.artPoint = true;
		}

		getTextOutputArea().setText("");
		getTextOutputArea().setText(articulationP.size() + " Articulation points mapped out: " );
	}

	public Set<Node> articSearch(List<Node> nodeNotVis) {
		for (Node n : nodeNotVis)
			n.count = Integer.MAX_VALUE;

		articulationP = new HashSet<>();

		while (!nodeNotVis.isEmpty()) {
			Node start = nodeNotVis.get(0);
			start.count = 0;
			numSubTrees = 0;

			List<Node> neighbours = new ArrayList<>();

			for (Segment seg : start.segments) {
				if (seg.start == start) {
					neighbours.add(seg.end);
				} else if (seg.end == start) {
					neighbours.add(seg.start);
				}
			}

			for (Node node : neighbours) {
				if (node.count == Integer.MAX_VALUE) {
					iterArtPts(node, 1, start, nodeNotVis);
					numSubTrees++;
				}
			}
			if (numSubTrees > 1) {
				articulationP.add(start);
			}
			nodeNotVis.remove(start);
		}
		return articulationP;
	}

	public void iterArtPts(Node start, int count, Node root, List<Node> nodeNotVis) {
		Stack<ArticulationPoint> apStack = new Stack<>();
		apStack.push(new ArticulationPoint(start, 1, new ArticulationPoint(root, 0, null)));

		while (!apStack.isEmpty()) {
			ArticulationPoint currentAP = apStack.peek();
			Node currentNode = currentAP.node;

			if (currentNode.count == Integer.MAX_VALUE) {
				currentAP.reachBack = currentAP.count;
				currentNode.count = currentAP.count;

				currentNode.neighbours = new ArrayList<>();
				for (Segment s : currentNode.segments) {
					if (s.start == currentNode) {
						currentNode.neighbours.add(s.end);
					} else if (s.end == currentNode) {
						currentNode.neighbours.add(s.start);
					}
				}

				currentAP.children = new ArrayList<>();
				for (Node neighbour : currentNode.neighbours) {
					if (neighbour != currentAP.parent.node) {
						currentAP.children.add(neighbour);
					}
				}

			}
			else if (!currentAP.children.isEmpty()) {
				Node child = currentAP.children.remove(0);
				if (child.count < Integer.MAX_VALUE) {
					currentAP.reachBack = Math.min(currentAP.reachBack, child.count);
				} else {
					apStack.push(new ArticulationPoint(child, currentNode.count + 1, currentAP));
				}

			} else {
				if (currentNode.nodeID != start.nodeID) {
					if (currentAP.reachBack >= currentAP.parent.count) {
						articulationP.add(currentAP.parent.node);
					}
					currentAP.parent.reachBack = Math.min(currentAP.parent.reachBack, currentAP.reachBack);
				}
				apStack.pop();
				nodeNotVis.remove(currentAP.node);
			}
		}
	}

	@Override
	protected void onSearch() {
		if (trie == null)
			return;

		String query = getSearchBox().getText();
		Collection<Road> selected = trie.get(query);
		
		boolean exactMatch = false;
		for (Road road : selected)
			if (road.name.equals(query))
				exactMatch = true;
			
		if (exactMatch) {
			Collection<Road> exactMatches = new HashSet<>();
			for (Road road : selected)
				if (road.name.equals(query))
					exactMatches.add(road);
			selected = exactMatches;
		}

		graph.setHighlight(selected);
		
		Collection<String> names = new HashSet<>();
		for (Road road : selected)
			names.add(road.name);
		String str = "";
		for (String name : names)
			str += name + "; ";

		if (str.length() != 0)
			str = str.substring(0, str.length() - 2);
		getTextOutputArea().setText(str);
	}

	@Override
	protected void onMove(Move m) {
		if (m == GUI.Move.NORTH) {
			origin = origin.moveBy(0, MOVE_AMOUNT / scale);
		} else if (m == GUI.Move.SOUTH) {
			origin = origin.moveBy(0, -MOVE_AMOUNT / scale);
		} else if (m == GUI.Move.EAST) {
			origin = origin.moveBy(MOVE_AMOUNT / scale, 0);
		} else if (m == GUI.Move.WEST) {
			origin = origin.moveBy(-MOVE_AMOUNT / scale, 0);
		} else if (m == GUI.Move.ZOOM_IN) {
			if (scale < MAX_ZOOM) {
				// yes, this does allow you to go slightly over/under the
				// max/min scale, but it means that we always zoom exactly to
				// the centre.
				scaleOrigin(true);
				scale *= ZOOM_FACTOR;
			}
		} else if (m == GUI.Move.ZOOM_OUT) {
			if (scale > MIN_ZOOM) {
				scaleOrigin(false);
				scale /= ZOOM_FACTOR;
			}
		}
	}

	@Override
	protected void onLoad(File nodes, File roads, File segments, File polygons) {
		graph = new Graph(nodes, roads, segments, polygons);
		trie = new Trie(graph.roads.values());
		origin = new Location(-250, 250); // close enough
		scale = 1;

		findArticu();
	}

	/**
	 * This method does the nasty logic of making sure we always zoom into/out
	 * of the centre of the screen. It assumes that scale has just been updated
	 * to be either scale * ZOOM_FACTOR (zooming in) or scale / ZOOM_FACTOR
	 * (zooming out). The passed boolean should correspond to this, ie. be true
	 * if the scale was just increased.
	 */
	private void scaleOrigin(boolean zoomIn) {
		Dimension area = getDrawingAreaDimension();
		double zoom = zoomIn ? 1 / ZOOM_FACTOR : ZOOM_FACTOR;

		int dx = (int) ((area.width - (area.width * zoom)) / 2);
		int dy = (int) ((area.height - (area.height * zoom)) / 2);

		origin = Location.newFromPoint(new Point(dx, dy), origin, scale);
	}

	public static void main(String[] args) {
		new Mapper();
	}
}

// code for COMP261 assignments