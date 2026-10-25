package cn.fandmc.epoch.render;

import cn.fandmc.epoch.world.LightView;

/** Per-build scratch space for smooth light and ambient occlusion. No shared worker state. */
public final class FaceLighting {
    private static final int[] DX = {0,0,0,0,-1,1}, DY = {-1,1,0,0,0,0}, DZ = {0,0,-1,1,0,0};
    private static final int[][] EDGES = {{4,5,2,3},{5,4,2,3},{1,0,5,4},{4,5,0,1},{1,0,2,3},{0,1,2,3}};
    private static final int[] FIRST = {0,0,1,1}, SECOND = {3,2,2,3};

    /** Precomputed spatial weights survive blockstate rotations and arbitrary vertex order. */
    public record Geometry(int direction, boolean boundary, float[] weights) { }

    public static Geometry geometry(int direction, float[] vertices, boolean fullCube) {
        int axis = direction < 2 ? 1 : direction < 4 ? 2 : 0;
        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
        for (int v=0; v<4; v++) { min=Math.min(min,vertices[v*5+axis]); max=Math.max(max,vertices[v*5+axis]); }
        boolean boundary = min == max && (fullCube || ((direction&1)==0 ? min<.0001f : max>.9999f));
        int a=EDGES[direction][0], b=EDGES[direction][2];
        float[] weights=new float[16];
        float minA=Float.POSITIVE_INFINITY,maxA=Float.NEGATIVE_INFINITY,minB=Float.POSITIVE_INFINITY,maxB=Float.NEGATIVE_INFINITY;
        for(int v=0;v<4;v++) {
            float u=coordinate(vertices,v*5,a),w=coordinate(vertices,v*5,b);
            minA=Math.min(minA,u);maxA=Math.max(maxA,u);minB=Math.min(minB,w);maxB=Math.max(maxB,w);
        }
        for(int v=0;v<4;v++) {
            float u=coordinate(vertices,v*5,a),w=coordinate(vertices,v*5,b);
            // Horizontal faces retain corner samples, including slab tops. Side faces
            // interpolate within the cell for stairs, slabs and other partial models.
            if(direction<2){u=u>(minA+maxA)*.5f?1:0;w=w>(minB+maxB)*.5f?1:0;}
            weights[v*4]=u*(1-w);weights[v*4+1]=u*w;
            weights[v*4+2]=(1-u)*w;weights[v*4+3]=(1-u)*(1-w);
        }
        return new Geometry(direction,boundary,weights);
    }

    private static float coordinate(float[] vertices,int offset,int direction) {
        float value=vertices[offset+(direction<2?1:direction<4?2:0)];
        return (direction&1)==0?1-value:value;
    }

    private final LightView view;
    private final LightSampler sampler;
    private final int[] edgeLight=new int[4],cornerLight=new int[4],vertexLight=new int[4];
    private final float[] edgeAo=new float[4],cornerAo=new float[4],vertexAo=new float[4];
    private final boolean[] open=new boolean[4];

    public FaceLighting(LightView view) { this.view=view; sampler=new LightSampler(view,true); }
    public int light(int vertex) { return vertexLight[vertex]; }
    public float occlusion(int vertex) { return vertexAo[vertex]; }

    public void calculate(int x,int y,int z,Geometry geometry,boolean smooth) {
        int d=geometry.direction(),nx=DX[d],ny=DY[d],nz=DZ[d];
        int ox=x+(geometry.boundary()?nx:0),oy=y+(geometry.boundary()?ny:0),oz=z+(geometry.boundary()?nz:0);
        if(!smooth) {
            int light=sampler.packed(ox,oy,oz);
            for(int v=0;v<4;v++){vertexLight[v]=light;vertexAo[v]=1;}
            return;
        }
        int[] edges=EDGES[d];
        for(int e=0;e<4;e++) {
            int t=edges[e],ex=ox+DX[t],ey=oy+DY[t],ez=oz+DZ[t];
            edgeLight[e]=sampler.packed(ex,ey,ez);
            edgeAo[e]=view.lightDefinition(ex,ey,ez).lighting().ambientOcclusion();
            open[e]=view.lightDefinition(ex+nx,ey+ny,ez+nz).lighting().translucent();
        }
        int base=sampler.packed(x,y,z);
        if(geometry.boundary()||!view.lightDefinition(x+nx,y+ny,z+nz).lighting().opaqueCube())
            base=sampler.packed(x+nx,y+ny,z+nz);
        float baseAo=view.lightDefinition(ox,oy,oz).lighting().ambientOcclusion();
        for(int c=0;c<4;c++) {
            int a=FIRST[c],b=SECOND[c],diagonal=edgeLight[a];float ao=edgeAo[a];
            if(open[a]||open[b]) {
                int ex=ox+DX[edges[a]]+DX[edges[b]],ey=oy+DY[edges[a]]+DY[edges[b]],ez=oz+DZ[edges[a]]+DZ[edges[b]];
                diagonal=sampler.packed(ex,ey,ez);ao=view.lightDefinition(ex,ey,ez).lighting().ambientOcclusion();
            }
            cornerLight[c]=average(edgeLight[b],edgeLight[a],diagonal,base);
            cornerAo[c]=(edgeAo[b]+edgeAo[a]+ao+baseAo)*.25f;
        }
        for(int v=0;v<4;v++) {
            float ao=0,block=0,sky=0;
            for(int c=0;c<4;c++) {
                float w=geometry.weights()[v*4+c];
                ao+=cornerAo[c]*w;block+=(cornerLight[c]&255)*w;sky+=((cornerLight[c]>>>16)&255)*w;
            }
            vertexAo[v]=ao;vertexLight[v]=((int)sky<<16)|((int)block);
        }
    }

    private static int average(int a,int b,int c,int base) {
        if(a==0)a=base;if(b==0)b=base;if(c==0)c=base;
        return ((a+b+c+base)>>>2)&0x00ff00ff;
    }
}
