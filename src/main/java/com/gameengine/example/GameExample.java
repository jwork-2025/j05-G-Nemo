// Modified GameExample.java
package com.gameengine.example;

import com.gameengine.core.GameEngine;
import com.gameengine.graphics.Renderer;
import com.gameengine.scene.MenuScene;

public class GameExample {
    public static void main(String[] args) {
        System.out.println("启动游戏引擎...");

        try {
            GameEngine engine = new GameEngine(800, 600, "游戏引擎");
            Renderer renderer = engine.getRenderer();
            MenuScene menuScene = new MenuScene(engine, renderer);
            engine.setScene(menuScene);
            engine.run();
        } catch (Exception e) {
            System.err.println("游戏运行出错: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("游戏结束");
    }
}
