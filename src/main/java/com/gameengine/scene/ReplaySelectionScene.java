// New ReplaySelectionScene.java
package com.gameengine.scene;

import com.gameengine.core.GameEngine;
import com.gameengine.graphics.Renderer;
import com.gameengine.math.Vector2;
import com.gameengine.recording.FileRecordingStorage;
import com.gameengine.recording.RecordingStorage;
import com.gameengine.ui.Button;
import com.gameengine.ui.ButtonClickHandler;

import java.io.File;
import java.util.List;

public class ReplaySelectionScene extends Scene {
    private GameEngine engine;
    private Renderer renderer;
    private MenuScene menuScene;

    public ReplaySelectionScene(GameEngine engine, Renderer renderer, MenuScene menuScene) {
        super("ReplaySelection");
        this.engine = engine;
        this.renderer = renderer;
        this.menuScene = menuScene;
    }

    @Override
    public void initialize() {
        super.initialize();

        Button backButton = new Button("Back", new Vector2(50, 50), new Vector2(100, 50), renderer, () -> {
            engine.setScene(menuScene);
        });
        addGameObject(backButton);

        RecordingStorage storage = new FileRecordingStorage();
        List<File> recordings = storage.listRecordings();

        float yPos = 150;
        for (File file : recordings) {
            String fileName = file.getName();
            String filePath = file.getAbsolutePath();
            Button fileButton = new Button(fileName, new Vector2(200, yPos), new Vector2(400, 30), renderer, () -> {
                ReplayScene replayScene = new ReplayScene(engine, renderer, menuScene, filePath);
                engine.setScene(replayScene);
            });
            addGameObject(fileButton);
            yPos += 40;
        }
    }

    @Override
    public void render() {
        renderer.drawRect(0, 0, 800, 600, 0.2f, 0.2f, 0.2f, 1.0f);
        super.render();
    }
}