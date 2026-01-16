// New Player.java (extracted from original anonymous GameObject)
package com.gameengine.core;

import com.gameengine.components.HealthComponent;
import com.gameengine.components.PhysicsComponent;
import com.gameengine.components.TransformComponent;
import com.gameengine.graphics.Renderer;
import com.gameengine.math.Vector2;

public class Player extends GameObject {
    private Renderer renderer;
    private Vector2 basePosition;

    public Player(Renderer renderer) {
        super("Player");
        this.renderer = renderer;
        addComponent(new TransformComponent(new Vector2(400, 300)));
        PhysicsComponent physics = addComponent(new PhysicsComponent(1.0f));
        physics.setFriction(0.95f);
        HealthComponent health = addComponent(new HealthComponent(1000));
        health.setRenderer(renderer);
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform != null) {
            basePosition = transform.getPosition();
        }
    }

    @Override
    public void render() {
        if (basePosition == null) return;
        renderer.drawRect(basePosition.x - 8, basePosition.y - 10, 16, 20, 1.0f, 0.0f, 0.0f, 1.0f);
        renderer.drawRect(basePosition.x - 6, basePosition.y - 22, 12, 12, 1.0f, 0.5f, 0.0f, 1.0f);
        renderer.drawRect(basePosition.x - 13, basePosition.y - 5, 6, 12, 1.0f, 0.8f, 0.0f, 1.0f);
        renderer.drawRect(basePosition.x + 7, basePosition.y - 5, 6, 12, 0.0f, 1.0f, 0.0f, 1.0f);
        super.render();
    }
}
