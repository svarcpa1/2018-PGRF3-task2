package cz.uhk.model;

import com.jogamp.opengl.*;
import cz.uhk.grid.GridFactory;
import oglutils.*;
import transforms.*;
import java.awt.event.*;

/**
 * Requires JOGL 2.3.0 or newer
 *
 * @author  PGRF FIM UHK, Pavel ŠVARC
 * @version 1.0
 * @about   implementation of normal and prallax mapping
 * @DATE    2018-11-18
 */
public class Renderer implements GLEventListener, MouseListener,
        MouseMotionListener, KeyListener {

    private int width, height, ox, oy;
    private boolean modeOfRendering = true, modeOfProjection = true, sunMoving= true;
    private int locTime, locViewMat, locProjMat, locModeOfFunction, locModeOfLight,
            locMVPMatLight, locSpotCutOff, locModeOfLightSource, locModeOfMapping;
    private Vec3D positionLight, directionLight, upLight;
    private int  modeOfLight = 0, modeOfLightSource=0, modeOfMapping=0, textType=0;
    private float time = 0.5f;
    private float tmp = 1f;

    private int shaderProgram, shaderProgramLight;

    private Mat4 viewMat, projMat, viewMatLight, projMatLight, MVPMatLight;
    private Camera camera;
    private OGLBuffers buffers;
    private OGLTexture2D texture2DBase, texture2DDisp, texture2DNrm, texture2DBase1, texture2DDisp1, texture2DNrm1;
    private OGLRenderTarget renderTarget;
    private OGLTexture2D.Viewer textureViewer;

    @Override
    public void init(GLAutoDrawable glDrawable) {

        // check whether shaders are supported
        GL2GL3 gl = glDrawable.getGL().getGL2GL3();
        OGLUtils.shaderCheck(gl);

        OGLUtils.printOGLparameters(gl);

        shaderProgram = ShaderUtils.loadProgram(gl, "/start.vert",
                "/start.frag",
                null,null,null,null);
        shaderProgramLight = ShaderUtils.loadProgram(gl, "/light.vert",
                "/light.frag",
                null,null,null,null);

        buffers= GridFactory.create(gl,50,50);

        Vec3D position = new Vec3D(5, 5, 5);
        Vec3D direction = new Vec3D(0, 0, 0);
        Vec3D up = new Vec3D(1, 0, 0);

        positionLight = new Vec3D(5, 0, 8);
        directionLight = new Vec3D(0, 2, 0).sub(positionLight);
        upLight = new Vec3D(0, 0, 1);
        projMatLight = new Mat4OrthoRH(20,20,-20, 200.0);

        viewMat = new Mat4ViewRH(position, direction, up);
        viewMatLight = new Mat4ViewRH(positionLight, directionLight, upLight);
        MVPMatLight = viewMatLight.mul(projMatLight);

        camera = new Camera().withPosition(position)
                .withZenith(-Math.PI/5.)
                .withAzimuth(Math.PI*(5/4.));

        texture2DBase = new OGLTexture2D(gl, "/textures/wall.jpg");
        texture2DDisp = new OGLTexture2D(gl, "/textures/wall_h.png");
        texture2DNrm = new OGLTexture2D(gl, "/textures/wall_n.png");

        texture2DBase1 = new OGLTexture2D(gl, "/textures/base1_COLOR.png");
        texture2DDisp1 = new OGLTexture2D(gl, "/textures/base1_DISP.png");
        texture2DNrm1 = new OGLTexture2D(gl, "/textures/base1_NRM.png");

        textureViewer = new OGLTexture2D.Viewer(gl);

        renderTarget = new OGLRenderTarget(gl, 1024, 1024);

        gl.glEnable(GL.GL_DEPTH_TEST);
        //culling back faces
        gl.glCullFace(GL.GL_BACK);
        gl.glEnable(GL.GL_CULL_FACE);
    }

    @Override
    public void display(GLAutoDrawable glDrawable) {
        GL2GL3 gl = glDrawable.getGL().getGL2GL3();

        //choosing persp/ortho
        if(modeOfProjection){
            projMat = new Mat4PerspRH(Math.PI / 4, height / (double) width, 1, 100.0);
        }else {
            projMat = new Mat4OrthoRH(10, 10, 1, 100);
        }

        //choosing line/fill
        if(modeOfRendering){
            gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
        }else {
            gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
        }

        //call render methods
        renderFromLight(gl, shaderProgramLight);
        renderFromViewer(gl, shaderProgram);

        //sides windows
        gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
        textureViewer.view(renderTarget.getDepthTexture(), -1, -1, 0.5);
    }

    private void renderFromLight(GL2GL3 gl, int shaderProgramLight){
        gl.glUseProgram(shaderProgramLight);
        renderTarget.bind();

        gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

        setupSameVars(gl, shaderProgramLight);
        locMVPMatLight = gl.glGetUniformLocation(shaderProgramLight,"MVPMatLight");

        timeHandle();

        //for moving shadows with light
        positionLight = new Vec3D(5, 0+time/3, 8);
        directionLight = new Vec3D(0, 2, 0).sub(positionLight);
        upLight = new Vec3D(0, 0, 1);
        projMatLight = new Mat4OrthoRH(10,10,-20, 200.0);
        viewMatLight = new Mat4ViewRH(positionLight, directionLight, upLight);
        MVPMatLight = viewMatLight.mul(projMatLight);

        gl.glUniform1f(locTime, time/10); // correct shader must be set before this
        gl.glUniformMatrix4fv(locMVPMatLight, 1, false, MVPMatLight.floatArray(), 0);

/*        //function trampoline
        gl.glUniform1i(locModeOfFunction,0);
        buffers.draw(GL2GL3.GL_TRIANGLE_STRIP, shaderProgramLight);*/

        //still sphere
        gl.glUniform1i(locModeOfFunction,10);
        buffers.draw(GL2GL3.GL_TRIANGLE_STRIP, shaderProgramLight);

/*        //still plain
        gl.glUniform1i(locModeOfFunction,11);
        buffers.draw(GL2GL3.GL_TRIANGLE_STRIP, shaderProgramLight);*/
    }

    private void renderFromViewer(GL2GL3 gl, int shaderProgram){
        gl.glUseProgram(shaderProgram);
        gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, 0);
        gl.glViewport(0,0,width,height);

        gl.glClearColor(0.2f, 0.2f, 0.3f, 1.0f);
        gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);

        setupSameVars(gl,shaderProgram);
        locModeOfLight = gl.glGetUniformLocation(shaderProgram, "modeOfLight");
        locMVPMatLight = gl.glGetUniformLocation(shaderProgram,"MVPMatLight");
        locSpotCutOff = gl.glGetUniformLocation(shaderProgram,"spotOff");
        locModeOfLightSource = gl.glGetUniformLocation(shaderProgram,"modeOfLightSource");

        timeHandle();

        gl.glUniform1f(locTime, time/10); // correct shader must be set before this
        gl.glUniformMatrix4fv(locViewMat, 1, false, camera.getViewMatrix().floatArray(), 0);
        gl.glUniformMatrix4fv(locProjMat, 1, false, projMat.floatArray(), 0);

        gl.glUniformMatrix4fv(locMVPMatLight,1,false,MVPMatLight.floatArray(),0);


        //mappingMode
        gl.glUniform1i(locModeOfMapping,modeOfMapping);

        textureSetup(modeOfMapping);

        //reflector
        gl.glUniform1f(locSpotCutOff,10.0f);

        //lightmode
        gl.glUniform1i(locModeOfLight,modeOfLight);
        gl.glUniform1i(locModeOfLightSource,modeOfLightSource);

        //function trampoline
