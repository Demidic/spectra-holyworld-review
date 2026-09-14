package ru.spectra.client.cape;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.MathHelper;

/**
 * Direct 1.21.4 adaptation of Spectra 1.16.5 StickSimulation.
 */
public final class CapeSimulation {
    private static final float MAX_BEND = 20.0f;
    private final List<Point> points = new ArrayList<>();
    private final List<Stick> sticks = new ArrayList<>();
    private CapeVector gravityDirection = new CapeVector(0.0f, -1.0f, 0.0f);
    private float gravity = 25.0f;
    private boolean sneaking;

    public boolean init(int partCount) {
        if (points.size() == partCount) {
            return false;
        }
        points.clear();
        sticks.clear();
        for (int i = 0; i < partCount; i++) {
            Point point = new Point();
            point.position.y = -i;
            point.position.x = -i;
            point.locked = i == 0;
            points.add(point);
            if (i > 0) {
                sticks.add(new Stick(points.get(i - 1), point, 1.0f));
            }
        }
        return true;
    }

    public void simulate() {
        applyGravity();
        preventClipping();
        preventSelfClipping();
        applyMotion();
        preventSelfClipping();
        preventHardBends();
        limitLength();
    }

    private void applyGravity() {
        float deltaTime = 0.05f;
        float downX = gravityDirection.x * gravity * deltaTime;
        float downY = gravityDirection.y * gravity * deltaTime;
        float downZ = gravityDirection.z * gravity * deltaTime;
        for (Point point : points) {
            if (point.locked) {
                continue;
            }
            float previousX = point.position.x;
            float previousY = point.position.y;
            float previousZ = point.position.z;
            point.position.x += downX;
            point.position.y += downY;
            point.position.z += downZ;
            point.previousPosition.x = previousX;
            point.previousPosition.y = previousY;
            point.previousPosition.z = previousZ;
        }
    }

    private void applyMotion() {
        for (int iteration = 0; iteration < 30; iteration++) {
            for (int index = sticks.size() - 1; index >= 0; index--) {
                Stick stick = sticks.get(index);
                float centerX = (stick.pointA.position.x + stick.pointB.position.x) * 0.5f;
                float centerY = (stick.pointA.position.y + stick.pointB.position.y) * 0.5f;
                float centerZ = (stick.pointA.position.z + stick.pointB.position.z) * 0.5f;
                float directionX = stick.pointA.position.x - stick.pointB.position.x;
                float directionY = stick.pointA.position.y - stick.pointB.position.y;
                float directionZ = stick.pointA.position.z - stick.pointB.position.z;
                float length = MathHelper.sqrt(directionX * directionX + directionY * directionY
                        + directionZ * directionZ);
                if (length < 1.0E-4f) {
                    continue;
                }
                directionX /= length;
                directionY /= length;
                directionZ /= length;
                float halfLength = stick.length * 0.5f;
                if (!stick.pointA.locked) {
                    stick.pointA.position.x = centerX + directionX * halfLength;
                    stick.pointA.position.y = centerY + directionY * halfLength;
                    stick.pointA.position.z = centerZ + directionZ * halfLength;
                }
                if (!stick.pointB.locked) {
                    stick.pointB.position.x = centerX - directionX * halfLength;
                    stick.pointB.position.y = centerY - directionY * halfLength;
                    stick.pointB.position.z = centerZ - directionZ * halfLength;
                }
            }
        }
    }

    private void limitLength() {
        for (Stick stick : sticks) {
            if (stick.pointB.locked) {
                continue;
            }
            float directionX = stick.pointA.position.x - stick.pointB.position.x;
            float directionY = stick.pointA.position.y - stick.pointB.position.y;
            float directionZ = stick.pointA.position.z - stick.pointB.position.z;
            float length = MathHelper.sqrt(directionX * directionX + directionY * directionY
                    + directionZ * directionZ);
            if (length < 1.0E-4f) {
                continue;
            }
            directionX /= length;
            directionY /= length;
            directionZ /= length;
            stick.pointB.position.x = stick.pointA.position.x - directionX * stick.length;
            stick.pointB.position.y = stick.pointA.position.y - directionY * stick.length;
            stick.pointB.position.z = stick.pointA.position.z - directionZ * stick.length;
        }
    }

