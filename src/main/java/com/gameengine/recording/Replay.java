package com.gameengine.recording;

import com.gameengine.graphics.Renderer;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.Timer;

public class Replay {
    private Renderer renderer;
    private boolean running;
    private Timer replayTimer;
    private double currentTime = 0.0;
    private float targetFPS = 60.0f;
    private List<Keyframe> keyframes = new ArrayList<>();
    private List<InputEvent> inputEvents = new ArrayList<>(); // Recorded but not used in this visual replay
    private double maxTime = 0.0;
    private int screenWidth;
    private int screenHeight;

    public Replay(String path) throws IOException {
        RecordingStorage storage = new FileRecordingStorage();
        Iterable<String> lines = storage.readLines(path);

        for (String line : lines) {
            String type = RecordingJson.stripQuotes(RecordingJson.field(line, "type"));
            if ("header".equals(type)) {
                screenWidth = (int) RecordingJson.parseDouble(RecordingJson.field(line, "w"));
                screenHeight = (int) RecordingJson.parseDouble(RecordingJson.field(line, "h"));
            } else if ("input".equals(type)) {
                double t = RecordingJson.parseDouble(RecordingJson.field(line, "t"));
                String keysStr = RecordingJson.field(line, "keys");
                if (keysStr != null) {
                    String inner = keysStr.substring(1, keysStr.length() - 1);
                    String[] keyStrs = inner.split(",");
                    List<Integer> keys = new ArrayList<>();
                    for (String k : keyStrs) {
                        String trimmed = k.trim();
                        if (!trimmed.isEmpty()) {
                            keys.add(Integer.parseInt(trimmed));
                        }
                    }
                    inputEvents.add(new InputEvent(t, keys));
                }
            } else if ("keyframe".equals(type)) {
                double t = RecordingJson.parseDouble(RecordingJson.field(line, "t"));
                maxTime = Math.max(maxTime, t);
                String entitiesStr = RecordingJson.field(line, "entities");
                if (entitiesStr != null) {
                    String inner = entitiesStr.substring(1, entitiesStr.length() - 1);
                    String[] entJsons = RecordingJson.splitTopLevel(inner);
                    List<EntityState> states = new ArrayList<>();
                    for (String ent : entJsons) {
                        long id = (long) RecordingJson.parseDouble(RecordingJson.field(ent, "id"));
                        String name = RecordingJson.stripQuotes(RecordingJson.field(ent, "name"));
                        double x = RecordingJson.parseDouble(RecordingJson.field(ent, "x"));
                        double y = RecordingJson.parseDouble(RecordingJson.field(ent, "y"));
                        String rt = RecordingJson.stripQuotes(RecordingJson.field(ent, "rt"));
                        double w = RecordingJson.parseDouble(RecordingJson.field(ent, "w"));
                        double h = RecordingJson.parseDouble(RecordingJson.field(ent, "h"));
                        String colorStr = RecordingJson.field(ent, "color");
                        float[] color = null;
                        if (colorStr != null) {
                            String colInner = colorStr.substring(1, colorStr.length() - 1);
                            String[] cols = colInner.split(",");
                            color = new float[4];
                            for (int j = 0; j < 4 && j < cols.length; j++) {
                                color[j] = (float) RecordingJson.parseDouble(cols[j]);
                            }
                        }
                        states.add(new EntityState(id, name, x, y, rt, w, h, color));
                    }
                    keyframes.add(new Keyframe(t, states));
                }
            }
        }

        keyframes.sort(Comparator.comparingDouble(k -> k.time));
        renderer = new Renderer(screenWidth, screenHeight, "Replay");
    }

    public void run() {
        running = true;
        replayTimer = new Timer((int) (1000 / targetFPS), (ActionEvent e) -> {
            if (running) {
                float delta = 1.0f / targetFPS;
                currentTime += delta;
                if (currentTime > maxTime + 1.0) { // Add buffer after end
                    stop();
                    return;
                }
                render();
            }
        });
        replayTimer.start();
    }

