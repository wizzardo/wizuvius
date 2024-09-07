package com.wizzardo.vulkan.ui.imgui;

import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.material.PushConstantInfo;
import com.wizzardo.vulkan.scene.Geometry;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImInt;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Properties;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_DYNAMIC_STATE_SCISSOR;

public class ImGuiQuad extends Geometry implements Alteration {
    protected ImGuiImplGlfw imGuiImplGlfw;
    protected String fontFilePath;
    protected Runnable render;
    protected float width;
    protected float height;

    public ImGuiQuad(DesktopVulkanApplication app, String fontFilePath, Runnable render) {
        this(app, "imgui", fontFilePath, render);
    }

    public ImGuiQuad(DesktopVulkanApplication app, String name, String fontFilePath, Runnable render) {
        this.name = name;
        this.fontFilePath = fontFilePath;
        this.render = render;

        Properties properties = System.getProperties();
        boolean shouldScale = properties.getProperty("os.name", "").contains("Mac");

        ImGui.createContext();
        imGuiImplGlfw = new ImGuiImplGlfw();
        imGuiImplGlfw.init(app.getWindow(), true);

        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);

        io.setConfigViewportsNoTaskBarIcon(true);
        ImFont imFont = io.getFonts().addFontFromFileTTF(
                fontFilePath,
                20 * (shouldScale ? app.getInputsManager().getWindowScaleX() : 1)
        );
        io.setFontDefault(imFont);
        io.getFonts().build();

        if (shouldScale) {
            width = app.getWidth() * app.getInputsManager().getWindowScaleX();
            height = app.getHeight() * app.getInputsManager().getWindowScaleY();
        } else {
            width = app.getWidth();
            height = app.getHeight();
        }


        ImInt fontTextureWidth = new ImInt();
        ImInt fontTextureHeight = new ImInt();
        ByteBuffer texDataAsRGBA32 = io.getFonts().getTexDataAsRGBA32(fontTextureWidth, fontTextureHeight);

        TextureImage fontTexture = TextureLoader.createTextureImage(
                app,
                texDataAsRGBA32,
                fontTextureWidth.get(),
                fontTextureHeight.get(),
                texDataAsRGBA32.limit(),
                VK_FORMAT_R8G8B8A8_UNORM,
                1
        );

        app.getGuiViewport().enableDynamicState(VK_DYNAMIC_STATE_VIEWPORT);
        app.getGuiViewport().enableDynamicState(VK_DYNAMIC_STATE_SCISSOR);

        material = new Material() {
            {
                withUBO = false;
                vertexLayout = new VertexLayout(
                        VertexLayout.BindingDescription.F2, // Position
                        VertexLayout.BindingDescription.TEXTURE_COORDINATES, // UV
                        VertexLayout.BindingDescription.B4UNORM // Color
                );

                setVertexShader("com/wizzardo/vulkan/ui/imgui/imgui.vert");
                setFragmentShader("com/wizzardo/vulkan/ui/imgui/imgui.frag");

                addTextureImage(fontTexture);
                setTextureSampler(app.createTextureSampler(1));

                addPushConstant(new PushConstantInfo(VK_SHADER_STAGE_VERTEX_BIT, Float.BYTES * 4) {
                    @Override
                    public void accept(ByteBuffer byteBuffer) {
                    }
                });
            }
        };

        update();
        mesh = new ImGuiMesh(app);
    }

    public void update() {
        imGuiImplGlfw.newFrame();
        ImGui.getIO().setDisplaySize(width, height);
        processImgui();
        ImGui.render();
        if (mesh != null)
            ((ImGuiMesh) mesh).update();
    }

    protected void processImgui() {
        render.run();
    }

    public void prepare(DesktopVulkanApplication app, Viewport viewport) {
        this.getMaterial().prepare(app, viewport);
        this.getMesh().prepare(app, this.getMaterial().getVertexLayout());
        this.prepare(app);
    }

    @Override
    public boolean isPrepared() {
        return super.isPrepared() && mesh.getVertexBuffer() != null;
    }

    @Override
    public boolean onUpdate(double tpf) {
        if (parent == null)
            return false;

        update();
        return true;
    }

    public static File findSystemFont(String fontName) {
        String[] fontDirs = {
                "C:\\Windows\\Fonts\\",                       // Windows
                "/usr/share/fonts/",                          // Linux
                "/usr/local/share/fonts/",                    // Linux
                System.getProperty("user.home") + "/.fonts/", // Linux
                "/System/Library/Fonts/",                     // macOS
                "/Library/Fonts/",                            // macOS
                System.getProperty("user.home") + "/Library/Fonts/" // macOS
        };

        for (String dir : fontDirs) {
            File dirFile = new File(dir);
            if (!dirFile.isDirectory())
                continue;

            File[] files = dirFile.listFiles();
            if (files == null)
                continue;

            for (File file : files) {
                if (file.getName().toLowerCase().contains(fontName.toLowerCase()) && file.getName().endsWith(".ttf")) {
                    return file;
                }
            }
        }
        return null;
    }
}