    private void preventSelfClipping() {
        boolean clipped;
        int runs = 0;
        do {
            clipped = false;
            for (int first = 0; first < points.size(); first++) {
                for (int second = first + 1; second < points.size(); second++) {
                    Point pointA = points.get(first);
                    Point pointB = points.get(second);
                    float directionX = pointA.position.x - pointB.position.x;
                    float directionY = pointA.position.y - pointB.position.y;
                    float directionZ = pointA.position.z - pointB.position.z;
                    float squaredMagnitude = directionX * directionX + directionY * directionY
                            + directionZ * directionZ;
                    if (squaredMagnitude >= 0.99f) {
                        continue;
                    }
                    clipped = true;
                    runs++;
                    float length = MathHelper.sqrt(squaredMagnitude);
                    if (length < 1.0E-4f) {
                        continue;
                    }
                    directionX /= length;
                    directionY /= length;
                    directionZ /= length;
                    float centerX = (pointA.position.x + pointB.position.x) * 0.5f;
                    float centerY = (pointA.position.y + pointB.position.y) * 0.5f;
                    float centerZ = (pointA.position.z + pointB.position.z) * 0.5f;
                    if (!pointA.locked) {
                        pointA.position.x = centerX + directionX * 0.5f;
                        pointA.position.y = centerY + directionY * 0.5f;
                        pointA.position.z = centerZ + directionZ * 0.5f;
                    }
                    if (!pointB.locked) {
                        pointB.position.x = centerX - directionX * 0.5f;
                        pointB.position.y = centerY - directionY * 0.5f;
                        pointB.position.z = centerZ - directionZ * 0.5f;
                    }
                }
            }
        } while (clipped && runs < 32);
    }

    private void preventHardBends() {
        for (int index = 1; index < points.size() - 2; index++) {
            double angle = getAngle(points.get(index).position, points.get(index - 1).position,
                    points.get(index + 1).position);
            if (angle < -MAX_BEND) {
                points.get(index + 1).position = getReplacement(points.get(index).position,
                        points.get(index - 1).position, -MAX_BEND * 2.0f);
            }
            if (angle > MAX_BEND) {
                points.get(index + 1).position = getReplacement(points.get(index).position,
                        points.get(index - 1).position, MAX_BEND * 2.0f);
            }
        }
    }

    private void preventClipping() {
        Point basePoint = points.getFirst();
        for (int index = 1; index < points.size(); index++) {
            Point point = points.get(index);
            if (point.position.x - basePoint.position.x > 0.0f) {
                point.position.x = basePoint.position.x;
            }
            float progress = index / (float) points.size();
            float maximumZ = progress * progress * 5.0f;
            float z = basePoint.position.z - point.position.z;
            if (z > maximumZ) {
                point.position.z = basePoint.position.z - maximumZ;
            }
            if (z < -maximumZ) {
                point.position.z = basePoint.position.z + maximumZ;
            }
        }
    }

    private CapeVector getReplacement(CapeVector middle, CapeVector previous, double target) {
        return middle.copy().subtract(previous).rotateDegrees((float) target).add(middle);
    }

    private double getAngle(CapeVector a, CapeVector b, CapeVector c) {
        float abx = b.x - a.x;
        float aby = b.y - a.y;
        float cbx = b.x - c.x;
        float cby = b.y - c.y;
        float dot = abx * cbx + aby * cby;
        float cross = abx * cby - aby * cbx;
        return MathHelper.atan2(cross, dot) * 180.0 / Math.PI;
    }

    public void applyMovement(CapeVector movement) {
        Point base = points.getFirst();
        base.previousPosition.set(base.position);
        base.position.add(movement);
    }

    public List<Point> points() {
        return points;
    }

    public boolean empty() {
        return sticks.isEmpty();
    }

    public void setGravityDirection(CapeVector direction) {
        gravityDirection = direction;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

    public static final class Point {
        private CapeVector position = new CapeVector(0.0f, 0.0f, 0.0f);
        private final CapeVector previousPosition = new CapeVector(0.0f, 0.0f, 0.0f);
        private boolean locked;

        public float lerpedX(float delta) {
            return MathHelper.lerp(delta, previousPosition.x, position.x);
        }

        public float lerpedY(float delta) {
            return MathHelper.lerp(delta, previousPosition.y, position.y);
        }

        public float lerpedZ(float delta) {
            return MathHelper.lerp(delta, previousPosition.z, position.z);
        }
    }

    private record Stick(Point pointA, Point pointB, float length) {
    }
}
