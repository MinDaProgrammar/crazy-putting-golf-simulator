package core;

public record Vector2D(double x, double y) {

    public Vector2D add(Vector2D other) {
        return new Vector2D(this.x + other.x, this.y + other.y);
    }

    public Vector2D subtract(Vector2D other) {
        return new Vector2D(this.x - other.x, this.y - other.y);
    }

    public Vector2D multiply(double scalar) {
        return new Vector2D(this.x * scalar, this.y * scalar);
    }

    public double dot(Vector2D other) {
        return this.x * other.x + this.y * other.y;
    }

    public double norm() {
        return Math.sqrt(x * x + y * y);
    }

    public Vector2D normalize() {
        double len = norm();
        if (len == 0) return new Vector2D(0, 0);
        return new Vector2D(x / len, y / len);
    }

    public Vector2D rotate(double angleRadians) {
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);
        return new Vector2D(
                x * cos - y * sin,
                x * sin + y * cos
        );
    }

    // project the calling vector onto other
    public Vector2D projectionTo(Vector2D other){
        Vector2D unit = other.normalize();
        return unit.multiply(this.dot(unit));
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
    
}