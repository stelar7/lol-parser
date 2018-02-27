package no.stelar7.cdragon.viewer.rendering;

import no.stelar7.cdragon.viewer.rendering.models.Model;
import no.stelar7.cdragon.viewer.rendering.shaders.*;
import org.joml.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.lang.Math;
import java.nio.IntBuffer;
import java.util.concurrent.locks.ReentrantLock;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.system.MemoryUtil.*;

public abstract class Renderer
{
    public volatile int width;
    public volatile int height;
    
    private volatile boolean needsRefresh = true;
    
    private long window;
    
    private final Vector2f      cursor = new Vector2f();
    private final ReentrantLock lock   = new ReentrantLock();
    
    private volatile boolean shouldClose;
    
    public static void main(String[] args)
    {
        new Renderer(600, 600)
        {
            
            @Override
            public void initPostGL()
            {
                float vertices[] = {0.0f, 0.5f, 0f, 0.5f, -0.5f, 0f, -0.5f, -0.5f, 0f};
                int   indecies[] = {0, 1, 2};
                
                float[] v2 = {
                        1, 1, 1, -1, 1, 1, -1, -1, 1, 1, -1, 1,
                        1, 1, 1, 1, -1, 1, 1, -1, -1, 1, 1, -1,
                        1, 1, 1, 1, 1, -1, -1, 1, -1, -1, 1, 1,
                        -1, 1, 1, -1, 1, -1, -1, -1, -1, -1, -1, 1,
                        -1, -1, -1, 1, -1, -1, 1, -1, 1, -1, -1, 1,
                        1, -1, -1, -1, -1, -1, -1, 1, -1, 1, 1, -1
                };
                
                int i2[] = {
                        0, 1, 2, 2, 3, 0,
                        4, 5, 6, 6, 7, 4,
                        8, 9, 10, 10, 11, 8,
                        12, 13, 14, 14, 15, 12,
                        16, 17, 18, 18, 19, 16,
                        20, 21, 22, 22, 23, 20
                };

//                model = new Model(vertices, indecies);
                model = new Model(v2, i2);
                
                Shader vert = new Shader("shaders/basic.vert");
                Shader frag = new Shader("shaders/basic.frag");
                
                prog = new Program();
                prog.attach(vert);
                prog.attach(frag);
                prog.bindVertLocation("position", 0);
                prog.bindFragLocation("color", 0);
                prog.link();
                
                pushMVP();
                
            }
            
            float x;
            float z;
            float time = 0;
            
            private void pushMVP()
            {
                float perspective = (float) width / (float) height;
                float fov         = (float) Math.toRadians(45);
                
                Matrix4f projection = new Matrix4f().setPerspective(fov, perspective, 0.1f, 10000f);
                
                x = (float) Math.sin(time) * 10;
                z = (float) Math.cos(time) * 10;
                
                Matrix4f view = new Matrix4f().setLookAt(
                        new Vector3f(x, 0.5f, z),
                        new Vector3f(0, 0, 0),
                        new Vector3f(0, 1f, 0));
                
                
                Matrix4f model = new Matrix4f().scaling(2);
                
                Matrix4f mvp = projection.mul(view, new Matrix4f()).mul(model, new Matrix4f());
                
                prog.bind();
                prog.setMatrix4f("mvp", mvp);
            }
            
            @Override
            public void update()
            {
                time += .01f;
                
                pushMVP();
            }
            
            float last = 0;
            
            Model model;
            Program prog;
            
            @Override
            public void render()
            {
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                
                prog.bind();
                model.bind();
                
                glDrawElements(GL_TRIANGLES, model.getMesh().getIndexCount(), GL_UNSIGNED_INT, 0);
                
                model.unbind();
                prog.unbind();
            }
        }.start();
    }
    
    public Renderer(int width, int height)
    {
        this.width = width;
        this.height = height;
    }
    
    public void start()
    {
        try
        {
            init();
            
            new Thread(this::loop).start();
            
            while (!shouldClose)
            {
                glfwWaitEvents();
            }
            
            lock.lock();
            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
            lock.unlock();
        } finally
        {
            glfwTerminate();
            glfwSetErrorCallback(null).free();
        }
    }
    
    private void init()
    {
        GLFWErrorCallback.createPrint(System.err).set();
        
        if (!glfwInit())
        {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_SAMPLES, 8);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE);
        
        
        window = glfwCreateWindow(width, height, "Basic Renderer!", NULL, NULL);
        if (window == NULL)
        {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        glfwSetCursorPosCallback(window, (windowPtr, x, y) -> cursor.set((float) x, (float) y));
        glfwSetFramebufferSizeCallback(window, (windowPtr, w, h) ->
                                       {
                                           if (w > 0 && h > 0)
                                           {
                                               width = w;
                                               height = h;
                                               needsRefresh = true;
                                           }
                                       }
                                      );
        
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            IntBuffer pWidth  = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            
            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            
            int x = (vidmode.width() - pWidth.get(0)) / 2;
            int y = (vidmode.height() - pHeight.get(0)) / 2;
            
            glfwSetWindowPos(window, x, y);
        }
    }
    
    private void loop()
    {
        glfwMakeContextCurrent(window);
        glfwShowWindow(window);
        GL.createCapabilities();
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        
        initPostGL();
        
        int   updatesPerSecond = 60;
        int   maxFramesSkipped = 20;
        float skipInterval     = 1000f / updatesPerSecond;
        
        int ups = 0;
        int fps = 0;
        
        int loops;
        
        double timer    = System.currentTimeMillis();
        long   fpstimer = System.currentTimeMillis();
        
        glEnable(GL_BLEND);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_MULTISAMPLE);
        
        glCullFace(GL_BACK);
        glDepthFunc(GL_LEQUAL);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        while (!shouldClose)
        {
            if (needsRefresh)
            {
                needsRefresh = false;
                glViewport(0, 0, width, height);
            }
            
            
            if (System.currentTimeMillis() > fpstimer + 1000)
            {
                System.out.format("fps: %d  ups: %d%n", fps, ups);
                fpstimer = System.currentTimeMillis();
                fps = ups = 0;
            }
            
            loops = 0;
            while (System.currentTimeMillis() > timer && loops < maxFramesSkipped)
            {
                update();
                loops++;
                ups++;
                timer += skipInterval;
            }
            
            render();
            fps++;
            
            lock.lock();
            shouldClose = glfwWindowShouldClose(window);
            if (!shouldClose)
            {
                glfwSwapBuffers(window);
            }
            lock.unlock();
        }
    }
    
    protected abstract void initPostGL();
    
    protected abstract void update();
    
    protected abstract void render();
    
    
}
