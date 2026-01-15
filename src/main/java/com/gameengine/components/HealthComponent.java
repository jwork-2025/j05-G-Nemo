package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.graphics.Renderer;
import com.gameengine.math.Vector2;

/**
 * 健康组件，管理生命值并渲染HP条
 */
public class HealthComponent extends Component<HealthComponent> {
    private int maxHP;
    private int currentHP;
    private Renderer renderer;

    public HealthComponent(int maxHP) {
        this.maxHP = maxHP;
        this.currentHP = maxHP;
    }

    @Override
    public void initialize() {
        // 初始化健康组件
    }

    @Override
    public void update(float deltaTime) {
        // 健康组件通常不需要每帧更新
    }

    @Override
    public void render() {
        if (!enabled || renderer == null) {
            return;
        }

        TransformComponent transform = owner.getComponent(TransformComponent.class);
        if (transform == null) {
            return;
        }

        Vector2 pos = transform.getPosition();
        float hpRatio = (float) currentHP / maxHP;

        // 绘制红色背景
        renderer.drawRect(pos.x - 10, pos.y - 40, 20, 5, 1.0f, 0.0f, 0.0f, 1.0f);

        // 绘制绿色HP条
        renderer.drawRect(pos.x - 10, pos.y - 40, 20 * hpRatio, 5, 0.0f, 1.0f, 0.0f, 1.0f);
    }

    /**
     * 受到伤害
     */
    public void takeDamage(int damage) {
        currentHP -= damage;
        if (currentHP < 0) currentHP = 0;
    }

    /**
     * 是否死亡
     */
    public boolean isDead() {
        return currentHP <= 0;
    }

    /**
     * 设置渲染器
     */
    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    // Getters
    public int getCurrentHP() {
        return currentHP;
    }

    public int getMaxHP() {
        return maxHP;
    }
}

