package cn.fandmc.epoch.render.blockentity;

import cn.fandmc.epoch.asset.BlockModels.*;
import cn.fandmc.epoch.asset.ResourceId;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;

/** Original model-box UV layout shared by the built-in block entity models. */
public final class ModelBoxGeometry {
    private ModelBoxGeometry(){ }
    public static void box(List<Face> out,ResourceId texture,Matrix4f transform,float x,float y,float z,int w,int h,int d,int u,int v,int tw,int th,float expand) {
        float X=x+w+expand,Y=y+h+expand,Z=z+d+expand;x-=expand;y-=expand;z-=expand;
        float[][] points={{x,y,z},{X,y,z},{X,Y,z},{x,Y,z},{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}};
        int[][] order={{5,1,2,6},{0,4,7,3},{5,4,0,1},{2,3,7,6},{1,0,3,2},{4,5,6,7}};
        int[][] uv={{u+d+w,v+d,u+d+w+d,v+d+h},{u,v+d,u+d,v+d+h},{u+d,v,u+d+w,v+d},
                {u+d+w,v+d,u+d+w+w,v},{u+d,v+d,u+d+w,v+d+h},{u+d+w+d,v+d,u+d+w+d+w,v+d+h}};
        for(int face=0;face<6;face++) {
            List<Vertex> vertices=new ArrayList<>();int[] rect=uv[face];
            for(int i=0;i<4;i++) {
                Vector3f p=transform.transformPosition(new Vector3f(points[order[face][i]]));
                vertices.add(new Vertex(p.x,p.y,p.z,(i==0||i==3?rect[2]:rect[0])/(float)tw,(i<2?rect[1]:rect[3])/(float)th));
            }
            var a=vertices.get(0);var b=vertices.get(1);var c=vertices.get(2);
            Vector3f normal=new Vector3f(b.x()-a.x(),b.y()-a.y(),b.z()-a.z()).cross(c.x()-a.x(),c.y()-a.y(),c.z()-a.z()).normalize();
            float shade=.6f+.4f*Math.max(0,normal.y)-.1f*Math.max(0,-normal.y)+.2f*Math.abs(normal.z);
            out.add(new Face(List.copyOf(vertices),texture,null,"up",false,shade));
        }
    }
}
