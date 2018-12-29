package cz.uhk.grid;

import com.jogamp.opengl.GL2GL3;
import oglutils.OGLBuffers;

public class GridFactory {

    public static OGLBuffers create(GL2GL3 gl,int rows, int cols){
        float[] vertexBuffer = new float[2*2*rows*cols];
        int index=0;

        if(rows<=1 || cols<=1) throw new RuntimeException("cols or rows <´1");

        for(int j = 0; j<cols; j++){
            for(int i = 0; i<rows; i++){
                vertexBuffer[index++]=((float)i/(rows-1));
                vertexBuffer[index++]=((float)j/(cols-1));

                //for textures
                vertexBuffer[index++]=((float)i/(rows-1));
                vertexBuffer[index++]=((float)j/(cols-1));
            }
        }

        //IB = (M-1)*(N-1)*2*3
        int[] indexBuffer = new int[(rows-1)*(cols-1)*2*3];
        int index2=0;

        //trinagle strip
        for(int i = 0; i < rows-1; i++){
            for(int j =0; j <= cols; j++){
                //controlling if the row is odd
                if(i%2 != 0){
                    //if last one...
                    if(j == cols){
                        indexBuffer[index2++]=cols-j+(i+1)*cols;
                        indexBuffer[index2++]=cols-j+(i+1)*cols;
                    }else{
                        indexBuffer[index2++]=i*cols+(cols-1)-j;
                        indexBuffer[index2++]=cols-1-j+(i+1)*cols;
                    }
                }else{
                    //if last one...
                    if(j == cols){
                        indexBuffer[index2++]=j-1+(i+1)*cols;
                        indexBuffer[index2++]=j-1+(i+1)*cols;

                    }else{
                        indexBuffer[index2++]=(1+i)*cols+j;
                        indexBuffer[index2++]=i*cols+j;
                    }
                }
            }
        }

        // vertex binding description, concise version
        OGLBuffers.Attrib[] attributes = {
                new OGLBuffers.Attrib("inPosition", 2), // 2 floats (x, y)
                new OGLBuffers.Attrib("inTexture", 2) //x,y to texture
        };
        return new OGLBuffers(gl, vertexBuffer, attributes, indexBuffer);
    }
}


