// New Button.java
package com.gameengine.ui;

import com.gameengine.core.GameObject;
import com.gameengine.graphics.Renderer;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;

public class Button extends GameObject {
    private Vector2 position;
    private Vector2 size;
    private String text;
    private Renderer renderer;
    private ButtonClickHandler onClick;

    public Button(String text, Vector2 position, Vector2 size, Renderer renderer, ButtonClickHandler onClick) {
        super("Button");
        this.text = text;
        this.position = position;
        this.size = size;
        this.renderer = renderer;
        this.onClick = onClick;
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        Vector2 mouse = InputManager.getInstance().getMousePosition();
        boolean isOver = mouse.x > position.x && mouse.x < position.x + size.x &&
                         mouse.y > position.y && mouse.y < position.y + size.y;
        if (isOver && InputManager.getInstance().isMouseButtonJustPressed(1)) {
            onClick.onClick();
        }
    }

    @Override
    public void render() {
        super.render();
        renderer.drawRect(position.x, position.y, size.x, size.y, 0.5f, 0.5f, 0.5f, 1.0f);
        renderer.drawText(position.x + 10, position.y + size.y / 2 + 10, text, 1.0f, 1.0f, 1.0f, 1.0f); // Adjust text position
    }
}

