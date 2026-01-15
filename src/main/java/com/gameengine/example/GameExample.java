package com.gameengine.example;

import com.gameengine.components.*;
import com.gameengine.core.GameObject;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameLogic;
import com.gameengine.graphics.Renderer;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

import java.util.List;
import java.util.Random;
import com.gameengine.recording.RecordingConfig;
import com.gameengine.recording.RecordingService;
import java.io.IOException;
public class GameExample {
    public static void main(String[] args) {
        System.out.println("启动游戏引擎...");

        try {
            GameEngine engine = new GameEngine(800, 600, "游戏引擎");
            Scene gameScene = new Scene("GameScene") {
                private Renderer renderer;
                private Random random;
                private float time;
                private GameLogic gameLogic;

                @Override
                public void initialize() {
                    super.initialize();
                    this.renderer = engine.getRenderer();
                    this.random = new Random();
                    this.time = 0;
                    this.gameLogic = new GameLogic(this, renderer, engine);
                    createPlayer();
                    createEnemies();
                    createDecorations();
                }

                
                @Override
                public void update(float deltaTime) {
                    if (gameLogic.isGameOver()) {
                        return; // Skip updates if game is over
                    }

                    super.update(deltaTime);
                    time += deltaTime;

                    gameLogic.handlePlayerInput(deltaTime);
                    gameLogic.handleEnemyShooting(deltaTime); // NEW: Add enemy shooting
                    gameLogic.checkCollisions(); // Fixed: Correct method name
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
                    gameLogic.renderGameOver(); // NEW: Call GameLogic's renderGameOver method
                }

                private void createPlayer() {
                    GameObject player = new GameObject("Player") {
                        private Vector2 basePosition;

                        @Override
                        public void update(float deltaTime) {
                            super.update(deltaTime);
                            updateComponents(deltaTime);
                            updateBodyParts();
                        }

                        @Override
                        public void render() {
                            renderBodyParts();
                            renderComponents(); // Ensure components are rendered after body parts
                        }

                        private void updateBodyParts() {
                            TransformComponent transform = getComponent(TransformComponent.class);
                            if (transform != null) {
                                basePosition = transform.getPosition();
                            }
                        }

                        private void renderBodyParts() {
                            if (basePosition == null) return;

                            renderer.drawRect(
                                basePosition.x - 8, basePosition.y - 10, 16, 20,
                                1.0f, 0.0f, 0.0f, 1.0f
                            );
                            renderer.drawRect(
                                basePosition.x - 6, basePosition.y - 22, 12, 12,
                                1.0f, 0.5f, 0.0f, 1.0f
                            );
                            renderer.drawRect(
                                basePosition.x - 13, basePosition.y - 5, 6, 12,
                                1.0f, 0.8f, 0.0f, 1.0f
                            );
                            renderer.drawRect(
                                basePosition.x + 7, basePosition.y - 5, 6, 12,
                                0.0f, 1.0f, 0.0f, 1.0f
                            );
                        }
                    };

                    TransformComponent transform = player.addComponent(new TransformComponent(new Vector2(400, 300)));
                    PhysicsComponent physics = player.addComponent(new PhysicsComponent(1.0f));
                    physics.setFriction(0.95f);
                    HealthComponent health = player.addComponent(new HealthComponent(1000));
                    health.setRenderer(renderer);
                    System.out.println("Player created with HealthComponent, renderer set: " + renderer);

                    addGameObject(player);
                }

                private void createEnemies() {
                    for (int i = 0; i < 30; i++) {
                        createEnemy();
                    }
                }

                private void createEnemy() {
                    GameObject enemy = new GameObject("Enemy") {
                        @Override
                        public void update(float deltaTime) {
                            super.update(deltaTime);
                            updateComponents(deltaTime);
                        }

                        @Override
                        public void render() {
                            renderComponents();
                        }
                    };

                    Vector2 position = new Vector2(
                        random.nextFloat() * 800,
                        random.nextFloat() * 600
                    );

                    TransformComponent transform = enemy.addComponent(new TransformComponent(position));
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
                    GameObject decoration = new GameObject("Decoration") {
                        @Override
                        public void update(float deltaTime) {
                            super.update(deltaTime);
                            updateComponents(deltaTime);
                        }

                        @Override
                        public void render() {
                            renderComponents();
                        }
                    };

                    Vector2 position = new Vector2(
                        random.nextFloat() * 800,
                        random.nextFloat() * 600
                    );

                    TransformComponent transform = decoration.addComponent(new TransformComponent(position));
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
            };

            engine.setScene(gameScene);
            
            // 开始录像
            try {
                engine.startRecording("recordings/game_session.jsonl");
                System.out.println("Recording started to recordings/game_session.jsonl");
            } catch (IOException e) {
                System.err.println("Failed to start recording: " + e.getMessage());
                // Continue without recording
            }

            engine.run();

        } catch (Exception e) {
            System.err.println("游戏运行出错: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("游戏结束");
    }
}
