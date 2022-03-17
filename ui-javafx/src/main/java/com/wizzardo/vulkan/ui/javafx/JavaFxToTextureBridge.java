package com.wizzardo.vulkan.ui.javafx;

import com.sun.glass.ui.Pixels;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.embed.AbstractEvents;
import com.sun.javafx.embed.EmbeddedStageInterface;
import com.sun.javafx.embed.EmbeddedSceneInterface;
import com.sun.javafx.stage.EmbeddedWindow;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.input.KeyState;
import javafx.application.Platform;
import javafx.scene.Scene;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    protected EmbeddedStageInterface embeddedStage;
    protected EmbeddedSceneInterface embeddedScene;
    protected ByteBuffer tempData;
    protected IntBuffer tempDataIntView;
    protected AtomicReference<TextureHolder> currentImage = new AtomicReference<>();
    protected Set<TextureHolder> freeTextures = Collections.newSetFromMap(new ConcurrentHashMap<>());
    protected Set<TextureHolder> occupiedTextures = Collections.newSetFromMap(new ConcurrentHashMap<>());
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

        return new TextureHolder(textureImage, new ArrayList<>(), textureCounter++);
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
        if (pixelsNativeFormat == Pixels.Format.BYTE_BGRA_PRE) {
            tempData.clear();
            byte alpha = tempData.get((y * textureWidth + x) * 4 + 3);
            return alpha != 0;
        }
        if (pixelsNativeFormat == Pixels.Format.BYTE_ARGB) {
            tempData.clear();
            byte alpha = tempData.get((y * textureWidth + x) * 4);
            return alpha != 0;
        }
        return false;
    }

    public static class TextureHolder {
        final TextureImage textureImage;
        final AtomicInteger counter = new AtomicInteger();
        final List<Long> descriptorSets;
        final int index;

        public TextureHolder(TextureImage textureImage, List<Long> descriptorSets, int index) {
            this.textureImage = textureImage;
            this.descriptorSets = descriptorSets;
            this.index = index;
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
        this(application);
        textureWidth = width;
        textureHeight = height;

        material = new Material();
        material.setVertexShader("shaders/javafx.vert.spv");
        material.setFragmentShader("shaders/javafx.frag.spv");
//        material.setTextureImage(textureImage);
        material.setTextureSampler(application.createTextureSampler(1));
//        material.prepare(application, application.getGuiViewport());

        int imageSize = textureWidth * textureHeight * 4;
        this.tempData = ByteBuffer.allocateDirect(imageSize).order(ByteOrder.nativeOrder());
        this.tempDataIntView = tempData.asIntBuffer();
        setCurrentImage(createTextureHolder());
        freeTextures.add(createTextureHolder());
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
        Iterator<TextureHolder> iterator = freeTextures.iterator();
        if (!iterator.hasNext())
            return null;

        TextureHolder holder = iterator.next();
        iterator.remove();

//        System.out.println("getFreeTexture after freer: " + freeTextures);
        return holder;
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
            if (!embeddedWindow.isShowing()) {
                embeddedWindow.show();
            }
        });
    }

    public EmbeddedSceneInterface getEmbeddedScene() {
        return embeddedScene;
    }

    public void setEmbeddedScene(EmbeddedSceneInterface embeddedScene) {
        this.embeddedScene = embeddedScene;
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

    public void repaint() {
        final EmbeddedSceneInterface sceneInterface = getEmbeddedScene();
        if (sceneInterface == null) return;

        final ByteBuffer tempData = this.tempData;
        tempData.clear();

        final int sceneWidth = getTextureWidth();
        final int sceneHeight = getTextureHeight();

        //todo replace tempData with smaller buffer
        if (!sceneInterface.getPixels(tempDataIntView, sceneWidth, sceneHeight)) {
            return;
        }

//        System.out.println("repaint. " + "free: " + freeTextures + ", occupied: " + occupiedTextures + ", current: " + (currentImage.get()));

        tempData.flip();
        tempData.limit(sceneWidth * sceneHeight * 4);

        TextureHolder holder = getFreeTexture();
        if (holder == null) {
            holder = createTextureHolder();
        }
        copy(tempData, holder);
        setCurrentImage(holder);
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
        TextureLoader.copyBufferToImage(
                application.getDevice(),
                application.getTransferQueue(),
                application.getCommandPool(),
                stagingBuffer.buffer,
                dst.textureImage.textureImage,
                textureWidth,
                textureHeight
        );
    }

    public void onMouseMove(int x, int y, int screenX, int screenY) {
        onMouseMotionEvent(
                x,
                y,
                screenX,
                screenY,
                AbstractEvents.MOUSEEVENT_MOVED,
                AbstractEvents.MOUSEEVENT_NONE_BUTTON,
                false,
                false,
                false
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