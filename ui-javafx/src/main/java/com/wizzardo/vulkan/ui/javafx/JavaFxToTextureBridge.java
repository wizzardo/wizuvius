package com.wizzardo.vulkan.ui.javafx;

import com.sun.glass.ui.Pixels;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.embed.AbstractEvents;
import com.sun.javafx.embed.EmbeddedStageInterface;
import com.sun.javafx.embed.EmbeddedSceneInterface;
import com.sun.javafx.stage.EmbeddedWindow;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.tools.reflection.FieldReflection;
import com.wizzardo.tools.reflection.FieldReflectionFactory;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.input.KeyState;
import com.wizzardo.vulkan.misc.AtomicArrayList;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class JavaFxToTextureBridge {
    protected static final char[] EMPTY_CHARS = new char[0];
    protected static volatile int pixelsNativeFormat;
    protected BufferHolder stagingBuffer;
    protected Material material;
    protected volatile EmbeddedWindow embeddedWindow;
    protected volatile Scene scene;
    protected int textureWidth;
    protected int textureHeight;
    protected float scale;
    protected EmbeddedStageInterface embeddedStage;
    protected EmbeddedSceneInterface embeddedScene;
    protected FieldReflection texBits;
    protected ByteBuffer tempData;
    protected IntBuffer tempDataIntView;
    protected AtomicReference<TextureHolder> currentImage = new AtomicReference<>();
    protected List<TextureHolder> freeTextures = new AtomicArrayList<>(10);
    protected List<TextureHolder> occupiedTextures = new AtomicArrayList<>(10);
    //    protected ConcurrentLinkedQueue<TextureHolder> occupiedTextures = new ConcurrentLinkedQueue<>();
    protected VulkanApplication application;
    protected static volatile int imageFormat;
    protected int textureCounter = 0;

    public static void init() {
        CountDownLatch javaFxInit = new CountDownLatch(1);
        startup(javaFxInit);
        Unchecked.run(javaFxInit::await);
    }

    public static void startup(CountDownLatch javaFxInit) {
        PlatformImpl.startup(() -> {
            pixelsNativeFormat = Pixels.getNativeFormat();
            switch (pixelsNativeFormat) {
                case Pixels.Format.BYTE_ARGB:
                    //todo do reordering in fragment shader
                    throw new IllegalArgumentException("Not supported javaFX pixel format " + pixelsNativeFormat);
                case Pixels.Format.BYTE_BGRA_PRE:
                    imageFormat = VK_FORMAT_B8G8R8A8_SRGB;
                    break;
                default:
                    throw new IllegalArgumentException("Not supported javaFX pixel format " + pixelsNativeFormat);
            }
            javaFxInit.countDown();
        });
    }

    public static void cleanup() {
        Platform.exit();
    }

    protected TextureHolder createTextureHolder() {
        System.out.println("createTextureHolder. " + "free: " + freeTextures.size() + ", occupied: " + occupiedTextures.size() + ", has current: " + (currentImage.get() != null));
        TextureImage textureImage = TextureLoader.createTextureImage(
                application.getPhysicalDevice(),
                application.getDevice(),
                application.getTransferQueue(),
                application.getCommandPool(),
                textureWidth,
                textureHeight,
                imageFormat,
                1
        );

        return new TextureHolder(textureImage, new ArrayList<>(), textureCounter++, this);
    }

    public void cleanupSwapChainObjects(VkDevice device) {
        for (TextureHolder texture : freeTextures) {
            texture.descriptorSets.clear();
        }
        for (TextureHolder texture : occupiedTextures) {
            texture.descriptorSets.clear();
        }
    }

    public boolean isMouseOver(int x, int y) {
        byte alpha = 0;
        if (pixelsNativeFormat == Pixels.Format.BYTE_BGRA_PRE) {
            if (texBits != null) {
                IntBuffer bits = (IntBuffer) texBits.getObject(getEmbeddedScene());
                alpha = (byte) ((bits.get(y * textureWidth + x) >> 24) & 0xFF);
            } else
                alpha = tempData.get((y * textureWidth + x) * 4 + 3);
        } else if (pixelsNativeFormat == Pixels.Format.BYTE_ARGB) {
            if (texBits != null) {
                IntBuffer bits = (IntBuffer) texBits.getObject(getEmbeddedScene());
                alpha = (byte) ((bits.get(y * textureWidth + x)) & 0xFF);
            } else
                alpha = tempData.get((y * textureWidth + x) * 4);
        }
        boolean result = alpha != 0;
//        if (result)
//            System.out.println("hover: " + x + " " + y);
        return result;
    }

    public static class TextureHolder {
        final TextureImage textureImage;
        final AtomicInteger counter = new AtomicInteger();
        final List<Long> descriptorSets;
        final int index;
        final Frame.FrameListener frameListener;

        public TextureHolder(TextureImage textureImage, List<Long> descriptorSets, int index, JavaFxToTextureBridge bridge) {
            this.textureImage = textureImage;
            this.descriptorSets = descriptorSets;
            this.index = index;
            frameListener = () -> bridge.release(this);
        }

        @Override
        public String toString() {
            return "TH{" +
                    "index=" + index +
                    ", counter=" + counter +
                    '}';
        }
    }

    protected JavaFxToTextureBridge(VulkanApplication application) {
        this.application = application;
    }

    public JavaFxToTextureBridge(VulkanApplication application, int width, int height) {
        this(application, width, height, 1f);
    }

    public JavaFxToTextureBridge(VulkanApplication application, int width, int height, float scale) {
        this(application);
        textureWidth = width;
        textureHeight = height;
        this.scale = scale;
        TextureHolder textureHolder = createTextureHolder();
        freeTextures.add(textureHolder);

        material = new Material() {
            {
                vertexLayout = new VertexLayout(
                        VertexLayout.BindingDescription.POSITION,
                        VertexLayout.BindingDescription.TEXTURE_COORDINATES
                );

                setVertexShader("shaders/javafx.vert.spv");
                setFragmentShader("shaders/javafx.frag.spv");
//        material.setTextureImage(textureImage);
                addTextureImage(textureHolder.textureImage);
                setTextureSampler(application.createTextureSampler(1));
//                prepare(application, application.getGuiViewport());
            }
        };

        int imageSize = textureWidth * textureHeight * 4;
        this.tempData = ByteBuffer.allocateDirect(imageSize).order(ByteOrder.nativeOrder());
        this.tempDataIntView = tempData.asIntBuffer();
        this.stagingBuffer = createStagingBuffer(application, imageSize);
    }

    public TextureHolder getCurrentImage() {
        TextureHolder holder = null;
        do {
            if (holder != null) {
                //current image was updated by ui-thread
                if (holder.counter.decrementAndGet() == 0) {
                    freeTextures.add(holder);
                }
//                System.out.println("current image was updated while getCurrentImage, " + holder);
            }

            holder = currentImage.get();
            if (holder == null)
                return null;

            holder.counter.incrementAndGet();
        } while (currentImage.get() != holder);

        if (!occupiedTextures.contains(holder))
            occupiedTextures.add(holder);
//        System.out.println("getCurrentImage after free: " + freeTextures + " occupied: " + occupiedTextures + " current: " + holder);
        return holder;
    }

    public void release(TextureHolder holder) {
        if (holder.counter.decrementAndGet() == 0) {
            if (currentImage.get() != holder)
                freeTextures.add(holder);

            occupiedTextures.remove(holder);
//            System.out.println("release after free: " + freeTextures + " occupied: " + occupiedTextures + " current: " + currentImage);
        }
    }

    protected TextureHolder getFreeTexture() {
//        System.out.println("getFreeTexture before " + freeTextures);
        if (freeTextures.isEmpty())
            return null;

        return freeTextures.remove(freeTextures.size() - 1);
    }

    protected void setCurrentImage(TextureHolder holder) {
        TextureHolder old = currentImage.getAndSet(holder);
        if (old != null && old.counter.get() == 0) {
            freeTextures.add(old);
        }
//        System.out.println("setCurrentImage after free: " + freeTextures + " current: " + currentImage);
    }

    public void setScene(Scene newScene) {
        Platform.runLater(() -> {
            this.scene = newScene;
            if (newScene == null) {
                if (embeddedWindow != null) {
                    embeddedWindow.hide();
                    embeddedWindow = null;
                }
                return;
            }

            if (embeddedWindow == null) {
                embeddedWindow = new EmbeddedWindow(new JavaFxHostInterface(this));
            }

            embeddedWindow.setScene(newScene);
            updateScale();
            if (!embeddedWindow.isShowing()) {
                embeddedWindow.show();
            }
        });
    }

    public void setScale(float scale) {
        this.scale = scale;

        if (scene != null)
            Platform.runLater(this::updateScale);
    }

    protected void updateScale() {
        Parent root = scene.getRoot();
        root.setScaleX(scale);
        root.setScaleY(scale);

        root.setTranslateX(-(textureWidth - textureWidth * scale) / 2f);
        root.setTranslateY(-(textureHeight - textureHeight * scale) / 2f);
    }

    public EmbeddedSceneInterface getEmbeddedScene() {
        return embeddedScene;
    }

    public void setEmbeddedScene(EmbeddedSceneInterface embeddedScene) {
        this.embeddedScene = embeddedScene;
        if (embeddedScene != null) {
            try {
                texBits = new FieldReflectionFactory().create(embeddedScene.getClass(), "texBits", true);
            } catch (NoSuchFieldException ignored) {
            }
        }
    }

    public EmbeddedStageInterface getEmbeddedStage() {
        return embeddedStage;
    }

    public void setEmbeddedStage(EmbeddedStageInterface embeddedStage) {
        this.embeddedStage = embeddedStage;
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public void setTextureWidth(int textureWidth) {
        this.textureWidth = textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }

    public void setTextureHeight(int textureHeight) {
        this.textureHeight = textureHeight;
    }

//    protected long previousPrintFps = System.nanoTime();
//    protected int fpsCounter = 0;

    public void repaint() {
        final EmbeddedSceneInterface sceneInterface = getEmbeddedScene();
        if (sceneInterface == null)
            return;

//        System.out.println("repaint.before " + "free: " + freeTextures + ", occupied: " + occupiedTextures + ", current: " + (currentImage.get()));

        ByteBuffer tempData;
        if (texBits != null) {
            IntBuffer bits = (IntBuffer) texBits.getObject(sceneInterface);
            TextureHolder holder = getFreeTexture();
            if (holder == null) {
                holder = createTextureHolder();
            }
            copy(bits, holder);
            setCurrentImage(holder);
        } else {
            tempData = this.tempData;
            tempData.clear();

            int sceneWidth = getTextureWidth();
            int sceneHeight = getTextureHeight();

            if (!sceneInterface.getPixels(tempDataIntView, sceneWidth, sceneHeight)) {
                return;
            }


            tempData.flip();
            tempData.limit(sceneWidth * sceneHeight * 4);

            TextureHolder holder = getFreeTexture();
            if (holder == null) {
                holder = createTextureHolder();
            }
            copy(tempData, holder);
            setCurrentImage(holder);
        }

//        System.out.println("repaint.after " + "free: " + freeTextures + ", occupied: " + occupiedTextures + ", current: " + (currentImage.get()));
//        fpsCounter++;
//        long time = System.nanoTime();
//        if (time - previousPrintFps >= 1_000_000_000) {
//            System.out.println("fps: " + fpsCounter);
//            fpsCounter = 0;
//            previousPrintFps = time;
//        }
    }

    protected BufferHolder createStagingBuffer(VulkanApplication application, int size) {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pStagingBuffer = stack.mallocLong(1);
            LongBuffer pStagingBufferMemory = stack.mallocLong(1);
            VulkanBuffers.createBuffer(
                    application.getPhysicalDevice(),
                    application.getDevice(),
                    size,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pStagingBuffer,
                    pStagingBufferMemory);


            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(application.getDevice(), pStagingBufferMemory.get(0), 0, size, 0, data);
//            memcpy(data.getByteBuffer(0, (int) imageSize), pixels, imageSize);
//            vkUnmapMemory(device, pStagingBufferMemory.get(0));
            BufferHolder bufferHolder = new BufferHolder(pStagingBuffer.get(0), pStagingBufferMemory.get(0));
            bufferHolder.setMappedBuffer(data.getByteBuffer(0, size));
            return bufferHolder;
        }
    }

    private void copy(ByteBuffer src, TextureHolder dst) {
        ByteBuffer buffer = stagingBuffer.getMappedBuffer();
        src.limit(buffer.capacity());
        buffer.rewind();
        buffer.put(src);
        copyToTexture(dst);
    }

    private void copy(IntBuffer src, TextureHolder dst) {
        ByteBuffer buffer = stagingBuffer.getMappedBuffer();
        MemoryUtil.memCopy(MemoryUtil.memAddress(src, 0), MemoryUtil.memAddress(buffer), src.capacity() * 4L);
        copyToTexture(dst);
    }

    private void copyToTexture(TextureHolder dst) {
        TextureLoader.copyBufferToImage(
                application.getDevice(),
                application.getTransferQueue(),
                application.getCommandPool(),
                stagingBuffer.buffer,
                dst.textureImage.getTextureImage(),
                textureWidth,
                textureHeight
        );
    }

    public void onMouseMove(int x, int y, int screenX, int screenY) {
        KeyState keyState = application.getInputsManager().getKeyState();
        int type = AbstractEvents.MOUSEEVENT_MOVED;
        int button = AbstractEvents.MOUSEEVENT_NONE_BUTTON;
        boolean primaryBtnDown = keyState.isMouseButtonPressed(0);
        boolean middleBtnDown = keyState.isMouseButtonPressed(2);
        boolean secondaryBtnDown = keyState.isMouseButtonPressed(1);
        if (primaryBtnDown) {
            type = AbstractEvents.MOUSEEVENT_DRAGGED;
            button = AbstractEvents.MOUSEEVENT_PRIMARY_BUTTON;
        } else if (secondaryBtnDown) {
            type = AbstractEvents.MOUSEEVENT_DRAGGED;
            button = AbstractEvents.MOUSEEVENT_SECONDARY_BUTTON;
        } else if (middleBtnDown) {
            type = AbstractEvents.MOUSEEVENT_DRAGGED;
            button = AbstractEvents.MOUSEEVENT_MIDDLE_BUTTON;
        }
        onMouseMotionEvent(
                x,
                y,
                screenX,
                screenY,
                type,
                button,
                primaryBtnDown,
                middleBtnDown,
                secondaryBtnDown
        );
    }

    public void onMouseMotionEvent(int x, int y, int screenX, int screenY, int type, int button, boolean primaryBtnDown, boolean middleBtnDown, boolean secondaryBtnDown) {
        KeyState keyState = application.getInputsManager().getKeyState();

        boolean shift = keyState.isShiftPressed();
        boolean ctrl = keyState.isCtrlPressed();
        boolean alt = keyState.isAltPressed();
        boolean meta = keyState.isMetaPressed();

        getEmbeddedScene().mouseEvent(type, button, primaryBtnDown, middleBtnDown, secondaryBtnDown, x, y,
                screenX, screenY, shift, ctrl, alt, meta, false);
    }

    public void onMouseButtonEvent(int x, int y, int screenX, int screenY, int type, int button) {
        KeyState keyState = application.getInputsManager().getKeyState();

        boolean primaryBtnDown = keyState.isMouseButtonPressed(0);
        boolean secondaryBtnDown = keyState.isMouseButtonPressed(1);
        boolean middleBtnDown = keyState.isMouseButtonPressed(2);

        boolean shift = keyState.isShiftPressed();
        boolean ctrl = keyState.isCtrlPressed();
        boolean alt = keyState.isAltPressed();
        boolean meta = keyState.isMetaPressed();
        boolean popupTrigger = button == AbstractEvents.MOUSEEVENT_SECONDARY_BUTTON;

        getEmbeddedScene().mouseEvent(type, button, primaryBtnDown, middleBtnDown, secondaryBtnDown, x, y,
                screenX, screenY, shift, ctrl, alt, meta, popupTrigger);
    }

    public void onMouseScrollEvent(int x, int y, int screenX, int screenY, double scrollX, double scrollY, int type) {
        KeyState keyState = application.getInputsManager().getKeyState();

        boolean shift = keyState.isShiftPressed();
        boolean ctrl = keyState.isCtrlPressed();
        boolean alt = keyState.isAltPressed();
        boolean meta = keyState.isMetaPressed();

        getEmbeddedScene().scrollEvent(type, scrollX, scrollY, scrollX, scrollY, 10, 10, x, y,
                screenX, screenY, shift, ctrl, alt, meta, false);
    }

    public void onKeyTyped(int keycode, char[] chars) {
        getEmbeddedScene().keyEvent(AbstractEvents.KEYEVENT_TYPED, keycode, chars, getKeyModifiers());
    }

    public void onKey(int key, boolean pressed, boolean repeat) {
        int keyCode = application.getInputsManager().getKeyState().nativeToAwt(key);
        if (repeat) {
            getEmbeddedScene().keyEvent(AbstractEvents.KEYEVENT_RELEASED, keyCode, EMPTY_CHARS, getKeyModifiers());
            getEmbeddedScene().keyEvent(AbstractEvents.KEYEVENT_PRESSED, keyCode, EMPTY_CHARS, getKeyModifiers());
        } else if (pressed)
            getEmbeddedScene().keyEvent(AbstractEvents.KEYEVENT_PRESSED, keyCode, EMPTY_CHARS, getKeyModifiers());
        else
            getEmbeddedScene().keyEvent(AbstractEvents.KEYEVENT_RELEASED, keyCode, EMPTY_CHARS, getKeyModifiers());
    }

    private int getKeyModifiers() {
        int embedModifiers = 0;
        KeyState keyState = application.getInputsManager().getKeyState();
        if (keyState.isShiftPressed())
            embedModifiers |= AbstractEvents.MODIFIER_SHIFT;
        if (keyState.isCtrlPressed())
            embedModifiers |= AbstractEvents.MODIFIER_CONTROL;
        if (keyState.isAltPressed())
            embedModifiers |= AbstractEvents.MODIFIER_ALT;
        if (keyState.isMetaPressed())
            embedModifiers |= AbstractEvents.MODIFIER_META;
        return embedModifiers;
    }
}