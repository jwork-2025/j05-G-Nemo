// Modified RecordingService.java to record numeric ID and name
package com.gameengine.recording;

import com.gameengine.components.TransformComponent;
import com.gameengine.core.GameObject;
import com.gameengine.input.InputManager;
import com.gameengine.scene.Scene;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class RecordingService {
    private final RecordingConfig config;
    private final BlockingQueue<String> lineQueue;
    private volatile boolean recording;
    private Thread writerThread;
    private RecordingStorage storage = new FileRecordingStorage();
    private double elapsed;
    private double keyframeElapsed;
    private double sampleAccumulator;
    private final double warmupSec = 0.1;
    private final DecimalFormat qfmt;
    private Scene lastScene;

    public RecordingService(RecordingConfig config) {
        this.config = config;
        this.lineQueue = new ArrayBlockingQueue<>(config.queueCapacity);
        this.recording = false;
        this.elapsed = 0.0;
        this.keyframeElapsed = 0.0;
        this.sampleAccumulator = 0.0;
        this.qfmt = new DecimalFormat();
        this.qfmt.setMaximumFractionDigits(Math.max(0, config.quantizeDecimals));
        this.qfmt.setGroupingUsed(false);
        this.qfmt.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));  // Force dot as decimal
        this.qfmt.setMaximumFractionDigits(Math.max(0, config.quantizeDecimals));
        this.qfmt.setGroupingUsed(false);
    }

    public boolean isRecording() {
        return recording;
    }

    public void start(Scene scene, int width, int height) throws IOException {
        if (recording) return;
        storage.openWriter(config.outputPath);
        writerThread = new Thread(() -> {
            try {
                while (recording || !lineQueue.isEmpty()) {
                    String s = lineQueue.poll();
                    if (s == null) {
                        try { Thread.sleep(2); } catch (InterruptedException ignored) {}
                        continue;
                    }
                    storage.writeLine(s);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try { storage.closeWriter(); } catch (Exception ignored) {}
            }
        }, "record-writer");
        recording = true;
        writerThread.start();

        enqueue("{\"type\":\"header\",\"version\":1,\"w\":" + width + ",\"h\":" + height + "}");
        keyframeElapsed = 0.0;
    }

    public void stop() {
        if (!recording) return;
        try {
            if (lastScene != null) {
                writeKeyframe(lastScene);  // Final snapshot of entities
            }
        } catch (Exception ignored) {}
        recording = false;
        try {
            writerThread.join();  // Wait indefinitely → prevents truncation
        } catch (InterruptedException ignored) {}
    }

    // NEW: Force write a keyframe immediately (useful at game over)
    public void forceKeyframe(Scene scene) {
        if (recording && scene != null) {
            writeKeyframe(scene);
        }
    }
    public void writeGameOver(double timestamp, int enemiesKilled, double averageFps) {
        if (!recording) return;
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"gameover\",");
        sb.append("\"t\":").append(qfmt.format(timestamp)).append(",");
        sb.append("\"killed\":").append(enemiesKilled).append(",");
        sb.append("\"fps\":").append(qfmt.format(averageFps));
        sb.append("}");
        enqueue(sb.toString());
    }

    public void update(double deltaTime, Scene scene, InputManager input) {
        if (!recording) return;
        elapsed += deltaTime;
        keyframeElapsed += deltaTime;
        sampleAccumulator += deltaTime;
        lastScene = scene;

        Set<Integer> just = input.getJustPressedKeysSnapshot();
        if (!just.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"type\":\"input\",\"t\":").append(qfmt.format(elapsed)).append(",\"keys\":[");
            boolean first = true;
            for (Integer k : just) {
                if (!first) sb.append(',');
                sb.append(k);
                first = false;
            }
            sb.append("]}");
            enqueue(sb.toString());
        }

        if (elapsed >= warmupSec && keyframeElapsed >= config.keyframeIntervalSec) {
            if (writeKeyframe(scene)) {
                keyframeElapsed = 0.0;
            }
        }
    }

    private boolean writeKeyframe(Scene scene) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"keyframe\",\"t\":").append(qfmt.format(elapsed)).append(",\"entities\":[");
        List<GameObject> objs = scene.getGameObjects();
        boolean first = true;
        int count = 0;
        for (GameObject obj : objs) {
            TransformComponent tc = obj.getComponent(TransformComponent.class);
            if (tc == null) continue;
            float x = tc.getPosition().x;
            float y = tc.getPosition().y;
            if (!first) sb.append(',');
            sb.append('{')
              .append("\"id\":").append(obj.getId()).append(',')
              .append("\"name\":\"").append(obj.getName()).append("\",")
              .append("\"x\":").append(qfmt.format(x)).append(',')
              .append("\"y\":").append(qfmt.format(y));

            com.gameengine.components.RenderComponent rc = obj.getComponent(com.gameengine.components.RenderComponent.class);
            if (rc != null) {
                com.gameengine.components.RenderComponent.RenderType rt = rc.getRenderType();
                com.gameengine.math.Vector2 sz = rc.getSize();
                com.gameengine.components.RenderComponent.Color col = rc.getColor();
                sb.append(',')
                  .append("\"rt\":\"").append(rt.name()).append("\",")
                  .append("\"w\":").append(qfmt.format(sz.x)).append(',')
                  .append("\"h\":").append(qfmt.format(sz.y)).append(',')
                  .append("\"color\":[")
                  .append(qfmt.format(col.r)).append(',')
                  .append(qfmt.format(col.g)).append(',')
                  .append(qfmt.format(col.b)).append(',')
                  .append(qfmt.format(col.a)).append(']');
            } else {
                sb.append(',').append("\"rt\":\"CUSTOM\"");
            }

            sb.append('}');
            first = false;
            count++;
        }
        sb.append("]}");
        if (count == 0) return false;
        enqueue(sb.toString());
        return true;
    }

    private void enqueue(String line) {
        if (!lineQueue.offer(line)) {
            // Drop if full
        }
    }
}