package viewer.rendering;

import org.joml.Vector2f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import viewer.rendering.models.Model;
import viewer.rendering.shaders.*;

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
                float vertices[] = {0.0f, 0.5f, 0.5f, -0.5f, -0.5f, -0.5f};
                int   indecies[] = {2, 1, 0};
                
                model = new Model(vertices, indecies);
                
                Shader vert = new Shader("shaders/basic.vert");
                Shader frag = new Shader("shaders/basic.frag");
                
                prog = new Program();
                prog.attach(vert);
                prog.attach(frag);
                prog.bindVertLocation("position", 0);
                prog.bindFragLocation("color", 0);
                prog.link();
            }
            
            @Override
            public void update()
            {
            
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
                
                glDrawElements(GL_TRIANGLES, 3, GL_UNSIGNED_INT, 0);
                
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
