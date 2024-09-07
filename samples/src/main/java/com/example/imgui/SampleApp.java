package com.example.imgui;

import com.example.AbstractSampleApp;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.material.predefined.UnshadedColor;
import com.wizzardo.vulkan.scene.Geometry;
import com.wizzardo.vulkan.scene.shape.Box;
import com.wizzardo.vulkan.ui.imgui.ImGuiQuad;
import com.wizzardo.vulkan.ui.imgui.memo.IntMemo;
import com.wizzardo.vulkan.ui.imgui.memo.Memo;
import imgui.*;
import imgui.callback.ImGuiInputTextCallback;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;
import org.joml.Vector3f;
import java.util.List;

public class SampleApp extends AbstractSampleApp {

    ImGuiQuad imGuiQuad;

    public static void main(String[] args) {
        new SampleApp().start();
    }

    @Override
    protected void initApp() {
        // need to run it as async task to set glfw callback after GlfwInputsManager, that does it by same addTask()
        addTask(() -> {
            int[] count = new int[]{0};
            ImString str = new ImString(5);
            float[] flt = new float[1];
            IntMemo<String> countToString = new IntMemo<>(String::valueOf);
            Memo<String, String> resultMemo = new Memo<>(arg -> "Result: " + arg);
            ImGuiInputTextCallback imGuiInputTextCallback = new ImGuiInputTextCallback() {
                @Override
                public void accept(ImGuiInputTextCallbackData imGuiInputTextCallbackData) {
                    System.out.println((char) imGuiInputTextCallbackData.getEventChar());
                }
            };
            imGuiQuad = new ImGuiQuad(this, ImGuiQuad.findSystemFont("Arial").getAbsolutePath(), () -> {
                ImGui.newFrame();
//                ImGui.text("Hello, World! " + FontAwesomeIcons.Smile);
                ImGui.setWindowSize(800, 600);
//                ImGui.setWindowSize(800, 600, ImGuiCond.Once);
                ImGui.text("Hello, World! ");
                if (ImGui.button(" Click")) {
                    count[0]++;
                }
                ImGui.sameLine();
                ImGui.text(countToString.get(count[0]));
                ImGui.inputText(
                        "string",
                        str,
                        ImGuiInputTextFlags.CallbackResize | ImGuiInputTextFlags.CallbackCharFilter,
                        imGuiInputTextCallback
                );
                ImGui.text(resultMemo.get(str.get()));

                ImGui.sliderFloat("float", flt, 0, 1);
                ImGui.separator();
            });


            imGuiQuad.prepare(this, getGuiViewport());

            getGuiViewport().getScene().attachChild(imGuiQuad);
            this.addAlteration(imGuiQuad);

            {
                Material material = new UnshadedColor(new Vector3f(1, 1, 0));
                Geometry geometry = new Box(material);
                addGeometry(geometry, getMainViewport());
            }
        });

        getMainViewport().getCamera().setLocation(new Vector3f(2, 2, 2));
        getMainViewport().getCamera().lookAt(Vectors.ZERO, Vectors.UNIT_Z);
    }

    @Override
    protected void simpleUpdate(double tpf) {
        List<Geometry> geometries = getMainViewport().getGeometries();
        for (int i = 0; i < geometries.size(); i++) {
            Geometry geometry = geometries.get(i);
            geometry.getLocalTransform().getRotation().setAngleAxis(this.getTime() * Math.toRadians(90) / 10f, 0.0f, 0.0f, 1.0f);
            if (geometry.getMaterial() instanceof UnshadedColor) {
                UnshadedColor material = (UnshadedColor) geometry.getMaterial();
                material.getColor().set((float) (Math.sin(this.getTime() / 3)), (float) (Math.sin(this.getTime() / 2)), (float) Math.sin(this.getTime()));
                material.updateUniforms();
            }
        }
    }
}
