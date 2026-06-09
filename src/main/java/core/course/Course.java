package core.course;

import java.util.ArrayList;
import java.util.List;

public class Course {
    private String heightProfile;
    private double frictionKinetic;
    private double frictionStatic;
    private double startX, startY;
    private double targetX, targetY;
    private double targetRadius;
    private double vMax = 5.0; // According to the manual, the maximum speeed is 5
    private List<Tree> trees = new ArrayList<>();
    private List<Sandpit> sandpits = new ArrayList<>();
    public Course() {}

    // Added tree and sandpit variables for obstacles
    public static class Tree {
        public double x, y, radius;
        public Tree(double x, double y, double radius) {
            this.x = x; this.y = y; this.radius = radius;
        }
    }

    public static class Sandpit {
        public final double x;
        public final double y;
        public final double radius;
        public final double kineticFriction;
        public final double staticFriction;

        public Sandpit(double x, double y, double radius, double kineticFriction, double staticFriction) {

            if (radius <= 0.0) { throw new IllegalArgumentException("Sandpit radius must be positive."); }
            if (kineticFriction < 0.0) { throw new IllegalArgumentException("Sandpit kinetic friction must be non-negative."); }
            if (staticFriction < kineticFriction) { throw new IllegalArgumentException("Sandpit static friction must be >= kinetic friction."); }
                
            this.x = x; 
            this.y = y; 
            this.radius = radius;
            this.kineticFriction = kineticFriction;
            this.staticFriction = staticFriction;
        }
    }

    //getters 
    public String getHeightProfile() { return heightProfile; }
    public double getFrictionKinetic() { return frictionKinetic; }
    public double getFrictionStatic() { return frictionStatic; }
    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getTargetX() { return targetX; }
    public double getTargetY() { return targetY; }
    public double getTargetRadius() { return targetRadius; }
    public double getVMax() { return vMax; }

    //setters
    public void setHeightProfile(String heightProfile) { this.heightProfile = heightProfile; }
    public void setFrictionKinetic(double frictionKinetic) { this.frictionKinetic = frictionKinetic; }
    public void setFrictionStatic(double frictionStatic) { this.frictionStatic = frictionStatic; }
    public void setStartX(double startX) { this.startX = startX; }
    public void setStartY(double startY) { this.startY = startY; }
    public void setTargetX(double targetX) { this.targetX = targetX; }
    public void setTargetY(double targetY) { this.targetY = targetY; }
    public void setTargetRadius(double targetRadius) { this.targetRadius = targetRadius; }
    public void setVMax(double vMax) { this.vMax = vMax; }


    // Add getters for the lists
    public List<Tree> getTrees() { return trees; }
    public List<Sandpit> getSandpits() { return sandpits; }

    // Methods to add single obstacle
    public void addTree(Tree tree) { this.trees.add(tree); }
    public void addSandpit(Sandpit sandpit) { this.sandpits.add(sandpit); }
}