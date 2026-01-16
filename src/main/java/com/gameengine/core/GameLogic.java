package com.gameengine.core;

import com.gameengine.components.TransformComponent;
import com.gameengine.components.PhysicsComponent;
import com.gameengine.components.HealthComponent;
import com.gameengine.core.GameObject;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.recording.RecordingService;
import com.gameengine.scene.Scene;
import com.gameengine.components.RenderComponent;
import com.gameengine.graphics.Renderer;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;     
import java.util.Set;        

public class GameLogic {
    private Scene scene;
    private InputManager inputManager;
    private boolean gameOver;
    private Renderer renderer;
    private float timeSinceLastShot = 0.0f;
    private float shootInterval = 0.2f;
    private Map<GameObject, Float> enemyShootTimers = new HashMap<>();
    private int enemiesKilled = 0;
    private GameEngine engine;
    private long endTime = 0;
    private long finalFrameCount = 0;
    private ExecutorService physicsExecutor;

    public GameLogic(Scene scene, Renderer renderer, GameEngine engine) {
        this.scene = scene;
        this.inputManager = InputManager.getInstance();
        this.gameOver = false;
        this.renderer = renderer;
        this.engine = engine;
        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        this.physicsExecutor = Executors.newFixedThreadPool(threadCount);
    }

