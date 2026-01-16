// New MenuScene.java
package com.gameengine.scene;

import com.gameengine.core.GameEngine;
import com.gameengine.graphics.Renderer;
import com.gameengine.math.Vector2;
import com.gameengine.ui.Button;
import com.gameengine.ui.ButtonClickHandler;

public class MenuScene extends Scene {
    private GameEngine engine;
    private Renderer renderer;

    public MenuScene(GameEngine engine, Renderer renderer) {
        super("Menu");
        this.engine = engine;
        this.renderer = renderer;
    }

    @Override
    public void initialize() {
        super.initialize();

        Button newGameButton = new Button("New Game", new Vector2(300, 200), new Vector2(200, 50), renderer, () -> {
            GameScene gameScene = new GameScene(engine, renderer);
            engine.setScene(gameScene);
            try {
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                String filename = "recordings/game_session_" + timestamp + ".jsonl";
                System.out.println("Recording to: " + filename);
                engine.startRecording(filename);
            } catch (java.io.IOException e) {
                System.err.println("Failed to start recording: " + e.getMessage());
            }
        });
        addGameObject(newGameButton);

        Button replayButton = new Button("Replay", new Vector2(300, 260), new Vector2(200, 50), renderer, () -> {
            ReplaySelectionScene replaySelection = new ReplaySelectionScene(engine, renderer, this);
            engine.setScene(replaySelection);
        });
        addGameObject(replayButton);

        Button exitButton = new Button("Exit", new Vector2(300, 320), new Vector2(200, 50), renderer, () -> {
            engine.stop();
        });
        addGameObject(exitButton);
    }

    @Override
    public void render() {
        renderer.drawRect(0, 0, 800, 600, 0.2f, 0.2f, 0.2f, 1.0f);
        super.render();
    }
}