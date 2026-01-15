package com.gameengine.core;

import com.gameengine.graphics.Renderer;
import com.gameengine.input.InputManager;
import com.gameengine.recording.RecordingConfig;
import com.gameengine.recording.RecordingService;
import com.gameengine.scene.Scene;

import java.io.IOException;

import javax.swing.Timer;
import com.gameengine.recording.RecordingConfig;
import com.gameengine.recording.RecordingService;
import java.io.IOException;

/**
 * 游戏引擎
 */
public class GameEngine {
    private Renderer renderer;
    private InputManager inputManager;
    private Scene currentScene;
    private boolean running;
    private float targetFPS;
    private float deltaTime;
    private long lastTime;
    private String title;
    private Timer gameTimer;
    //用来计算帧率
    private long startTime = 0;
    private long frameCount = 0;
    private final int screenWidth;
    private final int screenHeight;
    private RecordingService recordingService;
    public long getStartTime() {
        return startTime;
    }
    public long getFrameCount() {
        return frameCount;
    }
    public GameEngine(int width, int height, String title) {
        this.screenWidth = width;  // ADD
        this.screenHeight = height;  // ADD
        this.title = title;
        this.renderer = new Renderer(width, height, title);
        this.inputManager = InputManager.getInstance();
        this.running = false;
        this.targetFPS = 60.0f;
        this.deltaTime = 0.0f;
        this.lastTime = System.nanoTime();
    }
    
    /**
     * 初始化游戏引擎
     */
    public boolean initialize() {
        return true; // Swing渲染器不需要特殊初始化
    }
    
    /**
     * 运行游戏引擎
     */
    public void run() {
        if (!initialize()) {
            System.err.println("游戏引擎初始化失败");
            return;
        }
        
        running = true;
        // 记录开始时间
        startTime = System.nanoTime();
        
        // 初始化当前场景
        if (currentScene != null) {
            currentScene.initialize();
        }
        
        // 创建游戏循环定时器
        gameTimer = new Timer((int) (1000 / targetFPS), e -> {
            if (running) {
                update();
                render();
                //记录帧数
                frameCount++;
            }
        });
        
        gameTimer.start();
    }
    
    /**
     * 更新游戏逻辑
     */
    private void update() {
        // 计算时间间隔
        long currentTime = System.nanoTime();
        deltaTime = (currentTime - lastTime) / 1_000_000_000.0f;
        lastTime = currentTime;
        
        // 先处理事件（填充justPressedKeys）
        renderer.pollEvents();
        
        // 录像更新（捕获本帧输入 + 关键帧）
        if (recordingService != null && recordingService.isRecording()) {
            recordingService.update(deltaTime, currentScene, inputManager);
        }
        
        // 清空瞬态输入（为下一帧准备）
        inputManager.update();
        
        // 更新场景
        if (currentScene != null) {
            currentScene.update(deltaTime);
        }
        
        // 检查退出条件
        if (inputManager.isKeyPressed(27)) {  // ESC
            stop();
        }
        if (renderer.shouldClose()) {
            stop();
        }
    }
    
    /**
     * 渲染游戏
     */
    private void render() {
        renderer.beginFrame();
        
        // 渲染场景
        if (currentScene != null) {
            currentScene.render();
        }
        
        renderer.endFrame();
    }
    
    /**
     * 设置当前场景
     */
    public void setScene(Scene scene) {
        this.currentScene = scene;
        if (scene != null && running) {
            scene.initialize();
        }
    }
    
    /**
     * 获取当前场景
     */
    public Scene getCurrentScene() {
        return currentScene;
    }
    
    /**
     * 停止游戏引擎
     */
    public void stop() {
        if (recordingService != null) {
        recordingService.stop();
        recordingService = null;
        }
        running = false;
        if (gameTimer != null) {
            gameTimer.stop();
        }
        cleanup();
    }
    
    /**
     * 清理资源
     */
    private void cleanup() {
        if (currentScene != null) {
            currentScene.clear();
        }
        renderer.cleanup();
    }
    



    /**
     * 获取渲染器
     */
    public Renderer getRenderer() {
        return renderer;
    }
    
    /**
     * 获取输入管理器
     */
    public InputManager getInputManager() {
        return inputManager;
    }
    
    /**
     * 获取时间间隔
     */
    public float getDeltaTime() {
        return deltaTime;
    }
    
    /**
     * 设置目标帧率
     */
    public void setTargetFPS(float fps) {
        this.targetFPS = fps;
        if (gameTimer != null) {
            gameTimer.setDelay((int) (1000 / fps));
        }
    }
    
    /**
     * 获取目标帧率
     */
    public float getTargetFPS() {
        return targetFPS;
    }
    
    /**
     * 检查引擎是否正在运行
     */
    public boolean isRunning() {
        return running;
    }
    /**
     * 开始录像到指定路径
     */
    public void startRecording(String outputPath) throws IOException {
        if (recordingService != null && recordingService.isRecording()) {
            return;  // Already recording
        }
        RecordingConfig config = new RecordingConfig(outputPath);
        recordingService = new RecordingService(config);
        if (currentScene == null) {
            throw new IllegalStateException("Cannot start recording: no scene set");
        }
        recordingService.start(currentScene, screenWidth, screenHeight);
    }
}
