package cn.fandmc.epoch.physics;

/** Immutable axis-aligned collision box in block or world coordinates. */
public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    public Box {
        if (!Double.isFinite(minX + minY + minZ + maxX + maxY + maxZ)
                || minX > maxX || minY > maxY || minZ > maxZ) throw new IllegalArgumentException("Invalid collision box");
    }

    public Box move(double x, double y, double z) {
        return new Box(minX+x,minY+y,minZ+z,maxX+x,maxY+y,maxZ+z);
    }

    public Box sweep(double x, double y, double z) {
        return new Box(minX+Math.min(0,x),minY+Math.min(0,y),minZ+Math.min(0,z),
                maxX+Math.max(0,x),maxY+Math.max(0,y),maxZ+Math.max(0,z));
    }

    public boolean intersects(Box other) {
        return maxX>other.minX && minX<other.maxX && maxY>other.minY && minY<other.maxY && maxZ>other.minZ && minZ<other.maxZ;
    }

    public double clipX(Box moving, double delta) {
        if (moving.maxY<=minY || moving.minY>=maxY || moving.maxZ<=minZ || moving.minZ>=maxZ) return delta;
        if (delta>0 && moving.maxX<=minX) return Math.min(delta,minX-moving.maxX);
        if (delta<0 && moving.minX>=maxX) return Math.max(delta,maxX-moving.minX);
        return delta;
    }
    public double clipY(Box moving, double delta) {
        if (moving.maxX<=minX || moving.minX>=maxX || moving.maxZ<=minZ || moving.minZ>=maxZ) return delta;
        if (delta>0 && moving.maxY<=minY) return Math.min(delta,minY-moving.maxY);
        if (delta<0 && moving.minY>=maxY) return Math.max(delta,maxY-moving.minY);
        return delta;
    }
    public double clipZ(Box moving, double delta) {
        if (moving.maxX<=minX || moving.minX>=maxX || moving.maxY<=minY || moving.minY>=maxY) return delta;
        if (delta>0 && moving.maxZ<=minZ) return Math.min(delta,minZ-moving.maxZ);
        if (delta<0 && moving.minZ>=maxZ) return Math.max(delta,maxZ-moving.minZ);
        return delta;
    }
}
