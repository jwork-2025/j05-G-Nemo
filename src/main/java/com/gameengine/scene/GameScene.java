// New GameScene.java (extracted from original anonymous class)
package com.gameengine.scene;

import com.gameengine.components.*;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameLogic;
import com.gameengine.core.GameObject;
import com.gameengine.graphics.Renderer;
import com.gameengine.math.Vector2;

import java.util.Random;

public class GameScene extends Scene {
    private Renderer renderer;
    private Random random;
    private float time;
    private GameLogic gameLogic;
    private GameEngine engine;

    public GameScene(GameEngine engine, Renderer renderer) {
        super("GameScene");
        this.engine = engine;
        this.renderer = renderer;
        this.random = new Random();
        this.time = 0;
    }

    @Override
    public void initialize() {
        super.initialize();
        this.gameLogic = new GameLogic(this, renderer, engine);
        createPlayer();
        createEnemies();
        createDecorations();
    }

    @Override
    public void update(float deltaTime) {
        if (gameLogic.isGameOver()) {
            return;
        }

        super.update(deltaTime);
        time += deltaTime;

        gameLogic.handlePlayerInput(deltaTime);
        gameLogic.handleEnemyShooting(deltaTime);
        gameLogic.checkCollisions();
        gameLogic.updatePhysics();

        if (time > 0.2f) {
            createEnemy();
            time = 0;
        }
    }

    @Override
    public void render() {
        renderer.drawRect(0, 0, 800, 600, 0.1f, 0.1f, 0.2f, 1.0f);
        super.render();
        gameLogic.renderGameOver();
    }

    private void createPlayer() {
        Player player = new Player(renderer);
        addGameObject(player);
    }

    private void createEnemies() {
        for (int i = 0; i < 30; i++) {
            createEnemy();
        }
    }

    private void createEnemy() {
        GameObject enemy = new GameObject("Enemy");
        Vector2 position = new Vector2(random.nextFloat() * 800, random.nextFloat() * 600);
        enemy.addComponent(new TransformComponent(position));
        RenderComponent render = enemy.addComponent(new RenderComponent(
                RenderComponent.RenderType.RECTANGLE,
                new Vector2(20, 20),
                new RenderComponent.Color(1.0f, 0.5f, 0.0f, 1.0f)
        ));
        render.setRenderer(renderer);
        PhysicsComponent physics = enemy.addComponent(new PhysicsComponent(0.5f));
        physics.setVelocity(new Vector2(
                (random.nextFloat() - 0.5f) * 100,
                (random.nextFloat() - 0.5f) * 100
        ));
        physics.setFriction(0.98f);
        addGameObject(enemy);
    }

    private void createDecoration() {
        GameObject decoration = new GameObject("Decoration");
        Vector2 position = new Vector2(random.nextFloat() * 800, random.nextFloat() * 600);
        decoration.addComponent(new TransformComponent(position));
        RenderComponent render = decoration.addComponent(new RenderComponent(
                RenderComponent.RenderType.CIRCLE,
                new Vector2(5, 5),
                new RenderComponent.Color(0.5f, 0.5f, 1.0f, 0.8f)
        ));
        render.setRenderer(renderer);
        addGameObject(decoration);
    }

    private void createDecorations() {
        for (int i = 0; i < 5; i++) {
            createDecoration();
        }
    }
}
