import java.util.*;

public class ArticulationPoint {
    ArticulationPoint parent;
    Node node;
    int count;
    int reachBack;
    List<Node> children;

    public ArticulationPoint(Node node, int count, ArticulationPoint parent) {
        this.node = node;
        this.count = count;
        this.parent = parent;
        this.reachBack = Integer.MAX_VALUE;
        this.children = null;
    }
}