    private void render() {
        renderer.beginFrame();

        // Draw background
        renderer.drawRect(0, 0, screenWidth, screenHeight, 0.1f, 0.1f, 0.2f, 1.0f);

        // Find prev and next keyframe
        Keyframe prev = null;
        Keyframe next = null;
        for (Keyframe kf : keyframes) {
            if (kf.time <= currentTime) {
                prev = kf;
            } else {
                next = kf;
                break;
            }
        }

        if (prev == null) {
            renderer.endFrame();
            return;
        }

        Map<Long, EntityState> prevStatesMap = new HashMap<>();
        for (EntityState s : prev.states) {
            prevStatesMap.put(s.id, s);
        }

        List<EntityState> statesToRender;
        if (next == null) {
            statesToRender = prev.states;
        } else {
            double frac = (currentTime - prev.time) / (next.time - prev.time);
            Map<Long, EntityState> interpStates = new HashMap<>();
            Set<Long> allIds = new HashSet<>(prevStatesMap.keySet());
            for (EntityState s : next.states) {
                allIds.add(s.id);
            }
            for (Long id : allIds) {
                EntityState p = prevStatesMap.get(id);
                EntityState n = null;
                for (EntityState s : next.states) {
                    if (s.id == id) {
                        n = s;
                        break;
                    }
                }
                if (p == null && n != null) {
                    // New entity: appear at n position
                    interpStates.put(id, n);
                } else if (p != null && n == null) {
                    // Disappearing: keep at p position
                    interpStates.put(id, p);
                } else if (p != null && n != null) {
                    // Interpolate
                    double ix = p.x + frac * (n.x - p.x);
                    double iy = p.y + frac * (n.y - p.y);
                    interpStates.put(id, new EntityState(id, p.name, ix, iy, p.renderType, p.width, p.height, p.color));
                }
            }
            statesToRender = new ArrayList<>(interpStates.values());
        }

        renderStates(statesToRender);

        renderer.endFrame();
    }

    private void renderStates(List<EntityState> states) {
        for (EntityState s : states) {
            if (s.renderType == null) {
                continue;
            }
            if ("CUSTOM".equals(s.renderType)) {
                // Special rendering for player
                float bx = (float) s.x - 8;
                float by = (float) s.y - 10;
                renderer.drawRect(bx, by, 16, 20, 1.0f, 0.0f, 0.0f, 1.0f); // Body
                renderer.drawRect(bx + 2, by - 12, 12, 12, 1.0f, 0.5f, 0.0f, 1.0f); // Head
                renderer.drawRect(bx - 5, by + 5, 6, 12, 1.0f, 0.8f, 0.0f, 1.0f); // Left arm
                renderer.drawRect(bx + 9, by + 5, 6, 12, 0.0f, 1.0f, 0.0f, 1.0f); // Right arm
            } else if ("RECTANGLE".equals(s.renderType) && s.color != null) {
                renderer.drawRect((float) s.x, (float) s.y, (float) s.width, (float) s.height,
                        s.color[0], s.color[1], s.color[2], s.color[3]);
            } else if ("CIRCLE".equals(s.renderType) && s.color != null) {
                renderer.drawCircle((float) s.x, (float) s.y, (float) (s.width / 2), 32,
                        s.color[0], s.color[1], s.color[2], s.color[3]);
            }
        }
    }

    public void stop() {
        running = false;
        if (replayTimer != null) {
            replayTimer.stop();
        }
        if (renderer != null) {
            renderer.cleanup();
        }
    }

    private static class Keyframe {
        double time;
        List<EntityState> states;

        Keyframe(double time, List<EntityState> states) {
            this.time = time;
            this.states = states;
        }
    }

    private static class EntityState {
        long id;
        String name;
        double x, y;
        String renderType;
        double width, height;
        float[] color;

        EntityState(long id, String name, double x, double y, String renderType, double width, double height, float[] color) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.renderType = renderType;
            this.width = width;
            this.height = height;
            this.color = color;
        }
    }

    private static class InputEvent {
        double time;
        List<Integer> keys;

        InputEvent(double time, List<Integer> keys) {
            this.time = time;
            this.keys = keys;
        }
    }
}