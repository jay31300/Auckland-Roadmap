public class Tuple implements Comparable <Tuple>{

    Node nodeCurrent;
    Node nodeParent;
    double costToHere;
    double totalEstCost;

    public Tuple(Node nodeCurrent, Node nodeParent, double costToHere, double totalEstCost){
        this.nodeCurrent = nodeCurrent;
        this.nodeParent = nodeParent;
        this.costToHere = costToHere;
        this.totalEstCost = totalEstCost;
    }


    @Override
    public int compareTo(Tuple tooPoo) { //everytime you make Tuple node this will compare them

        if(this.totalEstCost < tooPoo.totalEstCost){
            return -1;
        }
        else if(this.totalEstCost > tooPoo.totalEstCost){
            return 1;
        }
        else {
            return 0;
        }
    }
}
