package cn.fandmc.epoch.render;

/** Reorders quad indices without rebuilding or uploading their vertex data. */
final class TranslucentOrder {
    private final float[] centers;
    private final double[] distances;
    private final int[] order, scratch, indices;
    private double lastX=Double.NaN,lastY,lastZ;

    TranslucentOrder(float[] vertices) {
        int faces=vertices.length/60;
        centers=new float[faces*3];distances=new double[faces];
        order=new int[faces];scratch=new int[faces];indices=new int[faces*6];
        for(int face=0;face<faces;face++){
            order[face]=face;
            // Triangles use 0,1,2,0,2,3; average the four distinct corners.
            for(int vertex:new int[]{0,1,2,5})for(int axis=0;axis<3;axis++)
                centers[face*3+axis]+=vertices[face*60+vertex*10+axis]*.25f;
        }
    }

    int[] update(double x,double y,double z) {
        double dx=x-lastX,dy=y-lastY,dz=z-lastZ;
        if(Double.isFinite(lastX)&&dx*dx+dy*dy+dz*dz<.0625)return null;
        lastX=x;lastY=y;lastZ=z;
        for(int face=0;face<order.length;face++){
            dx=centers[face*3]-x;dy=centers[face*3+1]-y;dz=centers[face*3+2]-z;
            distances[face]=dx*dx+dy*dy+dz*dz;
        }
        sort(0,order.length);
        for(int face=0;face<order.length;face++)for(int vertex=0;vertex<6;vertex++)
            indices[face*6+vertex]=order[face]*6+vertex;
        return indices;
    }

    private void sort(int from,int to){
        if(to-from<2)return;
        int middle=(from+to)>>>1;sort(from,middle);sort(middle,to);
        int left=from,right=middle;
        for(int index=from;index<to;index++)
            scratch[index]=right>=to||left<middle&&distances[order[left]]>=distances[order[right]]?order[left++]:order[right++];
        System.arraycopy(scratch,from,order,from,to-from);
    }
}
