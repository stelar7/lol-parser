package viewer.util;

import org.joml.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.IntBuffer;
import java.util.concurrent.locks.ReentrantLock;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public abstract class Renderer
{
    public volatile int width;
    public volatile int height;
    
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
            
            }
            
            @Override
            public void update()
            {
            
            }
            
            float last = 0;
            
            @Override
            public void render()
            {
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                
                Vector2f center = new Vector2f(width / 2, height / 2);
                int      size   = 50;
                
                // create frame to avoid overriding state
                glPushMatrix();
                
                // center object
                glTranslatef(width / 2f, height / 2f, 0);
                glRotatef(last = (last + 1f) % 360, 0, 0, 1);
                glTranslatef(-width / 2f, -height / 2f, 0);
                
                // make it red
                glColor3f(1, 0, 0);
                
                // draw it
                glBegin(GL_TRIANGLES);
                glVertex2f(center.x + size, center.y + size);
                glVertex2f(center.x, center.y - size);
                glVertex2f(center.x - size, center.y + size);
                glEnd();
                
                glPopMatrix();
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
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        
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
        
        glOrtho(0, width, 0, height, 1, -1);
        glViewport(0, 0, width, height);
        glEnable(GL_DEPTH_TEST);
        
        while (!shouldClose)
        {
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
