package com.wizzardo.vulkan.ui.javafx;

import com.sun.javafx.application.PlatformImpl;
import com.sun.webkit.WebPage;
import com.wizzardo.tools.misc.Unchecked;
import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import netscape.javascript.JSObject;

import java.lang.reflect.Field;
import java.util.Arrays;

public class WebViewSample extends Application {
    private Scene scene;
    private static boolean runningServer;

    @Override
    public void start(Stage stage) {
//        UIServer.main(null);
//        System.setProperty("prism.lcdtext", "false");

        stage.setTitle("Web View");
        stage.initStyle(StageStyle.TRANSPARENT);
//        scene = new Scene(new Browser(), 750, 500, Color.web("#666970"));
//        String url = "http://localhost:8080/ui";
        String url;
        if (runningServer)
            url = "http://localhost:8080/static/index.html";
        else
            url = "http://localhost:8080";

//        String url = "http://localhost:8080/windows";
//        url = "http://www.rlamana.com/ventus/code/examples/simple/";
        WebHolder webHolder = new WebHolder(url);
        scene = new Scene(webHolder, 1280, 800, Color.TRANSPARENT);
        stage.setScene(scene);
        stage.show();
        webHolder.browser.setPrefSize(1280, 800);
    }

    public static void main(String[] args) {
        runningServer = args.length > 0 && args[0].equals("env=prod");
//        if (runningServer)
//            UIServer.main(args);
        launch(args);
    }

    public static class WebController {
        public void printId(Object object) {
            System.out.println("printId for " + object);
            if (org.w3c.dom.html.HTMLElement.class.isAssignableFrom(object.getClass())) {
                org.w3c.dom.html.HTMLElement it = (org.w3c.dom.html.HTMLElement) object;
                System.out.println("Id is " + it.getId());
            }
        }
    }

    public static class JavaBridge {
        public void log(String... text) {
            System.out.println("log: " + Arrays.toString(text));
        }

        public void log(String text) {
            System.out.println("log: " + text);
        }

        public void log(Object... text) {
            System.out.println("log: " + Arrays.toString(text));
        }

        public void log(Object text) {
            System.out.println("log: " + text);
        }

        public void onCommand(String command, String data) {
            System.out.println("onCommand: " + command + " data: " + data);
        }
    }

    public static class WebHolder extends Region {

        final public WebView browser = new WebView();
        final public WebEngine webEngine = browser.getEngine();

        public WebHolder() {
            this("http://localhost:8080/");
        }

        public WebHolder(String url) {
            this(url, 800, 600, 1);
        }