    public void cleanup() {
        if (physicsExecutor != null && !physicsExecutor.isShutdown()) {
            physicsExecutor.shutdown();
            try {
                if (!physicsExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    physicsExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                physicsExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void handlePlayerInput(float deltaTime) {
        if (gameOver) return;

        List<GameObject> players = scene.findGameObjectsByComponent(TransformComponent.class);
        if (players.isEmpty()) return;

        GameObject player = players.get(0);
        TransformComponent transform = player.getComponent(TransformComponent.class);
        PhysicsComponent physics = player.getComponent(PhysicsComponent.class);

        if (transform == null || physics == null) return;

        Vector2 movement = new Vector2();

        if (inputManager.isKeyPressed(87) || inputManager.isKeyPressed(38)) {
            movement.y -= 1;
        }
        if (inputManager.isKeyPressed(83) || inputManager.isKeyPressed(40)) {
            movement.y += 1;
        }
        if (inputManager.isKeyPressed(65) || inputManager.isKeyPressed(37)) {
            movement.x -= 1;
        }
        if (inputManager.isKeyPressed(68) || inputManager.isKeyPressed(39)) {
            movement.x += 1;
        }

        if (movement.magnitude() > 0) {
            movement = movement.normalize().multiply(200);
            physics.setVelocity(movement);
        }

        Vector2 pos = transform.getPosition();
        if (pos.x < 0) pos.x = 0;
        if (pos.y < 0) pos.y = 0;
        if (pos.x > 800 - 20) pos.x = 800 - 20;
        if (pos.y > 600 - 20) pos.y = 600 - 20;
        transform.setPosition(pos);

        timeSinceLastShot += deltaTime;
        if (inputManager.isKeyPressed(32) && timeSinceLastShot >= shootInterval) {
            timeSinceLastShot -= shootInterval;

            Vector2 playerPos = transform.getPosition().add(new Vector2(10, 10));
            Vector2 mousePos = inputManager.getMousePosition();
            Vector2 direction = mousePos.subtract(playerPos).normalize();
            Vector2 bulletPos = playerPos.add(direction.multiply(25f));

            GameObject bullet = new GameObject("Bullet") {
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

            bullet.addComponent(new TransformComponent(bulletPos));

            PhysicsComponent bulletPhysics = bullet.addComponent(new PhysicsComponent(0.1f));
            bulletPhysics.setVelocity(direction.multiply(400f));
            bulletPhysics.setFriction(1.0f);
            bulletPhysics.setUseGravity(false);

            RenderComponent bulletRender = bullet.addComponent(new RenderComponent(
                RenderComponent.RenderType.CIRCLE,
                new Vector2(4, 4),
                new RenderComponent.Color(1.0f, 1.0f, 0.0f, 1.0f)
            ));
            bulletRender.setRenderer(renderer);

            scene.addGameObject(bullet);
        }
    }

    public void handleEnemyShooting(float deltaTime) {
        if (gameOver) return;

        List<GameObject> enemies = new ArrayList<>();
        for (GameObject obj : scene.getGameObjects()) {
            if (obj.getName().equals("Enemy")) {
                enemies.add(obj);
            }
        }

        for (GameObject enemy : enemies) {
            float timer = enemyShootTimers.getOrDefault(enemy, 0f);
            timer += deltaTime;
            float enemyShootInterval = 1.0f;
            if (timer >= enemyShootInterval) {
                timer -= enemyShootInterval;

                TransformComponent enemyTransform = enemy.getComponent(TransformComponent.class);
                if (enemyTransform == null) continue;

                List<GameObject> players = scene.findGameObjectsByComponent(HealthComponent.class);
                if (players.isEmpty()) continue;
                GameObject player = players.get(0);
                TransformComponent playerTransform = player.getComponent(TransformComponent.class);
                if (playerTransform == null) continue;

                Vector2 enemyPos = enemyTransform.getPosition().add(new Vector2(10, 10));
                Vector2 playerPos = playerTransform.getPosition().add(new Vector2(10, 10));
                Vector2 direction = playerPos.subtract(enemyPos).normalize();
                Vector2 bulletPos = enemyPos.add(direction.multiply(25f));

                GameObject bullet = new GameObject("EnemyBullet") {
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

                bullet.addComponent(new TransformComponent(bulletPos));

                PhysicsComponent bulletPhysics = bullet.addComponent(new PhysicsComponent(0.1f));
                bulletPhysics.setVelocity(direction.multiply(300f));
                bulletPhysics.setFriction(1.0f);
                bulletPhysics.setUseGravity(false);

                RenderComponent bulletRender = bullet.addComponent(new RenderComponent(
                    RenderComponent.RenderType.CIRCLE,
                    new Vector2(4, 4),
                    new RenderComponent.Color(1.0f, 0.0f, 0.0f, 1.0f)
                ));
                bulletRender.setRenderer(renderer);

                scene.addGameObject(bullet);
            }
            enemyShootTimers.put(enemy, timer);
        }
    }

    public void updatePhysics() {
        if (gameOver) return;

        List<PhysicsComponent> physicsComponents = scene.getComponents(PhysicsComponent.class);

        // 并行实现
        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        int batchSize = Math.max(1, physicsComponents.size() / threadCount + 1);
        List<Future<List<GameObject>>> futures = new ArrayList<>();

        for (int i = 0; i < physicsComponents.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, physicsComponents.size());
            Future<List<GameObject>> future = physicsExecutor.submit(() -> {
                List<GameObject> localToRemove = new ArrayList<>();
                for (int j = start; j < end; j++) {
                    PhysicsComponent physics = physicsComponents.get(j);
                    GameObject owner = physics.getOwner();
                    TransformComponent transform = owner.getComponent(TransformComponent.class);
                    if (transform != null) {
                        Vector2 pos = transform.getPosition();
                        Vector2 velocity = physics.getVelocity();
                        String name = owner.getName();
                        if (name.equals("Bullet") || name.equals("EnemyBullet")) {
                            if (pos.x < 0 || pos.x > 800 || pos.y < 0 || pos.y > 600) {
                                localToRemove.add(owner);
                            }
                        } else {
                            boolean velocityChanged = false;
                            if (pos.x <= 0 || pos.x >= 800 - 15) {
                                velocity.x = -velocity.x;
                                velocityChanged = true;
                            }
                            if (pos.y <= 0 || pos.y >= 600 - 15) {
                                velocity.y = -velocity.y;
                                velocityChanged = true;
                            }
                            if (pos.x < 0) pos.x = 0;
                            if (pos.y < 0) pos.y = 0;
                            if (pos.x > 800 - 15) pos.x = 800 - 15;
                            if (pos.y > 600 - 15) pos.y = 600 - 15;
                            transform.setPosition(pos);
                            if (velocityChanged) {
                                physics.setVelocity(velocity);
                            }
                        }
                    }
                }
                return localToRemove;
            });
            futures.add(future);
        }

        List<GameObject> allToRemove = new ArrayList<>();
        for (Future<List<GameObject>> future : futures) {
            try {
                allToRemove.addAll(future.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (GameObject obj : allToRemove) {
            scene.removeGameObject(obj);
        }
    }

    private static class CollisionPlayerResult {
        int damage = 0;
        int killed = 0;
        List<GameObject> toRemove = new ArrayList<>();
    }

    private static class BulletEnemyResult {
        Map<GameObject, List<GameObject>> hits = new HashMap<>();
    }

    public void checkCollisions() {
        if (gameOver) return;

        List<GameObject> players = scene.findGameObjectsByComponent(TransformComponent.class);
        if (players.isEmpty()) return;

        GameObject player = players.get(0);
        TransformComponent playerTransform = player.getComponent(TransformComponent.class);
        if (playerTransform == null) return;

        HealthComponent health = player.getComponent(HealthComponent.class);
        if (health == null) return;

        List<GameObject> bullets = new ArrayList<>();
        List<GameObject> enemies = new ArrayList<>();
        List<GameObject> enemyBullets = new ArrayList<>();

        for (GameObject obj : scene.getGameObjects()) {
            String name = obj.getName();
            if (name.equals("Bullet")) {
                bullets.add(obj);
            } else if (name.equals("Enemy")) {
                enemies.add(obj);
            } else if (name.equals("EnemyBullet")) {
                enemyBullets.add(obj);
            }
        }

        // 并行的碰撞检查
        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        int batchSize = Math.max(1, enemies.size() / threadCount + 1);
        List<Future<CollisionPlayerResult>> playerEnemyFutures = new ArrayList<>();

        for (int i = 0; i < enemies.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, enemies.size());
            Future<CollisionPlayerResult> future = physicsExecutor.submit(() -> {
                CollisionPlayerResult res = new CollisionPlayerResult();
                for (int j = start; j < end; j++) {
                    GameObject enemy = enemies.get(j);
                    TransformComponent enemyTransform = enemy.getComponent(TransformComponent.class);
                    RenderComponent enemyRender = enemy.getComponent(RenderComponent.class);
                    if (enemyTransform != null && enemyRender != null) {
                        Vector2 playerCenter = playerTransform.getPosition().add(new Vector2(10, 10));
                        Vector2 enemyCenter = enemyTransform.getPosition().add(enemyRender.getSize().multiply(0.5f));
                        float distance = playerCenter.distance(enemyCenter);
                        if (distance < 20) {
                            res.damage += 20;
                            res.killed++;
                            res.toRemove.add(enemy);
                        }
                    }
                }
                return res;
            });
            playerEnemyFutures.add(future);
        }

        int totalPlayerEnemyDamage = 0;
        int totalPlayerEnemyKilled = 0;
        Set<GameObject> markedToRemove = new HashSet<>();
        for (Future<CollisionPlayerResult> future : playerEnemyFutures) {
            try {
                CollisionPlayerResult res = future.get();
                totalPlayerEnemyDamage += res.damage;
                totalPlayerEnemyKilled += res.killed;
                markedToRemove.addAll(res.toRemove);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        enemiesKilled += totalPlayerEnemyKilled;

        
        batchSize = Math.max(1, bullets.size() / threadCount + 1);
        List<Future<BulletEnemyResult>> bulletEnemyFutures = new ArrayList<>();

        for (int i = 0; i < bullets.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, bullets.size());
            Future<BulletEnemyResult> future = physicsExecutor.submit(() -> {
                BulletEnemyResult res = new BulletEnemyResult();
                for (int j = start; j < end; j++) {
                    GameObject bullet = bullets.get(j);
                    TransformComponent bulletTransform = bullet.getComponent(TransformComponent.class);
                    RenderComponent bulletRender = bullet.getComponent(RenderComponent.class);
                    if (bulletTransform == null || bulletRender == null) continue;
                    Vector2 bulletCenter = bulletTransform.getPosition().add(bulletRender.getSize().multiply(0.5f));
                    boolean collided = false;
                    for (GameObject enemy : enemies) {
                        if (markedToRemove.contains(enemy)) continue;
                        TransformComponent enemyTransform = enemy.getComponent(TransformComponent.class);
                        RenderComponent enemyRender = enemy.getComponent(RenderComponent.class);
                        if (enemyTransform == null || enemyRender == null) continue;
                        Vector2 enemyCenter = enemyTransform.getPosition().add(enemyRender.getSize().multiply(0.5f));
                        float distance = bulletCenter.distance(enemyCenter);
                        float collisionThreshold = (enemyRender.getSize().x / 2) + (bulletRender.getSize().x / 2) + 3;
                        if (distance < collisionThreshold) {
                            List<GameObject> bulletList = res.hits.computeIfAbsent(enemy, k -> new ArrayList<>());
                            bulletList.add(bullet);
                            collided = true;
                            break;
                        }
                    }
                }
                return res;
            });
            bulletEnemyFutures.add(future);
        }

        Map<GameObject, List<GameObject>> globalHits = new HashMap<>();
        for (Future<BulletEnemyResult> future : bulletEnemyFutures) {
            try {
                BulletEnemyResult res = future.get();
                for (Map.Entry<GameObject, List<GameObject>> entry : res.hits.entrySet()) {
                    globalHits.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int totalBulletEnemyKilled = globalHits.size();
        enemiesKilled += totalBulletEnemyKilled;
        for (Map.Entry<GameObject, List<GameObject>> entry : globalHits.entrySet()) {
            markedToRemove.add(entry.getKey());
            markedToRemove.addAll(entry.getValue());
        }

        batchSize = Math.max(1, bullets.size() / threadCount + 1);
        List<Future<Set<GameObject>>> bulletBulletFutures = new ArrayList<>();

        for (int i = 0; i < bullets.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, bullets.size());
            Future<Set<GameObject>> future = physicsExecutor.submit(() -> {
                Set<GameObject> localToRemove = new HashSet<>();
                for (int ii = start; ii < end; ii++) {
                    GameObject b1 = bullets.get(ii);
                    if (markedToRemove.contains(b1)) continue;
                    TransformComponent t1 = b1.getComponent(TransformComponent.class);
                    RenderComponent r1 = b1.getComponent(RenderComponent.class);
                    if (t1 == null || r1 == null) continue;
                    Vector2 c1 = t1.getPosition().add(r1.getSize().multiply(0.5f));
                    for (int jj = ii + 1; jj < bullets.size(); jj++) {
                        GameObject b2 = bullets.get(jj);
                        if (markedToRemove.contains(b2)) continue;
                        TransformComponent t2 = b2.getComponent(TransformComponent.class);
                        RenderComponent r2 = b2.getComponent(RenderComponent.class);
                        if (t2 == null || r2 == null) continue;
                        Vector2 c2 = t2.getPosition().add(r2.getSize().multiply(0.5f));
                        float distance = c1.distance(c2);
                        float threshold = (r1.getSize().x / 2) + (r2.getSize().x / 2) + 2;
                        if (distance < threshold) {
                            localToRemove.add(b1);
                            localToRemove.add(b2);
                        }
                    }
                }
                return localToRemove;
            });
            bulletBulletFutures.add(future);
        }

        for (Future<Set<GameObject>> future : bulletBulletFutures) {
            try {
                markedToRemove.addAll(future.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        batchSize = Math.max(1, enemyBullets.size() / threadCount + 1);
        List<Future<CollisionPlayerResult>> enemyBulletPlayerFutures = new ArrayList<>();

        for (int i = 0; i < enemyBullets.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, enemyBullets.size());
            Future<CollisionPlayerResult> future = physicsExecutor.submit(() -> {
                CollisionPlayerResult res = new CollisionPlayerResult();
                for (int j = start; j < end; j++) {
                    GameObject bullet = enemyBullets.get(j);
                    TransformComponent bulletTransform = bullet.getComponent(TransformComponent.class);
                    RenderComponent bulletRender = bullet.getComponent(RenderComponent.class);
                    if (bulletTransform == null || bulletRender == null) continue;
                    Vector2 bulletCenter = bulletTransform.getPosition().add(bulletRender.getSize().multiply(0.5f));
                    Vector2 playerCenter = playerTransform.getPosition().add(new Vector2(10, 10));
                    float distance = playerCenter.distance(bulletCenter);
                    float collisionThreshold = 10 + (bulletRender.getSize().x / 2) + 3;
                    if (distance < collisionThreshold) {
                        res.damage += 1;
                        res.toRemove.add(bullet);
                    }
                }
                return res;
            });
            enemyBulletPlayerFutures.add(future);
        }

        int totalEnemyBulletDamage = 0;
        for (Future<CollisionPlayerResult> future : enemyBulletPlayerFutures) {
            try {
                CollisionPlayerResult res = future.get();
                totalEnemyBulletDamage += res.damage;
                markedToRemove.addAll(res.toRemove);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        batchSize = Math.max(1, enemyBullets.size() / threadCount + 1);
        List<Future<Set<GameObject>>> enemyBulletEnemyBulletFutures = new ArrayList<>();

        for (int i = 0; i < enemyBullets.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, enemyBullets.size());
            Future<Set<GameObject>> future = physicsExecutor.submit(() -> {
                Set<GameObject> localToRemove = new HashSet<>();
                for (int ii = start; ii < end; ii++) {
                    GameObject b1 = enemyBullets.get(ii);
                    if (markedToRemove.contains(b1)) continue;
                    TransformComponent t1 = b1.getComponent(TransformComponent.class);
                    RenderComponent r1 = b1.getComponent(RenderComponent.class);
                    if (t1 == null || r1 == null) continue;
                    Vector2 c1 = t1.getPosition().add(r1.getSize().multiply(0.5f));
                    for (int jj = ii + 1; jj < enemyBullets.size(); jj++) {
                        GameObject b2 = enemyBullets.get(jj);
                        if (markedToRemove.contains(b2)) continue;
                        TransformComponent t2 = b2.getComponent(TransformComponent.class);
                        RenderComponent r2 = b2.getComponent(RenderComponent.class);
                        if (t2 == null || r2 == null) continue;
                        Vector2 c2 = t2.getPosition().add(r2.getSize().multiply(0.5f));
                        float distance = c1.distance(c2);
                        float threshold = (r1.getSize().x / 2) + (r2.getSize().x / 2) + 2;
                        if (distance < threshold) {
                            localToRemove.add(b1);
                            localToRemove.add(b2);
                        }
                    }
                }
                return localToRemove;
            });
            enemyBulletEnemyBulletFutures.add(future);
        }

        for (Future<Set<GameObject>> future : enemyBulletEnemyBulletFutures) {
            try {
                markedToRemove.addAll(future.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Apply total damage to player
        int totalDamage = totalPlayerEnemyDamage + totalEnemyBulletDamage;
        health.takeDamage(totalDamage);

        if (health.isDead()) {
            gameOver = true;
            if (endTime == 0) {
                endTime = System.nanoTime();
                finalFrameCount = engine.getFrameCount();
            }
            // NEW: Handle recording when game ends
            RecordingService rs = engine.getRecordingService();
            if (rs != null && rs.isRecording()) {
                double totalTimeSec = (endTime - engine.getStartTime()) / 1_000_000_000.0;
                double avgFps = (totalTimeSec > 0) ? (finalFrameCount / totalTimeSec) : 0.0;

                rs.forceKeyframe(scene);                    // Capture final entity positions
                rs.writeGameOver(totalTimeSec, enemiesKilled, avgFps);  // Write game over info
                rs.stop();                                  // Immediately stop recording
            }

        }

        // Batch remove
        for (GameObject obj : markedToRemove) {
            scene.removeGameObject(obj);
        }
    }

    public void renderGameOver() {
        if (gameOver) {
            renderer.drawText(200, 280, "Game Over!", 1.0f, 0.0f, 0.0f, 1.0f);
            renderer.drawText(200, 320, "Enemies killed: " + enemiesKilled, 1.0f, 1.0f, 1.0f, 1.0f);
            double totalTime = (endTime - engine.getStartTime()) / 1_000_000_000.0;
            double fps = (totalTime > 0) ? (finalFrameCount / totalTime) : 0.0;
            renderer.drawText(200, 360, "Average FPS: " + String.format("%.2f", fps), 1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }
}