/*        gl.glUniform1i(locModeOfFunction, 0);
        buffers.draw(GL2GL3.GL_TRIANGLE_STRIP, shaderProgram);*/

        //still sphere
        gl.glUniform1i(locModeOfFunction,10);
        buffers.draw(GL2GL3.GL_TRIANGLE_STRIP, shaderProgram);

/*        //still plain
        gl.glUniform1i(locModeOfFunction,11);
        buffers.draw(GL2GL3.GL_TRIANGLE_STRIP, shaderProgram);*/

/*        //sun
        gl.glUniform1i(locModeOfFunction,12);
        buffers.draw(GL2GL3.GL_TRIANGLE_STRIP, shaderProgram);*/
    }

    private void timeHandle(){
        if(sunMoving) time = time + tmp;
        if(time >= 100.0f) tmp = -1f;
        if(time <= 0.0f) tmp = 1f;
    }

    private void setupSameVars(GL2GL3 gl, int shader){
        locViewMat = gl.glGetUniformLocation(shader, "viewMat");
        locProjMat = gl.glGetUniformLocation(shader, "projMat");
        locTime = gl.glGetUniformLocation(shader, "time");
        locModeOfFunction = gl.glGetUniformLocation(shader, "modeOfFunction");
        locModeOfMapping = gl.glGetUniformLocation(shader, "modeOfMapping");
    }

    private void textureSetup(int mode){
        //textures binding
        //normal
        if(mode==0){
            if(textType==0) {
                texture2DBase.bind(shaderProgram, "textureSampler", 0);
                texture2DNrm.bind(shaderProgram, "textureSamplerNrm", 1);
            }else{
                texture2DBase1.bind(shaderProgram, "textureSampler", 0);
                texture2DNrm1.bind(shaderProgram, "textureSamplerNrm", 1);
            }
            renderTarget.getDepthTexture().bind(shaderProgram, "textureSamplerDepth", 2);
        }else {
            //parallax
            if(textType==0){
                texture2DBase.bind(shaderProgram, "textureSampler", 0);
                texture2DNrm.bind(shaderProgram, "textureSamplerNrm", 1);
                texture2DDisp.bind(shaderProgram,"textureSamplerDisp",2);
            }else{
                texture2DBase1.bind(shaderProgram, "textureSampler", 0);
                texture2DNrm1.bind(shaderProgram, "textureSamplerNrm", 1);
                texture2DDisp1.bind(shaderProgram,"textureSamplerDisp",2);
            }
            renderTarget.getDepthTexture().bind(shaderProgram, "textureSamplerDepth", 3);
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        this.width = width;
        this.height = height;

        if(modeOfProjection){
            projMat = new Mat4PerspRH(Math.PI / 4, height / (double) width, 1, 100.0);
        }else {
            projMat = new Mat4OrthoRH(Math.PI / 4, height / (double) width, 1, 100.0);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }
    @Override
    public void mouseEntered(MouseEvent e) {
    }
    @Override
    public void mouseExited(MouseEvent e) {
    }
    @Override
    public void mousePressed(MouseEvent e) {
        ox = e.getX();
        oy = e.getY();
    }
    @Override
    public void mouseReleased(MouseEvent e) {

    }
    @Override
    public void mouseDragged(MouseEvent e) {
        camera = camera.addAzimuth(Math.PI * (ox - e.getX()) / width).addZenith(Math.PI * (e.getY() - oy) / width);
        ox = e.getX();
        oy = e.getY();
    }
    @Override
    public void mouseMoved(MouseEvent e) {
    }
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A:
                camera = camera.left(0.1);
                break;
            case KeyEvent.VK_S:
                camera = camera.backward(0.1);
                break;
            case KeyEvent.VK_D:
                camera = camera.right(0.1);
                break;
            case KeyEvent.VK_W:
                camera = camera.forward(0.1);
                break;
            case KeyEvent.VK_R:
                camera = camera.up(0.1);
                break;
            case KeyEvent.VK_F:
                camera = camera.down(0.1);
                break;
            //M for changing mode (fill, line)
            case KeyEvent.VK_M:
                modeOfRendering = !modeOfRendering;
                break;
            //N for changing mapping mode
            case KeyEvent.VK_N:
                modeOfMapping=(modeOfMapping+1)%2;
                System.out.println(modeOfMapping);
                break;
            //P for changing projection (persp, orto)
            case KeyEvent.VK_P:
                modeOfProjection = !modeOfProjection;
                break;
            //B for changing light (0 per vertex, i per pixel)
            case KeyEvent.VK_B:
                modeOfLight=(modeOfLight+1)%2;
                break;
            //C for changing light mode (reflector or not in per pixel only)
            case KeyEvent.VK_C:
                modeOfLightSource=(modeOfLightSource+1)%2;
                break;
            //X for changing textures
            case KeyEvent.VK_X:
                textType=(textType+1)%2;
                break;
            //L for moving the sun
            case KeyEvent.VK_L:
                sunMoving=!sunMoving;
                break;
        }
    }
    @Override
    public void keyReleased(KeyEvent e) {
    }
    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void dispose(GLAutoDrawable glDrawable) {
        GL2GL3 gl = glDrawable.getGL().getGL2GL3();
        gl.glDeleteProgram(shaderProgram);
    }
}