        public WebHolder(String url, int width, int height, float scale) {
            System.setProperty("com.sun.webkit.useHTTP2Loader", "false");
            System.setProperty("javafx.web.debug", "true");

            setScale(scale);
            if (scale > 1) {
                setTranslateX(width * (scale - 1) / 2);
                setTranslateY(height * (scale - 1) / 2);
            }
            browser.setFontSmoothingType(FontSmoothingType.GRAY);
            browser.setPrefSize(width, height);
//            Font.loadFont("https://fonts.gstatic.com/s/materialicons/v29/2fcrYFNaTjcS6g4U3t-Y5StnKWgpfO2iSkLzTz-AABg.ttf", 10);
            webEngine.setOnError(event -> event.getException().printStackTrace());
//            webEngine.load("http://localhost:8080/");
            webEngine.load(url);
//            setStyle("-fx-background-color: rgba(255, 255, 255, 0.5);");
//            browser.setStyle("-fx-background-color: rgba(255, 255, 255, 0.5);");

            setStyle("-fx-background-color: rgba(255, 255, 255, 0.0);");
            browser.setStyle("-fx-background-color: rgba(255, 255, 255, 0);");
            getChildren().add(browser);
            System.out.println("loading");

            Worker<Void> loadWorker = webEngine.getLoadWorker();
            loadWorker.stateProperty().addListener((observable, oldState, newState) -> {
                        System.out.println("stage: " + newState);

                        if (newState == State.SUCCEEDED) {

//                            System.out.println("adding listener");
                            JSObject window = (JSObject) webEngine.executeScript("window");
//                            window.setMember("clickController", new WebController());
//
                            JavaBridge bridge = new JavaBridge();
                            window.setMember("console", bridge);
                            window.setMember("javaBridge", bridge);
//                            webEngine.executeScript("document.getElementById('myButton').value = 'ololo';");
                            webEngine.executeScript("console.log('ololo')");
//                            System.out.println("adding clientId");
//                            System.out.println(webEngine.executeScript("window.setClientId && window.setClientId('" + clientId + "');"));
//                            System.out.println("listener added");

                            enableFirebug(webEngine);
//                            makeBackgroundTransparent();
                        }
                    }
            );
            JSObject window = (JSObject) webEngine.executeScript("window");
            JavaBridge bridge = new JavaBridge();
            window.setMember("console", bridge);

            loadWorker.progressProperty().addListener((observable, oldValue, newValue) -> System.out.println("progress: " + newValue));
            loadWorker.exceptionProperty().addListener((observable, oldValue, newValue) -> System.out.println("exception: " + newValue));
            loadWorker.valueProperty().addListener((observable, oldValue, newValue) -> System.out.println("value: " + newValue));
            loadWorker.totalWorkProperty().addListener((observable, oldValue, newValue) -> System.out.println("totalWork: " + newValue));
            loadWorker.runningProperty().addListener((observable, oldValue, newValue) -> System.out.println("running: " + newValue));
            loadWorker.messageProperty().addListener((observable, oldValue, newValue) -> System.out.println("message: " + newValue));
            loadWorker.titleProperty().addListener((observable, oldValue, newValue) -> System.out.println("title: " + newValue));
            loadWorker.workDoneProperty().addListener((observable, oldValue, newValue) -> System.out.println("workDone: " + newValue));
            webEngine.documentProperty().addListener((observable, oldValue, newValue) -> System.out.println("document: " + newValue));
            webEngine.onErrorProperty().addListener((observable, oldValue, newValue) -> System.out.println("error: " + newValue));

            makeBackgroundTransparent();
//            onClick("otherButton");
            System.out.println("end init");
//            Thread thread = new Thread(() -> {
//                Unchecked.run(() -> Thread.sleep(5000));
//
//                PlatformImpl.runLater(() -> {
//                    System.out.println(webEngine);
//
//                    System.out.println("waited");
//                });
//            });
//            thread.setDaemon(true);
//            thread.start();
        }

        public void setScale(double scale) {
            browser.scaleXProperty().set(scale);
            browser.scaleYProperty().set(scale);
        }

        private static void enableFirebug(final WebEngine engine) {
            engine.executeScript("var firebug=document.createElement('script');" +
                    "firebug.setAttribute('src','https://lupatec.eu/getfirebug/firebug-lite-compressed.js');" +
                    "document.body.appendChild(firebug);" +
                    "(function(){if(window.firebug.version){firebug.init();}else{setTimeout(arguments.callee);}})();void(firebug);");
        }

        public void makeBackgroundTransparent() {
            webEngine.documentProperty().addListener((observable, oldValue, newValue) -> {
                try {
                    Field field = webEngine.getClass().getDeclaredField("page");
                    field.setAccessible(true);
                    WebPage page = (WebPage) field.get(webEngine);
                    page.setBackgroundColor(new java.awt.Color(0, 0, 0, 0).getRGB());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        public void onClick(String id, Runnable runnable) {
            webEngine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
                        if (newState == State.SUCCEEDED) {
                            System.out.println("adding listener");

                            JSObject listeners = (JSObject) webEngine.executeScript("window.__listeners");
                            listeners.setMember(id, RunnableWrapper.of(runnable));

                            webEngine.executeScript("" +
                                    "var element = document.getElementById('" + id + "');\n" +
                                    "if(element) {\n" +
                                    "element.onclick = function(){\n" +
                                    "   var listener = __listeners[element.id];\n" +
                                    "   if(listener) listener.run();\n" +
                                    "   else console.log('There is no listener for ' + element.id);\n" +
                                    "}\n" +
                                    "}");


                            System.out.println("listener added for id " + id);
                        }
                    }
            );
        }
    }

    public static class RunnableWrapper implements Runnable {
        final Runnable runnable;

        public static RunnableWrapper of(Runnable runnable) {
            return new RunnableWrapper(runnable);
        }

        public RunnableWrapper(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() {
            runnable.run();
        }
    }
}