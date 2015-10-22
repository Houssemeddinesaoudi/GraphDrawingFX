package at.searles.graphdrawing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class Leveling<V> {
	
	private static final int CONTROL_POINT_DISTANCE = 24;
	private static final int SPACING = 14;
	private static final int LEVEL_GAP = 48;
	
	private static final int UNMARKED = Integer.MAX_VALUE;
	
	private List<Node> nodes;
	private Map<Integer, Level> levels;
	
	private Map<V, ? extends Collection<V>> graph;
	private Map<V, Node> map;
	
	public String toString() {
		return nodes.toString();
	}
	
	public void setGraph(Map<V, ? extends Collection<V>> graph) {
		this.graph = graph;
		initNodes();
		breakCycles();
		
		for(Node n : nodes) if(n.level == UNMARKED) {
			n.assignMinLevel();
		}

		for(Node n : nodes) if(n.level == 0) {
			// Only needs to be called for level 0 because every 
			// connected graph component has at least one such
			// node because of getMinLevel()
			n.normalizeLevel();
		}

		reinitNodes();

		System.out.println("Normalized: " + this);
		
		// TODO: Pull back up nodes so that they are closer to their last node.
		
		// Part 2:
		// Reduce edge crossings
		
		// Now, create vertices and dummies that actually can be drawn
				
		// Create vertices + edges + dummy nodes etc...
		List<Vertex> vertices = new LinkedList<Vertex>();
		this.levels = new TreeMap<Integer, Level>(); // therefore sorted
		
		for(Node n : nodes) {
			vertices.add(n.getGraphicElement());
		}
		
		for(Vertex v : vertices) {
			v.sortInOut();
		}
		
		//System.out.println(vertices);
		//System.out.println(this.levels);
		
		// Get maximum width of all levels
		double width = 0.;
		
		for(Level l : levels.values()) {
			width = Math.max(width, l.width);
		}

		//System.out.println("w=" + width);
		
		Iterator<Level> i = levels.values().iterator();
		double height = 0.;
		
		Level l = i.next();
		
		if(l.hasInEdges()) {
			height += CONTROL_POINT_DISTANCE;
		}
		
		while(true) {
			l.layout(height, width);
			height += l.height;
			
			if(i.hasNext()) {
				height += LEVEL_GAP;
				l = i.next();
			} else {
				if(l.hasOutEdges()) height += CONTROL_POINT_DISTANCE;
				break;
			}
		}
	}
	
	public boolean checkGraphIntegrity() {
		for(Node n : nodes) {
			for(Node m : n.out) {
				if(m.level <= n.level) {
					// n and m must be in a cycle
					// i.e. there must be a path from m to n
					if(m.isConnectedTo(n, new HashSet<Node>()) == -1) {
						System.out.println("Connection " + n + " -> " + m + " is wrong");
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	public void draw(Group g) {
		for(Node n : nodes) {
			n.vertex.drawEdges(g);
			n.vertex.draw(g);
		}
	}
	
	private void initNodes() {
		nodes = new LinkedList<Node>();
		map = new HashMap<V, Node>();

		// First add nodes
		for(Map.Entry<V, ? extends Collection<V>> entry : graph.entrySet()) {
			V srcLabel = entry.getKey();
			// get or create node
			Node src = map.get(srcLabel);

			if (src == null) {
				src = new Node(srcLabel);
				nodes.add(src);
				map.put(srcLabel, src);
			}
		}
		
		// Now add edges. This is not done in the previous 
		// step to better preserve the order of the nodes in
		// the graph set (maybe its order is meaningful?)
		for(Map.Entry<V, ? extends Collection<V>> entry : graph.entrySet()) {
			Node src = map.get(entry.getKey());
			
			for(V dstLabel : entry.getValue()) {
				Node dst = map.get(dstLabel);
				
				if(dst == null) {
					dst = new Node(dstLabel);
					nodes.add(dst);
					map.put(dstLabel, dst);
				} 
				
				src.out.add(dst);
				dst.in.add(src);
			}
		}
	}
	
	private void reinitNodes() {
		for(Node n : nodes) {
			n.in.clear();
			n.out.clear();
		}
		
		// Re-add edges
		for(Map.Entry<V, ? extends Collection<V>> entry : graph.entrySet()) {
			for(V w : entry.getValue()) {
				Node src = map.get(entry.getKey());
				Node dst = map.get(w);
				
				src.out.add(dst);
				dst.in.add(src);
			}
		}
	}

	private void breakCycles() {
		// Removes edges of cycles
		// Break cycle:
		// Find node such that all unmarked nodes in out (in) are cyclic, i.e., connected to the node itself
		// and minimize this number		
		while(true) {
			int minCycleLength = Integer.MAX_VALUE;

			Node src = null;
			Node dst = null;
			
			// We only enter this part if there is a cycle. Therefore
			// there is always a cycle
			for(Node u : nodes) {
				for(Node v : u.in) {
					int pathLength = u.isConnectedTo(v, new HashSet<Node>());
					
					if(pathLength >= 0 && pathLength < minCycleLength) {
						minCycleLength = pathLength;
						// This is the edge we want to delete
						src = v;
						dst = u;
					}
				}
			}
	
			if(minCycleLength == Integer.MAX_VALUE) {
				break;
			}
			
			src.out.remove(dst);
			dst.in.remove(src);
		}
	}

	
	private class Node {
		List<Node> in = new LinkedList<Node>();
		List<Node> out = new LinkedList<Node>();
		V v;
		
		int level = UNMARKED;
		
		Vertex vertex;
		
		Node(V v) {
			this.v = v;
			if(v == null) throw new IllegalArgumentException();
		}
		
		public String toString() {
			return v.toString() + "(" + level + ")";
		}

		int isConnectedTo(Node src, Set<Node> marked) {
			if(src == this) return 0;
			
			if(marked.contains(this)) return -1;
			
			marked.add(this);
			
			int minPathLength = Integer.MAX_VALUE;
			
			for(Node n : out) {
				int pathLength = n.isConnectedTo(src, marked);
				
				if(pathLength >= 0) {
					if(pathLength < minPathLength) minPathLength = pathLength;
				}
			}
			
			marked.remove(this);
			
			return minPathLength == Integer.MAX_VALUE ? -1 : minPathLength + 1;
		}

		int assignMinLevel() {
			// Assign the minimal possible layer according to
			// nodes above
			if(level == UNMARKED) {
				
				if(in.isEmpty()) {
					this.level = 0;
				} else {
					// Get maximum level of the ones above
					int maxLevel = Integer.MIN_VALUE;
					for(Node u : in) {
						if(u.assignMinLevel() > maxLevel) {
							maxLevel = u.assignMinLevel();
						}
					}
					
					this.level = maxLevel + 1;
				}
			}

			return level;
		}
		
		void normalizeLevel() {
			// similar to getMinLevel, after assigning the
			// minimal layer we try to reduce the distance of nodes and
			// edges by shifting them behind.
			// returns true, if the level was modified (which 
			// might affect other nodes)
			// needs only to be called by nodes that get level 0
			if(out.isEmpty()) return;
			
			int minNextLevel = Integer.MAX_VALUE;
			
			for(Node n : out) {
				n.normalizeLevel();
				if(n.level < minNextLevel) minNextLevel = n.level;
			}
			
			assert minNextLevel > this.level;
			
			if(minNextLevel != Integer.MAX_VALUE) {
				this.level = minNextLevel - 1;
			}
		}
		
		Vertex getGraphicElement() {
			if(this.vertex == null) {
				// after leveling nodes we create dummy nodes
				this.vertex = new Vertex(this);
				this.vertex.initLevel();
				
				for(Node n : out) {
					Vertex srcVertex = this.vertex;
					Vertex dstVertex = n.getGraphicElement();
					
					if(n.level > this.level) {
						// not cycle	
						if(this.level + 1 < n.level) {
							// we need dummy nodes
							int l = this.level + 1;
							
							Dummy dummy = new Dummy(l);
							dummy.initLevel();
							
							//dummy.prev = srcVertex;
							srcVertex.out.add(dummy);
							
							for(l++; l < n.level; l++) {
								Dummy next = new Dummy(l);
								next.initLevel();
								dummy.succ = next;
								//next.prev = dummy;
								
								dummy = next;
							}
							
							dummy.succ = dstVertex;
							dstVertex.in.add(dummy);
						} else {
							// connect directly
							srcVertex.out.add(dstVertex);
							dstVertex.in.add(srcVertex);							
						}
					} else {
						// cycle
						Dummy dummy = new Dummy(n.level, true);
						dummy.initLevel();
						
						dummy.succ = dstVertex;
						dstVertex.in.add(dummy);
						
						for(int l = n.level + 1; l <= this.level; l++) {
							Dummy next = new Dummy(l, true);
							next.initLevel();
							
							//dummy.prev = next;
							next.succ = dummy;
							
							dummy = next;
						}
						
						//dummy.prev = srcVertex;
						srcVertex.out.add(dummy);
					}
				}
			}
			
			return this.vertex;
		}
	}
	
	private class Level {
		ArrayList<GraphElement> elements = new ArrayList<GraphElement>();

		double height = -LEVEL_GAP; // maximum height of elements
		double width = 0.; // sum of width of all elements
		
		public String toString() {
			return elements.toString() + width + "x" + height;
		}
		
		void add(GraphElement element) {
			assert !elements.contains(element);
			height = Math.max(height, element.preferredHeight());
			width += element.width();
			elements.add(element);			
		}
		
		void layout(double y0, double totalWidth) {
			double x0 = (totalWidth - width) / 2.;

			for(GraphElement e : elements) {
				e.setPos(x0, y0);
				x0 += e.width() + SPACING;
			}
		}
		
		boolean hasInEdges() {
			for(GraphElement element : elements) {
				if(element instanceof Leveling.Vertex && !((Leveling<?>.Vertex) element).in.isEmpty()) {
					return true;
				}
			}
			
			return false;
		}

		boolean hasOutEdges() {
			for(GraphElement element : elements) {
				if(element instanceof Leveling.Vertex && !((Leveling<?>.Vertex) element).out.isEmpty()) {
					return true;
				}
			}
			
			return false;
		}
	}

	
	abstract class GraphElement {		
		int levelIndex;
		Level level;
		
		double x0;
		double y0;
		
		GraphElement(int levelIndex) {
			this.levelIndex = levelIndex;
		}
		
		void initLevel() {
			this.level = levels.get(levelIndex);
			
			if(level == null) {
				level = new Level();
				levels.put(levelIndex, level);
			}
			
			level.add(this);
		}
		
		public String toString() {
			return x0 + "|" + y0 + "(" + width() + "x" + height() + ")";
		}
		
		int pos() {
			return level.elements.indexOf(this);
		}
		
		void setPos(double x0, double y0) {
			this.x0 = x0;
			this.y0 = y0;
		}
		
		abstract double width();
		
		abstract double preferredHeight();
		
		double height() {
			return level.height;
		}
	}
	
	class Vertex extends GraphElement {
		Node n;
		List<GraphElement> in = new LinkedList<GraphElement>(); // all in are one level above this node
		List<GraphElement> out = new LinkedList<GraphElement>(); // all out are one level below this node

		Vertex(Node n) {
			super(n.level);
			this.n = n;
		}
		
		public String toString() {
			return "vertex[" + n + "]" + super.toString(); 
		}
		
		void sortInOut() {
			// Sort order of nodes depending on relative position of destination/source of edge
			Comparator<GraphElement> cmp = (u, v) -> {
				if (u.levelIndex == levelIndex
						&& v.levelIndex == levelIndex) {
					int du = u.pos() - pos(); // < 0 if left of this, > 0
											// otherwise
					int dv = v.pos() - pos();

					if (du < 0) { // if du is left
						return dv > 0 ? -1 : dv - du;
					} else {
						return dv < 0 ? 1 : dv - du;
					}
				} else if (u.levelIndex == levelIndex) {
					// v is in above level
					return u.pos() - pos();
				} else if (v.levelIndex == levelIndex) {
					// u is in above level
					return pos() - v.pos();
				} else {
					// both are in top layer
					return u.pos() - v.pos();
				}
			};

			Collections.sort(in, cmp);
			Collections.sort(out, cmp);
		}
		
		Point2D dst(Leveling<?>.GraphElement srcElement) {
			int i = in.indexOf(srcElement);
			int size = in.size();
			
			double x = x0 + width() * (i + 1) / (size + 1);
			
			return new Point2D(x, y0);
		}
		
		Point2D src(Leveling<?>.GraphElement dstElement) {
			int i = out.indexOf(dstElement);
			int size = out.size();
			
			assert i >= 0;
			
			double x = x0 + width() * (i + 1) / (size + 1);
			
			return new Point2D(x, y0 + height());
		}
		
		PathElement createCurve(Point2D src, Point2D dst, boolean srcIn, boolean dstIn) {
			CubicCurveTo curve = new CubicCurveTo();
			
			curve.setControlX1(src.getX());
			curve.setControlY1(src.getY() + CONTROL_POINT_DISTANCE * (srcIn ? -1 : 1));
			
			curve.setControlX2(dst.getX());
			curve.setControlY2(dst.getY() + CONTROL_POINT_DISTANCE * (dstIn ? -1 : 1));
			
			curve.setX(dst.getX());
			curve.setY(dst.getY());
			
			return curve;
		}

		void drawEdges(Group group) {
			// draw outgoing edges
			for (Leveling<?>.GraphElement next : out) {
				Leveling<?>.GraphElement last = this;
				
				Point2D src = this.src(next);
				
				Path path = new Path();
				
				path.getElements().add(new MoveTo(src.getX(), src.getY()));
				
				if(next instanceof Leveling.Dummy && ((Leveling<?>.Dummy) next).isReverse) {
					// It is a cycle
					
					boolean first = true;

					do {
						last = next;
						
						Leveling<?>.Dummy dummy = (Leveling<?>.Dummy) next;
						
						PathElement curve = createCurve(src, dummy.dst(), !first, false);
						
						LineTo line = new LineTo();
						
						line.setX(dummy.src().getX());
						line.setY(dummy.src().getY());
						
						path.getElements().add(curve);
						path.getElements().add(line);
						
						src = dummy.src();
						next = dummy.succ;
						
						first = false;
					} while(next instanceof Leveling.Dummy); 
					
					Point2D dst = ((Leveling<?>.Vertex) next).dst(last);
					
					PathElement curve = createCurve(src, dst, true, true);

					path.getElements().add(curve);
				} else {
					while(next instanceof Leveling.Dummy) {
						// Long forward edge
						last = next;
						Leveling<?>.Dummy dummy = (Leveling<?>.Dummy) next;
						
						PathElement curve = createCurve(src, dummy.src(), false, true);
						
						LineTo line = new LineTo();
						
						line.setX(dummy.dst().getX());
						line.setY(dummy.dst().getY());
						
						path.getElements().add(curve);
						path.getElements().add(line);
						
						src = dummy.dst();
						next = dummy.succ;
					}
					
					Point2D dst = ((Leveling<?>.Vertex) next).dst(last);
					
					PathElement curve = createCurve(src, dst, false, true);

					path.getElements().add(curve);
				} 
				
				path.setStroke(Color.BLACK.deriveColor(0,  0,  0,  0.25));
				path.setStrokeWidth(4);

				path.setFill(Color.TRANSPARENT);

				group.getChildren().add(path);
				
				/*if (!u.isDummy()) {
					// Add arrow head
					Polygon arrowhead = new Polygon();
					arrowhead.getPoints().addAll(
							new Double[] { xPts[3], yPts[3] - 2, xPts[3] - 3.5,
									yPts[3] - 7, xPts[3], yPts[3] - 6,
									xPts[3] + 3.5, yPts[3] - 7, });

					arrowhead.setStroke(Color.RED);
					arrowhead.setStrokeWidth(2.2);

					group.getChildren().add(arrowhead);
				}*/
			}
		}
		
		void draw(Group g) {
			// Draw node + incoming arrows
			Rectangle rect = new Rectangle();
			rect.setX(x0);
			rect.setWidth(width());
			rect.setY(y0);
			rect.setHeight(height());

			rect.setStroke(Color.GREEN);
			rect.setFill(Color.YELLOW);

			Text text = new Text(n.v.toString());
			text.setWrappingWidth(rect.getWidth());
			text.setTextAlignment(TextAlignment.CENTER);
			text.setX(rect.getX() + 10);
			text.setY(rect.getY() + 20);
			text.setStroke(Color.BROWN);

			g.getChildren().add(rect);
			g.getChildren().add(text);
		}

		@Override
		double width() {
			return Double.valueOf(this.n.v.toString()) * 5. + 10;
		}

		@Override
		double preferredHeight() {
			// TODO Auto-generated method stub
			return 40;
		}
	}

	private class Dummy extends GraphElement {
		
		//GraphElement prev = null;
		GraphElement succ = null;
		boolean isReverse;
		
		Dummy(int levelIndex) {
			this(levelIndex, false);
		}
		
		Dummy(int levelIndex, boolean isReverse) {
			super(levelIndex);
			this.isReverse = isReverse;
		}
		
		Point2D src() {
			return new Point2D(x0, y0);
		}
		
		Point2D dst() {
			return new Point2D(x0, y0 + height());
		}
		
		@Override
		double width() {
			return 0;
		}

		@Override
		double preferredHeight() {
			return 0;
		}
	}
}
